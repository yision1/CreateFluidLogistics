package com.yision.fluidlogistics.content.logistics.factoryGauge;

import org.jetbrains.annotations.ApiStatus;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;

import net.minecraft.world.item.ItemStack;

@ApiStatus.Internal
public final class FactoryGaugeDropPolicy {

    private FactoryGaugeDropPolicy() {
        throw new AssertionError("This class should not be instantiated");
    }

    public static ItemStack stackFor(FactoryPanelBehaviour behaviour) {
        if (!(behaviour instanceof ResourceFactoryPanelBehaviour resource) || !resource.isResourceGauge())
            return AllBlocks.FACTORY_GAUGE.asStack();

        return resource.registeredType()
            .<ItemStack>map(type -> new ItemStack(type.item()
                .get()))
            .orElseGet(AllBlocks.FACTORY_GAUGE::asStack);
    }
}
