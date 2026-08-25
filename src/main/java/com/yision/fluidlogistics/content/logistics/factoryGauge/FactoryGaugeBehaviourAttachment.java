package com.yision.fluidlogistics.content.logistics.factoryGauge;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.api.event.BlockEntityBehaviourEvent;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;

public final class FactoryGaugeBehaviourAttachment {

    private FactoryGaugeBehaviourAttachment() {
        throw new AssertionError("This class should not be instantiated");
    }

    public static void onAttachBehaviours(BlockEntityBehaviourEvent event) {
        event.forType(AllBlockEntityTypes.FACTORY_PANEL.get(), be -> {
            for (PanelSlot slot : PanelSlot.values()) {
                FactoryPanelBehaviour old = be.panels.get(slot);
                if (old == null || old.getClass() != FactoryPanelBehaviour.class)
                    continue;

                ResourceFactoryPanelBehaviour replacement =
                    ResourceFactoryPanelBehaviour.migrateRuntimeState(be, slot, old);

                be.panels.put(slot, replacement);
                event.attach(replacement);
            }
        });
    }
}
