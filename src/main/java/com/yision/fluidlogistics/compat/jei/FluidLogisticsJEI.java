package com.yision.fluidlogistics.compat.jei;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.content.logistics.filter.AbstractFilterScreen;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterScreen;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.Create;
import com.simibubi.create.compat.jei.ConversionRecipe;
import com.simibubi.create.compat.jei.DoubleItemIcon;
import com.simibubi.create.compat.jei.EmptyBackground;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.compat.jei.category.MysteriousItemConversionCategory;
import com.simibubi.create.compat.jei.category.ProcessingViaFanCategory;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.compat.CompatMods;
import com.yision.fluidlogistics.content.equipment.handPointer.filter.HandPointerFilterScreen;
import com.yision.fluidlogistics.content.logistics.factoryGauge.client.ResourceFactoryGaugeSetFilterScreen;
import com.yision.fluidlogistics.content.processing.cooling.BulkCoolingRecipe;
import com.yision.fluidlogistics.registry.AllBlocks;
import com.yision.fluidlogistics.registry.AllFluidLogisticsRecipeTypes;
import com.yision.fluidlogistics.mixin.accessor.RedstoneRequesterScreenAccessor;
import com.yision.fluidlogistics.util.FluidAmountHelper;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

@JeiPlugin
@SuppressWarnings("unused")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FluidLogisticsJEI implements IModPlugin {

    private static final ResourceLocation ID = FluidLogistics.asResource("jei_plugin");
    private static final List<RecipeType<BasinRecipe>> BASIN_RECIPE_TYPES = List.of(
        new RecipeType<>(Create.asResource("mixing"), BasinRecipe.class),
        new RecipeType<>(Create.asResource("automatic_shapeless"), BasinRecipe.class),
        new RecipeType<>(Create.asResource("automatic_brewing"), BasinRecipe.class),
        new RecipeType<>(Create.asResource("packing"), BasinRecipe.class),
        new RecipeType<>(Create.asResource("automatic_packing"), BasinRecipe.class)
    );

    private static final FluidGhostIngredientHandler<AbstractFilterScreen<?>> FILTER_FLUID_HANDLER =
        new FluidGhostIngredientHandler<>();
    private static final FluidGhostIngredientHandler<ResourceFactoryGaugeSetFilterScreen> RESOURCE_GAUGE_SET_FILTER_FLUID_HANDLER =
        new FluidGhostIngredientHandler<>();
    private static final FluidGhostIngredientHandler<RedstoneRequesterScreen> REDSTONE_REQUESTER_FLUID_HANDLER =
        new FluidGhostIngredientHandler<>((gui, slotIndex) ->
            ((RedstoneRequesterScreenAccessor) gui).getAmounts()
                .set(slotIndex, FluidAmountHelper.DEFAULT_FLUID_REQUEST_AMOUNT));

    @Nullable
    private static IJeiRuntime runtime;
    private static boolean conversionRecipesRegistered;
    private CreateRecipeCategory<BulkCoolingRecipe> bulkCooling;

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Nullable
    static IJeiRuntime getRuntime() {
        return runtime;
    }

    @Override
    public void registerAdvanced(IAdvancedRegistration registration) {
        registration.addRecipeManagerPlugin(new FluidTankRecipeLookupPlugin(
            registration.getJeiHelpers(), FluidLogisticsJEI::getRuntime));
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        if (!conversionRecipesRegistered) {
            MysteriousItemConversionCategory.RECIPES.add(ConversionRecipe.create(
                com.simibubi.create.AllBlocks.BLAZE_BURNER.asStack(), AllBlocks.BLAZE_COOLER.asStack()));
            MysteriousItemConversionCategory.RECIPES.add(ConversionRecipe.create(
                AllBlocks.BLAZE_COOLER.asStack(), com.simibubi.create.AllBlocks.BLAZE_BURNER.asStack()));
            conversionRecipesRegistered = true;
        }

        CreateRecipeCategory.Info<BulkCoolingRecipe> info = new CreateRecipeCategory.Info<>(
            new RecipeType<>(FluidLogistics.asResource("fan_cooling"), BulkCoolingRecipe.class),
            Component.translatable("fluidlogistics.recipe.fan_cooling"),
            new EmptyBackground(178, 72),
            new DoubleItemIcon(com.simibubi.create.AllItems.PROPELLER::asStack, AllBlocks.BLAZE_COOLER::asStack),
            FluidLogisticsJEI::getBulkCoolingRecipes,
            List.of(ProcessingViaFanCategory.getFan("fan_cooling"), AllBlocks.BLAZE_COOLER::asStack)
        );
        bulkCooling = new FanBulkCoolingCategory(info);
        registration.addRecipeCategories(bulkCooling);
    }

    private static List<BulkCoolingRecipe> getBulkCoolingRecipes() {
        if (Minecraft.getInstance().level == null)
            return List.of();
        List<BulkCoolingRecipe> recipes = new ArrayList<>();
        recipes.addAll(Minecraft.getInstance().level.getRecipeManager()
            .getAllRecipesFor(AllFluidLogisticsRecipeTypes.BULK_COOLING.getType()));
        recipes.addAll(Minecraft.getInstance().level.getRecipeManager()
            .getAllRecipesFor(AllFluidLogisticsRecipeTypes.INACTIVE_BULK_COOLING.getType()));
        return recipes;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        bulkCooling.registerRecipes(registration);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        bulkCooling.registerCatalysts(registration);
        for (RecipeType<BasinRecipe> recipeType : BASIN_RECIPE_TYPES)
            registration.addRecipeCatalyst(AllBlocks.COPPER_BASIN.asStack(), recipeType);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        if (!CompatMods.emiLoaded()) {
            registration.addGhostIngredientHandler((Class) AbstractFilterScreen.class, FILTER_FLUID_HANDLER);
            registration.addGhostIngredientHandler(ResourceFactoryGaugeSetFilterScreen.class,
                RESOURCE_GAUGE_SET_FILTER_FLUID_HANDLER);
            registration.addGhostIngredientHandler(RedstoneRequesterScreen.class, REDSTONE_REQUESTER_FLUID_HANDLER);
            registration.addGhostIngredientHandler(HandPointerFilterScreen.class, HandPointerFilterGhostHandler.INSTANCE);
        }
        registration.addGuiContainerHandler(StockKeeperRequestScreen.class,
            new StockKeeperRequestFluidGuiHandler());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }
}
