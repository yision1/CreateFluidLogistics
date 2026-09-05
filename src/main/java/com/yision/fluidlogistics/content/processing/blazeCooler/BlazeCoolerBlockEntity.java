package com.yision.fluidlogistics.content.processing.blazeCooler;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.yision.fluidlogistics.registry.AllBlockEntities;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class BlazeCoolerBlockEntity extends BlazeBurnerBlockEntity {

    static final String COOLING_LEVEL_TAG = "CFLBlazeCoolerLevel";
    private static final String FUEL_TANK_TAG = "CFLBlazeCoolerFuelTank";
    private static final int FUEL_TANK_CAPACITY = FluidType.BUCKET_VOLUME;

    private HeatLevel coolingLevel;
    private final SmartFluidTank fuelTank;

    public BlazeCoolerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        coolingLevel = BlazeBurnerBlock.getHeatLevelOf(state);
        fuelTank = new SmartFluidTank(FUEL_TANK_CAPACITY, stack -> setChanged()) {
            @Override
            public boolean isFluidValid(FluidStack stack) {
                return BlazeCoolerFuelManager.find(stack) != null;
            }
        };
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            AllBlockEntities.BLAZE_COOLER.get(),
            (cooler, side) -> cooler.stockKeeper ? null : cooler.fuelTank
        );
    }

    @Override
    public void lazyTick() {
        boolean wasStockKeeper = stockKeeper;
        super.lazyTick();
        if (wasStockKeeper != stockKeeper)
            invalidateCapabilities();
    }

    @Override
    public HeatLevel getHeatLevelFromBlock() {
        return coolingLevel;
    }

    @Override
    public HeatLevel getHeatLevelForRender() {
        if (!coolingLevel.isAtLeast(HeatLevel.FADING) && stockKeeper)
            return HeatLevel.FADING;
        return coolingLevel;
    }

    public IFluidHandler getFuelInput() {
        return fuelTank;
    }

    @Override
    protected boolean tryUpdateFuel(ItemStack stack, boolean forceOverflow, boolean simulate) {
        if (isCreative)
            return false;

        BlazeCoolerFuelManager.Fuel fuel = BlazeCoolerFuelManager.find(stack);
        if (fuel == null)
            return false;

        FuelType type = fuel.supercooled() ? FuelType.SPECIAL : FuelType.NORMAL;
        return tryUpdateFuel(type, fuel.coolTime(), forceOverflow, simulate);
    }

    private boolean tryUpdateFuel(FuelType newFuel, int coolingTime, boolean forceOverflow, boolean simulate) {

        if (newFuel.ordinal() < activeFuel.ordinal())
            return false;

        int newCoolingTime = coolingTime;
        if (newFuel == activeFuel) {
            if (remainingBurnTime <= INSERTION_THRESHOLD) {
                newCoolingTime += remainingBurnTime;
            } else if (forceOverflow && newFuel == FuelType.NORMAL && remainingBurnTime < MAX_HEAT_CAPACITY) {
                newCoolingTime = Math.min(remainingBurnTime + newCoolingTime, MAX_HEAT_CAPACITY);
            } else {
                return false;
            }
        }

        if (simulate)
            return true;

        activeFuel = newFuel;
        remainingBurnTime = newCoolingTime;

        if (level.isClientSide) {
            spawnParticleBurst(activeFuel == FuelType.SPECIAL);
            return true;
        }

        HeatLevel previousLevel = getHeatLevelFromBlock();
        playSound();
        updateBlockState();
        if (previousLevel != getHeatLevelFromBlock())
            level.playSound(null, worldPosition, SoundEvents.BLAZE_AMBIENT, SoundSource.BLOCKS,
                .125f + level.random.nextFloat() * .125f, 1.15f - level.random.nextFloat() * .25f);
        return true;
    }

    @Override
    protected void setBlockHeat(HeatLevel heat) {
        boolean coolingLevelChanged = coolingLevel != heat;
        coolingLevel = heat;
        if (level == null)
            return;

        BlockState state = getBlockState();
        boolean exposedLevelChanged = state.getValue(BlazeBurnerBlock.HEAT_LEVEL) != HeatLevel.SMOULDERING;
        if (!coolingLevelChanged && !exposedLevelChanged)
            return;
        if (exposedLevelChanged)
            level.setBlockAndUpdate(worldPosition,
                state.setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SMOULDERING));
        notifyUpdate();
    }

    @Override
    protected void spawnParticles(HeatLevel heatLevel, double burstMult) {
        if (level == null || heatLevel == HeatLevel.NONE)
            return;

        RandomSource random = level.getRandom();
        if (random.nextInt(4) != 0)
            return;

        Vec3 center = VecHelper.getCenterOf(worldPosition);
        Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, random, .35f);
        Vec3 particle = center.add(offset.x, .25f + Math.abs(offset.y), offset.z);
        level.addParticle(ParticleTypes.SNOWFLAKE, particle.x, particle.y, particle.z,
            offset.x / 24, .01f * burstMult, offset.z / 24);
    }

    @Override
    public void spawnParticleBurst(boolean ignored) {
        Vec3 center = VecHelper.getCenterOf(worldPosition);
        RandomSource random = level.random;
        for (int i = 0; i < 20; i++) {
            Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, random, .5f);
            Vec3 particle = center.add(offset).add(0, .125, 0);
            level.addParticle(ParticleTypes.SNOWFLAKE, particle.x, particle.y, particle.z,
                offset.x / 12, Math.abs(offset.y) / 12, offset.z / 12);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null)
            return;

        if (level.isClientSide) {
            if (VisualizationManager.supportsVisualization(level))
                tickHeadAnimation();
            return;
        }

        if (isVirtual())
            return;

        consumeFluidFuel();

        BlockState state = getBlockState();
        if (state.getValue(BlazeBurnerBlock.HEAT_LEVEL) != HeatLevel.SMOULDERING)
            level.setBlockAndUpdate(worldPosition,
                state.setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SMOULDERING));

        BlazeCoolerConversion.tickConversion(this, BlazeCoolerConversion.shouldWarm(this));
    }

    private void tickHeadAnimation() {
        boolean active = getHeatLevelFromBlock().isAtLeast(HeatLevel.FADING) && isValidBlockAbove();

        if (!active) {
            float target = 0;
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && !player.isInvisible()) {
                double x = isVirtual() ? -4 : player.getX();
                double z = isVirtual() ? -10 : player.getZ();
                double dx = x - (getBlockPos().getX() + .5);
                double dz = z - (getBlockPos().getZ() + .5);
                target = AngleHelper.deg(-Mth.atan2(dz, dx)) - 90;
            }
            target = headAngle.getValue() + AngleHelper.getShortestAngleDiff(headAngle.getValue(), target);
            headAngle.chase(target, .25f, Chaser.exp(5));
            headAngle.tickChaser();
        } else {
            Direction facing = getBlockState().getOptionalValue(BlazeBurnerBlock.FACING).orElse(Direction.SOUTH);
            headAngle.chase((AngleHelper.horizontalAngle(facing) + 180) % 360, .125f, Chaser.EXP);
            headAngle.tickChaser();
        }

        headAnimation.chase(active ? 1 : 0, .25f, Chaser.exp(.25f));
        headAnimation.tickChaser();
    }

    public float getHeadAngle(float partialTicks) {
        return headAngle.getValue(partialTicks);
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putInt(COOLING_LEVEL_TAG, coolingLevel.ordinal());
        compound.put(FUEL_TANK_TAG, fuelTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        fuelTank.readFromNBT(registries, compound.getCompound(FUEL_TANK_TAG));
        if (compound.contains(COOLING_LEVEL_TAG)) {
            int index = Mth.clamp(compound.getInt(COOLING_LEVEL_TAG), 0, HeatLevel.values().length - 1);
            coolingLevel = HeatLevel.byIndex(index);
        } else {
            coolingLevel = BlazeBurnerBlock.getHeatLevelOf(getBlockState());
        }
    }

    private void consumeFluidFuel() {
        if (isCreative || fuelTank.isEmpty() || remainingBurnTime > 0)
            return;

        BlazeCoolerFuelManager.Fuel fuel = BlazeCoolerFuelManager.find(fuelTank.getFluid());
        if (fuel == null)
            return;

        if (fuelTank.getFluidAmount() < fuel.amount())
            return;

        remainingBurnTime = fuel.coolTime();
        HeatLevel heatLevel = fuel.supercooled()
            ? HeatLevel.SEETHING
            : (double) remainingBurnTime / MAX_HEAT_CAPACITY < .0125
                ? HeatLevel.FADING
                : HeatLevel.KINDLED;
        setBlockHeat(heatLevel);
        fuelTank.getFluid().shrink(fuel.amount());
        notifyUpdate();
    }

}
