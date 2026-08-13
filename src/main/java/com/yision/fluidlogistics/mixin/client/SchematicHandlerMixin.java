package com.yision.fluidlogistics.mixin.client;

import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.schematics.client.SchematicHandler;
import com.simibubi.create.content.schematics.client.SchematicRenderer;
import com.simibubi.create.content.schematics.client.SchematicTransformation;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.schematics.SchematicInstances;
import com.yision.fluidlogistics.content.schematics.client.FluidSchematicRenderer;
import com.yision.fluidlogistics.content.schematics.network.FluidSchematicPlacePacket;
import com.yision.fluidlogistics.content.schematics.network.FluidSchematicSyncPacket;
import com.yision.fluidlogistics.registry.AllBlocks;
import com.yision.fluidlogistics.registry.AllItems;

import net.minecraft.client.Minecraft;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SchematicHandler.class)
public class SchematicHandlerMixin {

    @Shadow
    private boolean active;

    @Shadow
    private ItemStack activeSchematicItem;

    @Shadow
    private SchematicTransformation transformation;

    @Shadow
    private boolean deployed;

    @Shadow
    private int activeHotbarSlot;

    @Unique
    private boolean fluidlogistics$lastFluidSchematic;

    @Inject(method = "tick", at = @At("HEAD"))
    private void fluidlogistics$reinitializeWhenSchematicTypeChanges(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        boolean fluidSchematic = AllItems.FLUID_SCHEMATIC.isIn(minecraft.player.getMainHandItem());
        if (fluidSchematic != fluidlogistics$lastFluidSchematic) {
            active = false;
        }
        fluidlogistics$lastFluidSchematic = fluidSchematic;
    }

    @WrapOperation(
        method = "findBlueprintInHand",
        at = @At(
            value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/ItemEntry;isIn(Lnet/minecraft/world/item/ItemStack;)Z",
            remap = false
        )
    )
    private boolean fluidlogistics$acceptFluidSchematic(
        ItemEntry<?> entry, ItemStack stack, Operation<Boolean> original
    ) {
        return original.call(entry, stack) || AllItems.FLUID_SCHEMATIC.isIn(stack);
    }

    @WrapOperation(
        method = "setupRenderer",
        at = @At(
            value = "NEW",
            target = "Lcom/simibubi/create/content/schematics/client/SchematicRenderer;"
        )
    )
    private SchematicRenderer fluidlogistics$createRenderer(
        SchematicLevel world, Operation<SchematicRenderer> original
    ) {
        return AllItems.FLUID_SCHEMATIC.isIn(activeSchematicItem)
            ? new FluidSchematicRenderer(world)
            : original.call(world);
    }

    @WrapOperation(
        method = "onMouseInput",
        at = @At(
            value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
            ordinal = 0,
            remap = false
        )
    )
    private boolean fluidlogistics$allowOpeningCopperSchematicannon(
        BlockEntry<?> entry, BlockState state, Operation<Boolean> original
    ) {
        return original.call(entry, state) || AllBlocks.COPPER_SCHEMATICANNON.has(state);
    }

    @Inject(method = "sync", at = @At("HEAD"), cancellable = true)
    private void fluidlogistics$syncFluidSchematic(CallbackInfo ci) {
        if (activeSchematicItem == null || !AllItems.FLUID_SCHEMATIC.isIn(activeSchematicItem)) {
            return;
        }
        CatnipServices.NETWORK.sendToServer(new FluidSchematicSyncPacket(
            activeHotbarSlot, transformation.toSettings(), transformation.getAnchor(), deployed));
        ci.cancel();
    }

    @Inject(method = "printInstantly", at = @At("HEAD"), cancellable = true)
    private void fluidlogistics$placeFluidSchematicInstantly(CallbackInfo ci) {
        if (!AllItems.FLUID_SCHEMATIC.isIn(activeSchematicItem)) {
            return;
        }
        CatnipServices.NETWORK.sendToServer(new FluidSchematicPlacePacket(activeSchematicItem.copy()));
        activeSchematicItem.set(AllDataComponents.SCHEMATIC_DEPLOYED, false);
        SchematicInstances.clearHash(activeSchematicItem);
        active = false;
        ((SchematicHandler) (Object) this).markDirty();
        ci.cancel();
    }
}
