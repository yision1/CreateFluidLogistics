package com.yision.fluidlogistics.content.logistics.potatoServer;

import com.yision.fluidlogistics.FluidLogistics;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.level.ChunkEvent;

@EventBusSubscriber(modid = FluidLogistics.MODID)
public final class PotatoServerChunkTracker {
    private static final Map<ServerLevel, LongSet> LOADED = new WeakHashMap<>();

    private PotatoServerChunkTracker() {}

    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level)
            LOADED.computeIfAbsent(level, ignored -> new LongOpenHashSet()).add(event.getChunk().getPos().toLong());
    }

    @SubscribeEvent
    public static void chunkUnloaded(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LongSet chunks = LOADED.get(level);
            if (chunks != null)
                chunks.remove(event.getChunk().getPos().toLong());
        }
    }

    public static int countFullyLoaded(ServerLevel level) {
        LongSet chunks = LOADED.get(level);
        if (chunks == null)
            return 0;

        int count = 0;
        for (long packed : chunks)
            if (level.getChunkSource().getChunkNow(ChunkPos.getX(packed), ChunkPos.getZ(packed)) != null)
                count++;
        return count;
    }
}
