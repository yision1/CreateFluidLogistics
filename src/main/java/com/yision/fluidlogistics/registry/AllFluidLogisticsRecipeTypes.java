package com.yision.fluidlogistics.registry;

import java.util.Locale;
import java.util.function.Supplier;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeFactory;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeSerializer;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.content.processing.cooling.BulkCoolingRecipe;
import com.yision.fluidlogistics.content.processing.cooling.CoolingCompactingRecipe;
import com.yision.fluidlogistics.content.processing.cooling.CoolingMixingRecipe;
import com.yision.fluidlogistics.content.processing.cooling.InactiveBulkCoolingRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public enum AllFluidLogisticsRecipeTypes implements IRecipeTypeInfo {

    COOLING_MIXING(AllRecipeTypes.MIXING, CoolingMixingRecipe::new),
    COOLING_COMPACTING(AllRecipeTypes.COMPACTING, CoolingCompactingRecipe::new),
    BULK_COOLING(BulkCoolingRecipe::new),
    INACTIVE_BULK_COOLING(InactiveBulkCoolingRecipe::new);

    private final ResourceLocation id;
    private final Supplier<RecipeType<?>> type;
    private final RegistryObject<RecipeSerializer<?>> serializer;

    AllFluidLogisticsRecipeTypes(IRecipeTypeInfo backingType, ProcessingRecipeFactory<?> factory) {
        String name = name().toLowerCase(Locale.ROOT);
        id = FluidLogistics.asResource(name);
        type = backingType::getType;
        serializer = Registers.SERIALIZERS.register(name, () -> new ProcessingRecipeSerializer<>(factory));
    }

    AllFluidLogisticsRecipeTypes(ProcessingRecipeFactory<?> factory) {
        String name = name().toLowerCase(Locale.ROOT);
        id = FluidLogistics.asResource(name);
        RegistryObject<RecipeType<?>> registeredType = Registers.TYPES.register(name, () -> RecipeType.simple(id));
        type = registeredType;
        serializer = Registers.SERIALIZERS.register(name, () -> new ProcessingRecipeSerializer<>(factory));
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
    public <T extends RecipeType<?>> T getType() {
        return (T) type.get();
    }

    private static final class Registers {
        private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, FluidLogistics.MODID);
        private static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, FluidLogistics.MODID);

        private Registers() {
        }
    }
}
