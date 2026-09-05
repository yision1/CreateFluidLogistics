package com.yision.fluidlogistics.content.fluids.fluidInventoryAccessPort;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.yision.fluidlogistics.content.fluids.AbstractFluidPortBlockEntity;
import com.yision.fluidlogistics.content.fluids.AbstractFluidPortHandler;
import com.yision.fluidlogistics.util.MergedFluidDisplayHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class FluidInventoryAccessPortBlockEntity extends AbstractFluidPortBlockEntity {

    private final IFluidHandler fluidHandler;
    private LazyOptional<IFluidHandler> fluidCapability;

    public FluidInventoryAccessPortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        fluidHandler = new InventoryAccessPortFluidHandler(this);
        fluidCapability = LazyOptional.of(() -> fluidHandler);
    }

    private LazyOptional<IFluidHandler> getFluidCapability(@Nullable Direction side) {
        return isOutputSide(side) ? fluidCapability : LazyOptional.empty();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        fluidCapability = LazyOptional.of(() -> fluidHandler);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return getFluidCapability(side).cast();
        }
        return super.getCapability(cap, side);
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
        return handler != null
                && containedFluidTooltip(tooltip, isPlayerSneaking, LazyOptional.of(() -> handler));
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
