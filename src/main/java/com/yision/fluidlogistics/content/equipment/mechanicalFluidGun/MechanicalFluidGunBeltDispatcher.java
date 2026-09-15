package com.yision.fluidlogistics.content.equipment.mechanicalFluidGun;

import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class MechanicalFluidGunBeltDispatcher {

	private MechanicalFluidGunBeltDispatcher() {}

	static List<MechanicalFluidGunBlockEntity> gunsAt(Level level, BlockPos pos) {
		List<MechanicalFluidGunBlockEntity> guns = new ArrayList<>();
		for (BlockPos gunPos : MechanicalFluidGunTargetIndex.getGunsTargeting(level, pos)) {
			if (level.isLoaded(gunPos)
				&& gunPos.distSqr(pos) <= MechanicalFluidGunBlockEntity.RANGE * MechanicalFluidGunBlockEntity.RANGE
				&& level.getBlockEntity(gunPos) instanceof MechanicalFluidGunBlockEntity gun
				&& !gun.isRemoved() && gun.targetsBeltPos(pos)) guns.add(gun);
		}
		guns.sort(Comparator.comparing(gun -> gun.getBlockPos()));
		return guns;
	}

	static ProcessingResult received(TransportedItemStack item, TransportedItemStackHandlerBehaviour handler) {
		if (handler.blockEntity.isVirtual() || handler.getWorld().isClientSide) return ProcessingResult.PASS;
		boolean hold = false;
		for (var gun : gunsAt(handler.getWorld(), handler.blockEntity.getBlockPos())) {
			hold |= gun.getBeltHandlerHelper().accept(item, handler);
		}
		return hold ? ProcessingResult.HOLD : ProcessingResult.PASS;
	}

	static ProcessingResult held(TransportedItemStack item, TransportedItemStackHandlerBehaviour handler) {
		Level level = handler.getWorld();
		if (handler.blockEntity.isVirtual() || level.isClientSide) return ProcessingResult.PASS;
		BlockPos pos = handler.blockEntity.getBlockPos();
		var guns = gunsAt(level, pos);
		List<MechanicalFluidGunBlockEntity> ready = new ArrayList<>();
		for (var gun : guns) {
			var belt = gun.getBeltHandlerHelper();
			belt.accept(item, handler);
			if (belt.tick(item, handler)) ready.add(gun);
		}
		if (ready.isEmpty()) return holding(guns, item, handler);

		TransportedItemStack[] replacement = {item};
		boolean[] sameInput = {true};
		boolean[] visited = {false};
		List<MechanicalFluidGunBlockEntity> completed = new ArrayList<>();
		handler.handleProcessingOnAllItems(current -> {
			if (current != item) return TransportedResult.doNothing();
			visited[0] = true;
			ItemStack before = current.stack.copy();
			List<TransportedItemStack> outputs = new ArrayList<>();
			for (var gun : ready) {
				var filling = gun.getItemFillingHelper();
				var source = gun.sourceHandler();
				if (!outputs.isEmpty() && (outputs.get(outputs.size() - 1).stack.hasTag() && outputs.get(outputs.size() - 1).stack.getTag().contains("SequencedAssembly"))
					&& filling.refreshAssembly(gun, outputs.get(outputs.size() - 1).stack)) {
					if (source != null && !filling.drainPendingFluid(source).isEmpty()) {
						outputs.get(outputs.size() - 1).stack = filling.getPreparedResult().copy();
						completed.add(gun);
					}
					filling.clear();
					continue;
				}
				if (filling.canCommit(current.stack) && source != null && !filling.drainPendingFluid(source).isEmpty()) {
					TransportedItemStack result = current.copy();
					result.stack = filling.getPreparedResult().copy();
					result.clearFanProcessingData();
					outputs.add(result);
					current.stack.shrink(1);
					completed.add(gun);
				}
				filling.clear();
			}
			if (outputs.isEmpty()) return TransportedResult.doNothing();
			current.clearFanProcessingData();
			TransportedItemStack held = current.stack.isEmpty() ? null : current.copy();
			sameInput[0] = held != null;
			if (held == null) {
				for (var output : outputs) {
					boolean usable = false;
					for (var gun : guns) {
						if (gun.getSpeed() != 0 && !gun.isRedstoneLocked()
							&& gun.getProcessorHelper().nextTarget(pos, output) == gun.getTargetsHelper().getTargetIndexFor(gun.gunPos(), pos)) {
							usable = true;
							break;
						}
					}
					if (usable) { held = output; break; }
				}
				if (held == null && outputs.size() == 1 && ItemStack.matches(outputs.get(0).stack, before)) {
					held = outputs.get(0);
				}
				if (held != null) outputs.remove(held);
			}
			replacement[0] = held;
			return TransportedResult.convertToAndLeaveHeld(outputs, held);
		});
		if (!visited[0]) return ProcessingResult.HOLD;
		for (var gun : completed) gun.getProcessorHelper().playCompletion();
		for (var gun : guns) gun.getBeltHandlerHelper().replace(item, replacement[0], sameInput[0], handler);
		if (replacement[0] != null) {
			replacement[0].locked = holding(guns, replacement[0], handler) == ProcessingResult.HOLD;
			replacement[0].lockedExternally = false;
		}
		return replacement[0] != item ? ProcessingResult.HOLD : holding(guns, item, handler);
	}

	private static ProcessingResult holding(List<MechanicalFluidGunBlockEntity> guns, TransportedItemStack item,
		TransportedItemStackHandlerBehaviour handler) {
		for (var gun : guns) {
			if (gun.getSpeed() != 0 && gun.getItemFillingHelper().isFillingBelt()
				&& gun.getBeltHandlerHelper().matches(item, handler)) return ProcessingResult.HOLD;
		}
		return ProcessingResult.PASS;
	}
}
