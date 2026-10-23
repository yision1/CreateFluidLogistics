package com.yision.fluidlogistics.compat.createdragonsplus;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.yision.fluidlogistics.infrastructure.config.Config;
import com.yision.fluidlogistics.registry.AllBlocks;

import plus.dragons.createdragonsplus.common.processing.freeze.BlockFreezer;

public final class CreateDragonsPlusCompat {

	private CreateDragonsPlusCompat() {
	}

	public static void register() {
		BlockFreezer.REGISTRY.register(AllBlocks.SNOW_GOLEM_FREEZER.get(),
			(level, pos, state) -> Config.isSnowGolemFreezerEnabled()
				&& state.getValue(BlazeBurnerBlock.HEAT_LEVEL) != HeatLevel.NONE
				? BlockFreezer.PASSIVE_FREEZE
				: BlockFreezer.NO_FREEZE);
	}
}
