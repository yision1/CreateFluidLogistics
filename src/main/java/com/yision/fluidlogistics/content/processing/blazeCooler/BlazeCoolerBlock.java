package com.yision.fluidlogistics.content.processing.blazeCooler;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.yision.fluidlogistics.content.fluids.itemTransfer.HatchStyleItemTransfer;
import com.yision.fluidlogistics.registry.AllBlockEntities;
import com.yision.fluidlogistics.registry.AllBlocks;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BlazeCoolerBlock extends BlazeBurnerBlock {

    public BlazeCoolerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends BlazeBurnerBlockEntity> getBlockEntityType() {
        return AllBlockEntities.BLAZE_COOLER.get();
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos,
            Player player) {
        return AllBlocks.BLAZE_COOLER.asStack();
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, BlockEntity blockEntity) {
        return new ItemRequirement(ItemUseType.CONSUME, AllBlocks.BLAZE_COOLER.asStack());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        ItemStack stack = player.getItemInHand(hand);
        if (state.getValue(HEAT_LEVEL) != HeatLevel.NONE
                && level.getBlockEntity(pos) instanceof BlazeCoolerBlockEntity cooler
                && !cooler.stockKeeper
                && !HatchStyleItemTransfer.tryEmptyItem(level, player, hand, stack,
                    cooler.getFuelInput(), fluid -> BlazeCoolerFuelManager.find(fluid) != null,
                    false, cooler::setChanged).isEmpty()) {
            return InteractionResult.SUCCESS;
        }
        return super.use(state, level, pos, player, hand, hitResult);
    }
}
