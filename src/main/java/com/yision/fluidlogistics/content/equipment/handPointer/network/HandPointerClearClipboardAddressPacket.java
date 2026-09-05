package com.yision.fluidlogistics.content.equipment.handPointer.network;

import com.yision.fluidlogistics.api.handpointer.AddressEditFeedback;
import com.yision.fluidlogistics.network.FluidLogisticsPackets;
import com.yision.fluidlogistics.api.handpointer.PackagerAddresses;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public record HandPointerClearClipboardAddressPacket(BlockPos pos) implements ServerboundPacketPayload {
    private static final int STATUS_INVALID_COLOR = 0xFF6171;
    private static final int STATUS_NEUTRAL_COLOR = 0xA5A5A5;

    public static final StreamCodec<RegistryFriendlyByteBuf, HandPointerClearClipboardAddressPacket> STREAM_CODEC =
        StreamCodec.of(HandPointerClearClipboardAddressPacket::encode, HandPointerClearClipboardAddressPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, HandPointerClearClipboardAddressPacket packet) {
        BlockPos.STREAM_CODEC.encode(buf, packet.pos);
    }

    private static HandPointerClearClipboardAddressPacket decode(RegistryFriendlyByteBuf buf) {
        return new HandPointerClearClipboardAddressPacket(BlockPos.STREAM_CODEC.decode(buf));
    }

    public static void send(BlockPos pos) {
        CatnipServices.NETWORK.sendToServer(new HandPointerClearClipboardAddressPacket(pos));
    }

    @Override
    public void handle(ServerPlayer player) {
        if (!HandPointerInteractionGuard.canUseHandPointer(player, pos)) {
            return;
        }

        Level level = player.level();
        PackagerAddresses.EditResult result = PackagerAddresses.clear(level, pos);
        switch (result) {
            case NOT_TARGET -> {
                return;
            }
            case NETWORK_LINKED -> {
                player.displayClientMessage(
                    Component.translatable("logistically_linked.protected")
                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(STATUS_INVALID_COLOR))),
                    true
                );
                AddressEditFeedback.send(level, pos, player, false);
                return;
            }
            case SIGN_CONTROLLED -> {
                player.displayClientMessage(
                    Component.translatable("create.fluidlogistics.hand_pointer.address_clear_blocked_by_sign")
                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(STATUS_INVALID_COLOR))),
                    true
                );
                AddressEditFeedback.send(level, pos, player, false);
                return;
            }
            case ALREADY_EMPTY -> {
                player.displayClientMessage(
                    Component.translatable("create.fluidlogistics.hand_pointer.address_already_empty")
                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(STATUS_NEUTRAL_COLOR))),
                    true
                );
                AddressEditFeedback.send(level, pos, player, false);
                return;
            }
            case UPDATED -> {
            }
        }

        player.displayClientMessage(
            Component.translatable("create.fluidlogistics.hand_pointer.address_cleared")
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(STATUS_INVALID_COLOR))),
            true
        );
        AddressEditFeedback.send(level, pos, player, true);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return FluidLogisticsPackets.HAND_POINTER_CLEAR_CLIPBOARD_ADDRESS;
    }
}
