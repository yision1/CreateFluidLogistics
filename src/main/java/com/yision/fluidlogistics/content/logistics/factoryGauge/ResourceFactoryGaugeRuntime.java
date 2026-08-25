package com.yision.fluidlogistics.content.logistics.factoryGauge;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.nbt.CompoundTag;

@ApiStatus.Internal
public final class ResourceFactoryGaugeRuntime {

    int lastStored;
    int lastPromised;
    int lastUnloadedLinks;
    int remainingAdditionalStock;
    int requestTimer;

    public void reset() {
        lastStored = 0;
        lastPromised = 0;
        lastUnloadedLinks = 0;
        remainingAdditionalStock = 0;
        requestTimer = 0;
    }

    void write(CompoundTag tag) {
        tag.putInt("LastStored", lastStored);
        tag.putInt("LastPromised", lastPromised);
        tag.putInt("LastUnloadedLinks", lastUnloadedLinks);
        tag.putInt("RemainingAdditionalStock", remainingAdditionalStock);
        tag.putInt("RequestTimer", requestTimer);
    }

}
