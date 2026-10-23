package com.yision.fluidlogistics.migration.packageData;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.box.PackageStyles;
import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.content.logistics.fluidPackage.CompressedTankItem;
import com.yision.fluidlogistics.content.logistics.fluidPackage.FluidPackageItem;
import com.yision.fluidlogistics.datacomponent.FluidTankContent;
import com.yision.fluidlogistics.datacomponent.PackageFluidContents;
import com.yision.fluidlogistics.registry.AllDataComponents;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.fluids.FluidStack;

@EventBusSubscriber(modid = FluidLogistics.MODID)
public final class LegacyFluidPackageMigration {
	private LegacyFluidPackageMigration() {
	}

	@SubscribeEvent
	public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide() || !(event.getEntity() instanceof PackageEntity entity)) {
			return;
		}
		ItemStack original = entity.getBox();
		ItemStack migrated = migrateCopy(original);
		if (!ItemStack.matches(original, migrated)) {
			entity.setBox(migrated);
		}
	}

	public static boolean hasLegacyContents(ItemStack stack) {
		if (stack == null || !PackageItem.isPackage(stack)) {
			return false;
		}
		for (ItemStack item : contents(stack).nonEmptyItems()) {
			if (item.getItem() instanceof CompressedTankItem) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasConflictingContents(ItemStack stack) {
		return stack != null && stack.has(AllDataComponents.PACKAGE_FLUID_CONTENT) && hasLegacyContents(stack);
	}

	public static List<FluidStack> getContainedFluids(ItemStack stack) {
		if (stack == null || !PackageItem.isPackage(stack) || stack.has(AllDataComponents.PACKAGE_FLUID_CONTENT)) {
			return List.of();
		}
		return inspect(stack).fluids();
	}

	public static ItemStack migrateCopy(ItemStack stack) {
		if (stack == null) {
			return ItemStack.EMPTY;
		}
		ItemStack result = stack.copy();
		if (!PackageItem.isPackage(result) || result.has(AllDataComponents.PACKAGE_FLUID_CONTENT)) {
			return result;
		}
		LegacyContents legacy = inspect(result);
		if (legacy.fluids().isEmpty()) {
			return result;
		}
		if (FluidPackageItem.isFluidPackage(result) && (legacy.fluids().size() > 1 || legacy.items().getSlots() > 0)) {
			result = result.transmuteCopy(PackageStyles.getDefaultBox().getItem());
		}
		result.set(AllDataComponents.PACKAGE_FLUID_CONTENT,
			new PackageFluidContents(legacy.fluids().stream().map(FluidTankContent::new).toList()));
		if (legacy.items().getSlots() == 0) {
			result.remove(com.simibubi.create.AllDataComponents.PACKAGE_CONTENTS);
		} else {
			result.set(com.simibubi.create.AllDataComponents.PACKAGE_CONTENTS, legacy.items());
		}
		return result;
	}

	private static LegacyContents inspect(ItemStack stack) {
		ItemContainerContents contents = contents(stack);
		List<ItemStack> items = new ArrayList<>(contents.getSlots());
		List<FluidStack> fluids = new ArrayList<>();
		for (int slot = 0; slot < contents.getSlots(); slot++) {
			ItemStack item = contents.getStackInSlot(slot);
			items.add(item);
			if (!(item.getItem() instanceof CompressedTankItem)) {
				continue;
			}
			FluidStack fluid = CompressedTankItem.getFluid(item);
			if (fluid.isEmpty()) {
				return new LegacyContents(List.of(), contents);
			}
			FluidStack matching = null;
			for (FluidStack existing : fluids) {
				if (FluidStack.isSameFluidSameComponents(existing, fluid)) {
					matching = existing;
					break;
				}
			}
			long amount = (long) fluid.getAmount() * item.getCount()
				+ (matching == null ? 0 : matching.getAmount());
			if (amount > Integer.MAX_VALUE) {
				return new LegacyContents(List.of(), contents);
			}
			if (matching == null) {
				if (fluids.size() == PackageFluidContents.MAX_FLUIDS) {
					return new LegacyContents(List.of(), contents);
				}
				fluids.add(fluid.copyWithAmount((int) amount));
			} else {
				matching.setAmount((int) amount);
			}
			items.set(slot, ItemStack.EMPTY);
		}
		return new LegacyContents(List.copyOf(fluids), ItemContainerContents.fromItems(items));
	}

	private static ItemContainerContents contents(ItemStack stack) {
		return stack.getOrDefault(com.simibubi.create.AllDataComponents.PACKAGE_CONTENTS,
			ItemContainerContents.EMPTY);
	}

	private record LegacyContents(List<FluidStack> fluids, ItemContainerContents items) {
	}
}
