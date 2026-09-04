package com.yision.fluidlogistics.registry;

import java.util.function.Supplier;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.content.processing.cooling.BulkCoolingRecipe;
import com.yision.fluidlogistics.content.processing.cooling.CoolingCompactingRecipe;
import com.yision.fluidlogistics.content.processing.cooling.CoolingMixingRecipe;
import com.yision.fluidlogistics.content.processing.cooling.CoolingRecipeSerializer;
import com.yision.fluidlogistics.content.processing.cooling.InactiveBulkCoolingRecipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public enum AllFluidLogisticsRecipeTypes implements IRecipeTypeInfo {

    COOLING_MIXING(AllRecipeTypes.MIXING, CoolingMixingRecipe::new),
    COOLING_COMPACTING(AllRecipeTypes.COMPACTING, CoolingCompactingRecipe::new),
    BULK_COOLING(BulkCoolingRecipe::new),
    INACTIVE_BULK_COOLING(InactiveBulkCoolingRecipe::new);

    private final ResourceLocation id;
    private final Supplier<RecipeType<?>> type;
    private final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> serializer;

    <R extends BasinRecipe> AllFluidLogisticsRecipeTypes(IRecipeTypeInfo backingType,
            CoolingRecipeSerializer.Factory<R> factory) {
        String name = name().toLowerCase(java.util.Locale.ROOT);
        id = FluidLogistics.asResource(name);
        type = () -> backingType.getType();
        serializer = Registers.SERIALIZERS.register(name, () -> new CoolingRecipeSerializer<>(factory));
    }

    AllFluidLogisticsRecipeTypes(StandardProcessingRecipe.Factory<?> factory) {
        String name = name().toLowerCase(java.util.Locale.ROOT);
        id = FluidLogistics.asResource(name);
        type = Registers.TYPES.register(name, () -> RecipeType.simple(id));
        serializer = Registers.SERIALIZERS.register(name, () -> new StandardProcessingRecipe.Serializer<>(factory));
    }

    public static void register(IEventBus eventBus) {
        Registers.SERIALIZERS.register(eventBus);
        Registers.TYPES.register(eventBus);
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends RecipeSerializer<?>> T getSerializer() {
        return (T) serializer.get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <I extends RecipeInput, R extends Recipe<I>> RecipeType<R> getType() {
        return (RecipeType<R>) type.get();
    }

    private static final class Registers {
        private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, FluidLogistics.MODID);
        private static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, FluidLogistics.MODID);

        private Registers() {
        }
    }
}
