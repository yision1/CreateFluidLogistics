package com.yision.fluidlogistics.network;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.content.equipment.handPointer.network.HandPointerArmPlacementPacket;
import com.yision.fluidlogistics.content.equipment.handPointer.network.HandPointerAuthorizeLogisticsNetworkPacket;
import com.yision.fluidlogistics.content.equipment.handPointer.network.HandPointerClearClipboardAddressPacket;
import com.yision.fluidlogistics.content.equipment.handPointer.network.HandPointerCrafterConnectionPacket;
import com.yision.fluidlogistics.content.equipment.handPointer.network.HandPointerDisplayLinkConfigurationPacket;
import com.yision.fluidlogistics.content.equipment.handPointer.network.HandPointerFrogportConnectionPacket;
import com.yision.fluidlogistics.content.equipment.handPointer.network.HandPointerLogisticsNetworkPacket;
import com.yision.fluidlogistics.content.equipment.handPointer.network.HandPointerMailboxStationConnectionPacket;
import com.yision.fluidlogistics.content.equipment.handPointer.network.HandPointerOpenFilterMenuPacket;
import com.yision.fluidlogistics.content.equipment.handPointer.network.HandPointerPackagerTogglePacket;
import com.yision.fluidlogistics.content.equipment.mechanicalFluidGun.network.MechanicalFluidGunPackets;
import com.yision.fluidlogistics.content.fluids.faucet.network.FaucetDripParticlePacket;
import com.yision.fluidlogistics.content.schematics.network.FluidSchematicPlacePacket;
import com.yision.fluidlogistics.content.schematics.network.FluidSchematicSyncPacket;
import com.yision.fluidlogistics.network.factoryPanel.FactoryPanelSetResourceRestockSettingPacket;
import com.yision.fluidlogistics.network.factoryPanel.ResourceFactoryGaugeConfigurePacket;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PacketDistributor.TargetPoint;
import net.minecraftforge.network.simple.SimpleChannel;

public enum FluidLogisticsPackets {

    CLIPBOARD_SET_ADDRESS(ClipboardSetAddressPacket.class, ClipboardSetAddressPacket::new,
        NetworkDirection.PLAY_TO_SERVER),
    FACTORY_PANEL_SET_RESOURCE_RESTOCK_SETTING(FactoryPanelSetResourceRestockSettingPacket.class,
        FactoryPanelSetResourceRestockSettingPacket::new, NetworkDirection.PLAY_TO_SERVER),
    RESOURCE_FACTORY_GAUGE_CONFIGURE(ResourceFactoryGaugeConfigurePacket.class,
        ResourceFactoryGaugeConfigurePacket::new, NetworkDirection.PLAY_TO_SERVER),
    HAND_POINTER_AUTHORIZE_LOGISTICS_NETWORK(HandPointerAuthorizeLogisticsNetworkPacket.class,
        HandPointerAuthorizeLogisticsNetworkPacket::new, NetworkDirection.PLAY_TO_SERVER),
    HAND_POINTER_ARM_PLACEMENT(HandPointerArmPlacementPacket.class, HandPointerArmPlacementPacket::new,
        NetworkDirection.PLAY_TO_SERVER),
    HAND_POINTER_CRAFTER_CONNECTION(HandPointerCrafterConnectionPacket.class,
        HandPointerCrafterConnectionPacket::new, NetworkDirection.PLAY_TO_SERVER),
    HAND_POINTER_DISPLAY_LINK_CONFIGURATION(HandPointerDisplayLinkConfigurationPacket.class,
        HandPointerDisplayLinkConfigurationPacket::new, NetworkDirection.PLAY_TO_SERVER),
    HAND_POINTER_FROGPORT_CONNECTION(HandPointerFrogportConnectionPacket.class,
        HandPointerFrogportConnectionPacket::new, NetworkDirection.PLAY_TO_SERVER),
    HAND_POINTER_MAILBOX_STATION_CONNECTION(HandPointerMailboxStationConnectionPacket.class,
        HandPointerMailboxStationConnectionPacket::new, NetworkDirection.PLAY_TO_SERVER),
    HAND_POINTER_LOGISTICS_NETWORK(HandPointerLogisticsNetworkPacket.class, HandPointerLogisticsNetworkPacket::new,
        NetworkDirection.PLAY_TO_SERVER),
    HAND_POINTER_CLEAR_CLIPBOARD_ADDRESS(HandPointerClearClipboardAddressPacket.class,
        HandPointerClearClipboardAddressPacket::new, NetworkDirection.PLAY_TO_SERVER),
    HAND_POINTER_OPEN_FILTER_MENU(HandPointerOpenFilterMenuPacket.class, HandPointerOpenFilterMenuPacket::new,
        NetworkDirection.PLAY_TO_SERVER),
    HAND_POINTER_PACKAGER_TOGGLE(HandPointerPackagerTogglePacket.class, HandPointerPackagerTogglePacket::new,
        NetworkDirection.PLAY_TO_SERVER),
    FAUCET_DRIP_PARTICLE(FaucetDripParticlePacket.class, FaucetDripParticlePacket::new,
        NetworkDirection.PLAY_TO_CLIENT),
    MECHANICAL_FLUID_GUN_TARGET(MechanicalFluidGunPackets.TargetPacket.class,
        MechanicalFluidGunPackets.TargetPacket::new, NetworkDirection.PLAY_TO_SERVER),
    MECHANICAL_FLUID_GUN_ITEM_TARGET_SELECTION(MechanicalFluidGunPackets.ItemTargetSelectionPacket.class,
        MechanicalFluidGunPackets.ItemTargetSelectionPacket::new, NetworkDirection.PLAY_TO_SERVER),
    MECHANICAL_FLUID_GUN_SPRAY_PARTICLE(MechanicalFluidGunPackets.SprayParticlePacket.class,
        MechanicalFluidGunPackets.SprayParticlePacket::new, NetworkDirection.PLAY_TO_CLIENT),
    MECHANICAL_FLUID_GUN_VISUAL_STATE(MechanicalFluidGunPackets.VisualStatePacket.class,
        MechanicalFluidGunPackets.VisualStatePacket::new, NetworkDirection.PLAY_TO_CLIENT),
    PLACE_FLUID_SCHEMATIC(FluidSchematicPlacePacket.class, FluidSchematicPlacePacket::new,
        NetworkDirection.PLAY_TO_SERVER),
    SYNC_FLUID_SCHEMATIC(FluidSchematicSyncPacket.class, FluidSchematicSyncPacket::new,
        NetworkDirection.PLAY_TO_SERVER);

    public static final String NETWORK_VERSION = "1.0";

    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
        .named(FluidLogistics.asResource("main"))
        .networkProtocolVersion(() -> NETWORK_VERSION)
        .clientAcceptedVersions(NETWORK_VERSION::equals)
        .serverAcceptedVersions(NETWORK_VERSION::equals)
        .simpleChannel();

    private final PacketType<?> packetType;

    <T extends SimplePacketBase> FluidLogisticsPackets(Class<T> type, Function<FriendlyByteBuf, T> decoder,
            NetworkDirection direction) {
        packetType = new PacketType<>(type, decoder, direction);
    }

    public static void register() {
        for (FluidLogisticsPackets packet : values()) {
            packet.packetType.register();
        }
    }

    public static SimpleChannel getChannel() {
        return CHANNEL;
    }

    public static void sendToNear(Level level, BlockPos pos, int range, Object message) {
        CHANNEL.send(PacketDistributor.NEAR.with(TargetPoint.p(pos.getX(), pos.getY(), pos.getZ(), range,
            level.dimension())), message);
    }

    public static void sendToNear(Level level, Vec3 pos, int range, Object message) {
        CHANNEL.send(PacketDistributor.NEAR.with(TargetPoint.p(pos.x, pos.y, pos.z, range, level.dimension())),
            message);
    }

    private static class PacketType<T extends SimplePacketBase> {

        private static int index;

        private final BiConsumer<T, FriendlyByteBuf> encoder;
        private final Function<FriendlyByteBuf, T> decoder;
        private final BiConsumer<T, Supplier<Context>> handler;
        private final Class<T> type;
        private final NetworkDirection direction;

        private PacketType(Class<T> type, Function<FriendlyByteBuf, T> decoder, NetworkDirection direction) {
            this.encoder = T::write;
            this.decoder = decoder;
            this.handler = (packet, contextSupplier) -> {
                Context context = contextSupplier.get();
                if (packet.handle(context)) {
                    context.setPacketHandled(true);
                }
            };
            this.type = type;
            this.direction = direction;
        }

        private void register() {
            CHANNEL.messageBuilder(type, index++, direction)
                .encoder(encoder)
                .decoder(decoder)
                .consumerNetworkThread(handler)
                .add();
        }
    }
}
