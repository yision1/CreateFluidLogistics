package com.yision.fluidlogistics.content.logistics.copperBasket;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class CopperBasketRenderer extends SmartBlockEntityRenderer<CopperBasketBlockEntity> {
	private final RandomSource random = RandomSource.create();

	public CopperBasketRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(CopperBasketBlockEntity basket, float partialTicks, PoseStack pose,
		MultiBufferSource buffer, int light, int overlay) {
		super.renderSafe(basket, partialTicks, pose, buffer, light, overlay);
		pose.pushPose();
		pose.translate(.5, .5, .5);
		pose.mulPose(CopperBasketBlock.rotation(basket.getBlockState().getValue(CopperBasketBlock.FACING)));
		pose.translate(-.5, -.5, -.5);
		var fluids = basket.getFluids();
		int total = fluids.getFluidAmount();
		float surface = 0;
		if (total > 0) {
			float fill = Mth.clamp(basket.getFluidLevel(partialTicks), 0, 1);
			surface = (2 + 8.5f * (1 - (1 - fill) * (1 - fill))) / 16f;
			float left = 2 / 16f;
			for (int slot = 0; slot < fluids.getTanks(); slot++) {
				var fluid = fluids.getFluidInTank(slot);
				if (fluid.isEmpty())
					continue;
				float right = left + 12 / 16f * fluid.getAmount() / total;
				NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluid, left, 2 / 16f, 2 / 16f,
					right, surface, 14 / 16f, buffer, pose, light, false, false);
				left = right;
			}
		}
		List<ItemStack> displayedStacks = basket.getDisplayedItems();
		int itemCount = displayedStacks.size();
		if (itemCount == 0) {
			pose.popPose();
			return;
		}
		float itemHeight = Mth.clamp(surface - .3f, .125f, .6f);
		float angleStep = 360f / itemCount;
		Vec3 offset = new Vec3(itemCount == 1 ? 0 : .125, itemHeight, 0);
		random.setSeed(basket.getBlockPos().hashCode());
		pose.translate(.5, .2, .5);
		for (ItemStack stack : displayedStacks) {
			float angle = angleStep * itemCount;
			pose.pushPose();
			if (surface > 0) {
				float bob = (Mth.sin(AnimationTickHolder.getRenderTime(basket.getLevel()) / 12f + angle) + 1.5f) / 32f;
				pose.translate(0, bob, 0);
			}
			Vec3 position = VecHelper.rotate(offset, angle, Direction.Axis.Y);
			pose.translate(position.x, position.y, position.z);
			pose.mulPose(Axis.YP.rotationDegrees(angle + 35));
			pose.mulPose(Axis.XP.rotationDegrees(65));
			for (int copy = 0; copy <= stack.getCount() / 8; copy++) {
				pose.pushPose();
				Vec3 jitter = VecHelper.offsetRandomly(Vec3.ZERO, random, 1 / 16f);
				pose.translate(jitter.x, jitter.y, jitter.z);
				Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.GROUND, light,
					overlay, pose, buffer, basket.getLevel(), 0);
				pose.popPose();
			}
			pose.popPose();
			itemCount--;
		}
		pose.popPose();
	}

	@Override
	public int getViewDistance() {
		return 16;
	}
}
