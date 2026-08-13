package com.yision.fluidlogistics.content.schematics.network;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.schematics.SchematicInstances;
import com.yision.fluidlogistics.network.FluidLogisticsPackets;
import com.yision.fluidlogistics.registry.AllItems;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecs;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

public record FluidSchematicSyncPacket(
        int slot, boolean deployed, BlockPos anchor, Rotation rotation, Mirror mirror)
        implements ServerboundPacketPayload {

    public static final StreamCodec<ByteBuf, FluidSchematicSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, FluidSchematicSyncPacket::slot,
        ByteBufCodecs.BOOL, FluidSchematicSyncPacket::deployed,
        BlockPos.STREAM_CODEC, FluidSchematicSyncPacket::anchor,
        CatnipStreamCodecs.ROTATION, FluidSchematicSyncPacket::rotation,
        CatnipStreamCodecs.MIRROR, FluidSchematicSyncPacket::mirror,
        FluidSchematicSyncPacket::new
    );

    public FluidSchematicSyncPacket(
            int slot, StructurePlaceSettings settings, BlockPos anchor, boolean deployed) {
        this(slot, deployed, anchor, settings.getRotation(), settings.getMirror());
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return FluidLogisticsPackets.SYNC_FLUID_SCHEMATIC;
    }

    @Override
    public void handle(ServerPlayer player) {
        ItemStack stack = slot == -1
            ? player.getMainHandItem()
            : player.getInventory().getItem(slot);
        if (!AllItems.FLUID_SCHEMATIC.isIn(stack)) {
            return;
        }
        stack.set(AllDataComponents.SCHEMATIC_DEPLOYED, deployed);
        stack.set(AllDataComponents.SCHEMATIC_ANCHOR, anchor);
        stack.set(AllDataComponents.SCHEMATIC_ROTATION, rotation);
        stack.set(AllDataComponents.SCHEMATIC_MIRROR, mirror);
        SchematicInstances.clearHash(stack);
    }
}
