package com.yision.fluidlogistics.content.processing.cooling;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import com.yision.fluidlogistics.registry.AllFluidLogisticsRecipeTypes;

public class InactiveBulkCoolingFanProcessingType extends BulkCoolingFanProcessingType {

    @Override
    protected boolean isValidHeatLevel(HeatLevel heatLevel) {
        return !heatLevel.isAtLeast(HeatLevel.FADING);
    }

    @Override
    protected IRecipeTypeInfo getRecipeType() {
        return AllFluidLogisticsRecipeTypes.INACTIVE_BULK_COOLING;
    }

    @Override
    protected int getLightParticleColor() {
        return 0xF4F7F8;
    }

    @Override
    protected int getDarkParticleColor() {
        return 0xAAB8C0;
    }

    @Override
    protected float getParticleAlpha() {
        return .6f;
    }
}
