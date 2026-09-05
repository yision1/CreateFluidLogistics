package com.yision.fluidlogistics.content.fluids;

import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.redstone.DirectedDirectionalBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.yision.fluidlogistics.content.fluids.itemTransfer.HatchStyleItemTransfer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

@SuppressWarnings("deprecation")
public abstract class AbstractFluidPortBlock<T extends AbstractFluidPortBlockEntity> extends DirectedDirectionalBlock
        implements IBE<T>, IWrenchable {

    public static final BooleanProperty ATTACHED = BlockStateProperties.ATTACHED;

    protected AbstractFluidPortBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(ATTACHED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(ATTACHED));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        Direction preferredFacing = null;

        for (Direction face : context.getNearestLookingDirections()) {
            BlockPos adjacentPos = context.getClickedPos().relative(face);
            IFluidHandler handler = getBlockFluidHandler(context.getLevel(), adjacentPos, face.getOpposite());
            if (handler != null && !(handler instanceof FluidPortHandler)) {
                preferredFacing = face;
                break;
            }
        }

        if (preferredFacing == null) {
            Direction facing = context.getNearestLookingDirection();
            preferredFacing = context.getPlayer() != null && context.getPlayer().isShiftKeyDown()
                    ? facing
                    : facing.getOpposite();
        }

        if (preferredFacing.getAxis() == Axis.Y) {
            state = state.setValue(TARGET,
                    preferredFacing == Direction.UP ? AttachFace.CEILING : AttachFace.FLOOR);
            preferredFacing = context.getHorizontalDirection().getOpposite();
        }

        return state.setValue(FACING, preferredFacing);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (oldState.getBlock() == state.getBlock()) {
            return;
        }
        withBlockEntityDo(level, pos, AbstractFluidPortBlockEntity::updateConnectedStorage);
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
        super.onNeighborChange(state, level, pos, neighbor);
        withBlockEntityDo(level, pos, AbstractFluidPortBlockEntity::updateConnectedStorage);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        withBlockEntityDo(level, pos, AbstractFluidPortBlockEntity::updateConnectedStorage);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hitResult) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return InteractionResult.PASS;
        }

        Direction side = hitResult.getDirection();
        IFluidHandler handler = getBlockFluidHandler(level, pos, side);
        if (handler == null) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return HatchStyleItemTransfer.canItemBeEmptied(level, stack)
                    || HatchStyleItemTransfer.canItemBeFilled(level, stack)
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        }

        T blockEntity = getBlockEntity(level, pos);
        if (blockEntity == null) {
            return InteractionResult.PASS;
        }

        Predicate<FluidStack> filter = getTransferFilter(blockEntity, side);
        if (filter == null) {
            return InteractionResult.PASS;
        }

        boolean tankIsCreative = blockEntity.isConnectedTankCreative();
        Runnable onChanged = blockEntity::updateConnectedStorage;
        FluidHelper.FluidExchange exchange;
        FluidStack exchangedFluid;

        if (player.isSecondaryUseActive()) {
            exchangedFluid = HatchStyleItemTransfer.tryFillItem(
                    level, player, hand, stack, handler, filter, tankIsCreative, onChanged);
            if (!exchangedFluid.isEmpty()) {
                exchange = FluidHelper.FluidExchange.TANK_TO_ITEM;
            } else {
                exchangedFluid = HatchStyleItemTransfer.tryEmptyItem(
                        level, player, hand, stack, handler, filter, tankIsCreative, onChanged);
                exchange = exchangedFluid.isEmpty() ? null : FluidHelper.FluidExchange.ITEM_TO_TANK;
            }
        } else {
            exchangedFluid = HatchStyleItemTransfer.tryEmptyItem(
                    level, player, hand, stack, handler, filter, tankIsCreative, onChanged);
            if (!exchangedFluid.isEmpty()) {
                exchange = FluidHelper.FluidExchange.ITEM_TO_TANK;
            } else {
                exchangedFluid = HatchStyleItemTransfer.tryFillItem(
                        level, player, hand, stack, handler, filter, tankIsCreative, onChanged);
                exchange = exchangedFluid.isEmpty() ? null : FluidHelper.FluidExchange.TANK_TO_ITEM;
            }
        }

        if (exchange == null) {
            return HatchStyleItemTransfer.canItemBeEmptied(level, stack)
                    || HatchStyleItemTransfer.canItemBeFilled(level, stack)
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        }

        float pitch = Mth.clamp(1 - exchangedFluid.getAmount() / 16000f, 0, 1);
        pitch = pitch / 1.5f + .5f + (level.random.nextFloat() - .5f) / 4f;
        level.playSound(null, pos,
                exchange == FluidHelper.FluidExchange.ITEM_TO_TANK
                        ? FluidHelper.getEmptySound(exchangedFluid)
                        : FluidHelper.getFillSound(exchangedFluid),
                SoundSource.BLOCKS, .5f, pitch);
        return InteractionResult.SUCCESS;
    }

    @Nullable
    protected abstract Predicate<FluidStack> getTransferFilter(T blockEntity, Direction side);

    @Nullable
    private static IFluidHandler getBlockFluidHandler(Level level, BlockPos pos, @Nullable Direction side) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return null;
        }
        IFluidHandler handler = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, side).orElse(null);
        if (handler != null || side == null) {
            return handler;
        }
        return blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, null).orElse(null);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!state.getValue(ATTACHED)) {
            return 0;
        }
        BlockPos targetPos = pos.relative(getTargetDirection(state));
        BlockState targetState = level.getBlockState(targetPos);
        if (targetState.is(this)) {
            return 0;
        }
        return targetState.hasAnalogOutputSignal() ? targetState.getAnalogOutputSignal(level, targetPos) : 0;
    }
}
