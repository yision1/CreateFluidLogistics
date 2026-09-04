package com.yision.fluidlogistics.registry;

import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.content.processing.cooling.BulkCoolingFanProcessingType;
import com.yision.fluidlogistics.content.processing.cooling.InactiveBulkCoolingFanProcessingType;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AllFluidLogisticsFanProcessingTypes {

    private static final DeferredRegister<FanProcessingType> REGISTER =
        DeferredRegister.create(CreateBuiltInRegistries.FAN_PROCESSING_TYPE.key(), FluidLogistics.MODID);

    public static final DeferredHolder<FanProcessingType, BulkCoolingFanProcessingType> BULK_COOLING =
        REGISTER.register("bulk_cooling", BulkCoolingFanProcessingType::new);
    public static final DeferredHolder<FanProcessingType, InactiveBulkCoolingFanProcessingType> INACTIVE_BULK_COOLING =
        REGISTER.register("inactive_bulk_cooling", InactiveBulkCoolingFanProcessingType::new);

    private AllFluidLogisticsFanProcessingTypes() {
    }

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
