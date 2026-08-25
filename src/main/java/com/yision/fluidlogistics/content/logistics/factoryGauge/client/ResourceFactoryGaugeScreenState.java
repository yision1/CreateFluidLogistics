package com.yision.fluidlogistics.content.logistics.factoryGauge.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.yision.fluidlogistics.content.logistics.factoryGauge.ResourceFactoryPanelBehaviour;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;

final class ResourceFactoryGaugeScreenState {

    private List<FactoryPanelConnection> connections = List.of();
    private List<BigItemStack> inputConfig = List.of();
    private BigItemStack outputConfig = new BigItemStack(ItemStack.EMPTY, 1);

    public void refresh(ResourceFactoryPanelBehaviour behaviour, BlockAndTintGetter level) {
        connections = new ArrayList<>(behaviour.targetedBy.values());
        outputConfig = new BigItemStack(behaviour.getFilter(), behaviour.recipeOutput);
        inputConfig = connections.stream()
            .map(connection -> {
                FactoryPanelBehaviour source = FactoryPanelBehaviour.at(level, connection.from);
                return source == null ? new BigItemStack(ItemStack.EMPTY, 0)
                    : new BigItemStack(source.getFilter(), connection.amount);
            })
            .toList();
    }

    public List<FactoryPanelConnection> connections() {
        return connections;
    }

    public List<BigItemStack> inputConfig() {
        return inputConfig;
    }

    public BigItemStack outputConfig() {
        return outputConfig;
    }

    public boolean matchesConnectionCount(int count) {
        return inputConfig.size() == count;
    }

    public Map<FactoryPanelPosition, Integer> inputAmounts() {
        Map<FactoryPanelPosition, Integer> inputs = new HashMap<>();
        if (inputConfig.size() != connections.size())
            return inputs;
        for (int i = 0; i < inputConfig.size(); i++)
            inputs.put(connections.get(i).from, inputConfig.get(i).count);
        return inputs;
    }
}
