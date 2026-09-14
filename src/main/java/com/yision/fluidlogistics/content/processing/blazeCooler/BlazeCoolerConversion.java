package com.yision.fluidlogistics.content.processing.blazeCooler;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.config.Config;
import com.yision.fluidlogistics.registry.AllBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = FluidLogistics.MODID)
public final class BlazeCoolerConversion {

    public static final int CONVERSION_TICKS = 20 * 30;
    public static final String TIMER_TAG = "CFLBlazeCoolerConversionTime";

    private BlazeCoolerConversion() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        boolean cooling = stack.is(Items.POWDER_SNOW_BUCKET);
        if (!cooling && !stack.is(Items.LAVA_BUCKET))
            return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (!(level.getBlockEntity(pos) instanceof BlazeBurnerBlockEntity burner))
            return;
        if (cooling ? !canStartCooling(burner)
            : !(burner instanceof BlazeCoolerBlockEntity cooler) || !canStartWarming(cooler))
            return;

        if (!level.isClientSide) {
            startConversion(burner);
            Player player = event.getEntity();
            player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
            player.setItemInHand(event.getHand(),
                ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
            level.playSound(null, pos,
                cooling ? SoundEvents.BUCKET_EMPTY_POWDER_SNOW : SoundEvents.BUCKET_EMPTY_LAVA,
                SoundSource.BLOCKS, 1, 1);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    public static boolean shouldCool(BlazeBurnerBlockEntity burner) {
        Level level = burner.getLevel();
        return level != null
            && !level.isClientSide
            && burner.getPersistentData().contains(TIMER_TAG)
            && isIdleInColdBiome(burner);
    }

    public static boolean canStartCooling(BlazeBurnerBlockEntity burner) {
        return !burner.getPersistentData().contains(TIMER_TAG) && isIdleInColdBiome(burner);
    }

    private static boolean isIdleInColdBiome(BlazeBurnerBlockEntity burner) {
        if (!Config.isBlazeCoolerEnabled())
            return false;

        Level level = burner.getLevel();
        if (level == null || burner.isVirtual())
            return false;

        BlockState state = burner.getBlockState();
        if (!com.simibubi.create.AllBlocks.BLAZE_BURNER.has(state))
            return false;

        BlockPos pos = burner.getBlockPos();
        return !burner.isCreative()
            && state.getValue(BlazeBurnerBlock.HEAT_LEVEL) == HeatLevel.SMOULDERING
            && level.getBiome(pos).value().coldEnoughToSnow(pos);
    }

    public static void startConversion(BlazeBurnerBlockEntity burner) {
        burner.getPersistentData().putInt(TIMER_TAG, 0);
        burner.setChanged();
    }

    public static boolean shouldWarm(BlazeCoolerBlockEntity cooler) {
        Level level = cooler.getLevel();
        return level != null
            && !level.isClientSide
            && cooler.getPersistentData().contains(TIMER_TAG)
            && isIdleCooler(cooler);
    }

    public static boolean canStartWarming(BlazeCoolerBlockEntity cooler) {
        return !cooler.getPersistentData().contains(TIMER_TAG) && isIdleCooler(cooler);
    }

    private static boolean isIdleCooler(BlazeCoolerBlockEntity cooler) {
        Level level = cooler.getLevel();
        return level != null
            && !cooler.isVirtual()
            && AllBlocks.BLAZE_COOLER.has(cooler.getBlockState())
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
