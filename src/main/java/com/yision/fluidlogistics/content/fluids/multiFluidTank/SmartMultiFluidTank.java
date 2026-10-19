package com.yision.fluidlogistics.content.fluids.multiFluidTank;

import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class SmartMultiFluidTank implements IFluidHandler, IFluidTank, SharedCapacityFluidHandler {

	private final Consumer<FluidStack[]> updateCallback;
	private final int tankCount;
	protected int capacity;
	protected Predicate<FluidStack> validator;

	@NotNull
	protected FluidStack[] fluids;

	public SmartMultiFluidTank(int capacity, int tankCount, Consumer<FluidStack[]> updateCallback) {
		this.capacity = capacity;
		this.validator = e -> true;
		this.updateCallback = updateCallback;
		this.tankCount = tankCount;
		fluids = new FluidStack[tankCount];
		resetTanks();
	}

	public SmartMultiFluidTank setCapacity(int capacity) {
		this.capacity = capacity;
		return this;
	}

	public SmartMultiFluidTank setValidator(Predicate<FluidStack> validator) {
		if (validator != null) {
			this.validator = validator;
		}
		return this;
	}

	@Override
	public int getFluidAmount() {
		int amount = 0;
		for (int i = 0; i < getTanks(); i++) {
			amount += fluids[i].getAmount();
		}
		return amount;
	}

	public int getFluidAmount(int tank) {
		if (tank >= 0 && tank < getTanks())
			return fluids[tank].getAmount();

		return 0;
	}

	public void resetTanks() {
		for (int i = 0; i < getTanks(); i++) {
			fluids[i] = FluidStack.EMPTY;
		}
	}

	protected void onContentsChanged() {
		updateCallback.accept(getFluids());
	}

	public void setFluid(FluidStack stack) {
		for (int i = 1; i < tankCount; i++) {
			fluids[i] = FluidStack.EMPTY;
		}
		setFluid(0, stack);
	}

	public void setFluid(int tank, FluidStack stack) {
		if (tank >= 0 && tank < this.tankCount) {
			fluids[tank] = stack;
			updateCallback.accept(getFluids());
		}
	}

	public SmartMultiFluidTank load(HolderLookup.Provider registries, CompoundTag nbt) {
		resetTanks();
		for (int i = 0; i < getTanks(); i++) {
			if (!nbt.contains(Integer.toString(i), Tag.TAG_COMPOUND))
				continue;
			CompoundTag fluidNbt = nbt.getCompound(Integer.toString(i));
			FluidStack fluid = FluidStack.parseOptional(registries, fluidNbt);
			fluids[i] = fluid;
		}
		return this;
	}

	public CompoundTag save(HolderLookup.Provider registries, CompoundTag nbt) {
		for (int i = 0; i < getTanks(); i++) {
			if (fluids[i].isEmpty()) {
				nbt.remove(Integer.toString(i));
				continue;
			}
			Tag fluidNbt = fluids[i].save(registries);
			nbt.put(Integer.toString(i), fluidNbt);
		}
		return nbt;
	}

	@Override
	public int getTanks() {
		return tankCount;
	}

	@Override
	public @NotNull FluidStack getFluidInTank(int tank) {
		if (tank >= 0 && tank < getTanks()) {
			return fluids[tank];
		}
		return FluidStack.EMPTY;
	}

	@Override
	public int getTankCapacity(int tank) {
		return tank >= 0 && tank < tankCount ? getCapacity() : 0;
	}

	@Override
	public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
		return tank >= 0 && tank < tankCount && isFluidValid(stack);
	}

	OptionalInt findMatchingOrEmptyTank(FluidStack resource) {
		int firstEmpty = -1;
		for (int i = 0; i < getTanks(); i++) {
			if (fluids[i].isEmpty() && firstEmpty < 0)
				firstEmpty = i;

			if (FluidStack.isSameFluidSameComponents(fluids[i], resource))
				return OptionalInt.of(i);
		}
		if (firstEmpty >= 0)
			return OptionalInt.of(firstEmpty);
		return OptionalInt.empty();
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		if (resource.isEmpty() || !isFluidValid(resource)) {
			return 0;
		}
		if (getSpace() == 0)
			return 0;

		OptionalInt targetTankOpt = findMatchingOrEmptyTank(resource);
		if (targetTankOpt.isEmpty())
			return 0;

		int filled = Math.min(getSpace(), resource.getAmount());
		if (action.simulate())
			return filled;

		int targetTank = targetTankOpt.getAsInt();
		if (fluids[targetTank].isEmpty())
			fluids[targetTank] = resource.copyWithAmount(filled);
		else
			fluids[targetTank].grow(filled);
		onContentsChanged();
		return filled;
	}

	@Override
	public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
		if (resource.isEmpty())
			return FluidStack.EMPTY;
		OptionalInt targetTankOpt = findMatchingOrEmptyTank(resource);
		if (targetTankOpt.isEmpty())
			return FluidStack.EMPTY;
		int targetTank = targetTankOpt.getAsInt();

		if (fluids[targetTank].isEmpty())
			return FluidStack.EMPTY;

		int drained = Math.min(fluids[targetTank].getAmount(), resource.getAmount());
		FluidStack stack = fluids[targetTank].copyWithAmount(drained);

		if (action.execute() && drained > 0) {
			fluids[targetTank].shrink(drained);
			onContentsChanged();
		}
		return stack;
	}

	@Override
	public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
		if (maxDrain <= 0)
			return FluidStack.EMPTY;
		for (int i = 0; i < getTanks(); i++) {
			if (!fluids[i].isEmpty()) {
				int drained = Math.min(maxDrain, fluids[i].getAmount());
				FluidStack stack = fluids[i].copyWithAmount(drained);
				if (action.execute() && drained > 0) {
					fluids[i].shrink(drained);
					onContentsChanged();
				}
				return stack;
			}
		}
		return FluidStack.EMPTY;
	}

	@Override
	public @NotNull FluidStack getFluid() {
		for (int i = 0; i < getTanks(); i++) {
			if (!fluids[i].isEmpty())
				return fluids[i];
		}
		return FluidStack.EMPTY;
	}

	public @NotNull FluidStack[] getFluids() {
		return fluids;
	}

	@Override
	public int getCapacity() {
		return capacity;
	}

	@Override
	public boolean isFluidValid(FluidStack stack) {
		return validator.test(stack);
	}

	public boolean isEmpty() {
		for (int i = 0; i < getTanks(); i++) {
			if (!fluids[i].isEmpty())
				return false;
		}
		return true;
	}

	public int getSpace() {
		return Math.max(0, capacity - getFluidAmount());
	}

	@Override
	public boolean canFillAll(List<FluidStack> fluids) {
		if (fluids.isEmpty()) {
			return true;
		}

		FluidStack[] simulatedFluids = new FluidStack[tankCount];
		long totalAmount = 0;
		for (int i = 0; i < tankCount; i++) {
			simulatedFluids[i] = this.fluids[i].copy();
			totalAmount += simulatedFluids[i].getAmount();
		}

		for (FluidStack fluid : fluids) {
			if (fluid.isEmpty() || !isFluidValid(fluid)) {
				return false;
			}

			if (totalAmount + fluid.getAmount() > capacity) {
				return false;
			}

			int targetTank = -1;
			for (int i = 0; i < tankCount; i++) {
				if (FluidStack.isSameFluidSameComponents(simulatedFluids[i], fluid)) {
					targetTank = i;
					break;
				}
				if (targetTank == -1 && simulatedFluids[i].isEmpty()) {
					targetTank = i;
				}
			}

			if (targetTank == -1) {
				return false;
			}

			if (simulatedFluids[targetTank].isEmpty()) {
				simulatedFluids[targetTank] = fluid.copy();
			} else {
				simulatedFluids[targetTank].grow(fluid.getAmount());
			}
			totalAmount += fluid.getAmount();
		}

		return true;
	}
}
