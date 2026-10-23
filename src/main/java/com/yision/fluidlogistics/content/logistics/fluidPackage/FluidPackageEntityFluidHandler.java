package com.yision.fluidlogistics.content.logistics.fluidPackage;

import java.util.List;

import com.simibubi.create.content.logistics.box.PackageEntity;
import com.yision.fluidlogistics.infrastructure.config.Config;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class FluidPackageEntityFluidHandler implements IFluidHandler {
	private final PackageEntity entity;

	public FluidPackageEntityFluidHandler(PackageEntity entity) {
		this.entity = entity;
	}

	@Override
	public int getTanks() {
		return fluids().size();
	}

	@Override
	public FluidStack getFluidInTank(int tank) {
		List<FluidStack> fluids = fluids();
		if (tank < 0 || tank >= fluids.size()) {
			return FluidStack.EMPTY;
		}
		return fluids.get(tank);
	}

	@Override
	public int getTankCapacity(int tank) {
		FluidStack fluid = getFluidInTank(tank);
		return fluid.isEmpty() ? 0 : Math.max(Config.getFluidPerPackage(), fluid.getAmount());
	}

	private List<FluidStack> fluids() {
		return FluidPackageContentHelper.getContainedFluids(entity.getBox());
	}

	@Override
	public boolean isFluidValid(int tank, FluidStack stack) {
		return false;
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		return 0;
	}

	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		return FluidStack.EMPTY;
	}

	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		return FluidStack.EMPTY;
	}
}
