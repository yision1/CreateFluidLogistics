package com.yision.fluidlogistics.migration.item;

import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.registry.AllItems;

import net.minecraft.core.registries.Registries;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.MissingMappingsEvent;

@Mod.EventBusSubscriber(modid = FluidLogistics.MODID)
public final class LegacyItemAliases {
	private LegacyItemAliases() {
	}

	@SubscribeEvent
	public static void remap(MissingMappingsEvent event) {
		for (var mapping : event.getMappings(Registries.ITEM, FluidLogistics.MODID)) {
			String path = mapping.getKey().getPath();
			if (path.equals("rare_fluid_package") || path.equals("rare_fluid_package_1"))
				mapping.remap(AllItems.FLUID_PACKAGE.get());
		}
	}
}
