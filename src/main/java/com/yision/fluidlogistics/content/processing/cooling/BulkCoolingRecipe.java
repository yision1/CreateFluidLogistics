package com.yision.fluidlogistics.content.processing.cooling;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import com.yision.fluidlogistics.registry.AllFluidLogisticsRecipeTypes;

import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

@ParametersAreNonnullByDefault
public class BulkCoolingRecipe extends StandardProcessingRecipe<SingleRecipeInput> {

    public BulkCoolingRecipe(ProcessingRecipeParams params) {
        this(AllFluidLogisticsRecipeTypes.BULK_COOLING, params);
    }

    protected BulkCoolingRecipe(IRecipeTypeInfo recipeType, ProcessingRecipeParams params) {
        super(recipeType, params);
    }

    public boolean requiresActiveCooler() {
        return true;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return !input.isEmpty() && ingredients.getFirst().test(input.getItem(0));
    }

    @Override
    protected int getMaxInputCount() {
        return 1;
    }

    @Override
    protected int getMaxOutputCount() {
        return 12;
    }
}
