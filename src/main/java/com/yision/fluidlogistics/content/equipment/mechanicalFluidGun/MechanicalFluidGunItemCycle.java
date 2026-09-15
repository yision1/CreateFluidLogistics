package com.yision.fluidlogistics.content.equipment.mechanicalFluidGun;

import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;

final class MechanicalFluidGunItemCycle {

	private float progress;
	private long lastTick = Long.MIN_VALUE;

	boolean tick(long gameTime, float speed) {
		if (gameTime == lastTick || speed == 0) return false;
		lastTick = gameTime;
		progress += Math.min(256, Math.abs(speed));
		return progress >= 256f * SpoutBlockEntity.FILLING_TIME / 2;
	}

	boolean canSpray() {
		return progress > 0;
	}

	void reset() {
		progress = 0;
	}
}
