package com.yision.fluidlogistics.content.schematics;

import com.simibubi.create.foundation.fluid.FluidHelper;
import com.yision.fluidlogistics.content.schematics.FluidSchematicPlan.Cell;
import com.yision.fluidlogistics.content.schematics.FluidSchematicPlan.Kind;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.FluidStack;

public final class FluidSchematicPlacement {

    public enum Result {
        SATISFIED,
        READY,
        MISSING_HOST,
        BLOCKED
    }

    private FluidSchematicPlacement() {
    }

    public static Result evaluate(Level level, BlockPos target, Cell cell) {
        BlockState actual = level.getBlockState(target);
        if (cell.kind() == Kind.AIR) {
            return actual.isAir() ? Result.SATISFIED : Result.READY;
        }
        Fluid required = cell.fluid().getFluid();

        if (cell.kind() == Kind.WATERLOGGED) {
            if (!actual.is(cell.expectedHost())) {
                return Result.MISSING_HOST;
            }
            if (!actual.hasProperty(BlockStateProperties.WATERLOGGED)) {
                return Result.BLOCKED;
            }
            if (actual.getValue(BlockStateProperties.WATERLOGGED)) {
                return sameFluid(actual.getFluidState(), required) ? Result.SATISFIED : Result.BLOCKED;
            }
            if (!(actual.getBlock() instanceof LiquidBlockContainer container)) {
                return Result.BLOCKED;
            }
            return container.canPlaceLiquid(level, target, actual, required)
                ? Result.READY
                : Result.BLOCKED;
        }

        FluidState existingFluid = actual.getFluidState();
        if (existingFluid.isSource() && sameFluid(existingFluid, required)) {
            return Result.SATISFIED;
        }
        if (!existingFluid.isEmpty()) {
            return sameFluid(existingFluid, required) ? Result.READY : Result.BLOCKED;
        }
        return actual.isAir() || actual.canBeReplaced(required) ? Result.READY : Result.BLOCKED;
    }

    public static boolean place(Level level, BlockPos target, Cell cell, boolean allowReplacement) {
        Result result = evaluate(level, target, cell);
        if (result != Result.READY
            && !(allowReplacement && cell.kind() == Kind.FREE_SOURCE && result == Result.BLOCKED)) {
            return false;
        }
        if (cell.kind() == Kind.AIR) {
            BlockState actual = level.getBlockState(target);
            if (!level.getWorldBorder().isWithinBounds(target) || actual.getDestroySpeed(level, target) == -1) {
                return false;
            }
            return level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }

        FluidStack fluidStack = cell.fluid();
        Fluid fluid = fluidStack.getFluid();
        BlockState actual = level.getBlockState(target);

        if (fluid.getFluidType().isVaporizedOnPlacement(level, target, fluidStack)) {
            fluid.getFluidType().onVaporize(null, level, target, fluidStack);
            return true;
        }
        if (cell.kind() == Kind.WATERLOGGED) {
            if (!(actual.getBlock() instanceof LiquidBlockContainer container)
                || !(fluid instanceof FlowingFluid flowingFluid)) {
                return false;
            }
            boolean placed = container.placeLiquid(level, target, actual, flowingFluid.getSource(false));
            if (placed) {
                playEmptySound(level, target, fluidStack);
                level.gameEvent(null, GameEvent.FLUID_PLACE, target);
            }
            return placed;
        }
        if (!(fluid instanceof FlowingFluid flowingFluid)) {
            return false;
        }
        if (!level.setBlock(target, flowingFluid.getSource(false).createLegacyBlock(), Block.UPDATE_ALL)) {
            return false;
        }
        playEmptySound(level, target, fluidStack);
        level.gameEvent(null, GameEvent.FLUID_PLACE, target);
        return true;
    }

    private static boolean sameFluid(FluidState state, Fluid fluid) {
        return !state.isEmpty() && FluidHelper.convertToStill(state.getType()) == FluidHelper.convertToStill(fluid);
    }

    private static void playEmptySound(Level level, BlockPos target, FluidStack fluidStack) {
        SoundEvent sound = FluidHelper.getEmptySound(fluidStack);
        if (sound == null) {
            sound = fluidStack.getFluid().is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
        }
        level.playSound(null, target, sound, SoundSource.BLOCKS, 1.0f, 1.0f);
    }
}
