package com.yision.fluidlogistics.registry;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.yision.fluidlogistics.content.fluids.pressureGauge.FlowRateDisplaySource;

import static com.yision.fluidlogistics.FluidLogistics.REGISTRATE;

public class AllFluidLogisticsDisplaySources {
    public static final RegistryEntry<DisplaySource, FlowRateDisplaySource> FLOW_RATE = REGISTRATE
        .displaySource("flow_rate", FlowRateDisplaySource::new)
        .register();

    public static void register() {
    }
}
