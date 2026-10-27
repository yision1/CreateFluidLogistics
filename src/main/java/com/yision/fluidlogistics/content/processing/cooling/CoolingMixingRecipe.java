package com.yision.fluidlogistics.content.processing.cooling;

import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import com.yision.fluidlogistics.registry.AllFluidLogisticsRecipeTypes;

import net.minecraft.world.item.crafting.RecipeSerializer;

public class CoolingMixingRecipe extends MixingRecipe implements CoolingRecipe {

	public CoolingMixingRecipe(ProcessingRecipeParams params) {
		super(params);
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
