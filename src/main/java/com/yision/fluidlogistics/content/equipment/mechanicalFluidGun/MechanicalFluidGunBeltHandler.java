package com.yision.fluidlogistics.content.equipment.mechanicalFluidGun;

import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.yision.fluidlogistics.content.fluids.faucet.SmartFaucetBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

class MechanicalFluidGunBeltHandler {

	private final MechanicalFluidGunBlockEntity be;
	@Nullable private TransportedItemStack activeBeltItem;
	@Nullable private BlockPos activeBeltPos;
	private long boundAt;

	MechanicalFluidGunBeltHandler(MechanicalFluidGunBlockEntity be) {
		this.be = be;
	}

	boolean accept(TransportedItemStack item, TransportedItemStackHandlerBehaviour handler) {
		if (activeBeltItem != null) return matches(item, handler);
		if (be.getItemFillingHelper().isFilling() || be.getSpeed() == 0 || be.isRedstoneLocked()) return false;
		int index = be.getTargetsHelper().getTargetIndexFor(be.gunPos(), handler.blockEntity.getBlockPos());
		if (index < 0 || be.getProcessorHelper().nextTarget(handler.blockEntity.getBlockPos(), item) != index) return false;
		return start(item, handler, index);
	}

	boolean start(TransportedItemStack item, TransportedItemStackHandlerBehaviour handler, int index) {
		var source = be.sourceHandler();
		if (source == null || be.getSpeed() == 0 || be.isRedstoneLocked()) return false;
		FluidStack fluid = MechanicalFluidGunFillOperations.findFillableFluidForItem(be, source, item.stack);
		if (fluid.isEmpty()) return false;
		if (be.getTargetsHelper().getActiveTargetIndex() != index) be.getVisualsHelper().clearSpray();
		activeBeltItem = item;
		activeBeltPos = handler.blockEntity.getBlockPos().immutable();
		boundAt = be.getLevel().getGameTime();
		be.setDynamicAimPoint(index, getBeltItemAimPoint(item, handler));
		be.setActiveTarget(index);
		if (!MechanicalFluidGunItemFilling.startFilling(be, source, item.stack, fluid,
			MechanicalFluidGunItemFilling.ProcessingTarget.BELT, activeBeltPos)) {
			clearBeltState();
			return false;
		}
		be.getCycleHelper().markScheduledTarget(index);
		item.locked = true;
		handler.blockEntity.notifyUpdate();
		return true;
	}

	boolean matches(TransportedItemStack item, TransportedItemStackHandlerBehaviour handler) {
		return item == activeBeltItem && handler.blockEntity.getBlockPos().equals(activeBeltPos);
	}

	boolean tick(TransportedItemStack item, TransportedItemStackHandlerBehaviour handler) {
		if (!matches(item, handler)) return false;
		if (be.getSpeed() == 0 || !be.getItemFillingHelper().canCommit(item.stack)
			|| !be.getItemFillingHelper().hasPendingFluid(be.sourceHandler())) {
			be.getItemFillingHelper().clear();
			clearBeltState();
			be.getProcessorHelper().afterItem(null, null);
			return false;
		}
		return be.getItemFillingHelper().tick(be);
	}

	void replace(TransportedItemStack previous, @Nullable TransportedItemStack replacement, boolean sameInput,
		TransportedItemStackHandlerBehaviour handler) {
		if (activeBeltItem != previous) return;
		if (!sameInput && (replacement == null || !be.getItemFillingHelper().refreshAssembly(be, replacement.stack)))
			be.getItemFillingHelper().clear();
		activeBeltItem = replacement;
		boundAt = be.getLevel().getGameTime();
		if (replacement == null || !be.getItemFillingHelper().isFilling()) {
			clearBeltState();
			be.getProcessorHelper().afterItem(handler.blockEntity.getBlockPos(), replacement);
		} else {
			int index = be.getTargetsHelper().getTargetIndexFor(be.gunPos(), activeBeltPos);
			be.setDynamicAimPoint(index, getBeltItemAimPoint(replacement, handler));
			be.updateVisuals();
			be.notifyGunUpdate();
		}
	}

	void tickActiveBeltFillingFallback() {
		if (activeBeltPos == null || activeBeltItem == null
			|| !be.getTargetsHelper().isTargetValid(be.getLevel(), be.gunPos(), activeBeltPos)
			|| !targetsBeltPos(activeBeltPos) || !canProcessAt(be.getLevel(), activeBeltPos)) {
			cancel();
			return;
		}
		if (be.getLevel().getBlockEntity(activeBeltPos) instanceof BeltBlockEntity belt && belt.getSpeed() == 0) {
			be.getProcessorHelper().stopSpray();
			return;
		}
		if (be.getLevel().getGameTime() <= boundAt + 1) return;
		var handler = handlerAt(be.getLevel(), activeBeltPos);
		boolean[] present = {false};
		if (handler != null) handler.handleProcessingOnAllItems(item -> {
			present[0] |= item == activeBeltItem;
			return TransportedResult.doNothing();
		});
		if (!present[0] || !be.getItemFillingHelper().canCommit(activeBeltItem.stack)) cancel();
	}

	private void cancel() {
		be.getItemFillingHelper().clear();
		clearBeltState();
		be.getProcessorHelper().afterItem(null, null);
	}

	void clearBeltState() {
		activeBeltItem = null;
		activeBeltPos = null;
	}

	boolean resumeWaitingBeltItem() {
		if (be.getItemFillingHelper().isFilling() || be.getSpeed() == 0 || be.isRedstoneLocked()) return false;
		return be.getProcessorHelper().advanceToProcessableTargetOrIdle();
	}

	boolean hasActiveBeltWorkAt(BlockPos pos) {
		return pos.equals(activeBeltPos);
	}

	boolean targetsBeltPos(BlockPos pos) {
		return be.getTargetsHelper().getTargetIndexFor(be.gunPos(), pos) >= 0;
	}

	boolean isBeltTarget(BlockPos pos) {
		return be.getLevel().getBlockEntity(pos) instanceof BeltBlockEntity;
	}

	@Nullable
	static TransportedItemStackHandlerBehaviour handlerAt(Level level, BlockPos pos) {
		return BlockEntityBehaviour.get(level, pos, TransportedItemStackHandlerBehaviour.TYPE);
	}

	static boolean canProcessAt(Level level, BlockPos pos) {
		if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof BeltBlockEntity)) return false;
		if (level.getBlockState(pos.above()).getBlock() instanceof SmartFaucetBlock
			|| level.getBlockState(pos.above(2)).getBlock() instanceof SmartFaucetBlock) return false;
		var original = BlockEntityBehaviour.get(level, pos.above(2), BeltProcessingBehaviour.TYPE);
		return (original == null || original.blockEntity instanceof MechanicalFluidGunBlockEntity)
			&& !BeltProcessingBehaviour.isBlocked(level, pos);
	}

	@Nullable
	TransportedItemStack findItem(BlockPos pos) {
		if (!canProcessAt(be.getLevel(), pos)) return null;
		var handler = handlerAt(be.getLevel(), pos);
		var source = be.sourceHandler();
		if (handler == null || source == null) return null;
		TransportedItemStack[] found = {null};
		handler.handleProcessingOnAllItems(item -> {
			if (found[0] == null && !item.lockedExternally
				&& !MechanicalFluidGunFillOperations.findFillableFluidForItem(be, source, item.stack).isEmpty()) found[0] = item;
			return TransportedResult.doNothing();
		});
		return found[0];
	}

	private Vec3 getBeltItemAimPoint(TransportedItemStack item, TransportedItemStackHandlerBehaviour handler) {
		Vec3 pos = handler.getWorldPositionOf(item).add(0, 7.0 / 16.0, 0);
		if (!(handler.blockEntity instanceof BeltBlockEntity belt)) return pos;
		return belt.getBlockState().getValue(BeltBlock.HORIZONTAL_FACING).getAxis() == net.minecraft.core.Direction.Axis.Z
			? pos.add(item.sideOffset, 0, 0) : pos.add(0, 0, -item.sideOffset);
	}

	@Nullable
	static BeltProcessingBehaviour findProcessingAt(Level level, BlockPos pos) {
		var guns = MechanicalFluidGunBeltDispatcher.gunsAt(level, pos);
		return guns.isEmpty() ? null : guns.getFirst().beltProcessing;
	}
}
