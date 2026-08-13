package com.yision.fluidlogistics.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.schematics.client.SchematicHandler;
import com.simibubi.create.content.schematics.client.SchematicRenderer;
import com.simibubi.create.content.schematics.client.SchematicTransformation;
import com.simibubi.create.content.schematics.SchematicInstances;
import com.yision.fluidlogistics.content.schematics.client.FluidSchematicRenderer;
import com.yision.fluidlogistics.content.schematics.client.FluidSchematicGuiGraphics;
import com.yision.fluidlogistics.content.schematics.network.FluidSchematicPlacePacket;
import com.yision.fluidlogistics.content.schematics.network.FluidSchematicSyncPacket;
import com.yision.fluidlogistics.network.FluidLogisticsPackets;
import com.yision.fluidlogistics.registry.AllBlocks;
import com.yision.fluidlogistics.registry.AllItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SchematicHandler.class, remap = false)
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

    @ModifyExpressionValue(
        method = "findBlueprintInHand",
        at = @At(
            value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/ItemEntry;isIn(Lnet/minecraft/world/item/ItemStack;)Z",
            remap = false
        )
    )
    private boolean fluidlogistics$acceptFluidSchematic(boolean original, Player player) {
        return original || AllItems.FLUID_SCHEMATIC.isIn(player.getMainHandItem());
    }

    @WrapOperation(
        method = "setupRenderer",
        at = @At(
            value = "NEW",
            target = "Lcom/simibubi/create/content/schematics/client/SchematicRenderer;"
        )
    )
    private SchematicRenderer fluidlogistics$createRenderer(
            SchematicLevel world, Operation<SchematicRenderer> original) {
        return AllItems.FLUID_SCHEMATIC.isIn(activeSchematicItem)
            ? new FluidSchematicRenderer(world)
            : original.call(world);
    }

    @ModifyExpressionValue(
        method = "onMouseInput",
        at = @At(
            value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z",
            ordinal = 0,
            remap = false
        )
    )
    private boolean fluidlogistics$allowOpeningCopperSchematicannon(
            boolean original, @Local(name = "clickedBlock") BlockState state) {
        return original || AllBlocks.COPPER_SCHEMATICANNON.has(state);
    }

    @ModifyVariable(
        method = "render(Lnet/minecraftforge/client/gui/overlay/ForgeGui;Lnet/minecraft/client/gui/GuiGraphics;FII)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private GuiGraphics fluidlogistics$useFluidSchematicGui(GuiGraphics graphics) {
        return activeSchematicItem != null && AllItems.FLUID_SCHEMATIC.isIn(activeSchematicItem)
            ? new FluidSchematicGuiGraphics(graphics)
            : graphics;
    }

    @Inject(method = "sync", at = @At("HEAD"), cancellable = true)
    private void fluidlogistics$syncFluidSchematic(CallbackInfo ci) {
        if (activeSchematicItem == null || !AllItems.FLUID_SCHEMATIC.isIn(activeSchematicItem)) {
            return;
        }
        FluidLogisticsPackets.getChannel().sendToServer(new FluidSchematicSyncPacket(
            activeHotbarSlot, transformation.toSettings(), transformation.getAnchor(), deployed));
        ci.cancel();
    }

    @Inject(method = "printInstantly", at = @At("HEAD"), cancellable = true)
    private void fluidlogistics$placeFluidSchematicInstantly(CallbackInfo ci) {
        if (!AllItems.FLUID_SCHEMATIC.isIn(activeSchematicItem)) {
            return;
        }
        FluidLogisticsPackets.getChannel()
            .sendToServer(new FluidSchematicPlacePacket(activeSchematicItem.copy()));
        CompoundTag tag = activeSchematicItem.getOrCreateTag();
        tag.putBoolean("Deployed", false);
        SchematicInstances.clearHash(activeSchematicItem);
        active = false;
        ((SchematicHandler) (Object) this).markDirty();
        ci.cancel();
    }
}
