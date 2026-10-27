package com.yision.fluidlogistics.migration.block;

import com.simibubi.create.api.registry.CreateRegistries;
import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.registry.AllBlockEntities;
import com.yision.fluidlogistics.registry.AllBlocks;
import com.yision.fluidlogistics.registry.FluidLogisticsArmInteractionPointTypes;

import net.minecraft.core.registries.Registries;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.MissingMappingsEvent;

@Mod.EventBusSubscriber(modid = FluidLogistics.MODID)
public final class LegacyFreezerAliases {

	private LegacyFreezerAliases() {
	}

	@SubscribeEvent
	public static void remap(MissingMappingsEvent event) {
		for (var mapping : event.getMappings(Registries.BLOCK, FluidLogistics.MODID)) {
			if ((mapping.getKey().getPath().equals("blaze_cooler")
				|| mapping.getKey().getPath().equals("frostbite_freezer")))
				mapping.remap(AllBlocks.SNOW_GOLEM_FREEZER.get());
		}
		for (var mapping : event.getMappings(Registries.ITEM, FluidLogistics.MODID)) {
			if ((mapping.getKey().getPath().equals("blaze_cooler")
				|| mapping.getKey().getPath().equals("frostbite_freezer")))
				mapping.remap(AllBlocks.SNOW_GOLEM_FREEZER.get().asItem());
		}
		for (var mapping : event.getMappings(Registries.BLOCK_ENTITY_TYPE, FluidLogistics.MODID)) {
			if ((mapping.getKey().getPath().equals("blaze_cooler")
				|| mapping.getKey().getPath().equals("frostbite_freezer")))
				mapping.remap(AllBlockEntities.SNOW_GOLEM_FREEZER.get());
		}
		for (var mapping : event.getMappings(CreateRegistries.ARM_INTERACTION_POINT_TYPE, FluidLogistics.MODID)) {
			if ((mapping.getKey().getPath().equals("blaze_cooler")
				|| mapping.getKey().getPath().equals("frostbite_freezer")))
				mapping.remap(FluidLogisticsArmInteractionPointTypes.SNOW_GOLEM_FREEZER.get());
		}
	}
}
