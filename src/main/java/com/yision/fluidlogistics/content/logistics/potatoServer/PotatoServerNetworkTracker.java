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
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = FluidLogistics.MODID)
@ChannelHandler.Sharable
public final class PotatoServerNetworkTracker extends ChannelDuplexHandler {
    private static final String HANDLER_NAME = FluidLogistics.MODID + "_potato_traffic";
    private static final Map<MinecraftServer, PotatoServerNetworkTracker> SERVERS = new WeakHashMap<>();
    private final AtomicLong sentBytes = new AtomicLong();
    private final AtomicLong receivedBytes = new AtomicLong();

    private PotatoServerNetworkTracker() {}

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START)
            return;
        MinecraftServer server = event.getServer();
        PotatoServerNetworkTracker tracker = SERVERS.computeIfAbsent(server, ignored -> new PotatoServerNetworkTracker());
        List<Connection> connections = server.getConnection().getConnections();
        synchronized (connections) {
            for (Connection connection : connections) {
                if (!connection.isConnected())
                    continue;
                Channel channel = connection.channel();
                if (channel.pipeline().get(HANDLER_NAME) == null)
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

    private static int messageBytes(ChannelHandlerContext context, Object message, PacketFlow flow) {
        if (message instanceof ByteBuf buffer)
            return buffer.readableBytes();
        if (!(message instanceof Packet<?> packet))
            return 0;
        FriendlyByteBuf buffer = new FriendlyByteBuf(context.alloc().buffer());
        try {
            ConnectionProtocol protocol = ConnectionProtocol.getProtocolForPacket(packet);
            protocol.getBundlerInfo(flow).unbundlePacket(packet, part -> {
                buffer.writeVarInt(protocol.getPacketId(flow, part));
                part.write(buffer);
            });
            return buffer.readableBytes();
        } finally {
            buffer.release();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        receivedBytes.addAndGet(messageBytes(context, message, PacketFlow.SERVERBOUND));
        super.channelRead(context, message);
    }

    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception {
        sentBytes.addAndGet(messageBytes(context, message, PacketFlow.CLIENTBOUND));
        super.write(context, message, promise);
    }
}
