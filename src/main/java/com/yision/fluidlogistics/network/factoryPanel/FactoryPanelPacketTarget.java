package com.yision.fluidlogistics.network.factoryPanel;

import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.foundation.utility.AdventureUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

final class FactoryPanelPacketTarget {

    private static final double MAX_RANGE = 20;

    private FactoryPanelPacketTarget() {
    }

    @Nullable
    static FactoryPanelBehaviour resolve(ServerPlayer player, FactoryPanelPosition panelPosition) {
        if (player.isSpectator() || AdventureUtil.isAdventure(player)) {
            return null;
        }

        Level level = player.level();
        BlockPos pos = panelPosition.pos();
        if (!level.isLoaded(pos) || !pos.closerThan(player.blockPosition(), MAX_RANGE)) {
            return null;
        }

        FactoryPanelBehaviour behaviour = FactoryPanelBehaviour.at(level, panelPosition);
        if (behaviour == null || !Create.LOGISTICS.mayInteract(behaviour.network, player)) {
            return null;
        }
        return behaviour;
    }
}
