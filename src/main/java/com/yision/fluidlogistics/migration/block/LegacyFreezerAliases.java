package com.yision.fluidlogistics.migration.block;

import com.simibubi.create.api.registry.CreateRegistries;
import com.yision.fluidlogistics.FluidLogistics;

import net.minecraft.core.registries.Registries;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.IRegistryExtension;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = FluidLogistics.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class LegacyFreezerAliases {

	private LegacyFreezerAliases() {
	}

	@SubscribeEvent
	public static void register(RegisterEvent event) {
		if (event.getRegistryKey().equals(Registries.BLOCK)
			|| event.getRegistryKey().equals(Registries.ITEM)
			|| event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)
			|| event.getRegistryKey().equals(CreateRegistries.ARM_INTERACTION_POINT_TYPE)) {
			((IRegistryExtension<?>) event.getRegistry()).addAlias(FluidLogistics.asResource("blaze_cooler"),
				FluidLogistics.asResource("snow_golem_freezer"));
			((IRegistryExtension<?>) event.getRegistry()).addAlias(FluidLogistics.asResource("frostbite_freezer"),
				FluidLogistics.asResource("snow_golem_freezer"));
		}
	}
}
