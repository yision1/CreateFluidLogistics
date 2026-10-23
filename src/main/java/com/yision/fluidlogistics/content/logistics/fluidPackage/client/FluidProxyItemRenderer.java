package com.yision.fluidlogistics.content.logistics.fluidPackage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yision.fluidlogistics.content.logistics.fluidPackage.FluidProxyRegistry;
import com.yision.fluidlogistics.foundation.render.FluidSlotRenderer;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

final class FluidProxyItemRenderer extends BlockEntityWithoutLevelRenderer {
	FluidProxyItemRenderer() {
		super(null, null);
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
		MultiBufferSource buffer, int light, int overlay) {
		poseStack.pushPose();
		poseStack.translate(0.5f, 0.5f, 0.5f);
		FluidSlotRenderer.renderFluidItemIcon(FluidProxyRegistry.getFluid(stack), poseStack, buffer, light);
		poseStack.popPose();
	}
}
