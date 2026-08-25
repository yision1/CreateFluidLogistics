package com.yision.fluidlogistics.content.logistics.factoryGauge;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.ApiStatus;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;

@ApiStatus.Internal
public class FactoryGaugeLootModifier implements IGlobalLootModifier {

    public static final ResourceLocation FACTORY_GAUGE_LOOT_TABLE =
        ResourceLocation.fromNamespaceAndPath("create", "blocks/factory_gauge");

    public static final FactoryGaugeLootModifier INSTANCE = new FactoryGaugeLootModifier();

    public static final com.mojang.serialization.MapCodec<FactoryGaugeLootModifier> CODEC =
        com.mojang.serialization.MapCodec.unit(INSTANCE);

    @Override
    public ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ResourceLocation queriedTable = context.getQueriedLootTableId();
        if (!FACTORY_GAUGE_LOOT_TABLE.equals(queriedTable))
            return generatedLoot;

        BlockEntity blockEntity = context.getParamOrNull(LootContextParams.BLOCK_ENTITY);
        if (!(blockEntity instanceof FactoryPanelBlockEntity panelBE))
            return generatedLoot;

        Float explosionRadius = context.getParamOrNull(LootContextParams.EXPLOSION_RADIUS);
        RandomSource random = context.getRandom();

        ObjectArrayList<ItemStack> drops = new ObjectArrayList<>();
        Map<ItemStackKey, Integer> merged = new HashMap<>();

        for (ItemStack generated : generatedLoot)
            if (!AllBlocks.FACTORY_GAUGE.isIn(generated))
                drops.add(generated);

        for (PanelSlot slot : PanelSlot.values()) {
            FactoryPanelBehaviour behaviour = panelBE.panels.get(slot);
            if (behaviour == null || !behaviour.isActive())
                continue;
            ItemStack drop = FactoryGaugeDropPolicy.stackFor(behaviour);
            if (drop.isEmpty())
                continue;
            if (explosionRadius != null && random.nextFloat() >= 1.0F / explosionRadius)
                continue;
            merged.merge(new ItemStackKey(drop), drop.getCount(), Integer::sum);
        }

        for (Map.Entry<ItemStackKey, Integer> entry : merged.entrySet()) {
            ItemStack stack = entry.getKey()
                .stack()
                .copy();
            stack.setCount(entry.getValue());
            drops.add(stack);
        }

        return drops;
    }

    @Override
    public com.mojang.serialization.MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    private record ItemStackKey(ItemStack stack) {
        @Override
        public boolean equals(Object obj) {
            return obj instanceof ItemStackKey other
                && ItemStack.isSameItemSameComponents(stack, other.stack);
        }

        @Override
        public int hashCode() {
            return ItemStack.hashItemAndComponents(stack);
        }
    }
}
