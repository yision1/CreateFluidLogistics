package com.yision.fluidlogistics.content.schematics.network;

import com.simibubi.create.content.schematics.SchematicInstances;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.yision.fluidlogistics.registry.AllItems;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraftforge.network.NetworkEvent.Context;

public class FluidSchematicSyncPacket extends SimplePacketBase {

    private final int slot;
    private final boolean deployed;
    private final BlockPos anchor;
    private final Rotation rotation;
    private final Mirror mirror;

    public FluidSchematicSyncPacket(
            int slot, StructurePlaceSettings settings, BlockPos anchor, boolean deployed) {
        this(slot, deployed, anchor, settings.getRotation(), settings.getMirror());
    }

    private FluidSchematicSyncPacket(
            int slot, boolean deployed, BlockPos anchor, Rotation rotation, Mirror mirror) {
        this.slot = slot;
        this.deployed = deployed;
        this.anchor = anchor;
        this.rotation = rotation;
        this.mirror = mirror;
    }

    public FluidSchematicSyncPacket(FriendlyByteBuf buffer) {
        this(
            buffer.readVarInt(),
            buffer.readBoolean(),
            buffer.readBlockPos(),
            buffer.readEnum(Rotation.class),
            buffer.readEnum(Mirror.class));
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(slot);
        buffer.writeBoolean(deployed);
        buffer.writeBlockPos(anchor);
        buffer.writeEnum(rotation);
        buffer.writeEnum(mirror);
    }

    @Override
    public boolean handle(Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            ItemStack stack = slot == -1
                ? player.getMainHandItem()
                : player.getInventory().getItem(slot);
            if (!AllItems.FLUID_SCHEMATIC.isIn(stack)) {
                return;
            }
            CompoundTag tag = stack.getOrCreateTag();
            tag.putBoolean("Deployed", deployed);
            tag.put("Anchor", NbtUtils.writeBlockPos(anchor));
            tag.putString("Rotation", rotation.name());
            tag.putString("Mirror", mirror.name());
            SchematicInstances.clearHash(stack);
        });
        return true;
    }
}
