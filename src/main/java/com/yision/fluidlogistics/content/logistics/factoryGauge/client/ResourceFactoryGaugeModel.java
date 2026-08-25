package com.yision.fluidlogistics.content.logistics.factoryGauge.client;

import java.util.EnumMap;
import java.util.Map;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelState;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelType;
import com.yision.fluidlogistics.api.factorygauge.client.FactoryGaugeModelSet;
import com.yision.fluidlogistics.content.logistics.factoryGauge.ResourceFactoryPanelBehaviour;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelProperty;

@OnlyIn(Dist.CLIENT)
public final class ResourceFactoryGaugeModel {

    public static final ModelProperty<Map<PanelSlot, ResourceLocation>> GAUGE_TYPE_PROPERTY = new ModelProperty<>();

    private ResourceFactoryGaugeModel() {
        throw new AssertionError("This class should not be instantiated");
    }

    public static Map<PanelSlot, ResourceLocation> collectGaugeTypes(
        net.minecraft.world.level.BlockAndTintGetter world, net.minecraft.core.BlockPos pos) {
        Map<PanelSlot, ResourceLocation> types = new EnumMap<>(PanelSlot.class);
        if (!(world.getBlockEntity(pos) instanceof FactoryPanelBlockEntity be))
            return types;
        for (PanelSlot slot : PanelSlot.values()) {
            if (!(be.panels.get(slot) instanceof ResourceFactoryPanelBehaviour resource)
                || !resource.isResourceGauge())
                continue;
            types.put(slot, resource.gaugeTypeId());
        }
        return types;
    }

    public static PartialModel panelFor(FactoryGaugeModelSet set, PanelType type, PanelState panelState) {
        if (panelState == PanelState.PASSIVE)
            return type == PanelType.NETWORK ? set.panel() : set.panelRestocker();
        return type == PanelType.NETWORK ? set.panelWithBulb() : set.panelRestockerWithBulb();
    }
}
