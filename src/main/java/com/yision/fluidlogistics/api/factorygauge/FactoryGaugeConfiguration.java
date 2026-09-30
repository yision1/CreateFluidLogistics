package com.yision.fluidlogistics.api.factorygauge;

import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;

public record FactoryGaugeConfiguration(
    FactoryPanelPosition position,
    String address,
    Map<FactoryPanelPosition, Integer> inputAmounts,
    int outputAmount,
    int targetAmount,
    int promiseClearingInterval,
    int restockThreshold,
    int promiseLimit,
    int additionalStock,
    boolean enhancementsVisible,
    @Nullable FactoryPanelPosition removeConnection,
    boolean clearPromises,
    boolean reset,
    boolean redstoneReset) {

    public FactoryGaugeConfiguration {
        Objects.requireNonNull(position, "position");
        address = Objects.requireNonNullElse(address, "");
        inputAmounts = Map.copyOf(Objects.requireNonNull(inputAmounts, "inputAmounts"));
    }
}
