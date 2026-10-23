package com.yision.fluidlogistics.api.packager;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Contract;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public interface PackageResourceType {
	ResourceLocation id();

	Collection<Item> keyItems();

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

	default boolean isPhysicalCarrier(ItemStack stack) {
		return isValidCarrier(stack);
	}

	default List<PackageResource> readPackageResources(ItemStack packageStack) {
		return List.of();
	}

	default void setPackageResources(ItemStack packageStack, List<PackageResource> resources) {
		throw new UnsupportedOperationException("resource type does not support combined packages: " + id());
	}

	default Set<DataComponentType<?>> packageComponents() {
		return Set.of();
	}

	default void validatePackage(ItemStack packageStack) {
	}

	default ItemStack migratePackage(ItemStack packageStack) {
		return packageStack.copy();
	}

	int maxPerPackage(ItemStack normalizedKey);

	ItemStack createPackage(ItemStack normalizedKey, int amount);

	Optional<PackageResource> readCanonicalPackage(ItemStack packageStack);

	int unpackAmount(PackageUnpackContext context, PackageResource resource, boolean simulate);

	default ItemStack withPackageAmount(ItemStack packageStack, ItemStack key, int amount) {
		return createPackage(key, amount);
	}

	PackageResourceDisplay display();

	default boolean survivesWater(ItemStack packageStack, PackageInspection inspection) {
		return false;
	}

	default void onDestroyed(PackageDestroyContext context, List<PackageResource> resources) {
	}
}
