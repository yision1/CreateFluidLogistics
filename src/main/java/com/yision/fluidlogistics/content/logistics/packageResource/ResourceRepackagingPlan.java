package com.yision.fluidlogistics.content.logistics.packageResource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts.CraftingEntry;
import com.yision.fluidlogistics.api.packager.PackageInspection;
import com.yision.fluidlogistics.api.packager.PackageResource;
import com.yision.fluidlogistics.api.packager.PackageResourceType;
import com.yision.fluidlogistics.api.packager.PackageResources;
import com.yision.fluidlogistics.datacomponent.PackageFluidContents;
import com.yision.fluidlogistics.migration.item.LegacyFluidKeyMigration;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.items.ItemStackHandler;

final class ResourceRepackagingPlan {
	private final List<Group> groups = new ArrayList<>();
	private final Contents expected = new Contents();
	private PackageOrderWithCrafts context;

	ResourceRepackagingPlan(int orderId, List<PackageInspection> inspections) {
		List<PackageInspection> sorted = new ArrayList<>(inspections);
		sorted.sort(Comparator
			.comparingInt((PackageInspection inspection) -> PackageItem.getLinkIndex(inspection.packageStack()))
			.thenComparingInt(inspection -> PackageItem.getIndex(inspection.packageStack())));
		Map<CompoundTag, List<PackageInspection>> inputs = new LinkedHashMap<>();
		for (PackageInspection inspection : sorted) {
			expected.add(inspection);
			ItemStack source = inspection.packageStack();
			PackageOrderWithCrafts sourceContext = LegacyFluidKeyMigration.normalizeOrderContext(
				PackageItem.getOrderContext(source));
			if (sourceContext != null) {
				if (context != null && !context.write().equals(sourceContext.write())) {
					throw new IllegalArgumentException("conflicting resource order contexts");
				}
				context = sourceContext;
			}
			List<ItemStack> split = PackageResources.splitPackage(source);
			if (split.isEmpty()) {
				throw new IllegalStateException("resource package split produced no output");
			}
			Contents before = new Contents();
			before.add(inspection);
			Contents after = new Contents();
			for (ItemStack box : split) {
				if (!PackageItem.isPackage(box) || box.getCount() != 1) {
					throw new IllegalStateException("resource package split produced an invalid package");
				}
				PackageInspection result = PackageResources.inspectPackage(box);
				if (result.hasResources() && (!result.canonical() || result.hasOrdinaryItems())) {
					throw new IllegalStateException("resource package split left a noncanonical payload");
				}
				after.add(result);
				PackageResourceType type = result.hasResources()
					? PackageResources.get(result.resources().get(0).typeId()).orElseThrow() : null;
				inputs.computeIfAbsent(metadataOf(box, type), ignored -> new ArrayList<>()).add(result);
			}
			if (!before.matches(after)) {
				throw new IllegalStateException("resource package split changed its contents");
			}
		}

		List<CraftingEntry> crafts = context == null ? List.of() : context.orderedCrafts();
		List<InventorySummary> ingredients = new ArrayList<>();
		int[] remainingCrafts = new int[crafts.size()];
		for (int index = 0; index < crafts.size(); index++) {
			CraftingEntry craft = crafts.get(index);
			if (craft.count() < 0) {
				throw new IllegalArgumentException("negative crafting count");
			}
			InventorySummary required = new InventorySummary();
			for (BigItemStack item : craft.pattern().stacks()) {
				if (item.stack.isEmpty()) {
					continue;
				}
				if (PackageResources.findType(item.stack).isPresent()) {
					throw new IllegalArgumentException("resource ingredients cannot be repacked as crafting items");
				}
				required.add(item.stack.copyWithCount(1));
			}
			if (required.getStacks().isEmpty()) {
				throw new IllegalArgumentException("empty crafting pattern");
			}
			ingredients.add(required);
			remainingCrafts[index] = craft.count();
		}

		for (var input : inputs.entrySet()) {
			InventorySummary available = new InventorySummary();
			for (PackageInspection inspection : input.getValue()) {
				inspection.ordinaryItems().forEach(available::add);
			}
			List<BigItemStack> ordered = context == null ? List.of() : context.stacks().stream()
				.filter(item -> PackageResources.findType(item.stack).isEmpty()
					&& available.getCountOf(item.stack) > 0)
				.map(item -> new BigItemStack(item.stack.copy(), item.count))
				.toList();
			if (ordered.isEmpty()) {
				ordered = available.getStacks().stream()
					.map(item -> new BigItemStack(item.stack.copy(), item.count))
					.toList();
			}
			List<CraftingEntry> groupCrafts = new ArrayList<>();
			for (int index = 0; index < crafts.size(); index++) {
				int count = remainingCrafts[index];
				for (BigItemStack ingredient : ingredients.get(index).getStacks()) {
					count = Math.min(count, available.getCountOf(ingredient.stack) / ingredient.count);
				}
				if (count == 0) {
					continue;
				}
				groupCrafts.add(new CraftingEntry(crafts.get(index).pattern(), count));
				remainingCrafts[index] -= count;
				for (BigItemStack item : ingredients.get(index).getStacks()) {
					available.add(item.stack, -Math.multiplyExact(item.count, count));
				}
			}
			groups.add(new Group(orderId, input.getKey(), input.getValue(),
				new PackageOrderWithCrafts(new PackageOrder(ordered), groupCrafts)));
		}
	}

	List<Group> groups() {
		return groups;
	}

	List<BigItemStack> finish(int orderId, List<BigItemStack> recipes, List<BigItemStack> remainder) {
		for (int index = 0; index < remainder.size(); index++) {
			boolean last = index == remainder.size() - 1;
			PackageItem.setOrder(remainder.get(index).stack, orderId, 0, true, index, last, last ? context : null);
		}
		List<BigItemStack> result = new ArrayList<>(recipes);
		result.addAll(remainder);
		Contents actual = new Contents();
		for (BigItemStack entry : result) {
			if (entry.count <= 0 || !PackageItem.isPackage(entry.stack) || entry.stack.getCount() != 1) {
				throw new IllegalStateException("resource repack produced an invalid package");
			}
			actual.add(PackageResources.inspectPackage(entry.stack), entry.count);
		}
		if (!expected.matches(actual)) {
			throw new IllegalStateException("resource repack changed its contents");
		}
		return List.copyOf(result);
	}

	static final class Group {
		private final CompoundTag metadata;
		private final List<ItemStack> packages = new ArrayList<>();
		private final List<List<PackageResource>> resourcePackages = new ArrayList<>();
		private final int recipeCount;
		private int resourceIndex;

		private Group(int orderId, CompoundTag metadata, List<PackageInspection> inputs,
			PackageOrderWithCrafts context) {
			this.metadata = metadata;
			recipeCount = context.orderedCrafts().size();
			Map<PackageResourceKey, ResourceTotal> totals = new LinkedHashMap<>();
			for (PackageInspection input : inputs) {
				ItemStack box = input.packageStack();
				box.removeTagKey("Fragment");
				packages.add(box);
				for (PackageResource resource : input.resources()) {
					PackageResourceKey key = PackageResources.keyOf(resource.key()).orElseThrow();
					totals.merge(key, new ResourceTotal(resource, resource.amount()), (first, second) ->
						new ResourceTotal(first.resource(), Math.addExact(first.amount(), second.amount())));
				}
			}
			PackageItem.setOrder(packages.get(packages.size() - 1), orderId, 0, true, 0, true, context);
			List<PackageResource> target = null;
			int amount = 0;
			int capacity = Integer.MAX_VALUE;
			for (ResourceTotal total : totals.values()) {
				PackageResource resource = total.resource();
				int maximum = PackageResources.get(resource.typeId()).orElseThrow().maxPerPackage(resource.key());
				if (maximum <= 0) {
					throw new IllegalArgumentException("non-positive resource package capacity");
				}
				long remaining = total.amount();
				while (remaining > 0) {
					if (target == null || amount >= Math.min(capacity, maximum)
						|| target.size() >= PackageFluidContents.MAX_FLUIDS) {
						target = new ArrayList<>();
						resourcePackages.add(target);
						amount = 0;
						capacity = maximum;
					}
					capacity = Math.min(capacity, maximum);
					int transferred = (int) Math.min(remaining, capacity - amount);
					target.add(new PackageResource(resource.typeId(), resource.key(), transferred));
					amount += transferred;
					remaining -= transferred;
				}
			}
		}

		List<ItemStack> packages() {
			return packages;
		}

		void collect(List<BigItemStack> output, List<BigItemStack> recipes, List<BigItemStack> remainder) {
			for (int index = 0; index < output.size(); index++) {
				BigItemStack entry = output.get(index);
				entry.stack.getOrCreateTag().merge(metadata);
				if (index < recipeCount) {
					recipes.add(entry);
					continue;
				}
				if (entry.count != 1) {
					throw new IllegalStateException("ordinary remainder has multiple packages");
				}
				addResources(entry.stack);
				remainder.add(entry);
			}
			while (resourceIndex < resourcePackages.size()) {
				ItemStack box = PackageItem.containing(new ItemStackHandler(PackageItem.SLOTS));
				box.getOrCreateTag().merge(metadata);
				addResources(box);
				remainder.add(new BigItemStack(box, 1));
			}
		}

		private void addResources(ItemStack box) {
			if (resourceIndex >= resourcePackages.size()) {
				return;
			}
			Map<PackageResourceType, List<PackageResource>> byType = new LinkedHashMap<>();
			for (PackageResource resource : resourcePackages.get(resourceIndex++)) {
				PackageResourceType type = PackageResources.get(resource.typeId()).orElseThrow();
				byType.computeIfAbsent(type, ignored -> new ArrayList<>()).add(resource);
			}
			byType.forEach((type, resources) -> type.setPackageResources(box, List.copyOf(resources)));
		}
	}

	private record ResourceTotal(PackageResource resource, long amount) {
	}

	private static CompoundTag metadataOf(ItemStack packageStack, PackageResourceType type) {
		CompoundTag metadata = packageStack.hasTag() ? packageStack.getTag().copy() : new CompoundTag();
		metadata.remove("Items");
		metadata.remove("Fragment");
		if (PackageItem.getAddress(packageStack).isEmpty())
			metadata.remove("Address");
		if (type != null)
			type.packageComponents().forEach(metadata::remove);
		return metadata;
	}

	private static final class Contents {
		private final Map<PackageResourceKey, Long> resources = new HashMap<>();
		private final InventorySummary items = new InventorySummary();

		private void add(PackageInspection inspection) {
			add(inspection, 1);
		}

		private void add(PackageInspection inspection, int packageCount) {
			for (PackageResource resource : inspection.resources()) {
				PackageResourceKey key = PackageResources.keyOf(resource.key()).orElseThrow();
				resources.merge(key, Math.multiplyExact((long) resource.amount(), packageCount), Math::addExact);
			}
			inspection.ordinaryItems().forEach(item ->
				items.add(item, Math.multiplyExact(item.getCount(), packageCount)));
		}

		private boolean matches(Contents other) {
			if (!resources.equals(other.resources) || items.getStacks().size() != other.items.getStacks().size()) {
				return false;
			}
			for (BigItemStack stack : items.getStacks()) {
				if (other.items.getCountOf(stack.stack) != stack.count) {
					return false;
				}
			}
			return true;
		}
	}
}
