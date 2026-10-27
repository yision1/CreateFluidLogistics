package com.yision.fluidlogistics.registry;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.content.logistics.fluidPackage.FluidPackageItem;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class AllPartialModels {

	public static final PartialModel OSCILLATING_ENGINE_HOUSING = block("oscillating_steam_engine/block");
	public static final PartialModel OSCILLATING_ENGINE_CYLINDER = block("oscillating_steam_engine/cylinder");
	public static final PartialModel OSCILLATING_ENGINE_PISTON = block("oscillating_steam_engine/piston");
	public static final PartialModel OSCILLATING_ENGINE_LINKAGE = block("oscillating_steam_engine/linkage");
	public static final PartialModel OSCILLATING_ENGINE_CONNECTOR = block("oscillating_steam_engine/shaft_connector");

	public static final PartialModel FLOW_METER_HEAD = block("flow_meter/head");
	public static final PartialModel FLOW_METER_DIAL = block("flow_meter/dial");
	public static final PartialModel REDSTONE_FLUID_VALVE_POINTER = block("redstone_fluid_valve/pointer");

	public static final PartialModel FLUID_PACKAGER_TRAY = block("fluid_packager/tray");
	public static final PartialModel FLUID_PACKAGER_HATCH_OPEN = block("fluid_packager/hatch_open");
	public static final PartialModel FLUID_PACKAGER_HATCH_CLOSED = block("fluid_packager/hatch_closed");
	public static final PartialModel FLUID_REPACKAGER_TRAY = block("fluid_repackager/tray");
	public static final PartialModel COPPER_FROGPORT_BODY = block("copper_frogport/body");
	public static final PartialModel COPPER_FROGPORT_HEAD = block("copper_frogport/head");
	public static final PartialModel COPPER_FROGPORT_HEAD_GOGGLES = block("copper_frogport/head_goggles");
	public static final PartialModel COPPER_FROGPORT_TONGUE = block("copper_frogport/tongue");
	public static final Map<Direction, PartialModel> FAUCET_SOURCE_INTERFACE = new EnumMap<>(Direction.class);

	public static final PartialModel MECHANICAL_FLUID_GUN_BASE = block("mechanical_fluid_gun/base");
	public static final PartialModel MECHANICAL_FLUID_GUN_COG = block("mechanical_fluid_gun/cog");
	public static final PartialModel MECHANICAL_FLUID_GUN_GUN_BODY = block("mechanical_fluid_gun/gun_body");
	public static final PartialModel MECHANICAL_FLUID_GUN_GUNPOINT_TOP = block("mechanical_fluid_gun/top");
	public static final PartialModel MECHANICAL_FLUID_GUN_GUNPOINT_MIDDLE = block("mechanical_fluid_gun/middle");
	public static final PartialModel MECHANICAL_FLUID_GUN_GUNPOINT_BOTTOM = block("mechanical_fluid_gun/bottom");
	public static final PartialModel COPPER_SCHEMATICANNON_CONNECTOR = block("copper_schematicannon/connector");
	public static final PartialModel COPPER_SCHEMATICANNON_PIPE = block("copper_schematicannon/pipe");

	public static final PartialModel SNOW_GOLEM_FREEZER_CAGE = block("snow_golem_freezer/block");
	public static final PartialModel SNOW_GOLEM_FREEZER_INERT = block("snow_golem_freezer/snow_golem/inert");
	public static final PartialModel SNOW_GOLEM_FREEZER_IDLE = block("snow_golem_freezer/snow_golem/head");
	public static final PartialModel SNOW_GOLEM_FREEZER_PUMPKIN = block("snow_golem_freezer/snow_golem/pumpkin");
	public static final PartialModel SNOW_GOLEM_FREEZER_FLAME = block("snow_golem_freezer/flame");

	public static final PartialModel FLUID_PACKAGE = item("fluid_package");
	public static final PartialModel FLUID_PACKAGE_EXPOSED = item("fluid_package_exposed");
	public static final PartialModel FLUID_PACKAGE_OXIDIZED = item("fluid_package_oxidized");
	public static final PartialModel FLUID_PACKAGE_WEATHERED = item("fluid_package_weathered");
	public static final PartialModel RARE_PIG_PACKAGE = item("rare_pig_package");
	public static final PartialModel RARE_FOX_PACKAGE = item("rare_fox_package");
	public static final PartialModel FLUID_PACKAGE_RIGGING = PartialModel
		.of(FluidPackageItem.FLUID_STYLE.getRiggingModel());
	public static final PartialModel RARE_FLUID_PACKAGE_RIGGING = PartialModel
		.of(FluidPackageItem.RARE_PIG_STYLE.getRiggingModel());

	private static final ResourceLocation FLUID_PACKAGE_ID = FluidLogistics.asResource("fluid_package");
	private static final ResourceLocation FLUID_PACKAGE_EXPOSED_ID = FluidLogistics.asResource("fluid_package_exposed");
	private static final ResourceLocation FLUID_PACKAGE_OXIDIZED_ID = FluidLogistics
		.asResource("fluid_package_oxidized");
	private static final ResourceLocation FLUID_PACKAGE_WEATHERED_ID = FluidLogistics
		.asResource("fluid_package_weathered");
	private static final ResourceLocation RARE_PIG_PACKAGE_ID = FluidLogistics.asResource("rare_pig_package");
	private static final ResourceLocation RARE_FOX_PACKAGE_ID = FluidLogistics.asResource("rare_fox_package");

	private static boolean registered = false;

	static {
		FAUCET_SOURCE_INTERFACE.put(Direction.NORTH, block("faucet/source_interface/north"));
		FAUCET_SOURCE_INTERFACE.put(Direction.SOUTH, block("faucet/source_interface/south"));
		FAUCET_SOURCE_INTERFACE.put(Direction.EAST, block("faucet/source_interface/east"));
		FAUCET_SOURCE_INTERFACE.put(Direction.WEST, block("faucet/source_interface/west"));
	}

	private static PartialModel block(String path) {
		return PartialModel.of(ResourceLocation.fromNamespaceAndPath(FluidLogistics.MODID, "block/" + path));
	}

	private static PartialModel item(String path) {
		return PartialModel.of(ResourceLocation.fromNamespaceAndPath(FluidLogistics.MODID, "item/" + path));
	}

	public static List<ResourceLocation> customModelLocations() {
		return List.of(
			OSCILLATING_ENGINE_HOUSING.modelLocation(),
			OSCILLATING_ENGINE_CYLINDER.modelLocation(),
			OSCILLATING_ENGINE_PISTON.modelLocation(),
			OSCILLATING_ENGINE_LINKAGE.modelLocation(),
			OSCILLATING_ENGINE_CONNECTOR.modelLocation(),
			FLOW_METER_HEAD.modelLocation(),
			FLOW_METER_DIAL.modelLocation(),
			REDSTONE_FLUID_VALVE_POINTER.modelLocation(),
			FLUID_PACKAGER_TRAY.modelLocation(),
			FLUID_PACKAGER_HATCH_OPEN.modelLocation(),
			FLUID_PACKAGER_HATCH_CLOSED.modelLocation(),
			FLUID_REPACKAGER_TRAY.modelLocation(),
			COPPER_FROGPORT_BODY.modelLocation(),
			COPPER_FROGPORT_HEAD.modelLocation(),
			COPPER_FROGPORT_HEAD_GOGGLES.modelLocation(),
			COPPER_FROGPORT_TONGUE.modelLocation(),
			FLUID_PACKAGE.modelLocation(),
			FLUID_PACKAGE_EXPOSED.modelLocation(),
			FLUID_PACKAGE_OXIDIZED.modelLocation(),
			FLUID_PACKAGE_WEATHERED.modelLocation(),
			RARE_PIG_PACKAGE.modelLocation(),
			RARE_FOX_PACKAGE.modelLocation(),
			FAUCET_SOURCE_INTERFACE.get(Direction.NORTH).modelLocation(),
			FAUCET_SOURCE_INTERFACE.get(Direction.SOUTH).modelLocation(),
			FAUCET_SOURCE_INTERFACE.get(Direction.EAST).modelLocation(),
			FAUCET_SOURCE_INTERFACE.get(Direction.WEST).modelLocation(),
			MECHANICAL_FLUID_GUN_BASE.modelLocation(),
			MECHANICAL_FLUID_GUN_COG.modelLocation(),
			MECHANICAL_FLUID_GUN_GUN_BODY.modelLocation(),
			MECHANICAL_FLUID_GUN_GUNPOINT_TOP.modelLocation(),
			MECHANICAL_FLUID_GUN_GUNPOINT_MIDDLE.modelLocation(),
			MECHANICAL_FLUID_GUN_GUNPOINT_BOTTOM.modelLocation(),
			COPPER_SCHEMATICANNON_CONNECTOR.modelLocation(),
			COPPER_SCHEMATICANNON_PIPE.modelLocation(),
			SNOW_GOLEM_FREEZER_CAGE.modelLocation(),
			SNOW_GOLEM_FREEZER_INERT.modelLocation(),
			SNOW_GOLEM_FREEZER_IDLE.modelLocation(),
			SNOW_GOLEM_FREEZER_PUMPKIN.modelLocation(),
			SNOW_GOLEM_FREEZER_FLAME.modelLocation());
	}

	public static void register() {
		if (registered)
			return;
		registered = true;

		com.simibubi.create.AllPartialModels.PACKAGES.put(FLUID_PACKAGE_ID, FLUID_PACKAGE);
		com.simibubi.create.AllPartialModels.PACKAGE_RIGGING.put(FLUID_PACKAGE_ID, FLUID_PACKAGE_RIGGING);
		com.simibubi.create.AllPartialModels.PACKAGES.put(FLUID_PACKAGE_EXPOSED_ID, FLUID_PACKAGE_EXPOSED);
		com.simibubi.create.AllPartialModels.PACKAGE_RIGGING.put(FLUID_PACKAGE_EXPOSED_ID, FLUID_PACKAGE_RIGGING);
		com.simibubi.create.AllPartialModels.PACKAGES.put(FLUID_PACKAGE_OXIDIZED_ID, FLUID_PACKAGE_OXIDIZED);
		com.simibubi.create.AllPartialModels.PACKAGE_RIGGING.put(FLUID_PACKAGE_OXIDIZED_ID, FLUID_PACKAGE_RIGGING);
		com.simibubi.create.AllPartialModels.PACKAGES.put(FLUID_PACKAGE_WEATHERED_ID, FLUID_PACKAGE_WEATHERED);
		com.simibubi.create.AllPartialModels.PACKAGE_RIGGING.put(FLUID_PACKAGE_WEATHERED_ID, FLUID_PACKAGE_RIGGING);
		com.simibubi.create.AllPartialModels.PACKAGES.put(RARE_PIG_PACKAGE_ID, RARE_PIG_PACKAGE);
		com.simibubi.create.AllPartialModels.PACKAGE_RIGGING.put(RARE_PIG_PACKAGE_ID, RARE_FLUID_PACKAGE_RIGGING);
		com.simibubi.create.AllPartialModels.PACKAGES.put(RARE_FOX_PACKAGE_ID, RARE_FOX_PACKAGE);
		com.simibubi.create.AllPartialModels.PACKAGE_RIGGING.put(RARE_FOX_PACKAGE_ID, RARE_FLUID_PACKAGE_RIGGING);
	}

	public static PartialModel getFluidPackageModel(ItemStack stack) {
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		PartialModel model = com.simibubi.create.AllPartialModels.PACKAGES.get(id);
		return model != null ? model : FLUID_PACKAGE;
	}
}
