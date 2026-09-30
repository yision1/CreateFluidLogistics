package com.yision.fluidlogistics.content.logistics.potatoServer;

import com.yision.fluidlogistics.FluidLogistics;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = FluidLogistics.MODID)
@ChannelHandler.Sharable
public final class PotatoServerNetworkTracker extends ChannelDuplexHandler {
    private static final String HANDLER_NAME = FluidLogistics.MODID + "_potato_traffic";
    private static final Map<MinecraftServer, PotatoServerNetworkTracker> SERVERS = new WeakHashMap<>();
    private final AtomicLong sentBytes = new AtomicLong();
    private final AtomicLong receivedBytes = new AtomicLong();

    private PotatoServerNetworkTracker() {}

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Pre event) {
        MinecraftServer server = event.getServer();
        PotatoServerNetworkTracker tracker = SERVERS.computeIfAbsent(server, ignored -> new PotatoServerNetworkTracker());
        List<Connection> connections = server.getConnection().getConnections();
        synchronized (connections) {
            for (Connection connection : connections) {
                if (!connection.isConnected())
                    continue;
                Channel channel = connection.channel();
                if (channel.pipeline().get(HANDLER_NAME) == null)
                    // At the transport end, bytes include any compression and framing in both directions.
                    channel.pipeline().addFirst(HANDLER_NAME, tracker);
            }
        }
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        SERVERS.remove(event.getServer());
    }

    static long totalBytes(MinecraftServer server, boolean upload) {
        PotatoServerNetworkTracker tracker = SERVERS.get(server);
        return tracker == null ? 0 : upload ? tracker.sentBytes.get() : tracker.receivedBytes.get();
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        if (message instanceof ByteBuf buffer)
            receivedBytes.addAndGet(buffer.readableBytes());
        super.channelRead(context, message);
    }

    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
        if (message instanceof ByteBuf buffer)
            sentBytes.addAndGet(buffer.readableBytes());
        super.write(context, message, promise);
    }
}
