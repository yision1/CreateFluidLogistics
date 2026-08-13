package com.yision.fluidlogistics.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.equipment.clipboard.ClipboardScreen;
import com.yision.fluidlogistics.content.logistics.fluidPackage.FluidPackageResourceType;
import com.yision.fluidlogistics.render.GuiFluidBlockRenderer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@OnlyIn(Dist.CLIENT)
@Mixin(ClipboardScreen.class)
public class ClipboardScreenMixin {

    @WrapOperation(
        method = "renderWindow",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;renderItem("
                + "Lnet/minecraft/world/item/ItemStack;II)V",
            remap = true
        ),
        remap = false
    )
    private void fluidlogistics$renderFluidBlock(
        GuiGraphics graphics, ItemStack stack, int x, int y, Operation<Void> original
    ) {
        FluidStack fluid = FluidPackageResourceType.getFluid(stack);
        if (fluid.isEmpty()) {
            original.call(graphics, stack, x, y);
            return;
        }
        GuiFluidBlockRenderer.render(graphics, fluid, x, y, 1);
    }
}
