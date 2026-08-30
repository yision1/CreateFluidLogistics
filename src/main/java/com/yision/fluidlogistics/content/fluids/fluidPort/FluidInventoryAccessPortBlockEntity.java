package com.yision.fluidlogistics.content.fluids.fluidPort;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.yision.fluidlogistics.content.fluids.fluidPort.AbstractFluidPortBlockEntity;
import com.yision.fluidlogistics.content.fluids.fluidPort.AbstractFluidPortHandler;
import com.yision.fluidlogistics.registry.AllBlockEntities;
import com.yision.fluidlogistics.util.MergedFluidDisplayHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class FluidInventoryAccessPortBlockEntity extends AbstractFluidPortBlockEntity {

    private final IFluidHandler fluidCapability;

    public FluidInventoryAccessPortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        fluidCapability = new InventoryAccessPortFluidHandler(this);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, AllBlockEntities.FLUID_INVENTORY_ACCESS_PORT.get(),
            (be, side) -> be.getFluidCapability(side));
    }

    @Nullable
    public IFluidHandler getFluidCapability(@Nullable Direction side) {
        return isOutputSide(side) ? fluidCapability : null;
    }

    @Nullable
    @Override
    public IFluidHandler getFluidDisplayCapability(@Nullable Direction side) {
        IFluidHandler handler = getConnectedFluidHandler();
        return handler == null ? null : new MergedFluidDisplayHandler(handler);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        IFluidHandler handler = getConnectedFluidHandler();
        return handler != null && containedFluidTooltip(tooltip, isPlayerSneaking, handler);
    }

    @Override
    protected boolean isOutputSide(@Nullable Direction side) {
        return side == null || side != getTargetDirection();
    }

    private static class InventoryAccessPortFluidHandler extends AbstractFluidPortHandler {
        private final FluidInventoryAccessPortBlockEntity blockEntity;

        private InventoryAccessPortFluidHandler(FluidInventoryAccessPortBlockEntity blockEntity) {
            this.blockEntity = blockEntity;
        }

        @Nullable
        @Override
        protected IFluidHandler getSourceHandler() {
            return blockEntity.getConnectedFluidHandler();
        }
    }
}
