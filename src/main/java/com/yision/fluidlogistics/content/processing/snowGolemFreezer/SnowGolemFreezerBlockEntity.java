package com.yision.fluidlogistics.content.processing.snowGolemFreezer;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.yision.fluidlogistics.registry.AllBlockEntities;
import com.yision.fluidlogistics.registry.AllFluidLogisticsParticleTypes;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class SnowGolemFreezerBlockEntity extends BlazeBurnerBlockEntity {

	static final String COOLING_LEVEL_TAG = "CFLBlazeCoolerLevel";
	static final String PUMPKIN_HEAD_TAG = "PumpkinHead";
	private static final String FUEL_TANK_TAG = "CFLBlazeCoolerFuelTank";
	private static final int FUEL_TANK_CAPACITY = FluidType.BUCKET_VOLUME;

	private HeatLevel coolingLevel;
	private boolean pumpkinHead;
	private final SmartFluidTank fuelTank;
	private LazyOptional<IFluidHandler> fuelCapability;

	public SnowGolemFreezerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		coolingLevel = BlazeBurnerBlock.getHeatLevelOf(state);
		fuelTank = new SmartFluidTank(FUEL_TANK_CAPACITY, stack -> {
			if (level != null && !level.isClientSide)
				setChanged();
		}) {
			@Override
			public boolean isFluidValid(FluidStack stack) {
				return !isCreative && !stockKeeper && SnowGolemFreezerFuelManager.find(stack) != null;
			}
		};
		fuelCapability = LazyOptional.of(() -> fuelTank);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
		if (capability == ForgeCapabilities.FLUID_HANDLER)
			return stockKeeper ? LazyOptional.empty() : fuelCapability.cast();
		return super.getCapability(capability, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		fuelCapability.invalidate();
	}

	@Override
	public void reviveCaps() {
		super.reviveCaps();
		fuelCapability = LazyOptional.of(() -> fuelTank);
	}

	private void refreshFuelCapability() {
		fuelCapability.invalidate();
		fuelCapability = LazyOptional.of(() -> fuelTank);
	}

	@Override
	public void lazyTick() {
		boolean wasStockKeeper = stockKeeper;
		super.lazyTick();
		if (wasStockKeeper != stockKeeper)
			refreshFuelCapability();
	}

	@Override
	public HeatLevel getHeatLevelFromBlock() {
		return coolingLevel;
	}

	public IFluidHandler getFuelInput() {
		return fuelTank;
	}

	public boolean hasPumpkinHead() {
		return pumpkinHead;
	}

	public void applyPumpkinHead() {
		if (pumpkinHead)
			return;
		pumpkinHead = true;
		notifyUpdate();
	}

	@Override
	protected boolean tryUpdateFuel(ItemStack stack, boolean forceOverflow, boolean simulate) {
		if (isCreative)
			return false;

		SnowGolemFreezerFuelManager.Fuel fuel = SnowGolemFreezerFuelManager.find(stack);
		if (fuel == null)
			return false;

		int newCoolingTime = fuel.coolTime();
		if (activeFuel == FuelType.NORMAL) {
			if (remainingBurnTime <= INSERTION_THRESHOLD) {
				newCoolingTime += remainingBurnTime;
			} else if (forceOverflow) {
				newCoolingTime = remainingBurnTime < MAX_HEAT_CAPACITY
					? Math.min(remainingBurnTime + newCoolingTime, MAX_HEAT_CAPACITY)
					: remainingBurnTime;
			} else {
				return false;
			}
		}

		if (simulate)
			return true;

		activeFuel = FuelType.NORMAL;
		remainingBurnTime = newCoolingTime;

		if (level.isClientSide) {
			spawnParticleBurst(false);
			return true;
		}

		HeatLevel previousLevel = getHeatLevelFromBlock();
		playSound();
		updateBlockState();
		if (previousLevel != getHeatLevelFromBlock())
			level.playSound(null, worldPosition, SoundEvents.SNOW_GOLEM_AMBIENT, SoundSource.BLOCKS,
				.125f + level.random.nextFloat() * .125f, 1.15f - level.random.nextFloat() * .25f);
		return true;
	}

	@Override
	protected void applyCreativeFuel() {
		activeFuel = FuelType.NONE;
		remainingBurnTime = 0;
		isCreative = true;
		if (level.isClientSide) {
			spawnParticleBurst(false);
			return;
		}
		playSound();
		setBlockHeat(coolingLevel.isAtLeast(HeatLevel.FADING) ? HeatLevel.SMOULDERING : HeatLevel.KINDLED);
	}

	@Override
	protected void setBlockHeat(HeatLevel heat) {
		if (heat.isAtLeast(HeatLevel.KINDLED))
			heat = HeatLevel.KINDLED;
		boolean coolingLevelChanged = coolingLevel != heat;
		coolingLevel = heat;
		if (level == null)
			return;

		BlockState state = getBlockState();
		boolean exposedLevelChanged = state.getValue(BlazeBurnerBlock.HEAT_LEVEL) != HeatLevel.SMOULDERING;
		if (!coolingLevelChanged && !exposedLevelChanged)
			return;
		if (exposedLevelChanged)
			level.setBlockAndUpdate(worldPosition, state.setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SMOULDERING));
		if (!level.isClientSide && coolingLevelChanged) {
			level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
			if (level.getBlockEntity(worldPosition.above()) instanceof BasinBlockEntity basin)
				basin.notifyChangeOfContents();
		}
		notifyUpdate();
	}

	@Override
	protected void spawnParticles(HeatLevel heatLevel, double burstMult) {
		spawnParticles(level, VecHelper.getCenterOf(worldPosition), stockKeeper ? HeatLevel.SMOULDERING : heatLevel);
	}

	static void spawnParticles(Level level, Vec3 center, HeatLevel coolingLevel) {
		if (level == null || !level.isClientSide || coolingLevel == HeatLevel.NONE)
			return;

		RandomSource random = level.random;
		if (random.nextInt(4) != 0)
			return;

		Vec3 smoke = center.add(VecHelper.offsetRandomly(Vec3.ZERO, random, .125f).multiply(1, 0, 1));
		if (!coolingLevel.isAtLeast(HeatLevel.FADING)) {
			SnowGolemFreezerParticles.spawnSmoke(level, smoke, false);
			return;
		}

		BlockPos above = BlockPos.containing(center).above();
		boolean empty = level.getBlockState(above).getCollisionShape(level, above).isEmpty();
		if (empty || random.nextInt(8) == 0)
			SnowGolemFreezerParticles.spawnSmoke(level, smoke, true);

		Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, random, .5f).multiply(1, .25, 1).normalize()
			.scale((empty ? .25 : .5) + random.nextDouble() * .125);
		Vec3 particle = center.add(offset).add(0, .5, 0);
		level.addParticle(AllFluidLogisticsParticleTypes.SNOW_GOLEM_FREEZER_SNOWFLAKE.get(), particle.x, particle.y,
			particle.z, 0, empty ? .0625 : random.nextDouble() * .0125, 0);
	}

	@Override
	public void spawnParticleBurst(boolean ignored) {
		if (level == null || !level.isClientSide || stockKeeper)
			return;
		Vec3 center = VecHelper.getCenterOf(worldPosition);
		RandomSource random = level.random;
		for (int i = 0; i < 20; i++) {
			Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, random, .5f);
			Vec3 particle = center.add(offset).add(0, .125, 0);
			level.addParticle(AllFluidLogisticsParticleTypes.SNOW_GOLEM_FREEZER_SNOWFLAKE.get(), particle.x, particle.y,
				particle.z, offset.x / 32, Math.max(.025, Math.abs(offset.y) / 12), offset.z / 32);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null)
			return;

		if (level.isClientSide) {
			if (VisualizationManager.supportsVisualization(level))
				SnowGolemFreezerRenderer.tickHeadAnimation(this, headAngle);
			return;
		}

		if (isVirtual())
			return;

		consumeFluidFuel();

		if (isCreative)
			setBlockHeat(coolingLevel);
	}

	public float getHeadAngle(float partialTicks) {
		return headAngle.getValue(partialTicks);
	}

	@Override
	public void write(CompoundTag compound, boolean clientPacket) {
		super.write(compound, clientPacket);
		compound.putBoolean(PUMPKIN_HEAD_TAG, pumpkinHead);
		compound.putInt(COOLING_LEVEL_TAG, coolingLevel.ordinal());
		compound.put(FUEL_TANK_TAG, fuelTank.writeToNBT(new CompoundTag()));
	}

	@Override
	protected void read(CompoundTag compound, boolean clientPacket) {
		super.read(compound, clientPacket);
		pumpkinHead = compound.getBoolean(PUMPKIN_HEAD_TAG);
		if (activeFuel == FuelType.SPECIAL)
			activeFuel = FuelType.NORMAL;
		fuelTank.readFromNBT(compound.getCompound(FUEL_TANK_TAG));
		if (compound.contains(COOLING_LEVEL_TAG)) {
			int index = Mth.clamp(compound.getInt(COOLING_LEVEL_TAG), 0, HeatLevel.KINDLED.ordinal());
			coolingLevel = HeatLevel.byIndex(index);
		} else {
			coolingLevel = HeatLevel.byIndex(
				Math.min(BlazeBurnerBlock.getHeatLevelOf(getBlockState()).ordinal(), HeatLevel.KINDLED.ordinal()));
		}
	}

	private void consumeFluidFuel() {
		if (isCreative || fuelTank.isEmpty() || remainingBurnTime > 0)
			return;

		SnowGolemFreezerFuelManager.Fuel fuel = SnowGolemFreezerFuelManager.find(fuelTank.getFluid());
		if (fuel == null)
			return;

		if (fuelTank.getFluidAmount() < fuel.amount())
			return;

		activeFuel = FuelType.NORMAL;
		remainingBurnTime = fuel.coolTime();
		updateBlockState();
		fuelTank.drain(fuel.amount(), IFluidHandler.FluidAction.EXECUTE);
		notifyUpdate();
	}

}
