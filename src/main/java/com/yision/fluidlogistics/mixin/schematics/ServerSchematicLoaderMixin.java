package com.yision.fluidlogistics.mixin.schematics;

import java.util.Map;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.schematics.ServerSchematicLoader;
import com.simibubi.create.content.schematics.ServerSchematicLoader.SchematicUploadEntry;
import com.simibubi.create.content.schematics.table.SchematicTableBlockEntity;
import com.yision.fluidlogistics.content.schematics.FluidSchematicItem;
import com.yision.fluidlogistics.content.schematics.FluidSchematicUploadEntry;
import com.yision.fluidlogistics.registry.AllItems;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerSchematicLoader.class)
public abstract class ServerSchematicLoaderMixin {

    @Shadow
    @Final
    private Map<String, SchematicUploadEntry> activeUploads;

    @Shadow
    public abstract SchematicTableBlockEntity getTable(Level world, BlockPos pos);

    @Unique
    private boolean fluidlogistics$finishingFluidSchematic;

    @Inject(
        method = "handleNewUpload",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/schematics/table/SchematicTableBlockEntity;startUpload(Ljava/lang/String;)V"
        )
    )
    private void fluidlogistics$captureFluidSchematic(
        ServerPlayer player, String schematic, long size, BlockPos pos, CallbackInfo ci
    ) {
        SchematicTableBlockEntity table = getTable(player.getCommandSenderWorld(), pos);
        SchematicUploadEntry entry = activeUploads.get(player.getGameProfile().getName() + "/" + schematic);
        if (table == null || entry == null) {
            return;
        }
        boolean fluidSchematic = AllItems.EMPTY_FLUID_SCHEMATIC.isIn(table.inventory.getStackInSlot(0))
            || AllItems.FLUID_SCHEMATIC.isIn(table.inventory.getStackInSlot(0));
        if (fluidSchematic) {
            activeUploads.put(
                player.getGameProfile().getName() + "/" + schematic,
                new FluidSchematicUploadEntry(entry));
        }
    }

    @Inject(method = "handleFinishedUpload", at = @At("HEAD"))
    private void fluidlogistics$prepareFluidSchematicOutput(
        ServerPlayer player, String schematic, CallbackInfo ci
    ) {
        SchematicUploadEntry entry = activeUploads.get(player.getGameProfile().getName() + "/" + schematic);
        fluidlogistics$finishingFluidSchematic = entry instanceof FluidSchematicUploadEntry;
    }

    @WrapOperation(
        method = "handleFinishedUpload",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/schematics/SchematicItem;create(Lnet/minecraft/world/level/Level;Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private ItemStack fluidlogistics$createUploadedSchematic(
        Level level, String schematic, String owner, Operation<ItemStack> original
    ) {
        return fluidlogistics$finishingFluidSchematic
            ? FluidSchematicItem.create(level, schematic, owner)
            : original.call(level, schematic, owner);
    }

    @Inject(method = "handleFinishedUpload", at = @At("RETURN"))
    private void fluidlogistics$clearFluidSchematicOutput(
        ServerPlayer player, String schematic, CallbackInfo ci
    ) {
        fluidlogistics$finishingFluidSchematic = false;
    }
}
