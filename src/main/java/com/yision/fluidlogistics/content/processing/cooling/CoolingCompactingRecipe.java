package com.yision.fluidlogistics.content.processing.cooling;

import com.simibubi.create.content.kinetics.mixer.CompactingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import com.yision.fluidlogistics.registry.AllFluidLogisticsRecipeTypes;

import net.minecraft.world.item.crafting.RecipeSerializer;

public class CoolingCompactingRecipe extends CompactingRecipe implements CoolingRecipe {

	public CoolingCompactingRecipe(ProcessingRecipeParams params) {
		super(params);
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return AllFluidLogisticsRecipeTypes.COOLING_COMPACTING.getSerializer();
	}

	@Override
	public IRecipeTypeInfo getTypeInfo() {
		return AllFluidLogisticsRecipeTypes.COOLING_COMPACTING;
	}
}
