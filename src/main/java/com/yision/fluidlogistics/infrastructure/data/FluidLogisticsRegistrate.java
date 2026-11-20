package com.yision.fluidlogistics.infrastructure.data;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.api.registrate.CreateRegistrateRegistrationCallback;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.DataProviderInitializer;
import com.tterrag.registrate.providers.ProviderType;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

public final class FluidLogisticsRegistrate extends CreateRegistrate {
	private static final List<String> CREATIVE_BLOCK_ORDER = List.of(
		"copper_schematicannon",
		"oscillating_steam_engine",
		"mechanical_fluid_gun",
		"flow_meter",
		"redstone_fluid_valve",
		"fluid_pump",
		"fluid_hatch",
		"faucet",
		"smart_faucet",
		"smart_hopper",
		"fluid_transporter",
		"copper_basin",
		"copper_basket",
		"fluid_packager",
		"fluid_package",
		"fluid_repackager",
		"copper_frogport",
		"fluid_factory_gauge",
		"water_containing_copper_casing",
		"potato_server",
		"fluid_inventory_access_port",
		"multi_fluid_access_port",
		"multi_fluid_tank",
		"horizontal_multi_fluid_tank",
		"infinite_fluid_tank",
		"snow_golem_freezer",
		"package_list_filter",
		"empty_fluid_schematic",
		"copper_bucket",
		"hand_pointer",
		"phantom_chain");

	private final DataProviderInitializer dataGenInitializer = new LanglessDataProviderInitializer();

	private FluidLogisticsRegistrate(String modid) {
		super(modid);
	}

	public static FluidLogisticsRegistrate create(String modid) {
		FluidLogisticsRegistrate registrate = new FluidLogisticsRegistrate(modid);
		CreateRegistrateRegistrationCallback.provideRegistrate(registrate);
		return registrate;
	}

	@Override
	public DataProviderInitializer getDataGenInitializer() {
		return dataGenInitializer;
	}

	@Override
	protected void onBuildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
		super.onBuildCreativeModeTabContents(event);
		if (!event.getTabKey().location().getNamespace().equals(getModid())
			|| !event.getTabKey().location().getPath().equals("fluidlogistics_tab")) {
			return;
		}

		List<ItemStack> entries = new ArrayList<>(event.getParentEntries());
		entries.sort((first, second) -> Integer.compare(creativeOrder(first), creativeOrder(second)));
		entries.forEach(stack -> event.remove(stack, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY));
		entries.forEach(stack -> event.accept(stack, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY));
	}

	private int creativeOrder(ItemStack stack) {
		var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (!id.getNamespace().equals(getModid())) {
			return CREATIVE_BLOCK_ORDER.size();
		}
		int index = CREATIVE_BLOCK_ORDER.indexOf(id.getPath());
		return index < 0 ? CREATIVE_BLOCK_ORDER.size() : index;
	}

	private static final class LanglessDataProviderInitializer extends DataProviderInitializer {
		@Override
		protected List<Sorted> getSortedProviders() {
			return super.getSortedProviders().stream()
				.filter(provider -> provider.type() != ProviderType.LANG)
				.toList();
		}
	}
}
