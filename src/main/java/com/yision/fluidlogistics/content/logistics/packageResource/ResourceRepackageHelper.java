package com.yision.fluidlogistics.content.logistics.packageResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.repackager.PackageRepackageHelper;
import com.yision.fluidlogistics.api.packager.PackageInspection;
import com.yision.fluidlogistics.api.packager.PackageResources;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

public final class ResourceRepackageHelper extends PackageRepackageHelper {
	private final Map<Integer, List<BigItemStack>> preparedOrders = new HashMap<>();
	private final Set<Integer> rejectedOrders = new HashSet<>();

	@Override
	public void clear() {
		super.clear();
		preparedOrders.clear();
		rejectedOrders.clear();
	}

	@Override
	public int addPackageFragment(ItemStack box) {
		int orderId = PackageItem.getOrderId(box);
		if (!PackageResources.isBootstrapped() || rejectedOrders.contains(orderId)) {
			return -1;
		}
		int completedOrderId = super.addPackageFragment(box.copyWithCount(1));
		if (completedOrderId == -1) {
			return -1;
		}

		try {
			List<PackageInspection> inspections = collectedPackages.get(completedOrderId).stream()
				.map(PackageResources::inspectPackage).toList();
			if (inspections.stream().anyMatch(PackageInspection::hasResources)) {
				preparedOrders.put(completedOrderId, prepareOrder(completedOrderId, inspections, RandomSource.create()));
			}
			return completedOrderId;
		} catch (RuntimeException exception) {
			rejectedOrders.add(completedOrderId);
			return -1;
		}
	}

	@Override
	public List<BigItemStack> repack(int orderId, RandomSource random) {
		if (rejectedOrders.contains(orderId)) {
			throw new IllegalStateException("resource order could not be prepared: " + orderId);
		}
		List<BigItemStack> prepared = preparedOrders.get(orderId);
		if (prepared != null) {
			return prepared.stream().map(stack -> new BigItemStack(stack.stack.copy(), stack.count)).toList();
		}
		if (!PackageResources.isBootstrapped()) {
			return super.repack(orderId, random);
		}
		List<PackageInspection> inspections = collectedPackages.get(orderId).stream()
			.map(PackageResources::inspectPackage).toList();
		return inspections.stream().anyMatch(PackageInspection::hasResources)
			? prepareOrder(orderId, inspections, random)
			: super.repack(orderId, random);
	}

	private List<BigItemStack> prepareOrder(int orderId, List<PackageInspection> inspections, RandomSource random) {
		ResourceRepackagingPlan plan = new ResourceRepackagingPlan(orderId, inspections);
		List<ItemStack> original = collectedPackages.get(orderId);
		List<BigItemStack> recipes = new ArrayList<>();
		List<BigItemStack> remainder = new ArrayList<>();
		try {
			for (var group : plan.groups()) {
				collectedPackages.put(orderId, group.packages());
				group.collect(super.repack(orderId, random), recipes, remainder);
			}
			return plan.finish(orderId, recipes, remainder);
		} finally {
			collectedPackages.put(orderId, original);
		}
	}
}
