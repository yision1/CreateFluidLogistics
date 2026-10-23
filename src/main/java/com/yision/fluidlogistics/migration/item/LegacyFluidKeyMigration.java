package com.yision.fluidlogistics.migration.item;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem.PackageOrderData;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts.CraftingEntry;
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
		return new PackageOrder(order.stacks().stream()
			.map(stack -> new BigItemStack(normalizeKey(stack.stack), stack.count))
			.toList());
	}

	@Nullable
	public static PackageOrderWithCrafts normalizeOrderContext(@Nullable PackageOrderWithCrafts context) {
		if (context == null) {
			return null;
		}
		return new PackageOrderWithCrafts(normalizeOrder(context.orderedStacks()),
			context.orderedCrafts().stream()
				.map(entry -> new CraftingEntry(normalizeOrder(entry.pattern()), entry.count()))
				.toList());
	}

	public static ItemStack normalizePackageOrder(ItemStack packageStack) {
		ItemStack result = packageStack.copy();
		PackageOrderData data = result.get(AllDataComponents.PACKAGE_ORDER_DATA);
		if (data != null && data.orderContext() != null) {
			result.set(AllDataComponents.PACKAGE_ORDER_DATA,
				new PackageOrderData(data.orderId(), data.linkIndex(), data.isFinalLink(), data.fragmentIndex(),
					data.isFinal(), normalizeOrderContext(data.orderContext())));
		}
		return result;
	}
}
