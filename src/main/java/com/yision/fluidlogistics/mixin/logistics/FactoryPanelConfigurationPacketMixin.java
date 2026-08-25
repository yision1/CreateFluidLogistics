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
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.yision.fluidlogistics.content.logistics.factoryGauge.ResourceFactoryPanelBehaviour;

@Mixin(FactoryPanelConfigurationPacket.class)
public abstract class FactoryPanelConfigurationPacketMixin {

    @Shadow(remap = false)
    @Final
    private PanelSlot slot;

    @Inject(
        method = "applySettings",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void fluidlogistics$rejectResourceGaugeSlots(FactoryPanelBlockEntity be, CallbackInfo ci) {
        FactoryPanelBehaviour behaviour = be.panels.get(slot);
        if (behaviour instanceof ResourceFactoryPanelBehaviour resource && resource.isResourceGauge())
            ci.cancel();
    }
}
