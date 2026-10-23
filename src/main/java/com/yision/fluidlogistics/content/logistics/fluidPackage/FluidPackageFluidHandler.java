package com.yision.fluidlogistics.content.logistics.fluidPackage;

import java.util.List;

import com.yision.fluidlogistics.infrastructure.config.Config;

import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

public class FluidPackageFluidHandler implements IFluidHandlerItem {
	private final ItemStack container;

	public FluidPackageFluidHandler(ItemStack container) {
		this.container = container;
	}

	@Override
	public ItemStack getContainer() {
		return container;
	}

	@Override
	public int getTanks() {
		return FluidPackageContentHelper.getContainedFluids(container).size();
	}

	@Override
	public FluidStack getFluidInTank(int tank) {
		List<FluidStack> fluids = FluidPackageContentHelper.getContainedFluids(container);
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

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		return 0;
	}

	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		if (maxDrain < FluidPackageContentHelper.PACKAGE_DRAIN_AMOUNT) {
			return FluidStack.EMPTY;
		}
		for (int tank = 0; tank < getTanks(); tank++) {
			FluidStack drained = FluidPackageContentHelper.drainOneBucket(container, tank, action.simulate());
			if (!drained.isEmpty()) {
				return drained;
			}
		}
		return FluidStack.EMPTY;
	}

	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		if (resource.getAmount() < FluidPackageContentHelper.PACKAGE_DRAIN_AMOUNT) {
			return FluidStack.EMPTY;
		}
		List<FluidStack> fluids = FluidPackageContentHelper.getContainedFluids(container);
		for (int tank = 0; tank < fluids.size(); tank++) {
			if (!FluidStack.isSameFluidSameComponents(fluids.get(tank), resource)) {
				continue;
			}
			FluidStack drained = FluidPackageContentHelper.drainOneBucket(container, tank, action.simulate());
			if (!drained.isEmpty()) {
				return drained;
			}
		}
		return FluidStack.EMPTY;
	}

	@Override
	public boolean isFluidValid(int tank, FluidStack stack) {
		return false;
	}
}
