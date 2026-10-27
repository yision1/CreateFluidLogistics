package com.yision.fluidlogistics.content.logistics.packageResource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.repackager.PackageRepackageHelper;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts.CraftingEntry;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.yision.fluidlogistics.api.packager.PackageInspection;
import com.yision.fluidlogistics.api.packager.PackageResource;
import com.yision.fluidlogistics.api.packager.PackageResourceType;
import com.yision.fluidlogistics.api.packager.PackageResources;
import com.yision.fluidlogistics.datacomponent.PackageFluidContents;
import com.yision.fluidlogistics.migration.item.LegacyFluidKeyMigration;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.items.ItemStackHandler;

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
				preparedOrders.put(completedOrderId, prepareOrder(completedOrderId, inspections));
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
		if (prepared == null) {
			return super.repack(orderId, random);
		}
		return prepared.stream().map(stack -> new BigItemStack(stack.stack.copy(), stack.count)).toList();
	}

	private List<BigItemStack> prepareOrder(int orderId, List<PackageInspection> inspections) {
		List<PackageInspection> sorted = new ArrayList<>(inspections);
		sorted.sort(Comparator
			.comparingInt((PackageInspection inspection) -> PackageItem.getLinkIndex(inspection.packageStack()))
			.thenComparingInt(inspection -> PackageItem.getIndex(inspection.packageStack())));
		List<BigItemStack> output = new ArrayList<>();
		Map<CompoundTag, List<ItemStack>> ordinaryPackages = new LinkedHashMap<>();
		PackageOrderWithCrafts context = null;
		for (PackageInspection inspection : sorted) {
			ItemStack source = inspection.packageStack();
			PackageOrderWithCrafts sourceContext = LegacyFluidKeyMigration
				.normalizeOrderContext(PackageItem.getOrderContext(source));
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
			Contents expected = new Contents();
			expected.add(inspection);
			Contents actual = new Contents();
			for (ItemStack packageStack : split) {
				if (!PackageItem.isPackage(packageStack) || packageStack.getCount() != 1) {
					throw new IllegalStateException("resource package split produced an invalid package");
				}
				PackageInspection result = PackageResources.inspectPackage(packageStack);
				if (result.hasResources() && (!result.canonical() || result.hasOrdinaryItems())) {
					throw new IllegalStateException("resource package split left a noncanonical payload");
				}
				actual.add(result);
				if (result.hasResources()) {
					output.add(new BigItemStack(packageStack.copyWithCount(1), 1));
				} else if (result.hasOrdinaryItems()) {
					ordinaryPackages.computeIfAbsent(metadataOf(packageStack, null), ignored -> new ArrayList<>())
						.add(packageStack.copyWithCount(1));
				}
			}
			if (!expected.matches(actual)) {
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

		List<BigItemStack> recipeOutput = new ArrayList<>();
		RandomSource random = RandomSource.create();
		for (var group : ordinaryPackages.entrySet()) {
			InventorySummary available = new InventorySummary();
			Contents expected = new Contents();
			for (ItemStack box : group.getValue()) {
				PackageInspection inspection = PackageResources.inspectPackage(box);
				inspection.ordinaryItems().forEach(available::add);
				expected.add(inspection);
			}
			List<BigItemStack> ordered = context == null ? List.of()
				: context.stacks().stream()
					.filter(
						item -> PackageResources.findType(item.stack).isEmpty() && available.getCountOf(item.stack) > 0)
					.map(item -> new BigItemStack(item.stack.copy(), item.count)).toList();
			if (ordered.isEmpty()) {
				ordered = available.getStacks().stream().map(item -> new BigItemStack(item.stack.copy(), item.count))
					.toList();
			}
			List<CraftingEntry> groupCrafts = new ArrayList<>();
			for (int index = 0; index < crafts.size(); index++) {
				int count = completeCrafts(available, ingredients.get(index), remainingCrafts[index]);
				if (count == 0) {
					continue;
				}
				groupCrafts.add(new CraftingEntry(crafts.get(index).pattern(), count));
				remainingCrafts[index] -= count;
				for (BigItemStack item : ingredients.get(index).getStacks()) {
					available.add(item.stack, -Math.multiplyExact(item.count, count));
				}
			}
			PackageOrderWithCrafts groupContext = LegacyFluidKeyMigration
				.normalizeOrderContext(new PackageOrderWithCrafts(new PackageOrder(ordered), groupCrafts));
			List<BigItemStack> repacked = repackOrdinaryGroup(orderId, group.getValue(), groupContext, random);
			Contents actual = new Contents();
			for (int index = 0; index < repacked.size(); index++) {
				BigItemStack entry = repacked.get(index);
				if (entry.count <= 0) {
					throw new IllegalStateException("ordinary repack produced a non-positive package count");
				}
				entry.stack.getOrCreateTag().merge(group.getKey());
				actual.add(PackageResources.inspectPackage(entry.stack), entry.count);
				if (index < groupCrafts.size()) {
					recipeOutput.add(entry);
					continue;
				}
				if (entry.count != 1) {
					throw new IllegalStateException("ordinary remainder has multiple packages");
				}
				output.add(entry);
			}
			if (!expected.matches(actual)) {
				throw new IllegalStateException("ordinary repack changed its contents");
			}
		}
		output = combinePackages(output);
		for (int index = 0; index < output.size(); index++) {
			boolean last = index == output.size() - 1;
			PackageItem.setOrder(output.get(index).stack, orderId, 0, true, index, last, last ? context : null);
		}
		recipeOutput.addAll(output);
		Contents expected = new Contents();
		sorted.forEach(expected::add);
		Contents actual = new Contents();
		for (BigItemStack entry : recipeOutput) {
			actual.add(PackageResources.inspectPackage(entry.stack), entry.count);
		}
		if (!expected.matches(actual)) {
			throw new IllegalStateException("resource repack changed its contents");
		}
		return List.copyOf(recipeOutput);
	}

	private static List<BigItemStack> combinePackages(List<BigItemStack> packages) {
		Map<CompoundTag, List<PackageInspection>> groups = new LinkedHashMap<>();
		for (BigItemStack entry : packages) {
			PackageInspection inspection = PackageResources.inspectPackage(entry.stack);
			PackageResourceType type = inspection.hasResources()
				? PackageResources.get(inspection.resources().get(0).typeId()).orElseThrow()
				: null;
			groups.computeIfAbsent(metadataOf(entry.stack, type), ignored -> new ArrayList<>()).add(inspection);
		}
		List<BigItemStack> output = new ArrayList<>();
		for (var group : groups.entrySet()) {
			List<CombinedPackage> combined = new ArrayList<>();
			Map<PackageResourceKey, ResourceTotal> totals = new LinkedHashMap<>();
			for (PackageInspection inspection : group.getValue()) {
				for (PackageResource resource : inspection.resources()) {
					PackageResourceKey key = PackageResources.keyOf(resource.key()).orElseThrow();
					totals.merge(key, new ResourceTotal(resource, resource.amount()), (first,
						second) -> new ResourceTotal(first.resource(), Math.addExact(first.amount(), second.amount())));
				}
			}
			for (var entry : totals.entrySet()) {
				PackageResource resource = entry.getValue().resource();
				PackageResourceType type = PackageResources.get(resource.typeId()).orElseThrow();
				int capacity = type.maxPerPackage(resource.key());
				long remaining = entry.getValue().amount();
				while (remaining > 0) {
					CombinedPackage target = combined.isEmpty() ? null : combined.get(combined.size() - 1);
					if (target == null || target.amount >= capacity
						|| target.resources.size() >= PackageFluidContents.MAX_FLUIDS) {
						target = new CombinedPackage();
						combined.add(target);
					}
					int transferred = (int) Math.min(remaining, capacity - target.amount);
					target.resources.add(new PackageResource(resource.typeId(), resource.key(), transferred));
					target.amount += transferred;
					remaining -= transferred;
				}
			}
			int packageIndex = 0;
			int itemSlot = 0;
			for (PackageInspection inspection : group.getValue()) {
				for (ItemStack item : inspection.ordinaryItems()) {
					if (packageIndex == combined.size()) {
						combined.add(new CombinedPackage());
					}
					combined.get(packageIndex).items.setStackInSlot(itemSlot++, item.copy());
					if (itemSlot == PackageItem.SLOTS) {
						packageIndex++;
						itemSlot = 0;
					}
				}
			}
			for (CombinedPackage contents : combined) {
				output.add(combinedPackage(contents.items, List.copyOf(contents.resources), group.getKey()));
			}
		}
		return output;
	}

	private static BigItemStack combinedPackage(ItemStackHandler items, List<PackageResource> resources,
		CompoundTag metadata) {
		ItemStack box = PackageItem.containing(items);
		box.getOrCreateTag().merge(metadata);
		Map<PackageResourceType, List<PackageResource>> byType = new LinkedHashMap<>();
		for (PackageResource resource : resources) {
			PackageResourceType type = PackageResources.get(resource.typeId()).orElseThrow();
			byType.computeIfAbsent(type, ignored -> new ArrayList<>()).add(resource);
		}
		byType.forEach((type, contents) -> type.setPackageResources(box, List.copyOf(contents)));
		return new BigItemStack(box, 1);
	}

	private static final class CombinedPackage {
		private final List<PackageResource> resources = new ArrayList<>();
		private final ItemStackHandler items = new ItemStackHandler(PackageItem.SLOTS);
		private int amount;
	}

	private record ResourceTotal(PackageResource resource, long amount) {
	}

	private List<BigItemStack> repackOrdinaryGroup(int orderId, List<ItemStack> packages,
		PackageOrderWithCrafts context, RandomSource random) {
		List<ItemStack> inputs = packages.stream().map(ItemStack::copy).toList();
		PackageItem.setOrder(inputs.get(inputs.size() - 1), orderId, 0, true, 0, true, context);
		List<ItemStack> original = collectedPackages.put(orderId, inputs);
		try {
			return super.repack(orderId, random);
		} finally {
			collectedPackages.put(orderId, original);
		}
	}

	private static int completeCrafts(InventorySummary available, InventorySummary ingredients, int maximum) {
		int count = maximum;
		for (BigItemStack ingredient : ingredients.getStacks()) {
			count = Math.min(count, available.getCountOf(ingredient.stack) / ingredient.count);
		}
		return count;
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
			inspection.ordinaryItems()
				.forEach(item -> items.add(item, Math.multiplyExact(item.getCount(), packageCount)));
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
