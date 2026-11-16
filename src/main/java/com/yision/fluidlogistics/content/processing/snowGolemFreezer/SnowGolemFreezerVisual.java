package com.yision.fluidlogistics.content.processing.snowGolemFreezer;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.burner.ScrollInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import com.yision.fluidlogistics.registry.AllPartialModels;
import com.yision.fluidlogistics.registry.AllSpriteShifts;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

public class SnowGolemFreezerVisual extends AbstractBlockEntityVisual<SnowGolemFreezerBlockEntity>
	implements SimpleDynamicVisual {

	@Nullable
	private TransformedInstance head;
	@Nullable
	private TransformedInstance goggles;
	@Nullable
	private TransformedInstance hat;
	@Nullable
	private ScrollInstance flame;
	@Nullable
	private PartialModel headModel;
	@Nullable
	private PartialModel gogglesModel;
	@Nullable
	private PartialModel hatModel;

	public SnowGolemFreezerVisual(VisualizationContext ctx, SnowGolemFreezerBlockEntity blockEntity, float partialTick) {
		super(ctx, blockEntity, partialTick);
		animate(partialTick);
	}

	@Override
	public void beginFrame(Context ctx) {
		if (!isVisible(ctx.frustum()) || doDistanceLimitThisFrame(ctx))
			return;
		animate(ctx.partialTick());
	}

	private void animate(float partialTick) {
		HeatLevel heatLevel = blockEntity.getHeatLevelForRender();
		boolean visible = heatLevel != HeatLevel.NONE;
		boolean smallHead = !blockEntity.hasPumpkinHead() && !heatLevel.isAtLeast(HeatLevel.FADING);
		PartialModel nextHead = !visible ? null : blockEntity.hasPumpkinHead()
			? AllPartialModels.SNOW_GOLEM_FREEZER_PUMPKIN
			: smallHead ? AllPartialModels.SNOW_GOLEM_FREEZER_INERT : AllPartialModels.SNOW_GOLEM_FREEZER_IDLE;
		PartialModel nextGoggles = !visible || !blockEntity.goggles ? null : smallHead
			? com.simibubi.create.AllPartialModels.BLAZE_GOGGLES_SMALL : com.simibubi.create.AllPartialModels.BLAZE_GOGGLES;
		PartialModel nextHat = !visible ? null : blockEntity.stockKeeper
			? com.simibubi.create.AllPartialModels.LOGISTICS_HAT
			: blockEntity.hat ? com.simibubi.create.AllPartialModels.TRAIN_HAT : null;

		head = updateInstance(head, headModel, nextHead);
		goggles = updateInstance(goggles, gogglesModel, nextGoggles);
		hat = updateInstance(hat, hatModel, nextHat);
		headModel = nextHead;
		gogglesModel = nextGoggles;
		hatModel = nextHat;

		float animation = blockEntity.headAnimation.getValue(partialTick) * .175f;
		float time = AnimationTickHolder.getRenderTime(level);
		float renderTick = time + blockEntity.hashCode() % 13 * 16f;
		float offsetMult = heatLevel.isAtLeast(HeatLevel.FADING) ? 64 : 16;
		float offset = Mth.sin((float) ((renderTick / 16f) % (2 * Math.PI))) / offsetMult;
		float headY = offset - animation * .75f;
		float horizontalAngle = AngleHelper.rad(blockEntity.getHeadAngle(partialTick));

		if (head != null) {
			head.setIdentityTransform()
				.translate(getVisualPosition())
				.translateY(headY)
				.rotateCentered(horizontalAngle, Direction.UP)
				.setChanged();
		}
		if (goggles != null) {
			goggles.setIdentityTransform()
				.translate(getVisualPosition())
				.translateY(headY + 8 / 16f)
				.rotateCentered(horizontalAngle, Direction.UP)
				.setChanged();
		}
		if (hat != null) {
			hat.setIdentityTransform()
				.translate(getVisualPosition())
				.translateY(headY);
			if (smallHead)
				hat.translateY(.5f).center().scale(.75f).uncenter();
			else
				hat.translateY(.75f);
			hat.rotateCentered(horizontalAngle + Mth.PI, Direction.UP)
				.translate(.5f, 0, .5f)
				.setChanged();
		}

		boolean drawFlame = heatLevel.isAtLeast(HeatLevel.FADING) && animation > .125f;
		if (drawFlame) {
			if (flame == null) {
				flame = instancerProvider().instancer(AllInstanceTypes.SCROLLING,
					Models.partial(AllPartialModels.SNOW_GOLEM_FREEZER_FLAME)).createInstance();
				flame.position(getVisualPosition())
					.setSpriteShift(AllSpriteShifts.SNOW_GOLEM_FREEZER_FLAME)
					.light(LightTexture.FULL_BRIGHT);
			}
			float speed = 1 / 32f + 1 / 64f * heatLevel.ordinal();
			flame.speed(speed / 2, speed);
			flame.rotation.rotationY(horizontalAngle);
			flame.setChanged();
		} else if (flame != null) {
			flame.delete();
			flame = null;
		}
	}

	@Nullable
	private TransformedInstance updateInstance(@Nullable TransformedInstance instance,
		@Nullable PartialModel previousModel, @Nullable PartialModel nextModel) {
		if (previousModel == nextModel)
			return instance;
		if (nextModel == null) {
			if (instance != null)
				instance.delete();
			return null;
		}
		var instancer = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(nextModel));
		if (instance == null) {
			instance = instancer.createInstance();
			instance.light(LightTexture.FULL_BRIGHT);
		} else {
			instancer.stealInstance(instance);
		}
		return instance;
	}

	@Override
	public void updateLight(float partialTick) {
	}

	@Override
	public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
	}

	@Override
	protected void _delete() {
		if (head != null)
			head.delete();
		if (goggles != null)
			goggles.delete();
		if (hat != null)
			hat.delete();
		if (flame != null)
			flame.delete();
	}
}
