package com.yision.fluidlogistics.content.kinetics.oscillatingSteamEngine;

import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class OscillatingSteamEngineValueBox extends ValueBoxTransform.Sided {
	@Override
	protected boolean isSideActive(BlockState state, Direction side) {
		return side.getAxis() == OscillatingSteamEngineBlock.getHousingAxis(state);
	}

	@Override
	public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
		return new Vec3(.5, .5, .5)
			.add(Vec3.atLowerCornerOf(getSide().getNormal()).scale(7.01 / 16))
			.add(Vec3.atLowerCornerOf(SteamEngineBlock.getFacing(state).getNormal()).scale(1 / 16f));
	}

	@Override
	protected Vec3 getSouthLocation() {
		return Vec3.ZERO;
	}
}
