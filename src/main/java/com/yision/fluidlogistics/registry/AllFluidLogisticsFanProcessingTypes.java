package com.yision.fluidlogistics.registry;

import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.content.processing.cooling.BulkCoolingFanProcessingType;
import com.yision.fluidlogistics.content.processing.cooling.InactiveBulkCoolingFanProcessingType;

import net.minecraft.core.Registry;

public final class AllFluidLogisticsFanProcessingTypes {

    public static final FanProcessingType BULK_COOLING = register("bulk_cooling", new BulkCoolingFanProcessingType());
    public static final FanProcessingType INACTIVE_BULK_COOLING =
        register("inactive_bulk_cooling", new InactiveBulkCoolingFanProcessingType());

    private AllFluidLogisticsFanProcessingTypes() {
    }

    private static FanProcessingType register(String name, FanProcessingType type) {
        return Registry.register(CreateBuiltInRegistries.FAN_PROCESSING_TYPE,
            FluidLogistics.asResource(name), type);
    }

    public static void register() {
    }
}
