package com.yision.fluidlogistics.content.fluids;

import java.util.List;

import javax.annotation.Nullable;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.contraptions.actors.psi.PortableFluidInterfaceBlockEntity;
import com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity;
import com.simibubi.create.content.redstone.DirectedDirectionalBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.yision.fluidlogistics.util.SidedCapabilityCache;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;

public abstract class AbstractFluidPortBlockEntity extends SmartBlockEntity
        implements IHaveGoggleInformation, FluidPort {

    private final SidedCapabilityCache<IFluidHandler> connectedCache =
            new SidedCapabilityCache<>(ForgeCapabilities.FLUID_HANDLER);
    private boolean powered;

    protected AbstractFluidPortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void initialize() {
        super.initialize();
        updateConnectedStorage();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) {
            return;
        }
        updateConnectedStorage();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        connectedCache.clear();
    }

    @Nullable
    protected final IFluidHandler getCachedConnectedFluidHandler() {
        if (level == null) {
            return null;
        }
        Direction targetDirection = getTargetDirection();
        BlockPos targetPos = worldPosition.relative(targetDirection);
        IFluidHandler handler = connectedCache.get(level, targetPos, targetDirection);
        return handler != null ? handler : getBlockFluidHandler(targetPos, null);
    }

    @Nullable
    protected final IFluidHandler getConnectedFluidHandler() {
        return powered ? null : getConnectedFluidHandlerIgnoringPower();
    }

    @Nullable
    protected final IFluidHandler getConnectedFluidHandlerIgnoringPower() {
        IFluidHandler handler = getCachedConnectedFluidHandler();
        return handler instanceof FluidPortHandler ? null : handler;
    }

    public final void updateConnectedStorage() {
        Level currentLevel = getLevel();
        if (currentLevel == null) {
            return;
        }

        boolean newPowered = currentLevel.hasNeighborSignal(worldPosition);
        boolean poweredChanged = powered != newPowered;
        powered = newPowered;

        boolean attached = getConnectedFluidHandler() != null;
        BlockState state = getBlockState();
        boolean attachedChanged = state.getValue(BlockStateProperties.ATTACHED) != attached;
        if (attachedChanged) {
            currentLevel.setBlockAndUpdate(worldPosition, state.setValue(BlockStateProperties.ATTACHED, attached));
        }
        if (poweredChanged || attachedChanged) {
            notifyUpdate();
        }
    }

    @Override
    public final boolean blocksFluidPackagerPlacement(Direction side) {
        if (!isOutputSide(side) || level == null) {
            return false;
        }
        BlockPos targetPos = worldPosition.relative(getTargetDirection());
        return level.getBlockEntity(targetPos) instanceof PortableFluidInterfaceBlockEntity;
    }

    public final boolean isConnectedTankCreative() {
        if (level == null) {
            return false;
        }
        BlockPos targetPos = worldPosition.relative(getTargetDirection());
        return level.getBlockEntity(targetPos) instanceof CreativeFluidTankBlockEntity;
    }

    protected abstract boolean isOutputSide(@Nullable Direction side);

    protected final boolean isPowered() {
        return powered;
    }

    protected final Direction getTargetDirection() {
        return DirectedDirectionalBlock.getTargetDirection(getBlockState());
    }

    @Nullable
    private IFluidHandler getBlockFluidHandler(BlockPos pos, @Nullable Direction side) {
        if (level == null) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return null;
        }
        return blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, side).orElse(null);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        powered = tag.getBoolean("Powered");
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putBoolean("Powered", powered);
    }
}
