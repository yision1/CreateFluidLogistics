package com.yision.fluidlogistics.content.kinetics.oscillatingSteamEngine;

import java.lang.ref.WeakReference;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity.RotationDirection;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamJetParticleData;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class OscillatingSteamEngineBlockEntity extends SteamEngineBlockEntity {
	private Float previousParticleAngle;

	public OscillatingSteamEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		movementDirection = new ScrollOptionBehaviour<>(RotationDirection.class,
			CreateLang.translateDirect("contraptions.windmill.rotation_direction"), this,
			new OscillatingSteamEngineValueBox());
		movementDirection.onlyActiveWhen(() -> {
			PoweredShaftBlockEntity shaft = getShaft();
			return shaft == null || !shaft.hasSource();
		});
		behaviours.add(movementDirection);
		registerAwardables(behaviours, AllAdvancements.STEAM_ENGINE);
	}

	@Override
	public void tick() {
		super.tick();
		PoweredShaftBlockEntity shaft = getShaft();
		FluidTankBlockEntity tank = getTank();
		if (shaft == null || tank == null || !isValid())
			return;
		if (level.isClientSide) {
			CatnipServices.PLATFORM.executeOnClientOnly(() -> this::spawnSteam);
			return;
		}
		float efficiency = tank.boiler.attachedEngines > 0
			? Mth.clamp(tank.boiler.getEngineEfficiency(tank.getTotalTankSize()), 0, 1) : 0;
		Axis axis = shaft.getBlockState().getValue(ShaftBlock.AXIS);
		Direction facing = SteamEngineBlock.getFacing(getBlockState());
		if (facing.getAxis() == Axis.Y)
			facing = getBlockState().getValue(SteamEngineBlock.FACING);
		int direction = efficiency == 0 || axis == Axis.Y ? 1
			: (int) GeneratingKineticBlockEntity.convertToDirection(1, facing);
		if (axis == Axis.Z)
			direction *= -1;
		if (movementDirection.get() == RotationDirection.COUNTER_CLOCKWISE)
			direction *= -1;
		float speed = shaft.getTheoreticalSpeed();
		if (shaft.hasSource() && speed != 0 && (speed > 0) != (direction > 0)) {
			movementDirection.setValue(1 - movementDirection.get().ordinal());
			direction *= -1;
		}
		shaft.update(worldPosition, direction, efficiency);
		if (efficiency > 0)
			award(AllAdvancements.STEAM_ENGINE);
	}

	@Override
	public PoweredShaftBlockEntity getShaft() {
		if (level == null)
			return null;
		BlockState state = getBlockState();
		BlockPos pos = OscillatingSteamEngineBlock.getShaftPos(state, worldPosition);
		PoweredShaftBlockEntity previous = target.get();
		if (previous != null && !previous.isRemoved() && !previous.getBlockPos().equals(pos)
			&& !level.isClientSide && level.isLoaded(previous.getBlockPos())) {
			previous.remove(worldPosition);
			level.scheduleTick(previous.getBlockPos(), previous.getBlockState().getBlock(), 1);
		}
		if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PoweredShaftBlockEntity shaft) {
			if (OscillatingSteamEngineBlock.isShaftValid(state, shaft.getBlockState())) {
				if (shaft.canBePoweredBy(worldPosition)) {
					if (previous != shaft)
						target = new WeakReference<>(shaft);
					return shaft;
				}
			} else if (!level.isClientSide && (previous == shaft || shaft.isPoweredBy(worldPosition))) {
				shaft.remove(worldPosition);
				level.scheduleTick(pos, shaft.getBlockState().getBlock(), 1);
			}
		}
		if (previous != null)
			target = new WeakReference<>(null);
		return null;
	}

	@Override
	public FluidTankBlockEntity getTank() {
		if (level == null)
			return null;
		BlockPos pos = worldPosition.relative(SteamEngineBlock.getFacing(getBlockState()).getOpposite());
		FluidTankBlockEntity cached = source.get();
		if (!level.isLoaded(pos))
			return null;
		if (cached != null && (cached.isRemoved() || !cached.getBlockPos().equals(pos)))
			source = new WeakReference<>(null);
		return super.getTank();
	}

	@Override
	@Nullable
	@OnlyIn(Dist.CLIENT)
	public Float getTargetAngle() {
		PoweredShaftBlockEntity shaft = getShaft();
		if (shaft == null)
			return null;
		return KineticBlockEntityRenderer.getAngleForBe(shaft, shaft.getBlockPos(),
			shaft.getBlockState().getValue(ShaftBlock.AXIS));
	}

	@OnlyIn(Dist.CLIENT)
	private void spawnSteam() {
		Float angle = getTargetAngle();
		PoweredShaftBlockEntity shaft = target.get();
		if (angle == null || shaft == null || shaft.getSpeed() == 0 || shaft.engineEfficiency <= 0) {
			previousParticleAngle = null;
			return;
		}
		boolean crossed = previousParticleAngle != null && Math.abs(angle - previousParticleAngle) > Math.PI;
		previousParticleAngle = angle;
		if (!crossed)
			return;
		Direction facing = SteamEngineBlock.getFacing(getBlockState());
		FluidTankBlockEntity tank = getTank();
		if (tank != null)
			tank.boiler.queueSoundOnSide(worldPosition, facing);
		Vec3 outward = Vec3.atLowerCornerOf(facing.getNormal());
		Vec3 pos = Vec3.atCenterOf(worldPosition).add(outward.scale(.4));
		level.addParticle(new SteamJetParticleData(1), pos.x, pos.y, pos.z,
			outward.x * .1, outward.y * .1, outward.z * .1);
	}
}
