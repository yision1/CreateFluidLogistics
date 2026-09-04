package com.yision.fluidlogistics.content.processing.cooling;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Joiner;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.processing.basin.BasinRecipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CoolingRecipeSerializer<R extends BasinRecipe> implements RecipeSerializer<R> {

    private final MapCodec<R> codec;
    private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;

    public CoolingRecipeSerializer(Factory<R> factory) {
        codec = CoolingRecipeParams.CODEC
            .xmap(factory::create, recipe -> (CoolingRecipeParams) recipe.getParams())
            .validate(recipe -> {
                var errors = recipe.validate();
                if (errors.isEmpty())
                    return DataResult.success(recipe);
                errors.add(recipe.getClass().getSimpleName() + " failed validation:");
                return DataResult.error(() -> Joiner.on('\n').join(errors), recipe);
            });
        streamCodec = CoolingRecipeParams.STREAM_CODEC
            .map(factory::create, recipe -> (CoolingRecipeParams) recipe.getParams());
    }

    @Override
    public MapCodec<R> codec() {
        return codec;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() {
        return streamCodec;
    }

    @FunctionalInterface
    public interface Factory<R extends BasinRecipe> {
        R create(CoolingRecipeParams params);
    }
}
