package com.yision.fluidlogistics.content.logistics.potatoServer;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.yision.fluidlogistics.registry.AllBlockEntities;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class PotatoServerBlock extends HorizontalDirectionalBlock implements IWrenchable, IBE<PotatoServerBlockEntity> {
    public static final MapCodec<PotatoServerBlock> CODEC = simpleCodec(PotatoServerBlock::new);
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    public PotatoServerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(PART, Part.SINGLE));
    }

    @Override
    public MapCodec<PotatoServerBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelAccessor level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState below = level.getBlockState(pos.below());
        BlockState above = level.getBlockState(pos.above());
        Direction facing = below.is(this) ? below.getValue(FACING)
            : above.is(this) ? above.getValue(FACING) : context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, facing).setValue(PART, partAt(level, pos));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction.getAxis() != Direction.Axis.Y)
            return state;
        if (direction == Direction.DOWN && neighborState.is(this))
            state = state.setValue(FACING, neighborState.getValue(FACING));
        return state.setValue(PART, partAt(level, pos));
    }

    private Part partAt(LevelAccessor level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (below.is(this) && below.getValue(PART) == Part.LOWER)
            return Part.UPPER;
        return level.getBlockState(pos.above()).is(this) ? Part.LOWER : Part.SINGLE;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        while (level.getBlockState(pos.below()).is(this))
            pos = pos.below();
        level.setBlock(pos, level.getBlockState(pos).rotate(Rotation.CLOCKWISE_90), Block.UPDATE_ALL);
        IWrenchable.playRotateSound(level, context.getClickedPos());
        return InteractionResult.SUCCESS;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    public Class<PotatoServerBlockEntity> getBlockEntityClass() {
        return PotatoServerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PotatoServerBlockEntity> getBlockEntityType() {
        return AllBlockEntities.POTATO_SERVER.get();
    }

    public enum Part implements StringRepresentable {
        SINGLE("single"), LOWER("lower"), UPPER("upper");

        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
