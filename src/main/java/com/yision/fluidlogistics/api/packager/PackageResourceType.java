package com.yision.fluidlogistics.api.packager;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.jetbrains.annotations.Contract;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public interface PackageResourceType {
	enum SawAction {
		DEFAULT, DESTROY_WITHOUT_DROPS
	}

	enum DropAction {
		PASS, CONSUME_CARRIERS
	}

	ResourceLocation id();

	default Supplier<? extends Item> carrierItem() {
		throw new UnsupportedOperationException("resource type uses multiple key items: " + id());
	}

	default Collection<Item> keyItems() {
		return List.of(carrierItem().get());
	}

	@Contract(pure = true)
	boolean isValidCarrier(ItemStack stack);

	ItemStack normalizeKey(ItemStack stack);

	boolean matches(ItemStack firstNormalizedKey, ItemStack secondNormalizedKey);

	@Contract(pure = true)
	default int identityHash(ItemStack normalizedKey) {
		return 0;
	}

	@Contract(pure = true)
	int amountOf(ItemStack carrierStack);

	default ItemStack createCarrier(ItemStack normalizedKey, int amount) {
		throw new UnsupportedOperationException("resource type does not use physical carriers: " + id());
	}

	default boolean isPhysicalCarrier(ItemStack stack) {
		return isValidCarrier(stack);
	}

	default List<PackageResource> readPackageResources(ItemStack packageStack) {
		return List.of();
	}

	default void setPackageResources(ItemStack packageStack, List<PackageResource> resources) {
		throw new UnsupportedOperationException("resource type does not support combined packages: " + id());
	}

	default Set<String> packageComponents() {
		return Set.of();
	}

	default void validatePackage(ItemStack packageStack) {
	}

	default ItemStack migratePackage(ItemStack packageStack) {
		return packageStack.copy();
	}

	default ItemStack withPackageAmount(ItemStack packageStack, ItemStack key, int amount) {
		return createPackage(key, amount);
	}

	int maxPerPackage(ItemStack normalizedKey);

	ItemStack createPackage(ItemStack normalizedKey, int amount);

	Optional<PackageResource> readCanonicalPackage(ItemStack packageStack);

	default boolean unpack(PackageUnpackContext context, PackageResource resource, boolean simulate) {
		return false;
	}

	default int unpackAmount(PackageUnpackContext context, PackageResource resource, boolean simulate) {
		return unpack(context, resource, simulate) ? resource.amount() : 0;
	}

	PackageResourceDisplay display();

	default boolean survivesWater(ItemStack packageStack, PackageInspection inspection) {
		return false;
	}

	default SawAction sawAction(ItemStack packageStack, PackageInspection inspection) {
		return SawAction.DEFAULT;
	}

	default DropAction onDestroyed(PackageDestroyContext context, List<PackageResource> resources) {
		return DropAction.PASS;
	}
}
