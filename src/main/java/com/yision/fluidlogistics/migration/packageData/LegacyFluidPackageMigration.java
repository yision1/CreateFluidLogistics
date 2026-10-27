package com.yision.fluidlogistics.migration.packageData;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.box.PackageStyles;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.content.logistics.fluidPackage.CompressedTankItem;
import com.yision.fluidlogistics.content.logistics.fluidPackage.FluidPackageContentHelper;
import com.yision.fluidlogistics.content.logistics.fluidPackage.FluidPackageItem;
import com.yision.fluidlogistics.datacomponent.FluidTankContent;
import com.yision.fluidlogistics.datacomponent.PackageFluidContents;

import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.items.ItemStackHandler;

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
		ItemStackHandler contents = FluidPackageContentHelper.readRawContents(stack);
		for (int slot = 0; slot < contents.getSlots(); slot++) {
			if (contents.getStackInSlot(slot).getItem() instanceof CompressedTankItem) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasConflictingContents(ItemStack stack) {
		return stack != null && PackageFluidContents.has(stack) && hasLegacyContents(stack);
	}

	public static List<FluidStack> getContainedFluids(ItemStack stack) {
		if (stack == null || !PackageItem.isPackage(stack) || PackageFluidContents.has(stack)) {
			return List.of();
		}
		return inspect(stack).fluids();
	}

	public static ItemStack migrateCopy(ItemStack stack) {
		if (stack == null) {
			return ItemStack.EMPTY;
		}
		ItemStack result = stack.copy();
		if (!PackageItem.isPackage(result) || PackageFluidContents.has(result)) {
			return result;
		}
		LegacyContents legacy = inspect(result);
		if (legacy.fluids().isEmpty()) {
			return result;
		}
		boolean hasItems = false;
		for (int slot = 0; slot < legacy.items().getSlots(); slot++) {
			hasItems |= !legacy.items().getStackInSlot(slot).isEmpty();
		}
		if (FluidPackageItem.isFluidPackage(result) && (legacy.fluids().size() > 1 || hasItems)) {
			ItemStack ordinary = new ItemStack(PackageStyles.getDefaultBox().getItem(), result.getCount());
			ordinary.setTag(result.getTag() == null ? null : result.getTag().copy());
			result = ordinary;
		}
		PackageFluidContents.set(result,
			new PackageFluidContents(legacy.fluids().stream().map(FluidTankContent::new).toList()));
		if (!hasItems) {
			result.removeTagKey("Items");
		} else {
			result.getOrCreateTag().put("Items", legacy.items().serializeNBT());
		}
		return result;
	}

	private static LegacyContents inspect(ItemStack stack) {
		ItemStackHandler contents = FluidPackageContentHelper.readRawContents(stack);
		ItemStackHandler items = new ItemStackHandler(contents.getSlots());
		List<FluidStack> fluids = new ArrayList<>();
		for (int slot = 0; slot < contents.getSlots(); slot++) {
			ItemStack item = contents.getStackInSlot(slot);
			items.setStackInSlot(slot, item.copy());
			if (!(item.getItem() instanceof CompressedTankItem)) {
				continue;
			}
			FluidStack fluid = CompressedTankItem.getFluid(item);
			if (fluid.isEmpty()) {
				return new LegacyContents(List.of(), contents);
			}
			FluidStack matching = null;
			for (FluidStack existing : fluids) {
				if (existing.isFluidEqual(fluid)) {
					matching = existing;
					break;
				}
			}
			long amount = (long) fluid.getAmount() * item.getCount() + (matching == null ? 0 : matching.getAmount());
			if (amount > Integer.MAX_VALUE) {
				return new LegacyContents(List.of(), contents);
			}
			if (matching == null) {
				if (fluids.size() == PackageFluidContents.MAX_FLUIDS) {
					return new LegacyContents(List.of(), contents);
				}
				fluids.add(FluidHelper.copyStackWithAmount(fluid, (int) amount));
			} else {
				matching.setAmount((int) amount);
			}
			items.setStackInSlot(slot, ItemStack.EMPTY);
		}
		return new LegacyContents(List.copyOf(fluids), items);
	}

	private record LegacyContents(List<FluidStack> fluids, ItemStackHandler items) {
	}
}
