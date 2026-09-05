package com.yision.fluidlogistics.content.processing.cooling;

import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import com.yision.fluidlogistics.registry.AllFluidLogisticsRecipeTypes;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class CoolingMixingRecipe extends MixingRecipe implements CoolingRecipe {

    private boolean supercooled;

    public CoolingMixingRecipe(ProcessingRecipeParams params) {
        super(params);
    }

    @Override
    public boolean requiresSupercooling() {
        return supercooled;
    }

    @Override
    public void readAdditional(JsonObject json) {
        supercooled = json.has("supercooled") && json.get("supercooled").getAsBoolean();
    }

    @Override
    public void readAdditional(FriendlyByteBuf buffer) {
        supercooled = buffer.readBoolean();
    }

    @Override
    public void writeAdditional(JsonObject json) {
        json.addProperty("supercooled", supercooled);
    }

    @Override
    public void writeAdditional(FriendlyByteBuf buffer) {
        buffer.writeBoolean(supercooled);
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
