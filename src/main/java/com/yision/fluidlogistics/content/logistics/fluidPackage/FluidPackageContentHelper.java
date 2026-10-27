package com.yision.fluidlogistics.content.logistics.fluidPackage;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.yision.fluidlogistics.config.Config;
import com.yision.fluidlogistics.datacomponent.FluidTankContent;
import com.yision.fluidlogistics.datacomponent.PackageFluidContents;
import com.yision.fluidlogistics.migration.packageData.LegacyFluidPackageMigration;
import com.yision.fluidlogistics.registry.AllItems;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.items.ItemStackHandler;

public final class FluidPackageContentHelper {
	public static final int PACKAGE_DRAIN_AMOUNT = FluidType.BUCKET_VOLUME;

	private FluidPackageContentHelper() {
	}

	public static FluidStack getSingleContainedFluid(ItemStack packageStack) {
		if (packageStack == null || !PackageItem.isPackage(packageStack) || hasConflictingContents(packageStack)) {
			return FluidStack.EMPTY;
		}
		ItemStack normalized = migrateCopy(packageStack);
		if (hasItemContents(normalized)) {
			return FluidStack.EMPTY;
		}
		return getContainedFluid(normalized);
	}

	public static FluidStack getContainedFluid(ItemStack packageStack) {
		List<FluidStack> fluids = getContainedFluids(packageStack);
		return fluids.size() == 1 ? fluids.get(0) : FluidStack.EMPTY;
	}

	public static List<FluidStack> getContainedFluids(ItemStack packageStack) {
		if (packageStack == null || !PackageItem.isPackage(packageStack) || hasConflictingContents(packageStack)) {
			return List.of();
		}
		PackageFluidContents content = PackageFluidContents.get(packageStack);
		if (content == null) {
			return LegacyFluidPackageMigration.getContainedFluids(packageStack);
		}
		return content.tanks().stream().map(FluidTankContent::fluid).toList();
	}

	public static boolean hasConflictingContents(ItemStack packageStack) {
		return LegacyFluidPackageMigration.hasConflictingContents(packageStack);
	}

	public static ItemStack migrateCopy(ItemStack packageStack) {
		return LegacyFluidPackageMigration.migrateCopy(packageStack);
	}

	public static boolean isCanonicalPackage(ItemStack packageStack) {
		return packageStack != null && FluidPackageItem.isFluidPackage(packageStack)
			&& !getSingleContainedFluid(packageStack).isEmpty();
	}

	public static ItemStackHandler readRawContents(ItemStack packageStack) {
		ItemStackHandler contents = new ItemStackHandler(PackageItem.SLOTS);
		if (packageStack.hasTag()) {
			CompoundTag items = packageStack.getTag().getCompound("Items");
			if (!items.isEmpty()) {
				contents.deserializeNBT(items);
			}
		}
		return contents;
	}

	public static boolean hasItemContents(ItemStack packageStack) {
		ItemStackHandler contents = readRawContents(packageStack);
		for (int slot = 0; slot < contents.getSlots(); slot++) {
			if (!contents.getStackInSlot(slot).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	public static int getCapacity(ItemStack packageStack) {
		return Math.max(Config.getFluidPerPackage(), getContainedFluid(packageStack).getAmount());
	}

	public static ItemStack createCanonicalPackage(FluidStack fluid) {
		ItemStack packageStack = AllItems.createFluidPackage();
		setCanonicalContents(packageStack, fluid);
		return packageStack;
	}

	public static void setCanonicalContents(ItemStack packageStack, FluidStack fluid) {
		if (packageStack == null || !PackageItem.isPackage(packageStack) || hasConflictingContents(packageStack)) {
			throw new IllegalArgumentException("Cannot replace conflicting or invalid package contents");
		}
		int capacity = getCapacity(packageStack);
		if (fluid.isEmpty() || fluid.getAmount() > capacity) {
			throw new IllegalArgumentException("fluid amount must be between 1 and " + capacity + " mB");
		}
		ItemStack normalized = migrateCopy(packageStack);
		if (normalized.getItem() != packageStack.getItem()) {
			throw new IllegalArgumentException("Legacy mixed fluid package must be migrated before replacing contents");
		}
		if (LegacyFluidPackageMigration.hasLegacyContents(normalized)) {
			throw new IllegalArgumentException("Legacy mixed package must be split before replacing fluid contents");
		}
		PackageFluidContents content = PackageFluidContents.get(normalized);
		if (content != null && content.tanks().size() > 1) {
			throw new IllegalArgumentException("Combined package must be split before replacing fluid contents");
		}
		if (FluidPackageItem.isFluidPackage(packageStack) && hasItemContents(normalized)) {
			throw new IllegalArgumentException("Fluid package cannot contain ordinary items");
		}
		copyItemContents(normalized, packageStack);
		PackageFluidContents.set(packageStack, PackageFluidContents.single(fluid));
	}

	public static FluidStack peekDrainOneBucket(ItemStack packageStack) {
		return drainOneBucket(packageStack, true);
	}

	public static FluidStack drainOneBucket(ItemStack packageStack, boolean simulate) {
		return drainOneBucket(packageStack, 0, simulate);
	}

	public static FluidStack drainOneBucket(ItemStack packageStack, int tank, boolean simulate) {
		if (packageStack == null || !PackageItem.isPackage(packageStack) || packageStack.getCount() != 1
			|| hasConflictingContents(packageStack)) {
			return FluidStack.EMPTY;
		}
		ItemStack normalized = migrateCopy(packageStack);
		if (normalized.getItem() != packageStack.getItem()) {
			return FluidStack.EMPTY;
		}
		PackageFluidContents content = PackageFluidContents.get(normalized);
		if (content == null || tank < 0 || tank >= content.tanks().size()) {
			return FluidStack.EMPTY;
		}
		FluidStack contained = content.tanks().get(tank).fluid();
		if (contained.getAmount() < PACKAGE_DRAIN_AMOUNT) {
			return FluidStack.EMPTY;
		}

		FluidStack drained = FluidHelper.copyStackWithAmount(contained, PACKAGE_DRAIN_AMOUNT);
		if (!simulate) {
			copyItemContents(normalized, packageStack);
			List<FluidTankContent> remainingTanks = new ArrayList<>(content.tanks());
			int remaining = contained.getAmount() - PACKAGE_DRAIN_AMOUNT;
			if (remaining <= 0) {
				remainingTanks.remove(tank);
			} else {
				remainingTanks.set(tank, new FluidTankContent(FluidHelper.copyStackWithAmount(contained, remaining)));
			}
			if (remainingTanks.isEmpty()) {
				PackageFluidContents.remove(packageStack);
				if (!hasItemContents(packageStack)) {
					packageStack.shrink(1);
				}
			} else {
				PackageFluidContents.set(packageStack, new PackageFluidContents(remainingTanks));
			}
		}
		return drained;
	}

	private static void copyItemContents(ItemStack source, ItemStack target) {
		if (!source.hasTag() || !source.getTag().contains("Items")) {
			target.removeTagKey("Items");
		} else {
			target.getOrCreateTag().put("Items", source.getTag().getCompound("Items").copy());
		}
	}
}
