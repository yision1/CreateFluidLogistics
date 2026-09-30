package com.yision.fluidlogistics.content.fluids.pressureGauge;

import com.simibubi.create.api.contraption.transformable.TransformableBlock;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.pipes.AxisPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.kinetics.gauge.GaugeBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.yision.fluidlogistics.registry.AllBlockEntities;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.theme.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

public class PressureGaugeBlock extends AxisPipeBlock implements IBE<PressureGaugeBlockEntity>, ProperWaterloggedBlock,
    TransformableBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public PressureGaugeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Z).setValue(FACING, Direction.UP)
            .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        Direction attachedSide = face.getOpposite();
        Direction.Axis axis = getAxisForPlacement(context, face);
        Direction facing = face;

        if (canConnectTo(context, attachedSide, false)) {
            axis = face.getAxis();
            facing = getFacingForAttachedConnection(context, face);
        }

        return withWater(defaultBlockState().setValue(AXIS, axis).setValue(FACING, facing), context);
    }

    private static Direction.Axis getAxisForPlacement(BlockPlaceContext context, Direction face) {
        Direction.Axis faceAxis = face.getAxis();
        if (faceAxis.isHorizontal()) {
            if (hasConnectionAlong(context, Direction.Axis.Y, false))
                return Direction.Axis.Y;
            return faceAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        }

        Direction.Axis fallback = context.getHorizontalDirection().getAxis() == Direction.Axis.X
            ? Direction.Axis.Z : Direction.Axis.X;
        Direction.Axis preferred = getHorizontalConnectionAxis(context, true, fallback);
        if (preferred == null)
            preferred = getHorizontalConnectionAxis(context, false, fallback);
        return preferred == null ? fallback : preferred;
    }

    private static Direction.Axis getHorizontalConnectionAxis(BlockPlaceContext context, boolean pipesOnly,
        Direction.Axis fallback) {
        boolean alongX = hasConnectionAlong(context, Direction.Axis.X, pipesOnly);
        boolean alongZ = hasConnectionAlong(context, Direction.Axis.Z, pipesOnly);
        if (alongX == alongZ)
            return alongX ? fallback : null;
        return alongX ? Direction.Axis.X : Direction.Axis.Z;
    }

    private static boolean hasConnectionAlong(BlockPlaceContext context, Direction.Axis axis, boolean pipesOnly) {
        for (Direction side : Iterate.directions)
            if (side.getAxis() == axis && canConnectTo(context, side, pipesOnly))
                return true;
        return false;
    }

    private static Direction getFacingForAttachedConnection(BlockPlaceContext context, Direction face) {
        Direction nearest = context.getNearestLookingDirection();
        boolean lookPositive = nearest.getAxisDirection() == AxisDirection.POSITIVE;
        return switch (face.getAxis()) {
            case X -> lookPositive ? Direction.NORTH : Direction.SOUTH;
            case Y -> context.getHorizontalDirection().getOpposite();
            case Z -> lookPositive ? Direction.WEST : Direction.EAST;
        };
    }

    private static boolean canConnectTo(BlockPlaceContext context, Direction side, boolean pipesOnly) {
        BlockPos neighbour = context.getClickedPos().relative(side);
        return (!pipesOnly || FluidPropagator.getPipe(context.getLevel(), neighbour) != null)
            && FluidPipeBlock.canConnectTo(context.getLevel(), neighbour,
            context.getLevel().getBlockState(neighbour), side);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
        Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos,
        Player player) {
        return new ItemStack(this);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction side, BlockState neighbour, LevelAccessor level,
        BlockPos pos, BlockPos neighbourPos) {
        updateWater(level, state, pos);
        return state;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return fluidState(state);
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return GaugeBlock.GAUGE.get(state.getValue(FACING), isAxisAlongFirstCoordinate(state));
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof PressureGaugeBlockEntity gauge)
            return gauge.getComparatorOutput();
        return 0;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof PressureGaugeBlockEntity gauge)
            || gauge.getDialTarget() == 0)
            return;

        Vector3f rgb = new Color(gauge.getColor()).asVectorF();
        int particleCount = gauge.getDialTarget() > 1 ? 4 : 1;

        for (Direction face : Iterate.directions) {
            if (!shouldRenderDialOnFace(level, pos, state, face)
                || particleCount == 1 && random.nextFloat() > .25f)
                continue;
            Vec3 faceVector = Vec3.atLowerCornerOf(face.getNormal());
            Direction positiveFace = Direction.get(AxisDirection.POSITIVE, face.getAxis());
            Vec3 positiveFaceVector = Vec3.atLowerCornerOf(positiveFace.getNormal());

            for (int i = 0; i < particleCount; i++) {
                Vec3 motion = VecHelper.offsetRandomly(Vec3.ZERO, random, .25f)
                    .multiply(new Vec3(1, 1, 1).subtract(positiveFaceVector))
                    .normalize()
                    .scale(.3f);
                Vec3 offset = VecHelper.getCenterOf(pos).add(faceVector.scale(.55)).add(motion);
                level.addParticle(new DustParticleOptions(rgb, 1), offset.x, offset.y, offset.z,
                    motion.x, motion.y, motion.z);
            }
        }
    }

    static boolean shouldRenderDialOnFace(Level level, BlockPos pos, BlockState state, Direction face) {
        if (face.getAxis().isVertical()
            || face == state.getValue(FACING).getOpposite()
            || face.getAxis() == state.getValue(AXIS)
            || state.getValue(AXIS) == Direction.Axis.Y && face != state.getValue(FACING))
            return false;
        return Block.shouldRenderFace(state, level, pos, face, pos.relative(face));
    }

    public static boolean isAxisAlongFirstCoordinate(BlockState state) {
        return switch (state.getValue(FACING).getAxis()) {
            case X -> state.getValue(AXIS) == Direction.Axis.Y;
            case Y, Z -> state.getValue(AXIS) == Direction.Axis.X;
        };
    }

    @Override
    public BlockState getRotatedBlockState(BlockState state, Direction targetedFace) {
        Direction.Axis rotationAxis = targetedFace.getAxis();
        Direction rotatedAxis = Direction.get(AxisDirection.POSITIVE, state.getValue(AXIS))
            .getClockWise(rotationAxis);
        return state.setValue(AXIS, rotatedAxis.getAxis())
            .setValue(FACING, state.getValue(FACING).getClockWise(rotationAxis));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return super.rotate(state, rotation).setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public BlockState transform(BlockState state, StructureTransform transform) {
        return state.setValue(AXIS, transform.rotateAxis(state.getValue(AXIS)))
            .setValue(FACING, transform.rotateFacing(transform.mirrorFacing(state.getValue(FACING))));
    }

    @Override
    public Class<PressureGaugeBlockEntity> getBlockEntityClass() {
        return PressureGaugeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PressureGaugeBlockEntity> getBlockEntityType() {
        return AllBlockEntities.FLOW_METER.get();
    }
}
