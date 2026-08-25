package com.yision.fluidlogistics.content.logistics.factoryGauge;

import java.util.Map;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockItem;
import com.yision.fluidlogistics.api.factorygauge.FactoryGauges;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

public class RegisteredFactoryGaugeItem extends FactoryPanelBlockItem {

    private final ResourceLocation gaugeTypeId;

    public RegisteredFactoryGaugeItem(ResourceLocation gaugeTypeId, Item.Properties properties) {
        super(getFactoryGaugeBlock(), properties);
        this.gaugeTypeId = gaugeTypeId;
    }

    public ResourceLocation gaugeTypeId() {
        return gaugeTypeId;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult result = super.useOn(context);
        BlockPos hostPos = FactoryGaugeHostHooks.existingHostAfterFailedPlacement(result, context);
        if (hostPos == null)
            return result;

        return FactoryGauges.get(gaugeTypeId)
            .map(type -> FactoryGaugeHostHooks.useRegisteredGaugeOnExistingHost(type, context, hostPos))
            .orElse(result);
    }

    @Override
    public void registerBlocks(Map<Block, Item> blockToItem, Item self) {
    }

    @Override
    public String getDescriptionId() {
        return Util.makeDescriptionId("item", BuiltInRegistries.ITEM.getKey(this));
    }

    private static Block getFactoryGaugeBlock() {
        return AllBlocks.FACTORY_GAUGE.get();
    }
}
