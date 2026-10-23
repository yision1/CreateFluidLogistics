package com.yision.fluidlogistics.migration.item;

import com.yision.fluidlogistics.FluidLogistics;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

import net.neoforged.neoforge.registries.IRegistryExtension;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class LegacyItemAliases {
	private LegacyItemAliases() {
	}

	@SuppressWarnings("unchecked")
	public static void register(RegisterEvent event) {
		var registry = event.getRegistry(Registries.ITEM);
		if (registry == null) {
			return;
		}

		((IRegistryExtension<Item>) registry).addAlias(
			FluidLogistics.asResource("rare_fluid_package"),
			FluidLogistics.asResource("fluid_package"));
		((IRegistryExtension<Item>) registry).addAlias(
			FluidLogistics.asResource("rare_fluid_package_1"),
			FluidLogistics.asResource("fluid_package"));
	}
}
