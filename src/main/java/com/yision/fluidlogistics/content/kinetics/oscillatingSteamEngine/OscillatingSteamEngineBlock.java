package com.yision.fluidlogistics.content.kinetics.oscillatingSteamEngine;

import java.util.function.Predicate;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlock;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.yision.fluidlogistics.registry.AllBlockEntities;

import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class OscillatingSteamEngineBlock extends SteamEngineBlock {
	public static final BooleanProperty ROTATED = BooleanProperty.create("rotated");
	private static final int PLACEMENT_HELPER = PlacementHelpers.register(new PlacementHelper());
	private static final VoxelShape[][] SHAPES = makeShapes();

	public OscillatingSteamEngineBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(ROTATED, false));
	}

	public static BlockPos getShaftPos(BlockState state, BlockPos pos) {
		return pos.relative(getConnectedDirection(state));
	}

	public static Axis getHousingAxis(BlockState state) {
		Direction axis = state.getValue(FACING).getClockWise();
		return state.getValue(ROTATED) ? axis.getClockWise(getFacing(state).getAxis()).getAxis() : axis.getAxis();
	}

	public static boolean isShaftValid(BlockState state, BlockState shaft) {
		return SteamEngineBlock.isShaftValid(state, shaft) && shaft.getValue(ShaftBlock.AXIS) == getHousingAxis(state);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(ROTATED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		if (state == null)
			return null;
		BlockState shaft = context.getLevel().getBlockState(getShaftPos(state, context.getClickedPos()));
		if (SteamEngineBlock.isShaftValid(state, shaft))
			return state.setValue(ROTATED, shaft.getValue(ShaftBlock.AXIS) != getHousingAxis(state));
		for (Direction direction : context.getNearestLookingDirections())
			if (direction.getAxis() != getFacing(state).getAxis())
				return state.setValue(ROTATED, direction.getAxis() != getHousingAxis(state));
		return state;
	}

	@Override
	public BlockState getRotatedBlockState(BlockState state, Direction targetedFace) {
		return state.cycle(ROTATED);
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
		if (level.isClientSide)
			return;
		FluidTankBlock.updateBoilerState(state, level, pos.relative(getFacing(state).getOpposite()));
		BlockPos shaftPos = getShaftPos(state, pos);
		BlockState shaft = level.getBlockState(shaftPos);
		if (AllBlocks.SHAFT.has(shaft) && isShaftValid(state, shaft))
			level.setBlock(shaftPos, PoweredShaftBlock.getEquivalent(shaft), Block.UPDATE_ALL);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		if (!state.is(newState.getBlock()) || getFacing(state) != getFacing(newState)
			|| getHousingAxis(state) != getHousingAxis(newState)) {
			BlockPos shaftPos = getShaftPos(state, pos);
			if (!level.isClientSide && level.getBlockEntity(shaftPos) instanceof PoweredShaftBlockEntity shaft) {
				shaft.remove(pos);
				level.scheduleTick(shaftPos, shaft.getBlockState().getBlock(), 1);
			}
		}
		if (!state.is(newState.getBlock()))
			level.removeBlockEntity(pos);
		if (!level.isClientSide)
			FluidTankBlock.updateBoilerState(state, level, pos.relative(getFacing(state).getOpposite()));
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hit) {
		if (player.isShiftKeyDown() || !player.mayBuild())
			return InteractionResult.PASS;
		ItemStack stack = player.getItemInHand(hand);
		IPlacementHelper helper = PlacementHelpers.get(PLACEMENT_HELPER);
		if (helper.matchesItem(stack))
			return helper.getOffset(player, level, state, pos, hit).placeInWorld(level, (BlockItem) stack.getItem(),
				player, hand, hit);
		return InteractionResult.PASS;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[getFacing(state).ordinal()][getHousingAxis(state).ordinal()];
	}

	private static VoxelShape[][] makeShapes() {
		double[][] boxes = { { 1, 0, 1, 15, 2, 15 }, { 1, 2, 1, 3, 3, 15 }, { 13, 2, 1, 15, 3, 15 },
			{ 3, 2, 1, 13, 3, 3 }, { 3, 2, 13, 13, 3, 15 }, { 1, 3, 4, 3, 13, 12 }, { 13, 3, 4, 15, 13, 12 },
			{ 3, 3, 3, 13, 15, 13 } };
		VoxelShape[][] shapes = new VoxelShape[Direction.values().length][Axis.values().length];
		for (Direction facing : Direction.values()) {
			for (Axis axis : Axis.values()) {
				if (axis == facing.getAxis())
					continue;
				Vec3i x = Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE).getNormal();
				Vec3i y = facing.getNormal();
				Vec3i z = x.cross(y);
				VoxelShape shape = Shapes.empty();
				for (double[] box : boxes) {
					double x1 = 8 + x.getX() * (box[0] - 8) + y.getX() * (box[1] - 8) + z.getX() * (box[2] - 8);
					double y1 = 8 + x.getY() * (box[0] - 8) + y.getY() * (box[1] - 8) + z.getY() * (box[2] - 8);
					double z1 = 8 + x.getZ() * (box[0] - 8) + y.getZ() * (box[1] - 8) + z.getZ() * (box[2] - 8);
					double x2 = 8 + x.getX() * (box[3] - 8) + y.getX() * (box[4] - 8) + z.getX() * (box[5] - 8);
					double y2 = 8 + x.getY() * (box[3] - 8) + y.getY() * (box[4] - 8) + z.getY() * (box[5] - 8);
					double z2 = 8 + x.getZ() * (box[3] - 8) + y.getZ() * (box[4] - 8) + z.getZ() * (box[5] - 8);
					shape = Shapes.or(shape, Block.box(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
						Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2)));
				}
				shapes[facing.ordinal()][axis.ordinal()] = shape.optimize();
			}
		}
		return shapes;
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public BlockEntityType<? extends SteamEngineBlockEntity> getBlockEntityType() {
		return AllBlockEntities.OSCILLATING_STEAM_ENGINE.get();
	}

	private static class PlacementHelper implements IPlacementHelper {
		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return AllBlocks.SHAFT::isIn;
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return state -> state.getBlock() instanceof OscillatingSteamEngineBlock;
		}

		@Override
		public PlacementOffset getOffset(Player player, Level level, BlockState state, BlockPos pos,
			BlockHitResult hit) {
			BlockPos shaftPos = getShaftPos(state, pos);
			if (!level.getBlockState(shaftPos).canBeReplaced())
				return PlacementOffset.fail();
			return PlacementOffset.success(shaftPos,
				placed -> BlockHelper
					.copyProperties(placed,
						(level.isClientSide ? AllBlocks.SHAFT : AllBlocks.POWERED_SHAFT).getDefaultState())
					.setValue(ShaftBlock.AXIS, getHousingAxis(state)));
		}
	}
}
