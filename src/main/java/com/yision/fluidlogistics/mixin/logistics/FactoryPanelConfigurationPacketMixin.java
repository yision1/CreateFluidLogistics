package com.yision.fluidlogistics.mixin.logistics;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConfigurationPacket;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.yision.fluidlogistics.content.logistics.factoryGauge.ResourceFactoryPanelBehaviour;

import net.minecraft.server.level.ServerPlayer;

@Mixin(FactoryPanelConfigurationPacket.class)
public abstract class FactoryPanelConfigurationPacketMixin {

    @Shadow(remap = false)
    @Final
    private FactoryPanelPosition position;

    @Inject(
        method = "applySettings",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void fluidlogistics$rejectResourceGaugeSlots(ServerPlayer player, FactoryPanelBlockEntity be,
        CallbackInfo ci) {
        FactoryPanelBehaviour behaviour = be.panels.get(position.slot());
        if (behaviour instanceof ResourceFactoryPanelBehaviour resource && resource.isResourceGauge())
            ci.cancel();
    }
}
