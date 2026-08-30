package com.yision.fluidlogistics.content.fluids.fluidPort;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.contraptions.actors.psi.PortableFluidInterfaceBlockEntity;
import com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity;
import com.simibubi.create.content.redstone.DirectedDirectionalBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public abstract class AbstractFluidPortBlockEntity extends SmartBlockEntity
    implements IHaveGoggleInformation, FluidPort {

    private boolean powered;

    @Nullable
    private BlockCapabilityCache<IFluidHandler, @Nullable Direction> connectedFluidCache;
    @Nullable
    private Direction cachedTargetDirection;

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
    public void invalidate() {
        connectedFluidCache = null;
        cachedTargetDirection = null;
        super.invalidate();
    }

    @Nullable
    protected final IFluidHandler getCachedConnectedFluidHandler() {
        if (level == null) {
            return null;
        }
        Direction targetDirection = getTargetDirection();
        BlockPos targetPos = worldPosition.relative(targetDirection);
        if (!(level instanceof ServerLevel serverLevel)) {
            return level.getCapability(Capabilities.FluidHandler.BLOCK, targetPos, targetDirection.getOpposite());
        }
        if (connectedFluidCache == null || cachedTargetDirection != targetDirection) {
            connectedFluidCache = BlockCapabilityCache.create(
                Capabilities.FluidHandler.BLOCK, serverLevel, targetPos, targetDirection.getOpposite());
            cachedTargetDirection = targetDirection;
        }
        return connectedFluidCache.getCapability();
    }

    @Nullable
    protected final IFluidHandler getConnectedFluidHandler() {
        if (powered) {
            return null;
        }
        return getConnectedFluidHandlerIgnoringPower();
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

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        powered = tag.getBoolean("Powered");
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("Powered", powered);
    }
}
