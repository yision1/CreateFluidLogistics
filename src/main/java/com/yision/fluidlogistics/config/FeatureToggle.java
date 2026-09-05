package com.yision.fluidlogistics.config;

import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.compat.CompatMods;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

public final class FeatureToggle {

    public static final ResourceLocation FLUID_TRANSPORTER = FluidLogistics.asResource("fluid_transporter");
    public static final ResourceLocation SMART_FAUCET = FluidLogistics.asResource("smart_faucet");
    public static final ResourceLocation FAUCET = FluidLogistics.asResource("faucet");
    public static final ResourceLocation MULTI_FLUID_TANK = FluidLogistics.asResource("multi_fluid_tank");
    public static final ResourceLocation HORIZONTAL_MULTI_FLUID_TANK = FluidLogistics.asResource("horizontal_multi_fluid_tank");
    public static final ResourceLocation MULTI_FLUID_ACCESS_PORT = FluidLogistics.asResource("multi_fluid_access_port");
    public static final ResourceLocation FLUID_INVENTORY_ACCESS_PORT =
            FluidLogistics.asResource("fluid_inventory_access_port");
    public static final ResourceLocation SMART_HOPPER = FluidLogistics.asResource("smart_hopper");
    public static final ResourceLocation FLUID_PUMP = FluidLogistics.asResource("fluid_pump");
    public static final ResourceLocation INFINITE_FLUID_TANK = FluidLogistics.asResource("infinite_fluid_tank");
    public static final ResourceLocation WATER_CONTAINING_COPPER_CASING = FluidLogistics.asResource("water_containing_copper_casing");
    public static final ResourceLocation COPPER_BASIN = FluidLogistics.asResource("copper_basin");
    public static final ResourceLocation MECHANICAL_FLUID_GUN = FluidLogistics.asResource("mechanical_fluid_gun");
    public static final ResourceLocation HAND_POINTER = FluidLogistics.asResource("hand_pointer");
    public static final ResourceLocation COPPER_FROGPORT = FluidLogistics.asResource("copper_frogport");
    public static final ResourceLocation FLUID_PACKAGER = FluidLogistics.asResource("fluid_packager");
    public static final ResourceLocation FLUID_REPACKAGER = FluidLogistics.asResource("fluid_repackager");
    public static final ResourceLocation COPPER_BUCKET = FluidLogistics.asResource("copper_bucket");
    public static final ResourceLocation PHANTOM_CHAIN = FluidLogistics.asResource("phantom_chain");

    public static final ResourceLocation FLUID_HATCH = FluidLogistics.asResource("fluid_hatch");
    public static final ResourceLocation FLUID_FACTORY_GAUGE = FluidLogistics.asResource("fluid_factory_gauge");
    public static final ResourceLocation BLAZE_COOLER = FluidLogistics.asResource("blaze_cooler");
    public static final ResourceLocation COPPER_SCHEMATICANNON = FluidLogistics.asResource("copper_schematicannon");
    public static final ResourceLocation INDUSTRIAL_COPPER_BLOCK = FluidLogistics.asResource("industrial_copper_block");
    public static final ResourceLocation FLUID_SCHEMATIC = FluidLogistics.asResource("fluid_schematic");
    public static final ResourceLocation FROST_CAKE = FluidLogistics.asResource("frost_cake");

    private static final Map<ResourceLocation, BooleanSupplier> FEATURE_MAP;

    static {
        Map<ResourceLocation, BooleanSupplier> map = new LinkedHashMap<>();
        map.put(FLUID_TRANSPORTER, Config::isFluidTransporterEnabled);
        map.put(SMART_FAUCET, Config::isSmartFaucetEnabled);
        map.put(FAUCET, Config::isFaucetEnabled);
        map.put(MULTI_FLUID_TANK, Config::isMultiFluidTankEnabled);
        map.put(HORIZONTAL_MULTI_FLUID_TANK, Config::isHorizontalMultiFluidTankEnabled);
        map.put(MULTI_FLUID_ACCESS_PORT, Config::isMultiFluidAccessPortEnabled);
        map.put(FLUID_INVENTORY_ACCESS_PORT, Config::isFluidInventoryAccessPortEnabled);
        map.put(SMART_HOPPER, Config::isSmartHopperEnabled);
        map.put(FLUID_PUMP, Config::isFluidPumpEnabled);
        map.put(INFINITE_FLUID_TANK, Config::isInfiniteFluidTankEnabled);
        map.put(WATER_CONTAINING_COPPER_CASING, Config::isWaterContainingCopperCasingEnabled);
        map.put(COPPER_BASIN, Config::isCopperBasinEnabled);
        map.put(MECHANICAL_FLUID_GUN, Config::isMechanicalFluidGunEnabled);
        map.put(HAND_POINTER, Config::isHandPointerEnabled);
        map.put(COPPER_FROGPORT, Config::isCopperFrogportEnabled);
        map.put(FLUID_PACKAGER, Config::isFluidPackagerEnabled);
        map.put(FLUID_REPACKAGER, Config::isFluidRepackagerEnabled);
        map.put(COPPER_BUCKET, Config::isCopperBucketEnabled);
        map.put(PHANTOM_CHAIN, Config::isPhantomChainEnabled);
        map.put(FLUID_HATCH, Config::isFluidHatchEnabled);
        map.put(FLUID_FACTORY_GAUGE, Config::isFluidFactoryGaugeEnabled);
        map.put(BLAZE_COOLER, Config::isBlazeCoolerEnabled);
        map.put(COPPER_SCHEMATICANNON, Config::isCopperSchematicannonEnabled);
        map.put(INDUSTRIAL_COPPER_BLOCK, Config::isIndustrialCopperBlockEnabled);
        map.put(FLUID_SCHEMATIC, Config::isFluidSchematicEnabled);
        map.put(FROST_CAKE, Config::isFrostCakeEnabled);
        FEATURE_MAP = Collections.unmodifiableMap(map);
    }

    private FeatureToggle() {
    }

    private static volatile Map<ResourceLocation, Boolean> CACHE = Map.of();

    public static void reload() {
        Map<ResourceLocation, Boolean> next = new HashMap<>();
        FEATURE_MAP.forEach((key, supplier) -> next.put(key, supplier.getAsBoolean()));
        CACHE = Map.copyOf(next);
    }

    public static boolean isEnabled(ResourceLocation feature) {
        Boolean cached = CACHE.get(feature);
        if (cached != null) {
            return cached;
        }
        BooleanSupplier supplier = FEATURE_MAP.get(feature);
        return supplier != null ? supplier.getAsBoolean() : true;
    }

    public static boolean isFluidHatchAdvertised() {
        return Config.isFluidHatchEnabled() && !CompatMods.createDragonsPlusLoaded();
    }
}
