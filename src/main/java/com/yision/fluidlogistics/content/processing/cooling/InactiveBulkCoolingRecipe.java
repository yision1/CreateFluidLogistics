package com.yision.fluidlogistics.content.processing.cooling;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.yision.fluidlogistics.registry.AllFluidLogisticsRecipeTypes;

@ParametersAreNonnullByDefault
public class InactiveBulkCoolingRecipe extends BulkCoolingRecipe {

    public InactiveBulkCoolingRecipe(ProcessingRecipeParams params) {
        super(AllFluidLogisticsRecipeTypes.INACTIVE_BULK_COOLING, params);
    }

    @Override
    public boolean requiresActiveCooler() {
        return false;
    }
}
