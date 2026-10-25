package com.yision.fluidlogistics.content.processing.copperBasin;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.processing.basin.BasinBlock;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import com.simibubi.create.foundation.utility.CreateLang;
import com.yision.fluidlogistics.registry.AllBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class CopperBasinBlockEntity extends BasinBlockEntity {
	@Nullable
	private BlockCapabilityCache<IFluidHandler, @Nullable Direction> outputTargetCache;
	@Nullable
	private Direction cachedTargetFacing;

	public CopperBasinBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		behaviours.remove(inputTank);
		behaviours.remove(outputTank);

		inputTank = new SmartFluidTankBehaviour(
			SmartFluidTankBehaviour.INPUT,
			this,
			2,
			CopperBasinCapacity.SLOT_CAPACITY,
			true).whenFluidUpdates(this::notifyChangeOfContents);

		outputTank = new SmartFluidTankBehaviour(
			SmartFluidTankBehaviour.OUTPUT,
			this,
			2,
			CopperBasinCapacity.SLOT_CAPACITY,
			true).whenFluidUpdates(this::notifyChangeOfContents)
			.forbidInsertion();

		behaviours.add(inputTank);
		behaviours.add(outputTank);

		fluidCapability = new CombinedTankWrapper(
			outputTank.getCapability(),
			inputTank.getCapability());
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		int titleIndex = tooltip.size();
		boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		if (added && tooltip.size() > titleIndex) {
			replaceGoggleTitle(tooltip, titleIndex);
		}
		return added;
	}

	private static void replaceGoggleTitle(List<Component> tooltip, int titleIndex) {
		List<Component> title = new ArrayList<>();
		CreateLang.translate("gui.goggles.copper_basin_contents")
			.forGoggles(title);
		if (!title.isEmpty())
			tooltip.set(titleIndex, title.get(0));
	}

	@Override
	public boolean acceptOutputs(List<ItemStack> outputItems, List<FluidStack> outputFluids, boolean simulate) {
		if (outputFluids.isEmpty())
			return super.acceptOutputs(outputItems, outputFluids, simulate);

		outputTank.allowInsertion();
		try {
			IFluidHandler targetTank = outputTank.getCapability();
			if (targetTank == null)
				return false;

			if (!super.acceptOutputs(outputItems, List.of(), true))
				return false;
			if (!acceptFluidsIntoOutputTank(outputFluids, true, targetTank))
				return false;

			if (simulate)
				return true;

			if (!super.acceptOutputs(outputItems, List.of(), false))
				return false;
			return acceptFluidsIntoOutputTank(outputFluids, false, targetTank);
		} finally {
			outputTank.forbidInsertion();
		}
	}

	@Override
	public float getTotalFluidUnits(float partialTicks) {
		if (level != null && level.isClientSide)
			return shouldRenderCopperBasinMixedSurfaceParticles()
				? CopperBasinCapacity.RENDER_FULL_CAPACITY
				: 0;

		return super.getTotalFluidUnits(partialTicks);
	}

	private boolean shouldRenderCopperBasinMixedSurfaceParticles() {
		List<FluidStack> fullFluids = new ArrayList<>(2);

		if (!collectFullSlotFluids(inputTank, fullFluids))
			return false;
		if (!collectFullSlotFluids(outputTank, fullFluids))
			return false;

		if (fullFluids.size() != 2)
			return false;

		return !FluidStack.isSameFluidSameComponents(fullFluids.get(0), fullFluids.get(1));
	}

	private static boolean collectFullSlotFluids(SmartFluidTankBehaviour behaviour, List<FluidStack> fullFluids) {
		if (behaviour == null)
			return true;

		IFluidHandler handler = behaviour.getCapability();
		if (handler == null)
			return true;

		for (int tank = 0; tank < handler.getTanks(); tank++) {
			FluidStack fluidStack = handler.getFluidInTank(tank);
			if (fluidStack.isEmpty())
				continue;

			if (fluidStack.getAmount() != CopperBasinCapacity.SLOT_CAPACITY)
				return false;

			fullFluids.add(fluidStack);
			if (fullFluids.size() > 2)
				return false;
		}

		return true;
	}

	@Override
	public void tick() {
		super.tick();

		if (level == null || level.isClientSide)
			return;

		pushOutputFluidsToSpoutputTarget();
	}

	@Override
	public void invalidate() {
		outputTargetCache = null;
		cachedTargetFacing = null;
		super.invalidate();
	}

	private @Nullable IFluidHandler getSpoutputTarget(BlockPos targetPos, Direction facing) {
		if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
			return level == null ? null
				: level.getCapability(Capabilities.FluidHandler.BLOCK, targetPos, facing.getOpposite());
		}
		if (outputTargetCache == null || cachedTargetFacing != facing) {
			outputTargetCache = BlockCapabilityCache.create(
				Capabilities.FluidHandler.BLOCK, serverLevel, targetPos, facing.getOpposite());
			cachedTargetFacing = facing;
		}
		return outputTargetCache.getCapability();
	}

	private void pushOutputFluidsToSpoutputTarget() {
		BlockState blockState = getBlockState();
		if (!(blockState.getBlock() instanceof BasinBlock))
			return;

		Direction direction = blockState.getValue(BasinBlock.FACING);
		if (direction == Direction.DOWN)
			return;

		BlockPos targetPos = worldPosition.below()
			.relative(direction);

		IFluidHandler sourceTank = outputTank.getCapability();
		IFluidHandler targetTank = getSpoutputTarget(targetPos, direction);
		if (sourceTank == null || targetTank == null)
			return;

		for (int slot = 0; slot < sourceTank.getTanks(); slot++) {
			FluidStack available = sourceTank.getFluidInTank(slot)
				.copy();
			if (available.isEmpty())
				continue;
			if (movePartialFluid(sourceTank, targetTank, available) > 0) {
				notifyChangeOfContents();
				notifyUpdate();
				return;
			}
		}
	}

	private static int movePartialFluid(IFluidHandler sourceTank, IFluidHandler targetTank, FluidStack requested) {
		FluidStack available = sourceTank.drain(requested, FluidAction.SIMULATE);
		if (available.isEmpty())
			return 0;

		int accepted = fillTarget(targetTank, available.copy(), FluidAction.SIMULATE);
		if (accepted <= 0)
			return 0;

		FluidStack toMove = available.copyWithAmount(Math.min(accepted, available.getAmount()));
		int moved = fillTarget(targetTank, toMove, FluidAction.EXECUTE);
		if (moved <= 0)
			return 0;

		sourceTank.drain(available.copyWithAmount(moved), FluidAction.EXECUTE);
		return moved;
	}

	private static boolean acceptFluidsIntoOutputTank(List<FluidStack> outputFluids, boolean simulate,
		IFluidHandler targetTank) {
		if (simulate) {
			IFluidHandler[] tanks = new IFluidHandler[targetTank.getTanks()];
			for (int i = 0; i < tanks.length; i++) {
				FluidTank copy = new FluidTank(targetTank.getTankCapacity(i));
				copy.setFluid(targetTank.getFluidInTank(i).copy());
				tanks[i] = copy;
			}
			targetTank = new CombinedTankWrapper(tanks).enforceVariety();
		}
		for (FluidStack fluidStack : outputFluids) {
			if (fillTarget(targetTank, fluidStack.copy(), FluidAction.EXECUTE) != fluidStack.getAmount())
				return false;
		}
		return true;
	}

	private static int fillTarget(IFluidHandler targetTank, FluidStack fluidStack, FluidAction action) {
		return targetTank instanceof SmartFluidTankBehaviour.InternalFluidHandler internalFluidHandler
			? internalFluidHandler.forceFill(fluidStack, action)
			: targetTank.fill(fluidStack, action);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(
			Capabilities.ItemHandler.BLOCK,
			AllBlockEntities.COPPER_BASIN.get(),
			(be, context) -> be.itemCapability);
		event.registerBlockEntity(
			Capabilities.FluidHandler.BLOCK,
			AllBlockEntities.COPPER_BASIN.get(),
			(be, context) -> be.fluidCapability);
	}
}
