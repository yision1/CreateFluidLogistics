package com.yision.fluidlogistics.content.logistics.potatoServer;

import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;

import com.yision.fluidlogistics.FluidLogistics;

import net.minecraft.server.MinecraftServer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = FluidLogistics.MODID)
public final class PotatoServerTickTracker {
	private static final int SAMPLE_TICKS = 20;
	private static final long NANOS_PER_SECOND = 1_000_000_000L;
	private static final Map<MinecraftServer, PotatoServerTickTracker> SERVERS = new WeakHashMap<>();
	private final long[] sampleDurations = new long[60];
	private long totalNanos = sampleDurations.length * NANOS_PER_SECOND;
	private long previousTime;
	private boolean started;
	private int ticks;
	private int next;

	PotatoServerTickTracker() {
		Arrays.fill(sampleDurations, NANOS_PER_SECOND);
	}

	@SubscribeEvent
	public static void serverTick(ServerTickEvent.Post event) {
		SERVERS.computeIfAbsent(event.getServer(), ignored -> new PotatoServerTickTracker())
			.recordTick(System.nanoTime());
	}

	@SubscribeEvent
	public static void serverStopped(ServerStoppedEvent event) {
		SERVERS.remove(event.getServer());
	}

	static double ticksPerSecond(MinecraftServer server) {
		PotatoServerTickTracker tracker = SERVERS.get(server);
		return tracker == null ? 0 : tracker.ticksPerSecond();
	}

	void recordTick(long nanos) {
		if (!started) {
			previousTime = nanos;
			started = true;
			return;
		}
		if (++ticks < SAMPLE_TICKS)
			return;

		long elapsed = nanos - previousTime;
		previousTime = nanos;
		ticks = 0;
		if (elapsed <= 0)
			return;
		totalNanos += elapsed - sampleDurations[next];
		sampleDurations[next] = elapsed;
		next = (next + 1) % sampleDurations.length;
	}

	double ticksPerSecond() {
		return SAMPLE_TICKS * sampleDurations.length * (double) NANOS_PER_SECOND / totalNanos;
	}
}
