package com.yision.fluidlogistics.content.processing.snowGolemFreezer;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.yision.fluidlogistics.registry.AllPartialModels;
import com.yision.fluidlogistics.registry.AllSpriteShifts;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class SnowGolemFreezerRenderer extends SafeBlockEntityRenderer<BlazeBurnerBlockEntity> {

	public SnowGolemFreezerRenderer(BlockEntityRendererProvider.Context context) {
	}

	public static void tickHeadAnimation(BlazeBurnerBlockEntity burner, LerpedFloat headAngle) {
		boolean active = burner.getHeatLevelFromBlock().isAtLeast(HeatLevel.FADING) && burner.isValidBlockAbove();

		if (!active) {
			float target = 0;
			LocalPlayer player = Minecraft.getInstance().player;
			if (player != null && !player.isInvisible()) {
				double x = burner.isVirtual() ? -4 : player.getX();
				double z = burner.isVirtual() ? -10 : player.getZ();
				double dx = x - (burner.getBlockPos().getX() + .5);
				double dz = z - (burner.getBlockPos().getZ() + .5);
				target = AngleHelper.deg(-Mth.atan2(dz, dx)) - 90;
			}
			target = headAngle.getValue() + AngleHelper.getShortestAngleDiff(headAngle.getValue(), target);
			headAngle.chase(target, .25f, Chaser.exp(5));
			headAngle.tickChaser();
		} else {
			Direction facing = burner.getBlockState().getOptionalValue(BlazeBurnerBlock.FACING).orElse(Direction.SOUTH);
			headAngle.chase((AngleHelper.horizontalAngle(facing) + 180) % 360, .125f, Chaser.EXP);
			headAngle.tickChaser();
		}

		burner.headAnimation.chase(active ? 1 : 0, .25f, Chaser.exp(.25f));
		burner.headAnimation.tickChaser();
	}

	@Override
	protected void renderSafe(BlazeBurnerBlockEntity cooler, float partialTicks, PoseStack poseStack,
		MultiBufferSource bufferSource, int light, int overlay) {
		HeatLevel heatLevel = cooler.getHeatLevelForRender();
		if (heatLevel == HeatLevel.NONE)
			return;

		BlockState blockState = cooler.getBlockState();
		float animation = cooler.headAnimation.getValue(partialTicks) * .175f;
		float horizontalAngle = AngleHelper.rad(((SnowGolemFreezerBlockEntity) cooler).getHeadAngle(partialTicks));
		boolean canDrawFlame = heatLevel.isAtLeast(HeatLevel.FADING);
		PartialModel drawHat = getHat(cooler);

		renderShared(poseStack, bufferSource, cooler.getLevel(), blockState, heatLevel, animation, horizontalAngle,
			canDrawFlame, cooler.goggles, drawHat, cooler.hashCode(),
			((SnowGolemFreezerBlockEntity) cooler).hasPumpkinHead());
	}

	public static void renderInContraption(MovementContext context, ContraptionMatrices matrices,
		MultiBufferSource bufferSource, LerpedFloat headAngle, boolean conductor) {
		if (BlazeBurnerBlock.getHeatLevelOf(context.state) == HeatLevel.NONE)
			return;

		float horizontalAngle = AngleHelper.rad(
			headAngle.getValue(AnimationTickHolder.getPartialTicks(context.world)));
		boolean drawGoggles = context.blockEntityData.contains("Goggles");
		boolean drawHat = conductor || context.blockEntityData.contains("TrainHat");

		renderShared(matrices.getViewProjection(), matrices.getModel(), bufferSource, context.world, context.state,
			HeatLevel.FADING, 0, horizontalAngle, false, drawGoggles,
			drawHat ? com.simibubi.create.AllPartialModels.TRAIN_HAT : null, context.hashCode(),
			context.blockEntityData.getBoolean(SnowGolemFreezerBlockEntity.PUMPKIN_HEAD_TAG));
	}

	public static void renderShared(PoseStack poseStack, MultiBufferSource bufferSource, Level level,
		BlockState blockState, HeatLevel heatLevel, float animation, float horizontalAngle,
		boolean canDrawFlame, boolean drawGoggles, @Nullable PartialModel drawHat, int hashCode) {
		renderShared(poseStack, bufferSource, level, blockState, heatLevel, animation, horizontalAngle,
			canDrawFlame, drawGoggles, drawHat, hashCode, false);
	}

	public static void renderShared(PoseStack poseStack, MultiBufferSource bufferSource, Level level,
		BlockState blockState, HeatLevel heatLevel, float animation, float horizontalAngle,
		boolean canDrawFlame, boolean drawGoggles, @Nullable PartialModel drawHat, int hashCode, boolean pumpkinHead) {
		renderShared(poseStack, null, bufferSource, level, blockState, heatLevel, animation, horizontalAngle,
			canDrawFlame, drawGoggles, drawHat, hashCode, pumpkinHead);
	}

	private static void renderShared(PoseStack poseStack, @Nullable PoseStack modelTransform,
		MultiBufferSource bufferSource, Level level, BlockState blockState, HeatLevel heatLevel, float animation,
		float horizontalAngle, boolean canDrawFlame, boolean drawGoggles, @Nullable PartialModel drawHat,
		int hashCode, boolean pumpkinHead) {
		boolean blockAbove = animation > .125f;
		float time = AnimationTickHolder.getRenderTime(level);
		float renderTick = time + hashCode % 13 * 16f;
		float offsetMult = heatLevel.isAtLeast(HeatLevel.FADING) ? 64 : 16;
		float offset = Mth.sin((float) ((renderTick / 16f) % (2 * Math.PI))) / offsetMult;
		float headY = offset - animation * .75f;

		poseStack.pushPose();
		boolean smallHead = !pumpkinHead && !heatLevel.isAtLeast(HeatLevel.FADING);
		PartialModel headModel = pumpkinHead ? AllPartialModels.SNOW_GOLEM_FREEZER_PUMPKIN
			: smallHead ? AllPartialModels.SNOW_GOLEM_FREEZER_INERT : AllPartialModels.SNOW_GOLEM_FREEZER_IDLE;
		SuperByteBuffer headBuffer = CachedBuffers.partial(headModel, blockState);
		transform(headBuffer, modelTransform);
		headBuffer.translate(0, headY, 0);
		draw(headBuffer, horizontalAngle, poseStack, bufferSource.getBuffer(RenderType.solid()));

		if (drawGoggles) {
			PartialModel goggles = smallHead
				? com.simibubi.create.AllPartialModels.BLAZE_GOGGLES_SMALL
				: com.simibubi.create.AllPartialModels.BLAZE_GOGGLES;
			SuperByteBuffer gogglesBuffer = CachedBuffers.partial(goggles, blockState);
			transform(gogglesBuffer, modelTransform);
			gogglesBuffer.translate(0, headY + 8 / 16f, 0);
			draw(gogglesBuffer, horizontalAngle, poseStack, bufferSource.getBuffer(RenderType.solid()));
		}

		if (drawHat != null) {
			SuperByteBuffer hatBuffer = CachedBuffers.partial(drawHat, blockState);
			transform(hatBuffer, modelTransform);
			hatBuffer.translate(0, headY, 0);
			if (smallHead) {
				hatBuffer.translateY(.5f).center().scale(.75f).uncenter();
			} else {
				hatBuffer.translateY(.75f);
			}
			hatBuffer.rotateCentered(horizontalAngle + Mth.PI, Direction.UP)
				.translate(.5f, 0, .5f)
				.light(LightTexture.FULL_BRIGHT)
				.renderInto(poseStack, bufferSource.getBuffer(RenderType.cutoutMipped()));
		}

		if (canDrawFlame && blockAbove) {
			renderColdFlame(blockState, horizontalAngle, time, heatLevel, poseStack, modelTransform, bufferSource);
		}
		poseStack.popPose();
	}

	@Nullable
	private static PartialModel getHat(BlazeBurnerBlockEntity cooler) {
		if (cooler.stockKeeper)
			return com.simibubi.create.AllPartialModels.LOGISTICS_HAT;
		return cooler.hat ? com.simibubi.create.AllPartialModels.TRAIN_HAT : null;
	}

	private static void renderColdFlame(BlockState blockState, float horizontalAngle, float time,
		HeatLevel heatLevel, PoseStack poseStack, @Nullable PoseStack modelTransform,
		MultiBufferSource bufferSource) {
		SuperByteBuffer flameBuffer = getFlameBuffer(blockState, time,
			1 / 32f + 1 / 64f * heatLevel.ordinal());
		transform(flameBuffer, modelTransform);
		draw(flameBuffer, horizontalAngle, poseStack, bufferSource.getBuffer(RenderType.cutoutMipped()));
	}

	public static SuperByteBuffer getFlameBuffer(BlockState blockState, float time,
		float speed) {
		SpriteShiftEntry spriteShift = AllSpriteShifts.SNOW_GOLEM_FREEZER_FLAME;
		float spriteWidth = spriteShift.getTarget().getU1() - spriteShift.getTarget().getU0();
		float spriteHeight = spriteShift.getTarget().getV1() - spriteShift.getTarget().getV0();
		double vScroll = speed * time;
		vScroll = (vScroll - Math.floor(vScroll)) * spriteHeight / 2;
		double uScroll = speed * time / 2;
		uScroll = (uScroll - Math.floor(uScroll)) * spriteWidth / 2;

		return CachedBuffers.partial(AllPartialModels.SNOW_GOLEM_FREEZER_FLAME, blockState)
			.shiftUVScrolling(spriteShift, (float) uScroll, (float) vScroll);
	}

	private static void transform(SuperByteBuffer buffer, @Nullable PoseStack modelTransform) {
		if (modelTransform != null)
			buffer.transform(modelTransform);
	}

	private static void draw(SuperByteBuffer buffer, float horizontalAngle, PoseStack poseStack,
		VertexConsumer consumer) {
		buffer.rotateCentered(horizontalAngle, Direction.UP)
			.light(LightTexture.FULL_BRIGHT)
			.renderInto(poseStack, consumer);
	}
}
