package com.yision.fluidlogistics.content.schematics.network;

import com.simibubi.create.infrastructure.config.AllConfigs;
import com.yision.fluidlogistics.content.schematics.FluidSchematicPlacement;
import com.yision.fluidlogistics.content.schematics.FluidSchematicPlan.Cell;
import com.yision.fluidlogistics.content.schematics.FluidSchematicPlan.Kind;
import com.yision.fluidlogistics.content.schematics.cannon.FluidSchematicPrinter;
import com.yision.fluidlogistics.network.FluidLogisticsPackets;
import com.yision.fluidlogistics.registry.AllItems;

import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public record FluidSchematicPlacePacket(ItemStack stack) implements ServerboundPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidSchematicPlacePacket> STREAM_CODEC =
        ItemStack.STREAM_CODEC.map(FluidSchematicPlacePacket::new, FluidSchematicPlacePacket::stack);

    @Override
    public PacketTypeProvider getTypeProvider() {
        return FluidLogisticsPackets.PLACE_FLUID_SCHEMATIC;
    }

    @Override
    public void handle(ServerPlayer player) {
        if (player == null || !player.isCreative() || !AllItems.FLUID_SCHEMATIC.isIn(stack)) {
            return;
        }

        Level level = player.level();
        FluidSchematicPrinter printer = new FluidSchematicPrinter();
        printer.loadSchematic(stack, level, false);
        if (!printer.isLoaded() || printer.isErrored()) {
            return;
        }

        boolean includeAir = AllConfigs.server().schematics.creativePrintIncludesAir.get();
        while (printer.advanceCurrentPos()) {
            Cell cell = printer.currentCell();
            if (printer.shouldPlaceCurrent(level)
                && (cell.kind() != Kind.AIR || includeAir)) {
                FluidSchematicPlacement.place(
                    level, printer.getCurrentTarget(), cell, cell.kind() == Kind.FREE_SOURCE);
            }
            printer.completeCurrent();
        }
    }
}
