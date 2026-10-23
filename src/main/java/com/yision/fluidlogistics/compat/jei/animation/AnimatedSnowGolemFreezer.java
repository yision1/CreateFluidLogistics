package com.yision.fluidlogistics.compat.jei.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.yision.fluidlogistics.content.processing.snowGolemFreezer.SnowGolemFreezerRenderer;
import com.yision.fluidlogistics.registry.AllBlocks;
import com.yision.fluidlogistics.registry.AllPartialModels;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Blocks;

public class AnimatedSnowGolemFreezer extends AnimatedKinetics {

	@Override
	public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
		PoseStack poseStack = graphics.pose();
		poseStack.pushPose();
		poseStack.translate(xOffset, yOffset, 200);
		poseStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
		poseStack.mulPose(Axis.YP.rotationDegrees(22.5f));

		int scale = 23;

		blockElement(AllBlocks.SNOW_GOLEM_FREEZER.getDefaultState()).atLocal(0, 1.65, 0)
			.scale(scale)
			.render(graphics);
		blockElement(AllPartialModels.SNOW_GOLEM_FREEZER_IDLE)
			.atLocal(1, 1.8, 1)
			.rotate(0, 180, 0)
			.scale(scale)
			.render(graphics);

		poseStack.scale(scale, -scale, scale);
		poseStack.translate(0, -1.8, 0);
		renderColdFlame(graphics, poseStack);
		poseStack.popPose();
	}

	private static void renderColdFlame(GuiGraphics graphics, PoseStack poseStack) {
		float time = AnimationTickHolder.getRenderTime(Minecraft.getInstance().level);
		SnowGolemFreezerRenderer.getFlameBuffer(Blocks.AIR.defaultBlockState(), time,
			1 / 32f + 1 / 64f * 3)
			.light(LightTexture.FULL_BRIGHT)
			.renderInto(poseStack, graphics.bufferSource().getBuffer(RenderType.cutoutMipped()));
	}
}
