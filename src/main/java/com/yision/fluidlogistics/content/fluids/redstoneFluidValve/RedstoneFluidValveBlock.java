package com.yision.fluidlogistics.content.fluids.redstoneFluidValve;

import com.simibubi.create.AllShapes;
import com.simibubi.create.api.contraption.transformable.TransformableBlock;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.IAxisPipe;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.yision.fluidlogistics.registry.AllBlockEntities;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;

public class RedstoneFluidValveBlock extends Block
        implements IAxisPipe, IBE<RedstoneFluidValveBlockEntity>, IWrenchable, ProperWaterloggedBlock, TransformableBlock {

    public static final DirectionProperty FACING = DirectionalKineticBlock.FACING;
    public static final BooleanProperty AXIS_ALONG_FIRST_COORDINATE =
            DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE;
    public static final BooleanProperty ENABLED = BooleanProperty.create("enabled");

    public RedstoneFluidValveBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.UP)
                .setValue(AXIS_ALONG_FIRST_COORDINATE, false)
                .setValue(ENABLED, true)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(FACING, AXIS_ALONG_FIRST_COORDINATE, ENABLED, WATERLOGGED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Axis pipeAxis = getPipeAxis(state);
        return switch (getCoverAxis(state, pipeAxis)) {
            case X -> Shapes.or(AllShapes.FLUID_VALVE.get(pipeAxis),
                    box(1, 3, 3, 2, 13, 13), box(14, 3, 3, 15, 13, 13));
            case Y -> Shapes.or(AllShapes.FLUID_VALVE.get(pipeAxis),
                    box(3, 1, 3, 13, 2, 13), box(3, 14, 3, 13, 15, 13));
            case Z -> Shapes.or(AllShapes.FLUID_VALVE.get(pipeAxis),
                    box(3, 3, 1, 13, 13, 2), box(3, 3, 14, 13, 13, 15));
        };
    }

    static Axis getCoverAxis(BlockState state, Axis pipeAxis) {
        Axis facingAxis = state.getValue(FACING).getAxis();
        for (Axis axis : Iterate.axes)
            if (axis != facingAxis && axis != pipeAxis)
                return axis;
        throw new IllegalStateException("Impossible axis.");
    }

    public static Axis getPipeAxis(BlockState state) {
        if (!(state.getBlock() instanceof RedstoneFluidValveBlock))
            throw new IllegalStateException("Provided BlockState is for a different block.");
        Direction facing = state.getValue(FACING);
        boolean alongFirst = !state.getValue(AXIS_ALONG_FIRST_COORDINATE);
        for (Axis axis : Iterate.axes) {
            if (axis == facing.getAxis())
                continue;
            if (!alongFirst) {
                alongFirst = true;
                continue;
            }
            return axis;
        }
        throw new IllegalStateException("Impossible axis.");
    }

    @Override
    public Axis getAxis(BlockState state) {
        return getPipeAxis(state);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        Axis pipeAxis = rotation.rotate(Direction.get(Direction.AxisDirection.POSITIVE, getPipeAxis(state))).getAxis();
        return withOrientation(state, rotation.rotate(state.getValue(FACING)), pipeAxis);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public BlockState transform(BlockState state, StructureTransform transform) {
        return withOrientation(state, transform.rotateFacing(transform.mirrorFacing(state.getValue(FACING))),
                transform.rotateAxis(getPipeAxis(state)));
    }

    private static BlockState withOrientation(BlockState state, Direction facing, Axis pipeAxis) {
        BlockState rotated = state.setValue(FACING, facing);
        return getPipeAxis(rotated) == pipeAxis ? rotated : rotated.cycle(AXIS_ALONG_FIRST_COORDINATE);
    }

    public static boolean isOpenAt(BlockState state, Direction direction) {
        return direction.getAxis() == getPipeAxis(state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getNearestLookingDirection().getOpposite();
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown())
            facing = facing.getOpposite();

        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        Axis faceAxis = facing.getAxis();
        boolean alongFirst = false;

        if (faceAxis.isHorizontal()) {
            alongFirst = faceAxis == Axis.Z;
            Direction positivePerpendicular = faceAxis == Axis.X ? Direction.SOUTH : Direction.EAST;
            boolean preferLeft = prefersConnectionTo(level, pos, positivePerpendicular);
            boolean preferRight = prefersConnectionTo(level, pos, positivePerpendicular.getOpposite());
            if (preferLeft || preferRight)
                alongFirst = faceAxis == Axis.X;
        }

        if (faceAxis.isVertical()) {
            alongFirst = context.getHorizontalDirection().getAxis() == Axis.X;
            Direction preferredSide = null;
            for (Direction side : Iterate.horizontalDirections) {
                if (!prefersConnectionTo(level, pos, side.getClockWise()))
                    continue;
                if (preferredSide != null && preferredSide.getAxis() != side.getAxis()) {
                    preferredSide = null;
                    break;
                }
                preferredSide = side;
            }
            if (preferredSide != null)
                alongFirst = preferredSide.getAxis() == Axis.X;
        }

        BlockState state = defaultBlockState()
                .setValue(FACING, facing)
                .setValue(AXIS_ALONG_FIRST_COORDINATE, alongFirst)
                .setValue(ENABLED, !level.hasNeighborSignal(pos));
        return withWater(state, context);
    }

    private boolean prefersConnectionTo(LevelReader level, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        return FluidPipeBlock.canConnectTo(level, neighborPos, level.getBlockState(neighborPos), direction);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                BlockPos neighborPos, boolean moving) {
        if (level.isClientSide)
            return;
        withBlockEntityDo(level, pos, blockEntity -> blockEntity.setPowered(level.hasNeighborSignal(pos)));
        DebugPackets.sendNeighborsUpdatePacket(level, pos);
        Direction direction = FluidPropagator.validateNeighbourChange(state, level, pos, neighborBlock, neighborPos, moving);
        if (direction != null && isOpenAt(state, direction))
            level.scheduleTick(pos, this, 1, TickPriority.HIGH);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide)
            FluidPropagator.propagateChangedPipe(level, pos, state);
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (!level.isClientSide && state != oldState)
            level.scheduleTick(pos, this, 1, TickPriority.HIGH);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        FluidPropagator.propagateChangedPipe(level, pos, state);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public Class<RedstoneFluidValveBlockEntity> getBlockEntityClass() {
        return RedstoneFluidValveBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RedstoneFluidValveBlockEntity> getBlockEntityType() {
        return AllBlockEntities.REDSTONE_FLUID_VALVE.get();
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        updateWater(level, state, pos);
        return state;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return fluidState(state);
    }
}
