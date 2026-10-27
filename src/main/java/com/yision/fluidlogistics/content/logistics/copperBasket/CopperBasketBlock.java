package com.yision.fluidlogistics.content.logistics.copperBasket;

import java.util.EnumMap;
import java.util.Map;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.item.ItemHelper;
import com.yision.fluidlogistics.registry.AllBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class CopperBasketBlock extends Block
	implements IBE<CopperBasketBlockEntity>, IWrenchable, ProperWaterloggedBlock {
	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static {
		for (Direction facing : Direction.values()) {
			double[] min = { 2, 2, 2 };
			double[] max = { 14, 14, 14 };
			int axis = facing.getAxis().ordinal();
			if (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE)
				max[axis] = 16;
			else
				min[axis] = 0;
			SHAPES.put(facing,
				Shapes.join(Shapes.block(), box(min[0], min[1], min[2], max[0], max[1], max[2]), BooleanOp.ONLY_FIRST));
		}
	}

	public CopperBasketBlock(Properties properties) {
		super(properties);
		registerDefaultState(
			defaultBlockState().setValue(FACING, Direction.UP).setValue(POWERED, false).setValue(WATERLOGGED, false));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite())
			.setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()))
			.setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, POWERED, WATERLOGGED);
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
		BlockPos pos, BlockPos neighborPos) {
		updateWater(level, state, pos);
		return state;
	}

	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
		boolean moving) {
		if (!level.isClientSide) {
			boolean powered = level.hasNeighborSignal(pos);
			if (state.getValue(POWERED) != powered)
				level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
		return false;
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hit) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.getItem() instanceof WrenchItem)
			return InteractionResult.PASS;
		CopperBasketBlockEntity basket = getBlockEntity(level, pos);
		if (basket == null)
			return InteractionResult.PASS;
		if (FluidUtil.getFluidHandler(stack).isPresent()) {
			if (level.isClientSide)
				return InteractionResult.SUCCESS;
			IFluidHandler selected = selectFluid(basket, hit);
			return FluidUtil.interactWithFluidHandler(player, hand, selected) ? InteractionResult.SUCCESS
				: InteractionResult.CONSUME;
		}
		if (!level.isClientSide)
			player.openMenu(basket);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	private static IFluidHandler selectFluid(CopperBasketBlockEntity basket, BlockHitResult hit) {
		var tank = basket.getFluids();
		Vec3 point = hit.getLocation().subtract(Vec3.atCenterOf(basket.getBlockPos()));
		Vector3f local = new Vector3f((float) point.x, (float) point.y, (float) point.z);
		rotation(basket.getBlockState().getValue(FACING)).conjugate().transform(local);
		float fraction = Mth.clamp((local.x + .5f - 2 / 16f) / (12 / 16f), 0, .999999f);
		float target = fraction * tank.getFluidAmount();
		int accumulated = 0;
		FluidStack selected = FluidStack.EMPTY;
		for (int slot = 0; slot < tank.getTanks(); slot++) {
			FluidStack fluid = tank.getFluidInTank(slot);
			accumulated += fluid.getAmount();
			if (!fluid.isEmpty() && target < accumulated) {
				selected = fluid.copy();
				break;
			}
		}
		final FluidStack choice = selected;
		return new IFluidHandler() {
			@Override
			public int getTanks() {
				return tank.getTanks();
			}

			@Override
			public FluidStack getFluidInTank(int slot) {
				return tank.getFluidInTank(slot);
			}

			@Override
			public int getTankCapacity(int slot) {
				return tank.getTankCapacity(slot);
			}

			@Override
			public boolean isFluidValid(int slot, FluidStack fluid) {
				return tank.isFluidValid(slot, fluid);
			}

			@Override
			public int fill(FluidStack fluid, FluidAction action) {
				return tank.fill(fluid, action);
			}

			@Override
			public FluidStack drain(FluidStack fluid, FluidAction action) {
				return tank.drain(fluid, action);
			}

			@Override
			public FluidStack drain(int amount, FluidAction action) {
				if (choice.isEmpty())
					return FluidStack.EMPTY;
				FluidStack requested = choice.copy();
				requested.setAmount(amount);
				return tank.drain(requested, action);
			}
		};
	}

	public static Quaternionf rotation(Direction facing) {
		return new Quaternionf().rotationTo(0, 1, 0, facing.getStepX(), facing.getStepY(), facing.getStepZ());
	}

	public static boolean acceptsOutput(BlockState state, Direction movement) {
		Direction opening = state.getValue(FACING);
		return opening == Direction.UP || opening.getAxis().isHorizontal() && opening == movement.getOpposite();
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		IBE.onRemove(state, level, pos, newState);
		if (!state.is(newState.getBlock()))
			level.updateNeighbourForOutputSignal(pos, this);
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return getBlockEntityOptional(level, pos).map(be -> ItemHelper.calcRedstoneFromInventory(be.getInventory()))
			.orElse(0);
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return rotate(state, mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	public BlockState getRotatedBlockState(BlockState state, Direction targetedFace) {
		return state.cycle(FACING);
	}

	@Override
	public Class<CopperBasketBlockEntity> getBlockEntityClass() {
		return CopperBasketBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends CopperBasketBlockEntity> getBlockEntityType() {
		return AllBlockEntities.COPPER_BASKET.get();
	}
}
