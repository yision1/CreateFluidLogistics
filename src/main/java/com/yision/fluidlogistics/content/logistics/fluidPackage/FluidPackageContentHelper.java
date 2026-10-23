package com.yision.fluidlogistics.content.logistics.fluidPackage;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.item.ItemHelper;
import com.yision.fluidlogistics.datacomponent.FluidTankContent;
import com.yision.fluidlogistics.datacomponent.PackageFluidContents;
import com.yision.fluidlogistics.infrastructure.config.Config;
import com.yision.fluidlogistics.migration.packageData.LegacyFluidPackageMigration;
import com.yision.fluidlogistics.registry.AllItems;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.items.ItemStackHandler;

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
		return fluids.size() == 1 ? fluids.getFirst() : FluidStack.EMPTY;
	}

	public static List<FluidStack> getContainedFluids(ItemStack packageStack) {
		if (packageStack == null || !PackageItem.isPackage(packageStack) || hasConflictingContents(packageStack)) {
			return List.of();
		}
		PackageFluidContents content = packageStack.get(
			com.yision.fluidlogistics.registry.AllDataComponents.PACKAGE_FLUID_CONTENT);
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

	public static ItemStackHandler readRawContents(ItemStack packageStack) {
		ItemContainerContents component = packageStack.getOrDefault(
			AllDataComponents.PACKAGE_CONTENTS, ItemContainerContents.EMPTY);
		ItemStackHandler contents = new ItemStackHandler(Math.max(PackageItem.SLOTS, component.getSlots()));
		ItemHelper.fillItemStackHandler(component, contents);
		return contents;
	}

	public static boolean hasItemContents(ItemStack packageStack) {
		return packageStack.getOrDefault(AllDataComponents.PACKAGE_CONTENTS, ItemContainerContents.EMPTY)
			.nonEmptyItems().iterator().hasNext();
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
		PackageFluidContents content = normalized.get(
			com.yision.fluidlogistics.registry.AllDataComponents.PACKAGE_FLUID_CONTENT);
		if (content != null && content.tanks().size() > 1) {
			throw new IllegalArgumentException("Combined package must be split before replacing fluid contents");
		}
		if (FluidPackageItem.isFluidPackage(packageStack) && hasItemContents(normalized)) {
			throw new IllegalArgumentException("Fluid package cannot contain ordinary items");
		}
		copyItemContents(normalized, packageStack);
		packageStack.set(com.yision.fluidlogistics.registry.AllDataComponents.PACKAGE_FLUID_CONTENT,
			PackageFluidContents.single(fluid));
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
		PackageFluidContents content = normalized.get(
			com.yision.fluidlogistics.registry.AllDataComponents.PACKAGE_FLUID_CONTENT);
		if (content == null || tank < 0 || tank >= content.tanks().size()) {
			return FluidStack.EMPTY;
		}
		FluidStack contained = content.tanks().get(tank).fluid();
		if (contained.getAmount() < PACKAGE_DRAIN_AMOUNT) {
			return FluidStack.EMPTY;
		}

		FluidStack drained = contained.copyWithAmount(PACKAGE_DRAIN_AMOUNT);
		if (!simulate) {
			copyItemContents(normalized, packageStack);
			List<FluidTankContent> remainingTanks = new ArrayList<>(content.tanks());
			int remaining = contained.getAmount() - PACKAGE_DRAIN_AMOUNT;
			if (remaining <= 0) {
				remainingTanks.remove(tank);
			} else {
				remainingTanks.set(tank, new FluidTankContent(contained.copyWithAmount(remaining)));
			}
			if (remainingTanks.isEmpty()) {
				packageStack.remove(com.yision.fluidlogistics.registry.AllDataComponents.PACKAGE_FLUID_CONTENT);
				if (!hasItemContents(packageStack)) {
					packageStack.shrink(1);
				}
			} else {
				packageStack.set(com.yision.fluidlogistics.registry.AllDataComponents.PACKAGE_FLUID_CONTENT,
					new PackageFluidContents(remainingTanks));
			}
		}
		return drained;
	}

	private static void copyItemContents(ItemStack source, ItemStack target) {
		ItemContainerContents contents = source.get(AllDataComponents.PACKAGE_CONTENTS);
		if (contents == null) {
			target.remove(AllDataComponents.PACKAGE_CONTENTS);
		} else {
			target.set(AllDataComponents.PACKAGE_CONTENTS, contents);
		}
	}
}
