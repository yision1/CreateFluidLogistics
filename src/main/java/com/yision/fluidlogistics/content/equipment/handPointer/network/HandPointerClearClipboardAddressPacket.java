package com.yision.fluidlogistics.content.equipment.handPointer.network;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.yision.fluidlogistics.api.handpointer.AddressEditFeedback;
import com.yision.fluidlogistics.api.handpointer.PackagerAddresses;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent.Context;

public class HandPointerClearClipboardAddressPacket extends SimplePacketBase {
    private static final int STATUS_INVALID_COLOR = 0xFF6171;
    private static final int STATUS_NEUTRAL_COLOR = 0xA5A5A5;

    private final BlockPos pos;

    public HandPointerClearClipboardAddressPacket(BlockPos pos) {
        this.pos = pos;
    }

    public HandPointerClearClipboardAddressPacket(FriendlyByteBuf buffer) {
        this.pos = buffer.readBlockPos();
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
    }

    @Override
    public boolean handle(Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
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
                    fluidlogistics$sendStatus(player, STATUS_INVALID_COLOR, "logistically_linked.protected");
                    AddressEditFeedback.send(level, pos, false);
                    return;
                }
                case SIGN_CONTROLLED -> {
                    fluidlogistics$sendStatus(player, STATUS_INVALID_COLOR,
                        "create.fluidlogistics.hand_pointer.address_clear_blocked_by_sign");
                    AddressEditFeedback.send(level, pos, false);
                    return;
                }
                case ALREADY_EMPTY -> {
                    fluidlogistics$sendStatus(player, STATUS_NEUTRAL_COLOR,
                        "create.fluidlogistics.hand_pointer.address_already_empty");
                    AddressEditFeedback.send(level, pos, false);
                    return;
                }
                case UPDATED -> {
                }
            }

            fluidlogistics$sendStatus(player, STATUS_INVALID_COLOR,
                "create.fluidlogistics.hand_pointer.address_cleared");
            AddressEditFeedback.send(level, pos, true);
        });
        return true;
    }

    private static void fluidlogistics$sendStatus(ServerPlayer player, int color, String key) {
        player.displayClientMessage(Component.translatable(key)
            .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))), true);
    }

}
