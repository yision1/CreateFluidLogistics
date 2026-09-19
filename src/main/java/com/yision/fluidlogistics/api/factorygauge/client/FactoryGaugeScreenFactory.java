package com.yision.fluidlogistics.api.factorygauge.client;

import com.yision.fluidlogistics.content.logistics.factoryGauge.ResourceFactoryPanelBehaviour;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface FactoryGaugeScreenFactory {
    Screen create(ResourceFactoryPanelBehaviour behaviour);
}
