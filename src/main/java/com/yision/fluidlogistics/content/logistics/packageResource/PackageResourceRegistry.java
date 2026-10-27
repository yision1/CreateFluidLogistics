package com.yision.fluidlogistics.content.logistics.packageResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import org.jetbrains.annotations.ApiStatus;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.yision.fluidlogistics.api.packager.PackageDestroyContext;
import com.yision.fluidlogistics.api.packager.PackageInspection;
import com.yision.fluidlogistics.api.packager.PackageResource;
import com.yision.fluidlogistics.api.packager.PackageResourceDisplay;
import com.yision.fluidlogistics.api.packager.PackageResourceType;
import com.yision.fluidlogistics.api.packager.PackageUnpackContext;
import com.yision.fluidlogistics.api.packager.PackageUnpackResult;
import com.yision.fluidlogistics.content.logistics.fluidPackage.FluidPackageContentHelper;
import com.yision.fluidlogistics.migration.item.LegacyFluidKeyMigration;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraftforge.items.ItemStackHandler;

@ApiStatus.Internal
public final class PackageResourceRegistry {
	private enum State {
		OPEN, BOOTSTRAPPING, FROZEN
	}

	private final Map<ResourceLocation, PackageResourceType> pendingTypes = new LinkedHashMap<>();
	private final List<RequestSelectorRegistration> pendingRequestSelectors = new ArrayList<>();
	private volatile Map<ResourceLocation, PackageResourceType> typesById = Map.of();
	private volatile Map<Item, PackageResourceType> typesByCarrier = Map.of();
	private volatile Map<Item, ItemStack> requestKeysBySelector = Map.of();
	private volatile State state = State.OPEN;

	public static PackageResourceRegistry create() {
		return new PackageResourceRegistry();
	}

	public synchronized void register(PackageResourceType type) {
		Objects.requireNonNull(type, "type");
		ResourceLocation id = Objects.requireNonNull(type.id(), "type.id");
		if (state != State.OPEN) {
			throw new IllegalStateException("cannot register package resource type after bootstrap: " + id);
		}
		if (pendingTypes.containsKey(id)) {
			throw new IllegalStateException("duplicate package resource type id: " + id);
		}
		pendingTypes.put(id, type);
	}

	public synchronized void registerRequestSelector(Supplier<? extends Item> selectorItem,
		Supplier<ItemStack> resourceKey) {
		Objects.requireNonNull(selectorItem, "selectorItem");
		Objects.requireNonNull(resourceKey, "resourceKey");
		if (state != State.OPEN) {
			throw new IllegalStateException("cannot register package resource request selector after bootstrap");
		}
		pendingRequestSelectors.add(new RequestSelectorRegistration(selectorItem, resourceKey));
	}

	public synchronized void bootstrap() {
		if (state == State.FROZEN) {
			return;
		}
		if (state != State.OPEN) {
			throw new IllegalStateException("package resource registry is already bootstrapping");
		}
		state = State.BOOTSTRAPPING;
		try {
			Map<ResourceLocation, PackageResourceType> ids = new LinkedHashMap<>();
			IdentityHashMap<Item, PackageResourceType> carriers = new IdentityHashMap<>();
			IdentityHashMap<Item, ItemStack> requestKeys = new IdentityHashMap<>();
			for (Map.Entry<ResourceLocation, PackageResourceType> entry : pendingTypes.entrySet()) {
				PackageResourceType type = entry.getValue();
				ResourceLocation id = Objects.requireNonNull(type.id(), "type.id");
				if (!entry.getKey().equals(id)) {
					throw new IllegalStateException(
						"package resource type id changed during bootstrap: " + entry.getKey());
				}
				Objects.requireNonNull(type.display(), "display for " + id);
				for (Item carrier : type.keyItems()) {
					Objects.requireNonNull(carrier, "resource key item for " + id);
					if (carrier == Items.AIR || new ItemStack(carrier).getMaxStackSize() != 1) {
						throw new IllegalStateException("invalid resource key item for " + id);
					}
					PackageResourceType duplicate = carriers.put(carrier, type);
					if (duplicate != null) {
						throw new IllegalStateException(
							"duplicate resource key item for " + duplicate.id() + " and " + id);
					}
				}
				ids.put(id, type);
			}
			for (RequestSelectorRegistration registration : pendingRequestSelectors) {
				Item selector = Objects.requireNonNull(registration.selectorItem().get(), "request selector item");
				if (selector == Items.AIR || carriers.containsKey(selector)) {
					throw new IllegalStateException("invalid package resource request selector item");
				}
				ItemStack suppliedKey = Objects.requireNonNull(registration.resourceKey().get(),
					"request selector resource key");
				PackageResourceType type = suppliedKey.isEmpty() ? null : carriers.get(suppliedKey.getItem());
				if (type == null || !type.isValidCarrier(suppliedKey)) {
					throw new IllegalStateException("request selector maps to an unregistered resource key");
				}
				ItemStack normalizedKey = validateNormalizedKey(type, type.normalizeKey(suppliedKey.copy()), carriers);
				if (requestKeys.put(selector, normalizedKey) != null) {
					throw new IllegalStateException("duplicate package resource request selector item");
				}
			}
			typesById = Collections.unmodifiableMap(new LinkedHashMap<>(ids));
			typesByCarrier = Collections.unmodifiableMap(new IdentityHashMap<>(carriers));
			requestKeysBySelector = Collections.unmodifiableMap(new IdentityHashMap<>(requestKeys));
			pendingTypes.clear();
			pendingRequestSelectors.clear();
			state = State.FROZEN;
		} catch (RuntimeException | Error exception) {
			state = State.OPEN;
			throw exception;
		}
	}

	public boolean isBootstrapped() {
		return state == State.FROZEN;
	}

	public Optional<PackageResourceType> get(ResourceLocation id) {
		ensureFrozen();
		return Optional.ofNullable(typesById.get(Objects.requireNonNull(id, "id")));
	}

	public Optional<PackageResourceType> findType(ItemStack carrierStack) {
		ensureFrozen();
		if (carrierStack == null || carrierStack.isEmpty()) {
			return Optional.empty();
		}
		PackageResourceType type = typesByCarrier.get(carrierStack.getItem());
		if (type == null || !type.isValidCarrier(carrierStack)) {
			return Optional.empty();
		}
		return Optional.of(type);
	}

	public Optional<PackageResource> readResource(ItemStack carrierStack) {
		return findType(carrierStack).filter(type -> type.isPhysicalCarrier(carrierStack))
			.map(type -> readResource(type, carrierStack.copy()));
	}

	private PackageResource readResource(PackageResourceType type, ItemStack carrierStack) {
		int amount = type.amountOf(carrierStack);
		if (amount <= 0) {
			throw new IllegalArgumentException("resource amount must be positive for " + type.id());
		}
		ItemStack key = validateNormalizedKey(type, type.normalizeKey(carrierStack.copy()));
		return new PackageResource(type.id(), key, amount);
	}

	public Optional<ItemStack> normalizeKey(ItemStack carrierStack) {
		return findType(carrierStack).map(type -> validateNormalizedKey(type, type.normalizeKey(carrierStack.copy())));
	}

	public Optional<PackageResourceKey> keyOf(ItemStack carrierStack) {
		Optional<PackageResourceType> typeResult = findType(carrierStack);
		if (typeResult.isEmpty()) {
			return Optional.empty();
		}
		PackageResourceType type = typeResult.orElseThrow();
		ItemStack key = validateNormalizedKey(type, type.normalizeKey(carrierStack.copy()));
		return Optional.of(new PackageResourceKey(type, key));
	}

	public Optional<ItemStack> resolveRequestKey(ItemStack carrierOrSelector) {
		ensureFrozen();
		if (carrierOrSelector == null || carrierOrSelector.isEmpty()) {
			return Optional.empty();
		}
		Optional<ItemStack> normalizedCarrier = normalizeKey(carrierOrSelector);
		if (normalizedCarrier.isPresent()) {
			return normalizedCarrier;
		}
		ItemStack key = requestKeysBySelector.get(carrierOrSelector.getItem());
		return key == null ? Optional.empty() : Optional.of(key.copy());
	}

	public boolean sameResource(ItemStack first, ItemStack second) {
		Optional<PackageResourceType> firstTypeResult = findType(first);
		Optional<PackageResourceType> secondTypeResult = findType(second);
		if (firstTypeResult.isEmpty() || secondTypeResult.isEmpty()) {
			return false;
		}
		PackageResourceType firstType = firstTypeResult.orElseThrow();
		PackageResourceType secondType = secondTypeResult.orElseThrow();
		if (firstType != secondType) {
			return false;
		}
		ItemStack firstKey = validateNormalizedKey(firstType, firstType.normalizeKey(first.copy()));
		ItemStack secondKey = validateNormalizedKey(secondType, secondType.normalizeKey(second.copy()));
		return firstType.matches(firstKey.copy(), secondKey.copy());
	}

	public PackageInspection inspectPackage(ItemStack packageStack) {
		ensureFrozen();
		Objects.requireNonNull(packageStack, "packageStack");
		return analyzePackage(packageStack).inspection();
	}

	public ItemStack createPackage(ItemStack normalizedKey, int amount) {
		ensureFrozen();
		if (amount <= 0 || amount > BigItemStack.INF) {
			throw new IllegalArgumentException("resource amount must be positive");
		}
		PackageResourceType type = findType(normalizedKey)
			.orElseThrow(() -> new IllegalArgumentException("unregistered or invalid resource key"));
		ItemStack key = validateNormalizedKey(type, type.normalizeKey(normalizedKey.copy()));
		int maximum = type.maxPerPackage(key.copy());
		if (maximum <= 0 || amount > maximum) {
			throw new IllegalArgumentException("resource amount exceeds package capacity for " + type.id());
		}
		return validateCreatedPackage(type, key, amount, type.createPackage(key.copy(), amount));
	}

	public List<ItemStack> splitPackage(ItemStack packageStack) {
		ensureFrozen();
		Objects.requireNonNull(packageStack, "packageStack");
		if (!PackageItem.isPackage(packageStack)) {
			return List.of();
		}
		if (packageStack.getCount() != 1) {
			throw new IllegalArgumentException("Split one package at a time");
		}
		for (PackageResourceType type : typesById.values()) {
			packageStack = type.migratePackage(packageStack);
		}
		packageStack = LegacyFluidKeyMigration.normalizePackageOrder(packageStack);
		PackageAnalysis analysis = analyzePackage(packageStack);
		validatePackage(packageStack);
		analysis.requireExactPhysicalAmounts();
		PackageInspection inspection = analysis.inspection();
		if (!inspection.hasResources() || inspection.canonical() && inspection.resources().stream().allMatch(
			resource -> resource.amount() <= get(resource.typeId()).orElseThrow().maxPerPackage(resource.key()))) {
			return List.of(packageStack.copyWithCount(1));
		}

		List<ItemStack> output = new ArrayList<>();
		for (PackageResource resource : inspection.resources()) {
			PackageResourceType type = get(resource.typeId()).orElseThrow();
			ItemStack key = resource.key();
			int maximum = type.maxPerPackage(key.copy());
			if (maximum <= 0) {
				throw new IllegalStateException("non-positive package capacity for " + type.id());
			}
			int remaining = resource.amount();
			while (remaining > 0) {
				int amount = Math.min(remaining, maximum);
				output.add(validateCreatedPackage(type, key, amount, type.createPackage(key.copy(), amount)));
				remaining -= amount;
			}
		}
		output.addAll(createOrdinaryPackages(inspection.ordinaryItems()));
		applyMetadata(packageStack, output);
		return List.copyOf(output);
	}

	public boolean unpackPackage(PackageUnpackContext context, boolean simulate) {
		if (!unpackPackageResult(context, true).complete()) {
			return false;
		}
		return simulate || unpackPackageResult(context, false).complete();
	}

	public PackageUnpackResult unpackPackageResult(PackageUnpackContext context, boolean simulate) {
		ensureFrozen();
		Objects.requireNonNull(context, "context");
		if (context.packageStack().getCount() != 1) {
			return new PackageUnpackResult(0, context.packageStack());
		}
		validatePackage(context.packageStack());
		PackageAnalysis analysis = analyzePackage(context.packageStack());
		analysis.requireExactPhysicalAmounts();
		PackageInspection inspection = analysis.inspection();
		if (inspection.isMixed() || inspection.hasOrdinaryItems() || inspection.resources().size() != 1) {
			return new PackageUnpackResult(0, context.packageStack());
		}
		PackageResource resource = inspection.resources().get(0);
		PackageResourceType type = get(resource.typeId()).orElseThrow();
		int transferred = type.unpackAmount(context, resource, simulate);
		if (transferred < 0 || transferred > resource.amount()) {
			throw new IllegalStateException("invalid transferred amount for " + type.id());
		}
		ItemStack remainder = transferred == resource.amount() ? ItemStack.EMPTY
			: transferred == 0 ? context.packageStack()
				: withPackageAmount(context.packageStack(), resource.key(), resource.amount() - transferred);
		return new PackageUnpackResult(transferred, remainder);
	}

	public ItemStack withPackageAmount(ItemStack packageStack, ItemStack key, int amount) {
		PackageResourceType type = findType(key).orElseThrow();
		if (amount <= 0) {
			throw new IllegalArgumentException("remaining amount must be positive");
		}
		ItemStack output = type.withPackageAmount(packageStack.copy(), key.copy(), amount);
		copyMetadata(packageStack, output);
		return output;
	}

	private void validatePackage(ItemStack packageStack) {
		for (PackageResourceType type : typesById.values()) {
			type.validatePackage(packageStack);
		}
	}

	public Optional<PackageResourceDisplay> displayOf(ItemStack carrierOrKey) {
		return findType(carrierOrKey).map(PackageResourceType::display);
	}

	public Optional<Component> nameOf(ItemStack carrierOrKey) {
		return withDisplayKey(carrierOrKey)
			.map(displayKey -> Objects.requireNonNull(displayKey.display().name(displayKey.key().copy()),
				"resource name for " + displayKey.type().id()).copy());
	}

	public Optional<ItemStack> iconOf(ItemStack carrierOrKey) {
		return withDisplayKey(carrierOrKey).map(displayKey -> {
			ItemStack icon = Objects.requireNonNull(displayKey.display().icon(displayKey.key().copy()),
				"resource icon for " + displayKey.type().id());
			if (icon.isEmpty()) {
				throw new IllegalStateException("resource icon must not be empty for " + displayKey.type().id());
			}
			return icon.copyWithCount(1);
		});
	}

	public Optional<List<Component>> tooltipOf(ItemStack carrierOrKey, int amount, boolean advanced) {
		return withDisplayKey(carrierOrKey).map(displayKey -> copyTooltip(
			displayKey.display().tooltip(tooltipKey(carrierOrKey, displayKey), amount, advanced)));
	}

	public Optional<List<Component>> tooltipOf(ItemStack carrierOrKey, int amount, boolean advanced,
		PackageResourceDisplay.TooltipContext context) {
		Objects.requireNonNull(context, "context");
		return withDisplayKey(carrierOrKey).map(displayKey -> copyTooltip(
			displayKey.display().tooltip(tooltipKey(carrierOrKey, displayKey), amount, advanced, context)));
	}

	private static ItemStack tooltipKey(ItemStack original, DisplayKey displayKey) {
		return original.is(displayKey.key().getItem()) ? original.copyWithCount(1) : displayKey.key().copy();
	}

	private static List<Component> copyTooltip(List<Component> tooltip) {
		return Objects.requireNonNull(tooltip, "resource tooltip").stream()
			.<Component>map(line -> Objects.requireNonNull(line, "resource tooltip line").copy()).toList();
	}

	public Optional<String> formatAmount(ItemStack carrierOrKey, int amount, PackageResourceDisplay.Format format) {
		Objects.requireNonNull(format, "format");
		Optional<ItemStack> keyResult = normalizeKey(carrierOrKey);
		if (keyResult.isEmpty()) {
			return Optional.empty();
		}
		ItemStack key = keyResult.orElseThrow();
		PackageResourceType type = findType(key).orElseThrow();
		return Optional.of(Objects.requireNonNull(type.display().format(key.copy(), amount, format),
			"formatted amount for " + type.id()));
	}

	private Optional<DisplayKey> withDisplayKey(ItemStack carrierOrKey) {
		Optional<ItemStack> keyResult = normalizeKey(carrierOrKey);
		if (keyResult.isEmpty()) {
			return Optional.empty();
		}
		ItemStack key = keyResult.orElseThrow();
		PackageResourceType type = findType(key).orElseThrow();
		return Optional.of(new DisplayKey(type, type.display(), key));
	}

	public OptionalInt adjustAmount(ItemStack carrierOrKey, PackageResourceDisplay.Adjustment adjustment) {
		Objects.requireNonNull(adjustment, "adjustment");
		Optional<ItemStack> keyResult = normalizeKey(carrierOrKey);
		if (keyResult.isEmpty()) {
			return OptionalInt.empty();
		}
		ItemStack key = keyResult.orElseThrow();
		PackageResourceType type = findType(key).orElseThrow();
		int adjusted = type.display().adjust(key.copy(), adjustment);
		if (adjusted < adjustment.minAmount() || adjusted > adjustment.maxAmount()) {
			throw new IllegalStateException("resource display adjustment outside bounds for " + type.id());
		}
		return OptionalInt.of(adjusted);
	}

	public boolean blocksManualOpen(ItemStack packageStack) {
		return inspectPackage(packageStack).hasResources();
	}

	public PackageResourceType.SawAction sawAction(ItemStack stack) {
		PackageInspection inspection = inspectPackage(stack);
		for (PackageResourceType type : typesIn(inspection)) {
			PackageResourceType.SawAction action = type.sawAction(stack.copy(), inspection);
			if (action != PackageResourceType.SawAction.DEFAULT) {
				return action;
			}
		}
		return PackageResourceType.SawAction.DEFAULT;
	}

	public boolean survivesWater(ItemStack packageStack) {
		PackageInspection inspection = inspectPackage(packageStack);
		for (PackageResourceType type : typesIn(inspection)) {
			if (type.survivesWater(packageStack.copy(), inspection)) {
				return true;
			}
		}
		return false;
	}

	public Set<ResourceLocation> handleDestroyed(PackageDestroyContext context) {
		ensureFrozen();
		Objects.requireNonNull(context, "context");
		PackageInspection inspection = inspectPackage(context.packageStack());
		Map<ResourceLocation, List<PackageResource>> grouped = new LinkedHashMap<>();
		Set<ResourceLocation> consumed = new LinkedHashSet<>();
		for (PackageResource resource : inspection.resources()) {
			grouped.computeIfAbsent(resource.typeId(), ignored -> new ArrayList<>()).add(resource);
		}
		grouped.forEach((id, resources) -> {
			if (get(id).orElseThrow().onDestroyed(context,
				List.copyOf(resources)) == PackageResourceType.DropAction.CONSUME_CARRIERS)
				consumed.add(id);
		});
		return Set.copyOf(consumed);
	}

	private boolean isCanonical(ItemStack packageStack, List<ResourceGroup> groups, List<ItemStack> ordinaryItems) {
		if (groups.size() != 1 || !ordinaryItems.isEmpty()) {
			return false;
		}
		ResourceGroup group = groups.get(0);
		Optional<PackageResource> canonical = group.type.readCanonicalPackage(packageStack.copyWithCount(1));
		if (canonical.isEmpty()) {
			return false;
		}
		PackageResource candidate = canonical.orElseThrow();
		return candidate.typeId().equals(group.type.id()) && candidate.amount() == group.amount
			&& group.type.matches(group.key.copy(), validateNormalizedKey(group.type, candidate.key()));
	}

	private List<PackageResourceType> typesIn(PackageInspection inspection) {
		LinkedHashSet<ResourceLocation> ids = new LinkedHashSet<>();
		for (PackageResource resource : inspection.resources()) {
			ids.add(resource.typeId());
		}
		return ids.stream().map(id -> get(id).orElseThrow()).toList();
	}

	private PackageAnalysis analyzePackage(ItemStack packageStack) {
		if (!PackageItem.isPackage(packageStack)) {
			return new PackageAnalysis(packageStack, List.of(), List.of(), false);
		}
		List<ResourceGroup> groups = new ArrayList<>();
		List<ItemStack> ordinaryItems = new ArrayList<>();
		for (PackageResourceType type : typesById.values()) {
			for (PackageResource resource : type.readPackageResources(packageStack.copy())) {
				if (!resource.typeId().equals(type.id()) || resource.amount() <= 0) {
					throw new IllegalStateException("invalid package resource for " + type.id());
				}
				groups.add(new ResourceGroup(type, validateNormalizedKey(type, resource.key()), resource.amount()));
			}
		}
		ItemStackHandler contents = FluidPackageContentHelper.readRawContents(packageStack);
		for (int slot = 0; slot < contents.getSlots(); slot++) {
			ItemStack item = contents.getStackInSlot(slot);
			if (item.isEmpty()) {
				continue;
			}
			ItemStack stack = item.copy();
			Optional<PackageResourceType> typeResult = findType(stack);
			if (typeResult.isEmpty() || !typeResult.orElseThrow().isPhysicalCarrier(stack)) {
				ordinaryItems.add(stack);
				continue;
			}
			PackageResourceType type = typeResult.orElseThrow();
			if (type.packageComponents().stream()
				.anyMatch(key -> packageStack.hasTag() && packageStack.getTag().contains(key))) {
				ordinaryItems.add(stack);
				continue;
			}
			PackageResource resource = readResource(type, stack);
			ResourceGroup matching = null;
			for (ResourceGroup group : groups) {
				if (group.type == type && type.matches(group.key.copy(), resource.key())) {
					matching = group;
					break;
				}
			}
			if (matching == null) {
				groups.add(new ResourceGroup(type, resource.key(), resource.amount()));
				continue;
			}
			matching.amount = Math.addExact(matching.amount, resource.amount());
		}
		return new PackageAnalysis(packageStack, groups, ordinaryItems,
			isCanonical(packageStack, groups, ordinaryItems));
	}

	private static List<ItemStack> createOrdinaryPackages(List<ItemStack> ordinaryItems) {
		List<ItemStack> output = new ArrayList<>();
		ItemStackHandler target = new ItemStackHandler(PackageItem.SLOTS);
		int slot = 0;
		for (ItemStack item : ordinaryItems) {
			int remaining = item.getCount();
			int maximum = item.getMaxStackSize();
			while (remaining > 0) {
				int amount = Math.min(remaining, maximum);
				target.setStackInSlot(slot++, item.copyWithCount(amount));
				remaining -= amount;
				if (slot == PackageItem.SLOTS) {
					output.add(PackageItem.containing(target));
					target = new ItemStackHandler(PackageItem.SLOTS);
					slot = 0;
				}
			}
		}
		if (slot > 0) {
			output.add(PackageItem.containing(target));
		}
		return output;
	}

	private void applyMetadata(ItemStack input, List<ItemStack> output) {
		if (output.isEmpty()) {
			throw new IllegalStateException("resource package split produced no output");
		}
		String address = PackageItem.getAddress(input);
		PackageOrderWithCrafts context = PackageItem.getOrderContext(input);
		int originalOrderId = PackageItem.getOrderId(input);
		int orderId;
		do {
			orderId = ThreadLocalRandom.current().nextInt();
		} while (orderId == originalOrderId);
		for (int index = 0; index < output.size(); index++) {
			ItemStack packageStack = output.get(index);
			copyMetadata(input, packageStack);
			PackageItem.clearAddress(packageStack);
			PackageItem.addAddress(packageStack, address);
			boolean last = index == output.size() - 1;
			PackageItem.setOrder(packageStack, orderId, 0, true, index, last, last ? context : null);
		}
	}

	private void copyMetadata(ItemStack input, ItemStack output) {
		if (!input.hasTag()) {
			return;
		}
		CompoundTag metadata = input.getTag().copy();
		metadata.remove("Items");
		for (PackageResourceType type : typesById.values()) {
			type.packageComponents().forEach(metadata::remove);
		}
		output.getOrCreateTag().merge(metadata);
	}

	private ItemStack validateNormalizedKey(PackageResourceType type, ItemStack key) {
		return validateNormalizedKey(type, key, typesByCarrier);
	}

	private static ItemStack validateNormalizedKey(PackageResourceType type, ItemStack key,
		Map<Item, PackageResourceType> carriers) {
		Objects.requireNonNull(key, "normalized key for " + type.id());
		if (key.isEmpty() || key.getCount() != 1 || carriers.get(key.getItem()) != type || !type.isValidCarrier(key)) {
			throw new IllegalStateException("invalid normalized key for " + type.id());
		}
		return key.copyWithCount(1);
	}

	private ItemStack validateCreatedPackage(PackageResourceType type, ItemStack key, int amount,
		ItemStack packageStack) {
		Objects.requireNonNull(packageStack, "package created by " + type.id());
		if (!PackageItem.isPackage(packageStack) || packageStack.getCount() != 1) {
			throw new IllegalStateException("invalid package created by " + type.id());
		}
		PackageInspection inspection = analyzePackage(packageStack).inspection();
		if (!inspection.canonical() || inspection.hasOrdinaryItems() || inspection.resources().size() != 1) {
			throw new IllegalStateException("package contents mismatch for " + type.id());
		}
		PackageResource inspected = inspection.resources().get(0);
		if (!inspected.typeId().equals(type.id()) || inspected.amount() != amount
			|| !type.matches(key.copy(), inspected.key())) {
			throw new IllegalStateException("package contents mismatch for " + type.id());
		}
		return packageStack.copyWithCount(1);
	}

	private void ensureFrozen() {
		if (state != State.FROZEN) {
			throw new IllegalStateException("package resource registry has not been bootstrapped");
		}
	}

	private static final class ResourceGroup {
		private final PackageResourceType type;
		private final ItemStack key;
		private long amount;

		private ResourceGroup(PackageResourceType type, ItemStack key, long amount) {
			this.type = type;
			this.key = key.copy();
			this.amount = amount;
		}
	}

	private static final class PackageAnalysis {
		private final ItemStack packageStack;
		private final List<ResourceGroup> groups;
		private final List<ItemStack> ordinaryItems;
		private final boolean canonical;

		private PackageAnalysis(ItemStack packageStack, List<ResourceGroup> groups, List<ItemStack> ordinaryItems,
			boolean canonical) {
			this.packageStack = packageStack;
			this.groups = groups;
			this.ordinaryItems = ordinaryItems;
			this.canonical = canonical;
		}

		private PackageInspection inspection() {
			List<PackageResource> resources = groups.stream().map(group -> new PackageResource(group.type.id(),
				group.key, (int) Math.min(Integer.MAX_VALUE, group.amount))).toList();
			return new PackageInspection(packageStack, resources, ordinaryItems, canonical);
		}

		private void requireExactPhysicalAmounts() {
			for (ResourceGroup group : groups) {
				if (group.amount > Integer.MAX_VALUE) {
					throw new ArithmeticException(
						"physical package resource amount exceeds integer range for " + group.type.id());
				}
			}
		}
	}

	private record RequestSelectorRegistration(Supplier<? extends Item> selectorItem, Supplier<ItemStack> resourceKey) {
	}

	private record DisplayKey(PackageResourceType type, PackageResourceDisplay display, ItemStack key) {
	}
}
