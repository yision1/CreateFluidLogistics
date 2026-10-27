package com.yision.fluidlogistics.api.packager;

import net.minecraft.world.item.ItemStack;

public record PackageUnpackResult(int transferred, ItemStack remainder) {
	public PackageUnpackResult {
		if (transferred < 0) {
			throw new IllegalArgumentException("transferred amount must not be negative");
		}
		remainder = remainder.copy();
	}

	@Override
	public ItemStack remainder() {
		return remainder.copy();
	}

	public boolean complete() {
		return remainder.isEmpty();
	}
}
