package com.yision.fluidlogistics.content.logistics.factoryGauge;

import net.minecraft.world.item.ItemStack;

public record ResourceFactoryGaugeConfig(
    ItemStack normalizedResourceKey,
    int targetAmount,
    int restockThreshold,
    int promiseLimit,
    int additionalStock) {

    public boolean hasAdditionalStock() {
        return additionalStock > 0;
    }
}
