package com.yision.fluidlogistics.config;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = "fluidlogistics", bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();

    private static final boolean FLUID_TRANSPORTER_ENABLED_DEFAULT = true;
    private static final boolean SMART_FAUCET_ENABLED_DEFAULT = true;
    private static final boolean FAUCET_ENABLED_DEFAULT = true;
    private static final boolean MULTI_FLUID_TANK_ENABLED_DEFAULT = true;
    private static final boolean HORIZONTAL_MULTI_FLUID_TANK_ENABLED_DEFAULT = true;
    private static final boolean MULTI_FLUID_ACCESS_PORT_ENABLED_DEFAULT = true;
    private static final boolean SMART_HOPPER_ENABLED_DEFAULT = true;
    private static final boolean FLUID_PUMP_ENABLED_DEFAULT = true;
    private static final boolean INFINITE_FLUID_TANK_ENABLED_DEFAULT = true;
    private static final boolean WATER_CONTAINING_COPPER_CASING_ENABLED_DEFAULT = true;
    private static final boolean COPPER_BASIN_ENABLED_DEFAULT = true;
    private static final boolean MECHANICAL_FLUID_GUN_ENABLED_DEFAULT = true;
    private static final boolean HAND_POINTER_ENABLED_DEFAULT = true;
    private static final boolean COPPER_FROGPORT_ENABLED_DEFAULT = true;
    private static final boolean FLUID_PACKAGER_ENABLED_DEFAULT = true;
    private static final boolean FLUID_REPACKAGER_ENABLED_DEFAULT = true;
    private static final boolean COPPER_BUCKET_ENABLED_DEFAULT = true;
    private static final boolean PHANTOM_CHAIN_ENABLED_DEFAULT = true;
    private static final boolean FLUID_HATCH_ENABLED_DEFAULT = true;
    private static final boolean FLUID_FACTORY_GAUGE_ENABLED_DEFAULT = true;

    private static final int FLUID_PACKAGE_CAPACITY_DEFAULT = 10000;
    private static final int FLUID_PACKAGE_CAPACITY_MIN = 1;
    private static final int FLUID_PACKAGE_CAPACITY_MAX = Integer.MAX_VALUE;
    private static final int FLUID_PUMP_RANGE_DEFAULT = 24;
    private static final int FLUID_PUMP_RANGE_MIN = 1;
    private static final int FLUID_PUMP_RANGE_MAX = Integer.MAX_VALUE;
    private static final int HAND_POINTER_MAX_ARMS_DEFAULT = 5;
    private static final int HAND_POINTER_MAX_FROGPORTS_DEFAULT = 5;
    private static final int HAND_POINTER_MAX_MAILBOXES_DEFAULT = 5;
    private static final int HAND_POINTER_SELECTION_LIMIT_MIN = 1;
    private static final int HAND_POINTER_SELECTION_LIMIT_MAX = 64;

    private static final boolean FLUID_TRANSPORTER_INFINITE_WATER_ENABLED_DEFAULT = true;
    private static final boolean FAUCET_INFINITE_WATER_ENABLED_DEFAULT = true;
    private static final boolean SMART_HOPPER_INFINITE_WATER_ENABLED_DEFAULT = true;
    private static final int MILLIBUCKETS_PER_BUCKET = 1000;
    private static final int INFINITE_FLUID_TANK_CAPACITY_DEFAULT = -1;
    private static final int INFINITE_FLUID_TANK_CAPACITY_MIN = -1;
    private static final int INFINITE_FLUID_TANK_CAPACITY_MAX = Integer.MAX_VALUE / MILLIBUCKETS_PER_BUCKET;
    private static final boolean USE_ITEM_RENDERING_FOR_FLUID_FACTORY_GAUGE_FLUID_DEFAULT = false;

    public static final ModConfigSpec.BooleanValue USE_ITEM_RENDERING_FOR_FLUID_FACTORY_GAUGE_FLUID = CLIENT_BUILDER
            .translation("fluidlogistics.configuration.useItemRenderingForFluidFactoryGaugeFluid")
            .define("useItemRenderingForFluidFactoryGaugeFluid",
                    USE_ITEM_RENDERING_FOR_FLUID_FACTORY_GAUGE_FLUID_DEFAULT);

    public static final ModConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();

    static {
        BUILDER.translation("fluidlogistics.configuration.section.featureToggles")
                .push("featureToggles");
    }

    public static final ModConfigSpec.BooleanValue FLUID_TRANSPORTER_ENABLED = BUILDER
            .translation("block.fluidlogistics.fluid_transporter")
            .define("fluidTransporterEnabled", FLUID_TRANSPORTER_ENABLED_DEFAULT);

    public static final ModConfigSpec.BooleanValue SMART_FAUCET_ENABLED = BUILDER
            .translation("block.fluidlogistics.smart_faucet")
            .define("smartFaucetEnabled", SMART_FAUCET_ENABLED_DEFAULT);

    public static final ModConfigSpec.BooleanValue FAUCET_ENABLED = BUILDER
            .translation("block.fluidlogistics.faucet")
            .define("faucetEnabled", FAUCET_ENABLED_DEFAULT);

    public static final ModConfigSpec.BooleanValue MULTI_FLUID_TANK_ENABLED = BUILDER
            .translation("block.fluidlogistics.multi_fluid_tank")
            .define("multiFluidTankEnabled", MULTI_FLUID_TANK_ENABLED_DEFAULT);

    public static final ModConfigSpec.BooleanValue HORIZONTAL_MULTI_FLUID_TANK_ENABLED = BUILDER
            .translation("block.fluidlogistics.horizontal_multi_fluid_tank")
            .define("horizontalMultiFluidTankEnabled", HORIZONTAL_MULTI_FLUID_TANK_ENABLED_DEFAULT);

    public static final ModConfigSpec.BooleanValue MULTI_FLUID_ACCESS_PORT_ENABLED = BUILDER
            .translation("block.fluidlogistics.multi_fluid_access_port")
            .define("multiFluidAccessPortEnabled", MULTI_FLUID_ACCESS_PORT_ENABLED_DEFAULT);

    public static final ModConfigSpec.BooleanValue SMART_HOPPER_ENABLED = BUILDER
            .translation("block.fluidlogistics.smart_hopper")
            .define("smartHopperEnabled", SMART_HOPPER_ENABLED_DEFAULT);

    public static final ModConfigSpec.BooleanValue FLUID_PUMP_ENABLED = BUILDER
            .translation("block.fluidlogistics.fluid_pump")
            .define("fluidPumpEnabled", FLUID_PUMP_ENABLED_DEFAULT);

    public static final ModConfigSpec.BooleanValue INFINITE_FLUID_TANK_ENABLED = BUILDER
            .translation("block.fluidlogistics.infinite_fluid_tank")
            .define("infiniteFluidTankEnabled", INFINITE_FLUID_TANK_ENABLED_DEFAULT);

    public static final ModConfigSpec.BooleanValue WATER_CONTAINING_COPPER_CASING_ENABLED = BUILDER
            .translation("block.fluidlogistics.water_containing_copper_casing")
            .define("waterContainingCopperCasingEnabled", WATER_CONTAINING_COPPER_CASING_ENABLED_DEFAULT);

    public static final ModConfigSpec.BooleanValue COPPER_BASIN_ENABLED = BUILDER
            .translation("block.fluidlogistics.copper_basin")
            .define("copperBasinEnabled", COPPER_BASIN_ENABLED_DEFAULT);

    public static final ModConfigSpec.BooleanValue MECHANICAL_FLUID_GUN_ENABLED = BUILDER
            .translation("block.fluidlogistics.mechanical_fluid_gun")
            .define("mechanicalFluidGunEnabled", MECHANICAL_FLUID_GUN_ENABLED_DEFAULT);

    public static final ModConfigSpec.BooleanValue HAND_POINTER_ENABLED = BUILDER
            .translation("item.fluidlogistics.hand_pointer")
            .define("handPointerEnabled", HAND_POINTER_ENABLED_DEFAULT);

    public static final ModConfigSpec.BooleanValue COPPER_FROGPORT_ENABLED = BUILDER
            .translation("block.fluidlogistics.copper_frogport")
            .define("copperFrogportEnabled", COPPER_FROGPORT_ENABLED_DEFAULT);

    public static final ModConfigSpec.BooleanValue FLUID_PACKAGER_ENABLED = BUILDER
            .translation("block.fluidlogistics.fluid_packager")
            .define("fluidPackagerEnabled", FLUID_PACKAGER_ENABLED_DEFAULT);

    public static final ModConfigSpec.BooleanValue FLUID_REPACKAGER_ENABLED = BUILDER
            .translation("block.fluidlogistics.fluid_repackager")
            .define("fluidRepackagerEnabled", FLUID_REPACKAGER_ENABLED_DEFAULT);

    public static final ModConfigSpec.BooleanValue COPPER_BUCKET_ENABLED = BUILDER
            .translation("item.fluidlogistics.copper_bucket")
            .define("copperBucketEnabled", COPPER_BUCKET_ENABLED_DEFAULT);

    public static final ModConfigSpec.BooleanValue PHANTOM_CHAIN_ENABLED = BUILDER
            .translation("item.fluidlogistics.phantom_chain")
            .define("phantomChainEnabled", PHANTOM_CHAIN_ENABLED_DEFAULT);

    public static final ModConfigSpec.BooleanValue FLUID_HATCH_ENABLED = BUILDER
            .translation("block.fluidlogistics.fluid_hatch")
            .define("fluidHatchEnabled", FLUID_HATCH_ENABLED_DEFAULT);

    public static final ModConfigSpec.BooleanValue FLUID_FACTORY_GAUGE_ENABLED = BUILDER
            .translation("item.fluidlogistics.fluid_factory_gauge")
            .define("fluidFactoryGaugeEnabled", FLUID_FACTORY_GAUGE_ENABLED_DEFAULT);

    static {
        BUILDER.pop();
        BUILDER.translation("fluidlogistics.configuration.section.blockProperties")
                .push("blockProperties");
    }

    public static final ModConfigSpec.IntValue FLUID_PACKAGE_CAPACITY = BUILDER
            .comment("Maximum fluid amount a fluid package can hold (in mB)")
            .translation("fluidlogistics.configuration.fluidPackageCapacity")
            .defineInRange("fluidPackageCapacity",
                    FLUID_PACKAGE_CAPACITY_DEFAULT,
                    FLUID_PACKAGE_CAPACITY_MIN,
                    FLUID_PACKAGE_CAPACITY_MAX);

    public static final ModConfigSpec.IntValue FLUID_PUMP_RANGE = BUILDER
            .comment("Maximum distance a Fluid Pump can push or pull fluids on either side")
            .translation("fluidlogistics.configuration.fluidPumpRange")
            .defineInRange("fluidPumpRange",
                    FLUID_PUMP_RANGE_DEFAULT,
                    FLUID_PUMP_RANGE_MIN,
                    FLUID_PUMP_RANGE_MAX);

    public static final ModConfigSpec.IntValue HAND_POINTER_MAX_ARMS = BUILDER
            .comment("Maximum number of Mechanical Arms configurable in one Hand Pointer session")
            .translation("fluidlogistics.configuration.handPointerMaxArms")
            .defineInRange("handPointerMaxArms",
                    HAND_POINTER_MAX_ARMS_DEFAULT,
                    HAND_POINTER_SELECTION_LIMIT_MIN,
                    HAND_POINTER_SELECTION_LIMIT_MAX);

    public static final ModConfigSpec.IntValue HAND_POINTER_MAX_FROGPORTS = BUILDER
            .comment("Maximum number of Package Frogports configurable in one Hand Pointer session")
            .translation("fluidlogistics.configuration.handPointerMaxFrogports")
            .defineInRange("handPointerMaxFrogports",
                    HAND_POINTER_MAX_FROGPORTS_DEFAULT,
                    HAND_POINTER_SELECTION_LIMIT_MIN,
                    HAND_POINTER_SELECTION_LIMIT_MAX);

    public static final ModConfigSpec.IntValue HAND_POINTER_MAX_MAILBOXES = BUILDER
            .comment("Maximum number of Postboxes configurable in one Hand Pointer session")
            .translation("fluidlogistics.configuration.handPointerMaxMailboxes")
            .defineInRange("handPointerMaxMailboxes",
                    HAND_POINTER_MAX_MAILBOXES_DEFAULT,
                    HAND_POINTER_SELECTION_LIMIT_MIN,
                    HAND_POINTER_SELECTION_LIMIT_MAX);

    public static final ModConfigSpec.EnumValue<FluidContainerMode> FAUCET_FLUID_CONTAINER_MODE = BUILDER
            .comment("ALLOW_BY_TAG allows fluid containers in #fluidlogistics:faucet_fillable; ALLOW_ALL allows every fluid container; DENY_ALL allows none")
            .translation("fluidlogistics.configuration.faucetFluidContainerMode")
            .defineEnum("faucetFluidContainerMode", FluidContainerMode.ALLOW_BY_TAG);

    public static final ModConfigSpec.BooleanValue FLUID_TRANSPORTER_INFINITE_WATER_ENABLED = BUILDER
            .comment("Allow the fluid transporter to extract infinite water from waterlogged leaves")
            .translation("fluidlogistics.configuration.fluidTransporterInfiniteWater")
            .define("fluidTransporterInfiniteWaterEnabled", FLUID_TRANSPORTER_INFINITE_WATER_ENABLED_DEFAULT);

    public static final ModConfigSpec.BooleanValue FAUCET_INFINITE_WATER_ENABLED = BUILDER
            .comment("Allow the faucet and smart faucet to extract infinite water from waterlogged leaves")
            .translation("fluidlogistics.configuration.faucetInfiniteWater")
            .define("faucetInfiniteWaterEnabled", FAUCET_INFINITE_WATER_ENABLED_DEFAULT);

    public static final ModConfigSpec.BooleanValue SMART_HOPPER_INFINITE_WATER_ENABLED = BUILDER
            .comment("Allow the smart hopper to extract infinite water from waterlogged leaves")
            .translation("fluidlogistics.configuration.smartHopperInfiniteWater")
            .define("smartHopperInfiniteWaterEnabled", SMART_HOPPER_INFINITE_WATER_ENABLED_DEFAULT);

    public static final ModConfigSpec.IntValue INFINITE_FLUID_TANK_CAPACITY = BUILDER
            .comment("Infinite tank capacity (in B/buckets). -1 = follow Create's infinite fluid capacity config option")
            .translation("fluidlogistics.configuration.infiniteFluidTankCapacity")
            .defineInRange("infiniteFluidTankCapacity",
                    INFINITE_FLUID_TANK_CAPACITY_DEFAULT,
                    INFINITE_FLUID_TANK_CAPACITY_MIN,
                    INFINITE_FLUID_TANK_CAPACITY_MAX);

    public static final ModConfigSpec.EnumValue<InfiniteTankFluidMode> INFINITE_FLUID_TANK_ALLOWED_FLUIDS = BUILDER
            .comment("FOLLOW_CREATE = follow Create's bottomless fluid config option")
            .translation("fluidlogistics.configuration.infiniteFluidTankAllowedFluids")
            .defineEnum("infiniteFluidTankAllowedFluids", InfiniteTankFluidMode.FOLLOW_CREATE);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SERVER_SPEC = BUILDER.build();

    private static boolean fluidTransporterEnabled = FLUID_TRANSPORTER_ENABLED_DEFAULT;
    private static boolean smartFaucetEnabled = SMART_FAUCET_ENABLED_DEFAULT;
    private static boolean faucetEnabled = FAUCET_ENABLED_DEFAULT;
    private static boolean multiFluidTankEnabled = MULTI_FLUID_TANK_ENABLED_DEFAULT;
    private static boolean horizontalMultiFluidTankEnabled = HORIZONTAL_MULTI_FLUID_TANK_ENABLED_DEFAULT;
    private static boolean multiFluidAccessPortEnabled = MULTI_FLUID_ACCESS_PORT_ENABLED_DEFAULT;
    private static boolean smartHopperEnabled = SMART_HOPPER_ENABLED_DEFAULT;
    private static boolean fluidPumpEnabled = FLUID_PUMP_ENABLED_DEFAULT;
    private static boolean infiniteFluidTankEnabled = INFINITE_FLUID_TANK_ENABLED_DEFAULT;
    private static boolean waterContainingCopperCasingEnabled = WATER_CONTAINING_COPPER_CASING_ENABLED_DEFAULT;
    private static boolean copperBasinEnabled = COPPER_BASIN_ENABLED_DEFAULT;
    private static boolean mechanicalFluidGunEnabled = MECHANICAL_FLUID_GUN_ENABLED_DEFAULT;
    private static boolean handPointerEnabled = HAND_POINTER_ENABLED_DEFAULT;
    private static boolean copperFrogportEnabled = COPPER_FROGPORT_ENABLED_DEFAULT;
    private static boolean fluidPackagerEnabled = FLUID_PACKAGER_ENABLED_DEFAULT;
    private static boolean fluidRepackagerEnabled = FLUID_REPACKAGER_ENABLED_DEFAULT;
    private static boolean copperBucketEnabled = COPPER_BUCKET_ENABLED_DEFAULT;
    private static boolean phantomChainEnabled = PHANTOM_CHAIN_ENABLED_DEFAULT;
    private static boolean fluidHatchEnabled = FLUID_HATCH_ENABLED_DEFAULT;
    private static boolean fluidFactoryGaugeEnabled = FLUID_FACTORY_GAUGE_ENABLED_DEFAULT;
    private static int fluidPackageCapacity = FLUID_PACKAGE_CAPACITY_DEFAULT;
    private static int fluidPumpRange = FLUID_PUMP_RANGE_DEFAULT;
    private static int handPointerMaxArms = HAND_POINTER_MAX_ARMS_DEFAULT;
    private static int handPointerMaxFrogports = HAND_POINTER_MAX_FROGPORTS_DEFAULT;
    private static int handPointerMaxMailboxes = HAND_POINTER_MAX_MAILBOXES_DEFAULT;
    private static FluidContainerMode faucetFluidContainerMode = FluidContainerMode.ALLOW_BY_TAG;
    private static boolean fluidTransporterInfiniteWaterEnabled = FLUID_TRANSPORTER_INFINITE_WATER_ENABLED_DEFAULT;
    private static boolean faucetInfiniteWaterEnabled = FAUCET_INFINITE_WATER_ENABLED_DEFAULT;
    private static boolean smartHopperInfiniteWaterEnabled = SMART_HOPPER_INFINITE_WATER_ENABLED_DEFAULT;
    private static int infiniteFluidTankCapacity = INFINITE_FLUID_TANK_CAPACITY_DEFAULT;
    private static InfiniteTankFluidMode infiniteFluidTankAllowedFluids = InfiniteTankFluidMode.FOLLOW_CREATE;
    private static boolean useItemRenderingForFluidFactoryGaugeFluid =
            USE_ITEM_RENDERING_FOR_FLUID_FACTORY_GAUGE_FLUID_DEFAULT;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SERVER_SPEC) {
            reloadValues();
        } else if (event.getConfig().getSpec() == CLIENT_SPEC) {
            reloadClientValues();
        }
    }

    @SubscribeEvent
    static void onReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SERVER_SPEC) {
            reloadValues();
        } else if (event.getConfig().getSpec() == CLIENT_SPEC) {
            reloadClientValues();
        }
    }

    private static void reloadClientValues() {
        useItemRenderingForFluidFactoryGaugeFluid = USE_ITEM_RENDERING_FOR_FLUID_FACTORY_GAUGE_FLUID.get();
    }

    private static void reloadValues() {
        fluidTransporterEnabled = FLUID_TRANSPORTER_ENABLED.get();
        smartFaucetEnabled = SMART_FAUCET_ENABLED.get();
        faucetEnabled = FAUCET_ENABLED.get();
        multiFluidTankEnabled = MULTI_FLUID_TANK_ENABLED.get();
        horizontalMultiFluidTankEnabled = HORIZONTAL_MULTI_FLUID_TANK_ENABLED.get();
        multiFluidAccessPortEnabled = MULTI_FLUID_ACCESS_PORT_ENABLED.get();
        smartHopperEnabled = SMART_HOPPER_ENABLED.get();
        fluidPumpEnabled = FLUID_PUMP_ENABLED.get();
        infiniteFluidTankEnabled = INFINITE_FLUID_TANK_ENABLED.get();
        waterContainingCopperCasingEnabled = WATER_CONTAINING_COPPER_CASING_ENABLED.get();
        copperBasinEnabled = COPPER_BASIN_ENABLED.get();
        mechanicalFluidGunEnabled = MECHANICAL_FLUID_GUN_ENABLED.get();
        handPointerEnabled = HAND_POINTER_ENABLED.get();
        copperFrogportEnabled = COPPER_FROGPORT_ENABLED.get();
        fluidPackagerEnabled = FLUID_PACKAGER_ENABLED.get();
        fluidRepackagerEnabled = FLUID_REPACKAGER_ENABLED.get();
        copperBucketEnabled = COPPER_BUCKET_ENABLED.get();
        phantomChainEnabled = PHANTOM_CHAIN_ENABLED.get();
        fluidHatchEnabled = FLUID_HATCH_ENABLED.get();
        fluidFactoryGaugeEnabled = FLUID_FACTORY_GAUGE_ENABLED.get();
        fluidPackageCapacity = FLUID_PACKAGE_CAPACITY.get();
        fluidPumpRange = FLUID_PUMP_RANGE.get();
        handPointerMaxArms = HAND_POINTER_MAX_ARMS.get();
        handPointerMaxFrogports = HAND_POINTER_MAX_FROGPORTS.get();
        handPointerMaxMailboxes = HAND_POINTER_MAX_MAILBOXES.get();
        faucetFluidContainerMode = FAUCET_FLUID_CONTAINER_MODE.get();
        fluidTransporterInfiniteWaterEnabled = FLUID_TRANSPORTER_INFINITE_WATER_ENABLED.get();
        faucetInfiniteWaterEnabled = FAUCET_INFINITE_WATER_ENABLED.get();
        smartHopperInfiniteWaterEnabled = SMART_HOPPER_INFINITE_WATER_ENABLED.get();
        infiniteFluidTankCapacity = bucketsToMillibuckets(INFINITE_FLUID_TANK_CAPACITY.get());
        infiniteFluidTankAllowedFluids = INFINITE_FLUID_TANK_ALLOWED_FLUIDS.get();
        FeatureToggle.reload();
    }

    public static boolean isFluidTransporterEnabled() { return fluidTransporterEnabled; }
    public static boolean isSmartFaucetEnabled() { return smartFaucetEnabled; }
    public static boolean isFaucetEnabled() { return faucetEnabled; }
    public static boolean isMultiFluidTankEnabled() { return multiFluidTankEnabled; }
    public static boolean isHorizontalMultiFluidTankEnabled() { return horizontalMultiFluidTankEnabled; }
    public static boolean isMultiFluidAccessPortEnabled() { return multiFluidAccessPortEnabled; }
    public static boolean isSmartHopperEnabled() { return smartHopperEnabled; }
    public static boolean isFluidPumpEnabled() { return fluidPumpEnabled; }
    public static boolean isInfiniteFluidTankEnabled() { return infiniteFluidTankEnabled; }
    public static boolean isWaterContainingCopperCasingEnabled() { return waterContainingCopperCasingEnabled; }
    public static boolean isCopperBasinEnabled() { return copperBasinEnabled; }
    public static boolean isMechanicalFluidGunEnabled() { return mechanicalFluidGunEnabled; }
    public static boolean isHandPointerEnabled() { return handPointerEnabled; }
    public static boolean isCopperFrogportEnabled() { return copperFrogportEnabled; }
    public static boolean isFluidPackagerEnabled() { return fluidPackagerEnabled; }
    public static boolean isFluidRepackagerEnabled() { return fluidRepackagerEnabled; }
    public static boolean isCopperBucketEnabled() { return copperBucketEnabled; }
    public static boolean isPhantomChainEnabled() { return phantomChainEnabled; }
    public static boolean isFluidHatchEnabled() { return fluidHatchEnabled; }
    public static boolean isFluidFactoryGaugeEnabled() { return fluidFactoryGaugeEnabled; }

    public static int getFluidPumpRange() { return fluidPumpRange; }
    public static int getHandPointerMaxArms() { return handPointerMaxArms; }
    public static int getHandPointerMaxFrogports() { return handPointerMaxFrogports; }
    public static int getHandPointerMaxMailboxes() { return handPointerMaxMailboxes; }
    public static FluidContainerMode getFaucetFluidContainerMode() { return faucetFluidContainerMode; }

    public static int getFluidPerPackage() {
        return Math.max(FLUID_PACKAGE_CAPACITY_MIN, fluidPackageCapacity);
    }

    public static boolean isFluidTransporterInfiniteWaterEnabled() { return fluidTransporterInfiniteWaterEnabled; }
    public static boolean isFaucetInfiniteWaterEnabled() { return faucetInfiniteWaterEnabled; }
    public static boolean isSmartHopperInfiniteWaterEnabled() { return smartHopperInfiniteWaterEnabled; }
    public static int getInfiniteFluidTankCapacity() { return infiniteFluidTankCapacity; }
    public static InfiniteTankFluidMode getInfiniteFluidTankAllowedFluids() { return infiniteFluidTankAllowedFluids; }
    public static boolean useItemRenderingForFluidFactoryGaugeFluid() {
        return useItemRenderingForFluidFactoryGaugeFluid;
    }

    private static int bucketsToMillibuckets(int buckets) {
        if (buckets <= 0) {
            return buckets;
        }
        return buckets * MILLIBUCKETS_PER_BUCKET;
    }
}
