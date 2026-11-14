package com.yision.fluidlogistics.content.kinetics.oscillatingSteamEngine;

import java.util.EnumMap;
import java.util.Map;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlockEntity;
import com.yision.fluidlogistics.registry.AllBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class OscillatingEngineDrive {
	private static final String NBT_KEY = "FluidLogisticsOscillatingEngines";
	private static final int MAX_ENGINES = 2;
	private final PoweredShaftBlockEntity shaft;
	private final Map<Direction, Float> engines = new EnumMap<>(Direction.class);
	private boolean active;
	private float capacity;

	public OscillatingEngineDrive(PoweredShaftBlockEntity shaft) {
		this.shaft = shaft;
	}

	public static OscillatingEngineDrive of(PoweredShaftBlockEntity shaft) {
		return ((OscillatingEngineShaft) shaft).fluidlogistics$getOscillatingDrive();
	}

	public boolean isActive() {
		return active;
	}

	private Direction slot(BlockPos pos) {
		BlockPos offset = pos.subtract(shaft.getBlockPos());
		for (Direction direction : Direction.values())
			if (direction.getAxis() != shaft.getBlockState().getValue(ShaftBlock.AXIS)
				&& offset.equals(new BlockPos(direction.getNormal())))
				return direction;
		return null;
	}

	public boolean isEngine(BlockPos pos) {
		Level level = shaft.getLevel();
		if (level == null || slot(pos) == null || !level.isLoaded(pos))
			return false;
		BlockState state = level.getBlockState(pos);
		return state.getBlock() instanceof OscillatingSteamEngineBlock
			&& OscillatingSteamEngineBlock.getShaftPos(state, pos).equals(shaft.getBlockPos())
			&& OscillatingSteamEngineBlock.isShaftValid(state, shaft.getBlockState());
	}

	public boolean canConnect(BlockPos pos) {
		return shaft.initialTicks == 0 && isEngine(pos) && (active || shaft.enginePos == null)
			&& (contains(pos) || !shaft.getLevel().isClientSide && engines.size() < MAX_ENGINES);
	}

	public boolean contains(BlockPos pos) {
		Direction direction = slot(pos);
		return direction != null && engines.containsKey(direction);
	}

	public float getEfficiency(BlockPos pos) {
		Direction direction = slot(pos);
		return direction == null ? 0 : engines.getOrDefault(direction, 0f);
	}

	public float getStressContribution(BlockPos pos) {
		float efficiency = getEfficiency(pos);
		if (efficiency <= 0 || shaft.getTheoreticalSpeed() == 0)
			return 0;
		float totalEfficiency = 0;
		for (float value : engines.values())
			totalEfficiency += value;
		return shaft.calculateAddedStressCapacity() * Math.abs(shaft.getGeneratedSpeed())
			* (efficiency / totalEfficiency);
	}

	public RenderLayout getRenderLayout(BlockPos pos) {
		Direction engineSlot = slot(pos);
		if (engineSlot == null || !engines.containsKey(engineSlot))
			return RenderLayout.DISCONNECTED;
		Direction owner = null;
		int count = 0;
		int slotIndex = 0;
		int engineIndex = 0;
		for (Direction direction : Direction.values()) {
			if (direction.getAxis() == shaft.getBlockState().getValue(ShaftBlock.AXIS))
				continue;
			BlockPos enginePos = shaft.getBlockPos().relative(direction);
			if (engines.containsKey(direction) && isEngine(enginePos) && shaft.getLevel().getBlockEntity(enginePos)
				instanceof OscillatingSteamEngineBlockEntity engine && !engine.isRemoved()) {
				if (owner == null)
					owner = direction;
				count++;
			}
			if (direction == engineSlot)
				engineIndex = slotIndex;
			slotIndex++;
		}
		return new RenderLayout(owner == engineSlot, count > 1 ? 1 - (engineIndex + 1) / 512f : 1);
	}

	public record RenderLayout(boolean connector, float linkageScale) {
		public static final RenderLayout DISCONNECTED = new RenderLayout(false, 1);
	}

	public void connect(BlockPos pos) {
		if (active || !canConnect(pos) || shaft.getLevel().isClientSide)
			return;
		active = true;
		shaft.enginePos = null;
		shaft.movementDirection = 0;
		tick();
	}

	public void tick() {
		Level level = shaft.getLevel();
		if (!active || level == null || level.isClientSide)
			return;
		Map<Direction, Float> previous = new EnumMap<>(engines);
		int previousDirection = shaft.movementDirection;
		OscillatingSteamEngineBlockEntity first = null;
		float firstEfficiency = 0;
		for (Direction direction : Direction.values()) {
			if (direction.getAxis() == shaft.getBlockState().getValue(ShaftBlock.AXIS)) {
				engines.remove(direction);
				continue;
			}
			BlockPos pos = shaft.getBlockPos().relative(direction);
			if (!level.isLoaded(pos))
				continue;
			if (!isEngine(pos)) {
				engines.remove(direction);
				continue;
			}
			if (!engines.containsKey(direction) && engines.size() >= MAX_ENGINES)
				continue;
			float efficiency = engines.getOrDefault(direction, 0f);
			if (level.getBlockEntity(pos) instanceof OscillatingSteamEngineBlockEntity engine
				&& !engine.isRemoved()) {
				efficiency = engine.getEngineEfficiency(efficiency);
				if (first == null || efficiency > 0 && firstEfficiency == 0) {
					first = engine;
					firstEfficiency = efficiency;
				}
			}
			engines.put(direction, efficiency);
		}
		if (shaft.hasSource() && shaft.getTheoreticalSpeed() != 0)
			shaft.movementDirection = shaft.getTheoreticalSpeed() > 0 ? 1 : -1;
		else if (shaft.movementDirection == 0 && first != null)
			shaft.movementDirection = first.getShaftDirection();
		synchronizeDirections();
		recalculate(!previous.equals(engines) || previousDirection != shaft.movementDirection);
	}

	public void setDirection(BlockPos pos, int direction) {
		if (!active || !contains(pos) || shaft.getLevel().isClientSide || shaft.hasSource())
			return;
		if (shaft.movementDirection == direction)
			return;
		shaft.movementDirection = direction;
		synchronizeDirections();
		recalculate(true);
	}

	private void synchronizeDirections() {
		if (shaft.movementDirection == 0)
			return;
		Level level = shaft.getLevel();
		for (Direction direction : engines.keySet()) {
			BlockPos pos = shaft.getBlockPos().relative(direction);
			if (level.isLoaded(pos)
				&& level.getBlockEntity(pos) instanceof OscillatingSteamEngineBlockEntity engine
				&& !engine.isRemoved())
				engine.followShaftDirection(shaft.movementDirection);
		}
	}

	public void remove(BlockPos pos) {
		if (shaft.getLevel() == null || shaft.getLevel().isClientSide)
			return;
		Direction direction = slot(pos);
		if (direction == null || engines.remove(direction) == null)
			return;
		recalculate(true);
	}

	private void recalculate(boolean changed) {
		float previousCapacity = capacity;
		float previousEfficiency = shaft.engineEfficiency;
		boolean wasActive = active;
		capacity = 0;
		float maximumEfficiency = 0;
		float engineCapacity = (float) BlockStressValues.getCapacity(AllBlocks.OSCILLATING_STEAM_ENGINE.get());
		for (float efficiency : engines.values()) {
			capacity += efficiency * engineCapacity;
			maximumEfficiency = Math.max(maximumEfficiency, efficiency);
		}
		active = !engines.isEmpty();
		shaft.enginePos = null;
		shaft.engineEfficiency = maximumEfficiency;
		shaft.capacityKey = active ? AllBlocks.OSCILLATING_STEAM_ENGINE.get() : null;
		if (wasActive && !active)
			shaft.getLevel().scheduleTick(shaft.getBlockPos(), shaft.getBlockState().getBlock(), 1);
		if (changed || wasActive != active || !Mth.equal(previousCapacity, capacity)
			|| !Mth.equal(previousEfficiency, maximumEfficiency)) {
			shaft.setChanged();
			shaft.updateGeneratedRotation();
		}
	}

	public float getCombinedCapacity() {
		return capacity;
	}

	public void write(CompoundTag tag) {
		if (!active) {
			tag.remove(NBT_KEY);
			return;
		}
		CompoundTag data = new CompoundTag();
		for (var entry : engines.entrySet())
			data.putFloat(entry.getKey().getName(), entry.getValue());
		data.putFloat("Capacity", capacity);
		tag.put(NBT_KEY, data);
	}

	public void read(CompoundTag tag) {
		engines.clear();
		capacity = 0;
		float maximumEfficiency = 0;
		if (tag.contains(NBT_KEY)) {
			CompoundTag data = tag.getCompound(NBT_KEY);
			for (Direction direction : Direction.values()) {
				if (direction.getAxis() == shaft.getBlockState().getValue(ShaftBlock.AXIS)
					|| !data.contains(direction.getName()))
					continue;
				float efficiency = Mth.clamp(data.getFloat(direction.getName()), 0, 1);
				engines.put(direction, efficiency);
				maximumEfficiency = Math.max(maximumEfficiency, efficiency);
			}
			capacity = Math.max(0, data.getFloat("Capacity"));
		} else if (shaft.enginePos != null && shaft.capacityKey instanceof OscillatingSteamEngineBlock) {
			Direction direction = slot(shaft.getBlockPos().subtract(shaft.enginePos));
			if (direction != null) {
				maximumEfficiency = Mth.clamp(shaft.engineEfficiency, 0, 1);
				engines.put(direction, maximumEfficiency);
				capacity = maximumEfficiency * (float) BlockStressValues.getCapacity(shaft.capacityKey);
			}
		}
		active = !engines.isEmpty();
		if (active) {
			shaft.enginePos = null;
			shaft.engineEfficiency = maximumEfficiency;
			shaft.capacityKey = AllBlocks.OSCILLATING_STEAM_ENGINE.get();
		} else {
			capacity = 0;
			if (shaft.enginePos == null)
				shaft.capacityKey = null;
		}
	}
}
