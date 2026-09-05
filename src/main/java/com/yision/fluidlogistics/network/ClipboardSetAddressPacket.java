package com.yision.fluidlogistics.network;

import com.simibubi.create.AllBlocks;
import com.yision.fluidlogistics.api.handpointer.AddressEditFeedback;
import com.yision.fluidlogistics.api.handpointer.PackagerAddresses;
import com.yision.fluidlogistics.util.ClipboardAddressUtil;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public record ClipboardSetAddressPacket(BlockPos pos) implements ServerboundPacketPayload {
    private static final int STATUS_CONNECTABLE_COLOR = 0x9EF173;
    private static final int STATUS_INVALID_COLOR = 0xFF6171;

    public static final StreamCodec<RegistryFriendlyByteBuf, ClipboardSetAddressPacket> STREAM_CODEC =
        StreamCodec.of(ClipboardSetAddressPacket::encode, ClipboardSetAddressPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, ClipboardSetAddressPacket packet) {
        BlockPos.STREAM_CODEC.encode(buf, packet.pos);
    }

    private static ClipboardSetAddressPacket decode(RegistryFriendlyByteBuf buf) {
        return new ClipboardSetAddressPacket(BlockPos.STREAM_CODEC.decode(buf));
    }

    public static void send(BlockPos pos) {
        CatnipServices.NETWORK.sendToServer(new ClipboardSetAddressPacket(pos));
    }

    @Override
    public void handle(ServerPlayer player) {

        if (!player.mayBuild()) {
            return;
        }

        Level level = player.level();
        if (!level.isLoaded(pos)) {
            return;
        }

        if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D) {
            return;
        }

        ItemStack heldItem = player.getMainHandItem();
        if (!AllBlocks.CLIPBOARD.isIn(heldItem)) {
            return;
        }

        String address = ClipboardAddressUtil.extractFirstAddress(heldItem);
        if (address == null) {
            if (!PackagerAddresses.isTarget(level, pos)) {
                return;
            }
            player.displayClientMessage(
                Component.translatable("create.fluidlogistics.clipboard.no_valid_address")
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(STATUS_INVALID_COLOR))),
                true
            );
            AddressEditFeedback.send(level, pos, player, false);
            return;
        }

        PackagerAddresses.EditResult result = PackagerAddresses.set(level, pos, address);
        switch (result) {
            case NOT_TARGET, ALREADY_EMPTY -> {
                return;
            }
            case SIGN_CONTROLLED -> {
                String blockTypeName = fluidlogistics$getBlockTypeName(level.getBlockState(pos));
                player.displayClientMessage(
                    Component.translatable("create.fluidlogistics.clipboard.address_set_by_sign", blockTypeName)
                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(STATUS_INVALID_COLOR))),
                    true
                );
                AddressEditFeedback.send(level, pos, player, false);
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
            case UPDATED -> {
            }
        }

        String blockTypeName = fluidlogistics$getBlockTypeName(level.getBlockState(pos));
        player.displayClientMessage(
            Component.translatable("create.fluidlogistics.clipboard.address_set", blockTypeName, address)
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(STATUS_CONNECTABLE_COLOR))),
            true
        );
        AddressEditFeedback.send(level, pos, player, true);
    }

    private static String fluidlogistics$getBlockTypeName(BlockState state) {
        return state.getBlock().getName().getString();
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return FluidLogisticsPackets.CLIPBOARD_SET_ADDRESS;
    }
}
