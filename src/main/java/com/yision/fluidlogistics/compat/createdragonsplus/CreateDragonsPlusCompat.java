package com.yision.fluidlogistics.compat.createdragonsplus;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.yision.fluidlogistics.compat.CompatMods;
import com.yision.fluidlogistics.config.Config;
import com.yision.fluidlogistics.registry.AllBlocks;

import plus.dragons.createdragonsplus.common.processing.freeze.BlockFreezer;

public final class CreateDragonsPlusCompat {
	private static boolean registered;

	private CreateDragonsPlusCompat() {
	}

	public static void register() {
		if (registered || !CompatMods.createDragonsPlusLoaded())
			return;
		BlockFreezer.REGISTRY.register(AllBlocks.SNOW_GOLEM_FREEZER.get(),
			(level, pos,
				state) -> Config.isSnowGolemFreezerEnabled()
					&& state.getValue(BlazeBurnerBlock.HEAT_LEVEL) != HeatLevel.NONE ? BlockFreezer.PASSIVE_FREEZE
						: BlockFreezer.NO_FREEZE);
		registered = true;
	}

	public static boolean isRegistered() {
		return registered;
	}
}
