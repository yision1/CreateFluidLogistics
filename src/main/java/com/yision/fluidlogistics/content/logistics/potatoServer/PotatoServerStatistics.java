package com.yision.fluidlogistics.content.logistics.potatoServer;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.IntSupplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

final class PotatoServerStatistics {
    private static final Map<ServerLevel, PotatoServerStatistics> LEVELS = new WeakHashMap<>();
    private static final int CACHE_TICKS = 20;
    private long entityTick = Long.MIN_VALUE;
    private long chunkTick = Long.MIN_VALUE;
    private int entities;
    private int droppedItems;
    private int chunks;

    static int countEntities(ServerLevel level, boolean itemsOnly) {
        return LEVELS.computeIfAbsent(level, ignored -> new PotatoServerStatistics())
            .countEntities(level.getServer().getTickCount(), level.getAllEntities(), itemsOnly);
    }

    static int countChunks(ServerLevel level) {
        return LEVELS.computeIfAbsent(level, ignored -> new PotatoServerStatistics())
            .countChunks(level.getServer().getTickCount(), () -> PotatoServerChunkTracker.countFullyLoaded(level));
    }

    int countEntities(long tick, Iterable<? extends Entity> source, boolean itemsOnly) {
        if (expired(tick, entityTick)) {
            int total = 0;
            int items = 0;
            for (Entity entity : source) {
                total++;
                if (entity instanceof ItemEntity)
                    items++;
            }
            entities = total;
            droppedItems = items;
            entityTick = tick;
        }
        return itemsOnly ? droppedItems : entities;
    }

    int countChunks(long tick, IntSupplier source) {
        if (expired(tick, chunkTick)) {
            chunks = source.getAsInt();
            chunkTick = tick;
        }
        return chunks;
    }

    private static boolean expired(long tick, long sampledAt) {
        return sampledAt == Long.MIN_VALUE || tick < sampledAt || tick - sampledAt >= CACHE_TICKS;
    }
}
