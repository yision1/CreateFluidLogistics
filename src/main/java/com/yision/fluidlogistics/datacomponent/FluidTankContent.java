package com.yision.fluidlogistics.datacomponent;

import net.minecraftforge.fluids.FluidStack;

public record FluidTankContent(FluidStack fluid) {
	public FluidTankContent {
		fluid = fluid.copy();
	}

	@Override
	public FluidStack fluid() {
		return fluid.copy();
	}

	public boolean isEmpty() {
		return fluid.isEmpty();
	}

	public int getAmount() {
		return fluid.getAmount();
	}
}
