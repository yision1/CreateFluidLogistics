package com.yision.fluidlogistics.migration.item;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts.CraftingEntry;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.yision.fluidlogistics.api.packager.PackageResources;

import net.minecraft.world.item.ItemStack;

public final class LegacyFluidKeyMigration {
	private LegacyFluidKeyMigration() {
	}

	public static ItemStack normalizeKey(ItemStack stack) {
		if (stack.isEmpty() || !PackageResources.isBootstrapped()) {
			return stack.copy();
		}
		return PackageResources.normalizeKey(stack).orElseGet(stack::copy);
	}

	public static PackageOrder normalizeOrder(PackageOrder order) {
		return new PackageOrder(
			order.stacks().stream().map(stack -> new BigItemStack(normalizeKey(stack.stack), stack.count)).toList());
	}

	@Nullable
	public static PackageOrderWithCrafts normalizeOrderContext(@Nullable PackageOrderWithCrafts context) {
		if (context == null) {
			return null;
		}
		return new PackageOrderWithCrafts(normalizeOrder(context.orderedStacks()), context.orderedCrafts().stream()
			.map(entry -> new CraftingEntry(normalizeOrder(entry.pattern()), entry.count())).toList());
	}

	public static ItemStack normalizePackageOrder(ItemStack packageStack) {
		ItemStack result = packageStack.copy();
		PackageOrderWithCrafts context = PackageItem.getOrderContext(result);
		if (context != null) {
			result.getOrCreateTagElement("Fragment").put("OrderContext", normalizeOrderContext(context).write());
		}
		return result;
	}

}
