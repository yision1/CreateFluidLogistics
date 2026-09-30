package com.yision.fluidlogistics.content.fluids.pressureGauge;

import com.yision.fluidlogistics.FluidLogistics;
import com.simibubi.create.content.fluids.FluidPropagator;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.BlockEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = FluidLogistics.MODID)
public final class FlowMeasurementUpdates {
    private static final Map<Level, Map<BlockPos, Set<Watch>>> WATCHES = new WeakHashMap<>();

    private FlowMeasurementUpdates() {}

    static Watch watch(Level level, Collection<BlockPos> pipes) {
        Watch watch = new Watch();
        for (BlockPos pos : pipes) {
            watch.positions.add(pos.immutable());
            for (var side : Iterate.directions)
                watch.positions.add(pos.relative(side));
        }
        Map<BlockPos, Set<Watch>> index = WATCHES.computeIfAbsent(level, $ -> new HashMap<>());
        for (BlockPos pos : watch.positions)
            index.computeIfAbsent(pos, $ -> new HashSet<>()).add(watch);
        for (BlockPos pos : watch.positions) {
            if (!level.isLoaded(pos))
                continue;
            var pipe = FluidPropagator.getPipe(level, pos);
            if (pipe != null && pipe.blockEntity.getBehaviour(FlowMeasurementObserver.TYPE) == null)
                pipe.blockEntity.attachBehaviourLate(new FlowMeasurementObserver(pipe.blockEntity));
        }
        return watch;
    }

    static void attach(PressureGaugeBlockEntity meter, Watch watch) {
        release(meter);
        meter.flowWatch = watch;
        watch.users++;
    }

    public static void release(PressureGaugeBlockEntity meter) {
        Watch watch = meter.flowWatch;
        meter.flowWatch = null;
        if (watch == null || --watch.users != 0)
            return;
        Map<BlockPos, Set<Watch>> index = WATCHES.get(meter.getLevel());
        if (index == null)
            return;
        for (BlockPos pos : watch.positions) {
            Set<Watch> subscribers = index.get(pos);
            subscribers.remove(watch);
            if (subscribers.isEmpty())
                index.remove(pos);
        }
        if (index.isEmpty())
            WATCHES.remove(meter.getLevel());
    }

    public static void changed(Level level, BlockPos pos) {
        if (level == null || level.isClientSide)
            return;
        Map<BlockPos, Set<Watch>> index = WATCHES.get(level);
        if (index == null)
            return;
        Set<Watch> watches = index.get(pos);
        if (watches != null)
            for (Watch watch : watches)
                watch.invalidate(level.getGameTime());
    }

    static boolean observed(Level level, BlockPos pos) {
        Map<BlockPos, Set<Watch>> index = WATCHES.get(level);
        return index != null && index.containsKey(pos);
    }

    @SubscribeEvent
    public static void neighborsChanged(BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel() instanceof Level level) {
            changed(level, event.getPos());
            for (var side : event.getNotifiedSides())
                changed(level, event.getPos().relative(side));
        }
    }

    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load event) {
        chunkChanged(event);
    }

    @SubscribeEvent
    public static void chunkUnloaded(ChunkEvent.Unload event) {
        chunkChanged(event);
    }

    private static void chunkChanged(ChunkEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide)
            return;
        Map<BlockPos, Set<Watch>> index = WATCHES.get(level);
        if (index == null)
            return;
        var chunk = event.getChunk().getPos();
        index.forEach((pos, watches) -> {
            if ((pos.getX() >> 4) == chunk.x && (pos.getZ() >> 4) == chunk.z)
                for (Watch watch : watches)
                    watch.invalidate(level.getGameTime());
        });
    }

    static final class Watch {
        private final Set<BlockPos> positions = new HashSet<>();
        private int users;
        private boolean dirty;
        private long changedAt = Long.MIN_VALUE;

        void invalidate(long gameTime) {
            if (dirty)
                return;
            dirty = true;
            changedAt = gameTime;
        }

        boolean ready(long gameTime) {
            return dirty && gameTime > changedAt + 1;
        }
    }
}
