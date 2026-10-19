package com.yision.fluidlogistics.content.fluids.multiFluidTank.storage;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.api.contraption.storage.SyncedMountedStorage;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageType;
import com.simibubi.create.api.contraption.storage.fluid.WrapperMountedFluidStorage;
import com.yision.fluidlogistics.content.fluids.multiFluidTank.SmartMultiFluidTank;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public abstract class AbstractMultiFluidTankMountedStorage<H extends IFluidHandler>
	extends WrapperMountedFluidStorage<H> implements SyncedMountedStorage {

	protected boolean dirty;
	protected final int capacity;
	protected final int tanks;

	protected AbstractMultiFluidTankMountedStorage(MountedFluidStorageType<?> type, H handler, int capacity,
		int tanks) {
		super(type, handler);
		this.capacity = capacity;
		this.tanks = tanks;
	}

	public List<FluidStack> getFluidsList() {
		List<FluidStack> fluids = new ArrayList<>();
		for (int i = 0; i < wrapped.getTanks(); i++) {
			fluids.add(wrapped.getFluidInTank(i).copy());
		}
		return fluids;
	}

	public int getCapacity() {
		return this.capacity;
	}

	public int getTanks() {
		return this.tanks;
	}

	@Override
	public boolean isDirty() {
		return this.dirty;
	}

	@Override
	public void markClean() {
		this.dirty = false;
	}

	protected static List<FluidStack> readLegacyFluids(HolderLookup.Provider registries, CompoundTag nbt, int tanks) {
		List<FluidStack> fluids = new ArrayList<>();
		CompoundTag fluidsNbt = nbt.getCompound("Fluids");
		for (int i = 0; i < tanks; i++) {
			if (fluidsNbt.contains(Integer.toString(i))) {
				FluidStack fluid = FluidStack.parseOptional(registries, fluidsNbt.getCompound(Integer.toString(i)));
				fluids.add(fluid);
			} else {
				fluids.add(FluidStack.EMPTY);
			}
		}
		return fluids;
	}

	protected static int readLegacyTanks(CompoundTag nbt) {
		return nbt.contains("Tanks") ? nbt.getInt("Tanks") : 8;
	}

	public static final class Handler extends SmartMultiFluidTank {
		public Runnable onChange = () -> {
		};

		public Handler(int capacity, int tanks, List<FluidStack> fluids) {
			super(capacity, tanks, $ -> {
			});
			for (int i = 0; i < Math.min(tanks, fluids.size()); i++)
				setFluid(i, fluids.get(i).copy());
		}

		@Override
		protected void onContentsChanged() {
			onChange.run();
		}
	}
}
