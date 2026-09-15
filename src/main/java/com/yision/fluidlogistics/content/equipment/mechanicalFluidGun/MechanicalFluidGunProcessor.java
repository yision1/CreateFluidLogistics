package com.yision.fluidlogistics.content.equipment.mechanicalFluidGun;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.yision.fluidlogistics.content.fluids.faucet.FaucetFilling;
import com.yision.fluidlogistics.foundation.fluid.DepotFills;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

class MechanicalFluidGunProcessor {

	static final int TRANSFER_INTERVAL = 10;
	private static final int IDLE_RECHECK_INTERVAL = 20;
	private final MechanicalFluidGunBlockEntity be;
	private PendingTarget pendingTarget;

	MechanicalFluidGunProcessor(MechanicalFluidGunBlockEntity be) { this.be = be; }

	void tickServer() {
		var filling = be.getItemFillingHelper();
		if (be.getSpeed() == 0) {
			pause();
			return;
		}
		if (filling.isFillingBelt()) {
			be.getBeltHandlerHelper().tickActiveBeltFillingFallback();
			return;
		}
		if (filling.isFillingDepot()) {
			BlockPos pos = be.getTargetsHelper().getAbsoluteTarget(be.gunPos());
			if (pos == null || !be.getTargetsHelper().isTargetValid(be.getLevel(), be.gunPos(), pos)
				|| !filling.refreshAssembly(be, getItemOnDepot(be.getLevel().getBlockEntity(pos)))
				|| !filling.hasPendingFluid(be.sourceHandler())) {
				filling.clear();
				afterItem(null, null);
			} else if (filling.tick(be)) {
				finishDepotItemFilling(pos);
			}
			return;
		}
		if (be.isRedstoneLocked()) {
			pause();
			return;
		}
		if (be.getVisualsHelper().tickTransientSpray(false, this::advanceToProcessableTargetOrIdle)) {
			be.notifyGunUpdate();
			return;
		}
		if (be.getCycleHelper().tickCooldown()) return;
		Candidate next = null;
		if (pendingTarget != null) {
			var targets = be.getTargetsHelper();
			if (pendingTarget.index() < targets.size()
				&& targets.get(pendingTarget.index()).absoluteFrom(be.gunPos()).equals(pendingTarget.pos())) {
				next = candidateAt(pendingTarget.index(), null, null);
			}
		}
		if (next == null) next = findNext(null, null);
		if (next == null || !begin(next)) idle();
	}

	void clearPendingTarget() { pendingTarget = null; }

	void stopSpray() {
		if (!be.getVisualsHelper().isSpraying()) return;
		be.getVisualsHelper().clearSpray();
		be.notifyGunUpdate();
	}

	private void pause() {
		be.getItemFillingHelper().clear();
		be.getBeltHandlerHelper().clearBeltState();
		stopSpray();
		BlockPos pos = be.getTargetsHelper().getAbsoluteTarget(be.gunPos());
		boolean hasInput = pos != null && be.getTargetsHelper().isTargetValid(be.getLevel(), be.gunPos(), pos)
			&& candidateAt(be.getTargetsHelper().getActiveTargetIndex(), null, null) != null;
		if (!hasInput) be.endWorkCycle();
		be.updateVisuals();
	}

	private void idle() {
		be.endWorkCycle();
		be.getCycleHelper().setTransferCooldown(MechanicalFluidGunCycle.getSpeedAdjustedInterval(IDLE_RECHECK_INTERVAL, Math.abs(be.getSpeed())));
	}

	int nextTarget(@Nullable BlockPos knownPos, @Nullable TransportedItemStack knownItem) {
		Candidate candidate = findNext(knownPos, knownItem);
		return candidate == null ? -1 : candidate.index();
	}

	boolean advanceToProcessableTargetOrIdle() {
		return afterItem(null, null);
	}

	boolean afterItem(@Nullable BlockPos knownPos, @Nullable TransportedItemStack knownItem) {
		if (be.getSpeed() == 0 || be.isRedstoneLocked()) {
			pause();
			return false;
		}
		Candidate next = findNext(knownPos, knownItem);
		if (next != null && begin(next)) return true;
		idle();
		return false;
	}

	private Candidate findNext(@Nullable BlockPos knownPos, @Nullable TransportedItemStack knownItem) {
		var source = be.sourceHandler();
		if (source == null) return null;
		List<FluidStack> fluids = snapshotSource(source);
		for (int index : getCandidateIndices(be.getScheduleMode(), be.getTargetsHelper().size())) {
			Candidate candidate = candidateAt(index, knownPos, knownItem, fluids);
			if (candidate != null) return candidate;
		}
		return null;
	}

	private Candidate candidateAt(int index, @Nullable BlockPos knownPos, @Nullable TransportedItemStack knownItem) {
		var source = be.sourceHandler();
		return source == null ? null : candidateAt(index, knownPos, knownItem, snapshotSource(source));
	}

	private Candidate candidateAt(int index, @Nullable BlockPos knownPos, @Nullable TransportedItemStack knownItem,
		List<FluidStack> fluids) {
		if (index < 0 || index >= be.getTargetsHelper().size()) return null;
		var target = be.getTargetsHelper().get(index);
		BlockPos pos = target.absoluteFrom(be.gunPos());
		if (!be.getTargetsHelper().isTargetValid(be.getLevel(), be.gunPos(), pos)
			|| !be.getLevel().getBlockState(pos).is(MechanicalFluidGunBlock.TARGETS)) return null;
		var source = be.sourceHandler();
		if (source == null) return null;
		if (be.getBeltHandlerHelper().isBeltTarget(pos)) {
			if (!MechanicalFluidGunBeltHandler.canProcessAt(be.getLevel(), pos)) return null;
			TransportedItemStack item = pos.equals(knownPos) ? knownItem : be.getBeltHandlerHelper().findItem(pos);
			if (item == null) return null;
			FluidStack fluid = MechanicalFluidGunFillOperations.findFillableFluidForItem(be, fluids, item.stack);
			return fluid.isEmpty() ? null : new Candidate(index,
				new ResolvedProcess(ProcessKind.BELT, null, item.stack, fluid), item);
		}
		var process = resolveProcess(fluids, target, be.getLevel().getBlockState(pos), pos);
		return process.kind() == ProcessKind.NONE ? null : new Candidate(index, process, null);
	}

	private boolean begin(Candidate candidate) {
		int index = candidate.index();
		BlockPos pos = be.getTargetsHelper().get(index).absoluteFrom(be.gunPos());
		if (be.getTargetsHelper().getActiveTargetIndex() != index) {
			be.getVisualsHelper().clearSpray();
			be.clearDynamicAimPoint();
		}
		be.setActiveTarget(index);
		be.getCycleHelper().setTransferCooldown(0);
		if (candidate.process().kind() == ProcessKind.BELT) {
			var handler = MechanicalFluidGunBeltHandler.handlerAt(be.getLevel(), pos);
			return handler != null && be.getBeltHandlerHelper().start(candidate.beltItem(), handler, index);
		}
		be.clearDynamicAimPoint();
		if (candidate.process().kind() != ProcessKind.DEPOT && !be.aimAtTarget(index)) {
			pendingTarget = new PendingTarget(index, pos);
			be.getCycleHelper().setTransferCooldown(1);
			return true;
		}
		pendingTarget = null;
		var source = be.sourceHandler();
		if (source == null || !tryProcess(source, be.getTargetsHelper().get(index), pos, candidate.process())) return false;
		be.getCycleHelper().markScheduledTarget(index);
		if (candidate.process().kind() != ProcessKind.DEPOT) {
			be.getCycleHelper().setTransferCooldown(MechanicalFluidGunCycle.getSpeedAdjustedInterval(TRANSFER_INTERVAL, Math.abs(be.getSpeed())));
		}
		return true;
	}

	private void finishDepotItemFilling(BlockPos pos) {
		var entity = be.getLevel().getBlockEntity(pos);
		var handler = DepotFills.getTransportedHandler(be.getLevel(), entity);
		var source = be.sourceHandler();
		var filling = be.getItemFillingHelper();
		boolean completed = handler != null && source != null && DepotFills.fillFirstMatchingItem(handler,
			item -> filling.canCommit(item.stack), stack -> {
				if (filling.drainPendingFluid(source).isEmpty()) return ItemStack.EMPTY;
				stack.shrink(1);
				return filling.getPreparedResult().copy();
			});
		if (completed) {
			entity.setChanged();
			DepotFills.notifyTargetUpdate(be.getLevel(), entity);
			playCompletion();
		}
		filling.clear();
		afterItem(null, null);
	}

	void playCompletion() {
		be.getVisualsHelper().spawnServerSprayParticles(be.getLevel(), be.gunPos(),
			be.getTargetAimPoint(be.getTargetsHelper().getActiveTarget()));
		be.getLevel().playSound(null, be.gunPos(), SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, .5f,
			1 + be.getLevel().random.nextFloat() * .2f);
	}

	private List<FluidStack> snapshotSource(IFluidHandler sourceHandler) {
		List<FluidStack> sourceFluids = new ArrayList<>();
		for (int tank = 0; tank < sourceHandler.getTanks(); tank++) {
			FluidStack fluid = sourceHandler.getFluidInTank(tank);
			if (!fluid.isEmpty()) {
				sourceFluids.add(fluid.copy());
			}
		}
		return sourceFluids;
	}

	private List<Integer> getCandidateIndices(MechanicalFluidGunScheduleMode mode, int size) {
		if (size <= 0) return List.of();

		MechanicalFluidGunCycle cycle = be.getCycleHelper();
		int last = cycle.getLastScheduledTargetIndex();
		int start = Math.floorMod(last + 1, size);
		List<Integer> indices = new ArrayList<>();

		if (mode == MechanicalFluidGunScheduleMode.PREFER_FIRST) {
			for (int i = 0; i < size; i++) indices.add(i);
			return indices;
		}

		if (mode == MechanicalFluidGunScheduleMode.FORCED_ROUND_ROBIN) {
			indices.add(start);
			return indices;
		}

		for (int step = 0; step < size; step++) {
			indices.add(Math.floorMod(start + step, size));
		}
		return indices;
	}

	private ResolvedProcess resolveProcess(List<FluidStack> sourceFluids, MechanicalFluidGunTargetConfig target,
										   BlockState targetState, BlockPos absTarget) {
		if (!targetState.is(MechanicalFluidGunBlock.TARGETS)) return ResolvedProcess.NONE;

		BlockEntity targetEntity = be.getLevel().getBlockEntity(absTarget);

		if (targetEntity != null && isDepot(targetEntity)) {
			ItemStack itemOnDepot = getItemOnDepot(targetEntity);
			if (itemOnDepot.isEmpty() || !FaucetFilling.canItemBeFilled(be.getLevel(), itemOnDepot)) {
				return ResolvedProcess.NONE;
			}
			FluidStack fillableFluid = MechanicalFluidGunFillOperations.findFillableFluidForItem(be, sourceFluids, itemOnDepot);
			return fillableFluid.isEmpty()
				? ResolvedProcess.NONE
				: new ResolvedProcess(ProcessKind.DEPOT, null, itemOnDepot.copy(), fillableFluid);
		}

		if (targetEntity != null && isBelt(targetEntity)) {
			return ResolvedProcess.NONE;
		}

		if (targetState.is(Blocks.CAULDRON) || targetState.is(Blocks.WATER_CAULDRON)) {
			FluidStack fillableFluid = MechanicalFluidGunFillOperations.findFillableFluidForCauldron(be, sourceFluids, targetState);
			return fillableFluid.isEmpty()
				? ResolvedProcess.NONE
				: new ResolvedProcess(ProcessKind.CAULDRON, null, ItemStack.EMPTY, fillableFluid);
		}

		if (targetEntity == null) return ResolvedProcess.NONE;

		IFluidHandler targetHandler = MechanicalFluidGunFillOperations.getTargetFluidHandler(
			be.getLevel(), targetEntity.getBlockPos(), target.face());
		if (targetHandler != null) {
			FluidStack fillableFluid = MechanicalFluidGunFillOperations.findFillableFluidForContainer(be, sourceFluids, targetHandler, absTarget);
			if (!fillableFluid.isEmpty()) {
				return new ResolvedProcess(ProcessKind.CONTAINER, targetHandler, ItemStack.EMPTY, fillableFluid);
			}
		}

		FluidStack fuel = MechanicalFluidGunFillOperations.findFuelFluid(be, sourceFluids, targetState, absTarget);
		return fuel.isEmpty()
			? ResolvedProcess.NONE
			: new ResolvedProcess(ProcessKind.FUEL, null, ItemStack.EMPTY, fuel);
	}

	private boolean tryProcess(IFluidHandler sourceHandler, MechanicalFluidGunTargetConfig target,
							   BlockPos absTarget, ResolvedProcess process) {
		Level level = be.getLevel();
		BlockState targetState = level.getBlockState(absTarget);

		if (!targetState.is(MechanicalFluidGunBlock.TARGETS)) return false;

		if (process.kind() == ProcessKind.DEPOT) {
			return MechanicalFluidGunItemFilling.startFilling(be, sourceHandler, process.item(), process.fluid(),
				MechanicalFluidGunItemFilling.ProcessingTarget.DEPOT, null);
		}

		if (process.kind() == ProcessKind.CAULDRON) {
			MechanicalFluidGunVisuals visuals = be.getVisualsHelper();
			return MechanicalFluidGunFillOperations.tryFillCauldron(be, visuals, absTarget, targetState, process.fluid());
		}

		if (process.kind() == ProcessKind.CONTAINER && process.targetHandler() != null) {
			MechanicalFluidGunVisuals visuals = be.getVisualsHelper();
			return MechanicalFluidGunFillOperations.tryFillContainer(be, visuals, absTarget, sourceHandler,
				process.targetHandler(), process.fluid());
		}

		if (process.kind() == ProcessKind.FUEL) {
			MechanicalFluidGunVisuals visuals = be.getVisualsHelper();
			return MechanicalFluidGunFillOperations.tryFuel(be, visuals, sourceHandler, targetState, absTarget,
				process.fluid());
		}

		return false;
	}

	boolean isDepot(BlockEntity entity) { return DepotFills.isDepot(entity); }
	ItemStack getItemOnDepot(BlockEntity depot) { return DepotFills.getItemOnDepot(depot); }
	private boolean isBelt(BlockEntity entity) { return entity instanceof BeltBlockEntity; }

	private record Candidate(int index, ResolvedProcess process, @Nullable TransportedItemStack beltItem) {}
	private record PendingTarget(int index, BlockPos pos) {}
	private record ResolvedProcess(ProcessKind kind, @Nullable IFluidHandler targetHandler, ItemStack item, FluidStack fluid) {
		private static final ResolvedProcess NONE = new ResolvedProcess(ProcessKind.NONE, null, ItemStack.EMPTY, FluidStack.EMPTY);
	}
	private enum ProcessKind { NONE, DEPOT, BELT, CAULDRON, CONTAINER, FUEL }
}
