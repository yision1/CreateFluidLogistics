package com.yision.fluidlogistics.content.logistics.factoryGauge;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

public record ResourceGaugeSnapshot(
    int stored,
    int promised,
    int unloadedLinks,
    boolean restocker,
    int availableOnNetwork,
    boolean storageAvailable,
    boolean satisfiedLike,
    @Nullable UUID network,
    @Nullable Object storageIdentity,
    String address) {

    public static ResourceGaugeSnapshot monitor(
        int stored, int promised, int unloadedLinks, boolean restocker,
        boolean satisfiedLike) {
        return new ResourceGaugeSnapshot(stored, promised, unloadedLinks, restocker,
            0, true, satisfiedLike, null, null, "");
    }
}
