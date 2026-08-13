package com.yision.fluidlogistics.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.fluid.FluidHelper;

import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.platform.ForgeCatnipServices;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;

@OnlyIn(Dist.CLIENT)
public final class GuiFluidBlockRenderer {
    private static final float BLOCK_GUI_SCALE = 0.625f;

    private GuiFluidBlockRenderer() {
        throw new AssertionError("This class should not be instantiated");
    }

    public static void render(GuiGraphics graphics, FluidStack fluid, int x, int y, float scale) {
        if (fluid.isEmpty() || fluid.getFluid() == Fluids.EMPTY) {
            return;
        }

        FluidStack renderFluid = fluid.getAmount() == 0 ? FluidHelper.copyStackWithAmount(fluid, 1) : fluid;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 100);
        pose.scale(scale, scale, scale);
        UIRenderHelper.flipForGuiRender(pose);

        pose.translate(0, 0, 100);
        pose.translate(8, -8, 0);
        pose.scale(16, 16, 16);
        pose.mulPose(Axis.XP.rotationDegrees(30));
        pose.mulPose(Axis.YP.rotationDegrees(225));
        pose.scale(BLOCK_GUI_SCALE, BLOCK_GUI_SCALE, BLOCK_GUI_SCALE);
        pose.translate(-0.5f, -0.5f, -0.5f);
        ForgeCatnipServices.FLUID_RENDERER.renderFluidBox(renderFluid,
            0, 0, 0, 1, 1, 1,
            graphics.bufferSource(), pose, LightTexture.FULL_BRIGHT, false, true);
        graphics.flush();
        pose.popPose();
    }
}
