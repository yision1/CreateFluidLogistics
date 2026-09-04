package com.yision.fluidlogistics.content.processing.blazeCooler;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.yision.fluidlogistics.registry.AllBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class BlazeCoolerConversion {

    public static final int CONVERSION_TICKS = 20 * 30;
    public static final String TIMER_TAG = "CFLBlazeCoolerConversionTime";
    public static final String ACTIVATED_TAG = "CFLBlazeCoolerActivated";

    private BlazeCoolerConversion() {
    }

    public static boolean shouldCool(BlazeBurnerBlockEntity burner) {
        Level level = burner.getLevel();
        if (level == null || level.isClientSide || burner.isVirtual())
            return false;

        BlockState state = burner.getBlockState();
        if (!com.simibubi.create.AllBlocks.BLAZE_BURNER.has(state) || isConversionLocked(burner))
            return false;

        BlockPos pos = burner.getBlockPos();
        return !burner.isCreative()
            && state.getValue(BlazeBurnerBlock.HEAT_LEVEL) == HeatLevel.SMOULDERING
            && level.getBiome(pos).value().coldEnoughToSnow(pos);
    }

    public static boolean shouldWarm(BlazeCoolerBlockEntity cooler) {
        Level level = cooler.getLevel();
        return level != null
            && !level.isClientSide
            && !cooler.isVirtual()
            && !isConversionLocked(cooler)
            && !cooler.isCreative()
            && cooler.getFuelInput().getFluidInTank(0).isEmpty()
            && level.dimension() == Level.NETHER
            && cooler.getHeatLevelFromBlock() == HeatLevel.SMOULDERING;
    }

    public static void tickConversion(BlazeBurnerBlockEntity burner, boolean shouldConvert) {
        CompoundTag persistentData = burner.getPersistentData();
        if (!shouldConvert) {
            if (persistentData.contains(TIMER_TAG)) {
                persistentData.remove(TIMER_TAG);
                burner.setChanged();
            }
            return;
        }

        int conversionTime = persistentData.getInt(TIMER_TAG) + 1;
        persistentData.putInt(TIMER_TAG, conversionTime);
        if (conversionTime % 20 == 0)
            burner.setChanged();
        if (conversionTime >= CONVERSION_TICKS)
            convert(burner);
    }

    public static boolean isConversionLocked(BlazeBurnerBlockEntity burner) {
        CompoundTag persistentData = burner.getPersistentData();
        if (persistentData.getBoolean(ACTIVATED_TAG))
            return true;
        if (!burner.getHeatLevelFromBlock().isAtLeast(HeatLevel.FADING))
            return false;

        persistentData.putBoolean(ACTIVATED_TAG, true);
        burner.setChanged();
        return true;
    }

    public static void convert(BlazeBurnerBlockEntity burner) {
        Level level = burner.getLevel();
        if (level == null || level.isClientSide)
            return;

        BlockState source = burner.getBlockState();
        boolean cooling = com.simibubi.create.AllBlocks.BLAZE_BURNER.has(source);
        HeatLevel heat = burner instanceof BlazeCoolerBlockEntity cooler
            ? cooler.getHeatLevelFromBlock()
            : source.getValue(BlazeBurnerBlock.HEAT_LEVEL);
        BlockState target = (cooling ? AllBlocks.BLAZE_COOLER.getDefaultState()
                : com.simibubi.create.AllBlocks.BLAZE_BURNER.getDefaultState())
            .setValue(BlazeBurnerBlock.FACING, source.getValue(BlazeBurnerBlock.FACING))
            .setValue(BlazeBurnerBlock.HEAT_LEVEL, cooling ? HeatLevel.SMOULDERING : heat);

        burner.getPersistentData().remove(TIMER_TAG);
        CompoundTag data = burner.saveCustomOnly(level.registryAccess());
        BlockPos pos = burner.getBlockPos();
        level.setBlockAndUpdate(pos, target);
        if (level.getBlockEntity(pos) instanceof BlazeBurnerBlockEntity replacement) {
            replacement.loadCustomOnly(data, level.registryAccess());
            replacement.setChanged();
            replacement.notifyUpdate();
        }
    }
}
