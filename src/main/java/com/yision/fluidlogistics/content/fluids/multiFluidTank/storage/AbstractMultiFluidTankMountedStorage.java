package com.yision.fluidlogistics.content.fluids.multiFluidTank.storage;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.contraption.storage.SyncedMountedStorage;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageType;
import com.simibubi.create.api.contraption.storage.fluid.WrapperMountedFluidStorage;
import com.simibubi.create.content.contraptions.Contraption;
import com.yision.fluidlogistics.content.fluids.multiFluidTank.SmartMultiFluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public abstract class AbstractMultiFluidTankMountedStorage
	extends WrapperMountedFluidStorage<AbstractMultiFluidTankMountedStorage.Handler> implements SyncedMountedStorage {

	private boolean dirty;
	private final int capacity;
	private final int tanks;

	public AbstractMultiFluidTankMountedStorage(MountedFluidStorageType<?> type, int capacity, int tanks,
		List<FluidStack> fluids) {
		super(type, new Handler(capacity, tanks, fluids));
		this.capacity = capacity;
		this.tanks = tanks;
		this.wrapped.onChange = () -> this.dirty = true;
	}

	protected abstract boolean matchesController(BlockEntity be);

	protected abstract void applyMountedState(BlockEntity controller, int tanks, Handler wrapped);

	public List<FluidStack> getFluidsList() {
		List<FluidStack> fluids = new ArrayList<>();
		for (int i = 0; i < wrapped.getTanks(); i++) {
			fluids.add(wrapped.getFluidInTank(i).copy());
		}
		return fluids;
	}

	public int getCapacity() {
		return capacity;
	}

	public int getTanks() {
		return tanks;
	}

	@Override
	public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
		if (matchesController(be)) {
			applyMountedState(be, tanks, wrapped);
		}
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public void markClean() {
		dirty = false;
	}

	@Override
	public void afterSync(Contraption contraption, BlockPos localPos) {
		BlockEntity be = contraption.getBlockEntityClientSide(localPos);
		if (!matchesController(be)) {
			return;
		}
		applyMountedState(be, wrapped.getTanks(), wrapped);
	}

	public static List<FluidStack> readLegacyFluids(CompoundTag nbt) {
		int tanks = nbt.contains("Tanks") ? nbt.getInt("Tanks") : 8;
		List<FluidStack> fluids = new ArrayList<>();
		CompoundTag fluidsNbt = nbt.getCompound("Fluids");
		for (int i = 0; i < tanks; i++) {
			fluids.add(fluidsNbt.contains(Integer.toString(i))
				? FluidStack.loadFluidStackFromNBT(fluidsNbt.getCompound(Integer.toString(i)))
				: FluidStack.EMPTY);
		}
		return fluids;
	}

	public static int readLegacyCapacity(CompoundTag nbt) {
		return nbt.getInt("Capacity");
	}

	public static int readLegacyTanks(CompoundTag nbt) {
		return nbt.contains("Tanks") ? nbt.getInt("Tanks") : 8;
	}

	public static final class Handler extends SmartMultiFluidTank {
		private Runnable onChange = () -> {
		};

		public Handler(int capacity, int tanks, List<FluidStack> fluids) {
			super(capacity, tanks, $ -> {
			});
			for (int i = 0; i < Math.min(tanks, fluids.size()); i++) {
				setFluid(i, fluids.get(i));
			}
		}

		@Override
		protected void onContentsChanged() {
			onChange.run();
		}
	}
}
