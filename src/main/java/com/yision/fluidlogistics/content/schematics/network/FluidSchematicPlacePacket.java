package com.yision.fluidlogistics.content.schematics.network;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.yision.fluidlogistics.content.schematics.FluidSchematicPlacement;
import com.yision.fluidlogistics.content.schematics.FluidSchematicPlan.Cell;
import com.yision.fluidlogistics.content.schematics.FluidSchematicPlan.Kind;
import com.yision.fluidlogistics.content.schematics.cannon.FluidSchematicPrinter;
import com.yision.fluidlogistics.registry.AllItems;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent.Context;

public class FluidSchematicPlacePacket extends SimplePacketBase {

    private final ItemStack stack;

    public FluidSchematicPlacePacket(ItemStack stack) {
        this.stack = stack;
    }

    public FluidSchematicPlacePacket(FriendlyByteBuf buffer) {
        this.stack = buffer.readItem();
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeItem(stack);
    }

    @Override
    public boolean handle(Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
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
        });
        return true;
    }
}
