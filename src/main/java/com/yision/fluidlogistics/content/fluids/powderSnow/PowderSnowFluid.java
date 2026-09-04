package com.yision.fluidlogistics.content.fluids.powderSnow;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public class PowderSnowFluid extends BaseFlowingFluid {

    private final boolean source;

    public static PowderSnowFluid createSource(Properties properties) {
        return new PowderSnowFluid(properties, true);
    }

    public static PowderSnowFluid createFlowing(Properties properties) {
        return new PowderSnowFluid(properties, false);
    }

    private PowderSnowFluid(Properties properties, boolean source) {
        super(properties);
        this.source = source;
    }

    @Override
    public Fluid getSource() {
        return source ? this : super.getSource();
    }

    @Override
    public Fluid getFlowing() {
        return source ? super.getFlowing() : this;
    }

    @Override
    public Item getBucket() {
        return Items.POWDER_SNOW_BUCKET;
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return Blocks.POWDER_SNOW.defaultBlockState();
    }

    @Override
    public boolean isSource(FluidState state) {
        return source;
    }

    @Override
    public int getAmount(FluidState state) {
        return source ? 8 : 0;
    }
}
