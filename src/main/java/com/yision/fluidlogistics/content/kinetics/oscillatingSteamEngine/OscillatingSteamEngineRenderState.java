package com.yision.fluidlogistics.content.kinetics.oscillatingSteamEngine;

import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.yision.fluidlogistics.registry.AllPartialModels;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;

public record OscillatingSteamEngineRenderState(Quaternionf orientation, float angle, float swing,
	float pinY, float pinZ, boolean connected) {
	public static OscillatingSteamEngineRenderState of(OscillatingSteamEngineBlockEntity be) {
		Direction facing = SteamEngineBlock.getFacing(be.getBlockState());
		Axis axis = OscillatingSteamEngineBlock.getHousingAxis(be.getBlockState());
		Quaternionf orientation = orientation(facing, axis);
		Float targetAngle = be.getTargetAngle();
		float angle = targetAngle == null ? 0 : targetAngle;
		float pinY = (float) (24 - 5 * Math.cos(angle));
		float pinZ = (float) (8 - 5 * Math.sin(angle));
		float swing = (float) Math.atan2(pinZ - 8, pinY - 9);
		return new OscillatingSteamEngineRenderState(orientation, angle, swing, pinY, pinZ, targetAngle != null);
	}

	private static Quaternionf orientation(Direction facing, Axis axis) {
		Vector3f x = Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE).step();
		Vector3f y = facing.step();
		Vector3f z = new Vector3f(x).cross(y);
		return new Quaternionf().setFromNormalized(new Matrix3f()
			.setColumn(0, x).setColumn(1, y).setColumn(2, z));
	}

	public boolean isVisible(Part part) {
		return connected || part == Part.HOUSING || part == Part.CYLINDER;
	}

	public void transform(PoseStack pose, Part part) {
		pose.translate(.5, .5, .5);
		pose.mulPose(orientation);
		pose.translate(-.5, -.5, -.5);
		switch (part) {
			case HOUSING -> {}
			case CYLINDER -> {
				pose.translate(.5, 9 / 16f, .5);
				pose.mulPose(com.mojang.math.Axis.XP.rotation(swing));
				pose.translate(-.5, -9 / 16f, -.5);
			}
			case PISTON, LINKAGE -> {
				pose.translate(.5, pinY / 16f, pinZ / 16f);
				pose.mulPose(com.mojang.math.Axis.XP.rotation(swing));
				pose.translate(-.5, -.5, -.5);
			}
			case CONNECTOR -> {
				pose.translate(.5, 24 / 16f, .5);
				pose.mulPose(com.mojang.math.Axis.XP.rotation(angle));
				pose.translate(-.5, -.5, -.5);
			}
		}
	}

	public enum Part {
		HOUSING, CYLINDER, PISTON, LINKAGE, CONNECTOR;

		public PartialModel model() {
			return switch (this) {
				case HOUSING -> AllPartialModels.OSCILLATING_ENGINE_HOUSING;
				case CYLINDER -> AllPartialModels.OSCILLATING_ENGINE_CYLINDER;
				case PISTON -> AllPartialModels.OSCILLATING_ENGINE_PISTON;
				case LINKAGE -> AllPartialModels.OSCILLATING_ENGINE_LINKAGE;
				case CONNECTOR -> AllPartialModels.OSCILLATING_ENGINE_CONNECTOR;
			};
		}
	}
}
