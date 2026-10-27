package com.yision.fluidlogistics.migration.logistics;

import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.packagerLink.LogisticsNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;

public final class LegacyLogisticsNetworkMigration {
	private LegacyLogisticsNetworkMigration() {
	}

	public static void purgeRegisteredPositionFromAllNetworks(Level level, BlockPos pos) {
		GlobalPos globalPos = GlobalPos.of(level.dimension(), pos);
		boolean changed = false;

		var networkIterator = Create.LOGISTICS.logisticsNetworks.entrySet().iterator();
		while (networkIterator.hasNext()) {
			LogisticsNetwork network = networkIterator.next().getValue();
			if (network.totalLinks.remove(globalPos)) {
				changed = true;
			}
			network.loadedLinks.remove(globalPos);
			if (network.totalLinks.isEmpty()) {
				networkIterator.remove();
				changed = true;
			}
		}

		if (changed) {
			Create.LOGISTICS.markDirty();
		}
	}
}
