package com.yision.fluidlogistics.content.logistics.factoryGauge;

import java.util.UUID;

import net.minecraft.world.item.ItemStack;

public record ResourceGaugeDecision(
    boolean satisfied,
    boolean promisedSatisfied,
    boolean waitingForNetwork,
    int nextRemainingAdditionalStock,
    boolean notifyRedstoneOutputs,
    boolean playConfirmSound) {

    public record ResourceRequestPlan(
        ItemStack resourceKey,
        int amount,
        UUID network,
        Object storageIdentity,
        String address) {
    }
}

