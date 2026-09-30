package com.yision.fluidlogistics.registry;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.yision.fluidlogistics.content.fluids.pressureGauge.FlowRateDisplaySource;
import com.yision.fluidlogistics.content.logistics.potatoServer.PotatoServerDisplaySource;

import static com.yision.fluidlogistics.FluidLogistics.REGISTRATE;

public class AllFluidLogisticsDisplaySources {
    public static final RegistryEntry<DisplaySource, FlowRateDisplaySource> FLOW_RATE = REGISTRATE
        .displaySource("flow_rate", FlowRateDisplaySource::new)
        .register();

    public static final RegistryEntry<DisplaySource, PotatoServerDisplaySource> POTATO_TPS = potato("potato_tps", 0);
    public static final RegistryEntry<DisplaySource, PotatoServerDisplaySource> POTATO_MSPT = potato("potato_mspt", 1);
    public static final RegistryEntry<DisplaySource, PotatoServerDisplaySource> POTATO_MEMORY = potato("potato_memory", 5);
    public static final RegistryEntry<DisplaySource, PotatoServerDisplaySource> POTATO_CPU = potato("potato_cpu", 4);
    public static final RegistryEntry<DisplaySource, PotatoServerDisplaySource> POTATO_UPLOAD = potato("potato_upload", 10);
    public static final RegistryEntry<DisplaySource, PotatoServerDisplaySource> POTATO_DOWNLOAD = potato("potato_download", 11);
    public static final RegistryEntry<DisplaySource, PotatoServerDisplaySource> POTATO_TIME = potato("potato_time", 3);
    public static final RegistryEntry<DisplaySource, PotatoServerDisplaySource> POTATO_ENTITIES = potato("potato_entities", 2);
    public static final RegistryEntry<DisplaySource, PotatoServerDisplaySource> POTATO_DROPPED_ITEMS = potato("potato_dropped_items", 6);
    public static final RegistryEntry<DisplaySource, PotatoServerDisplaySource> POTATO_LOADED_CHUNKS = potato("potato_loaded_chunks", 7);
    public static final RegistryEntry<DisplaySource, PotatoServerDisplaySource> POTATO_GC_COUNT = potato("potato_gc_count", 8);
    public static final RegistryEntry<DisplaySource, PotatoServerDisplaySource> POTATO_GC_TIME = potato("potato_gc_time", 9);

    private static RegistryEntry<DisplaySource, PotatoServerDisplaySource> potato(String name, int metric) {
        return REGISTRATE.displaySource(name, () -> new PotatoServerDisplaySource(metric)).register();
    }

    public static void register() {
    }
}
