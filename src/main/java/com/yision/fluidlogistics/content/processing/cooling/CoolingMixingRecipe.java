package com.yision.fluidlogistics.content.processing.cooling;

import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import com.yision.fluidlogistics.registry.AllFluidLogisticsRecipeTypes;

import net.minecraft.world.item.crafting.RecipeSerializer;

public class CoolingMixingRecipe extends MixingRecipe implements CoolingRecipe {

    public CoolingMixingRecipe(CoolingRecipeParams params) {
        super(params);
    }

    @Override
    public boolean requiresSupercooling() {
        return ((CoolingRecipeParams) getParams()).supercooled();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return AllFluidLogisticsRecipeTypes.COOLING_MIXING.getSerializer();
    }

    @Override
    public IRecipeTypeInfo getTypeInfo() {
        return AllFluidLogisticsRecipeTypes.COOLING_MIXING;
    }
}
