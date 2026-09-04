package com.yision.fluidlogistics.compat.jei;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.content.logistics.filter.AbstractFilterScreen;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterScreen;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.Create;
import com.simibubi.create.compat.jei.ConversionRecipe;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.compat.jei.category.ProcessingViaFanCategory;
import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.client.RedstoneRequesterAmountsAccess;
import com.yision.fluidlogistics.compat.CompatMods;
import com.yision.fluidlogistics.content.processing.cooling.BulkCoolingRecipe;
import com.yision.fluidlogistics.content.equipment.handPointer.filter.HandPointerFilterScreen;
import com.yision.fluidlogistics.content.logistics.factoryGauge.client.ResourceFactoryGaugeSetFilterScreen;
import com.yision.fluidlogistics.util.FluidAmountHelper;
import com.yision.fluidlogistics.registry.AllBlocks;
import com.yision.fluidlogistics.registry.AllFluidLogisticsRecipeTypes;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import java.util.List;
import org.jetbrains.annotations.Nullable;

@JeiPlugin
@SuppressWarnings("unused")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FluidLogisticsJEI implements IModPlugin {

    private static final ResourceLocation ID = FluidLogistics.asResource("jei_plugin");
    private static final RecipeType<RecipeHolder<ConversionRecipe>> MYSTERY_CONVERSION =
        RecipeType.createRecipeHolderType(Create.asResource("mystery_conversion"));
    private static final List<RecipeType<RecipeHolder<BasinRecipe>>> BASIN_RECIPE_TYPES = List.of(
        RecipeType.createRecipeHolderType(Create.asResource("mixing")),
        RecipeType.createRecipeHolderType(Create.asResource("automatic_shapeless")),
        RecipeType.createRecipeHolderType(Create.asResource("automatic_brewing")),
        RecipeType.createRecipeHolderType(Create.asResource("packing")),
        RecipeType.createRecipeHolderType(Create.asResource("automatic_packing"))
    );
    private static IJeiRuntime runtime;
    private CreateRecipeCategory<BulkCoolingRecipe> bulkCooling;

    @SuppressWarnings("rawtypes")
    private static final FluidGhostIngredientHandler FILTER_FLUID_HANDLER = new FluidGhostIngredientHandler();
    private static final FluidGhostIngredientHandler<ResourceFactoryGaugeSetFilterScreen> RESOURCE_GAUGE_SET_FILTER_FLUID_HANDLER =
        new FluidGhostIngredientHandler<>();
    private static final FluidGhostIngredientHandler<RedstoneRequesterScreen> REDSTONE_REQUESTER_FLUID_HANDLER =
        new FluidGhostIngredientHandler<>((gui, slotIndex) ->
            ((RedstoneRequesterAmountsAccess) gui).fluidlogistics$getAmounts()
                .set(slotIndex, FluidAmountHelper.DEFAULT_FLUID_REQUEST_AMOUNT));

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
        bulkCooling = new CreateRecipeCategory.Builder<>(BulkCoolingRecipe.class)
            .addTypedRecipes(AllFluidLogisticsRecipeTypes.BULK_COOLING)
            .addTypedRecipes(AllFluidLogisticsRecipeTypes.INACTIVE_BULK_COOLING)
            .catalystStack(ProcessingViaFanCategory.getFan("fan_cooling"))
            .catalyst(AllBlocks.BLAZE_COOLER::get)
            .doubleItemIcon(com.simibubi.create.AllItems.PROPELLER.get(), AllBlocks.BLAZE_COOLER.get())
            .emptyBackground(178, 72)
            .build(FluidLogistics.asResource("fan_cooling"), FanBulkCoolingCategory::new);
        registration.addRecipeCategories(bulkCooling);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        bulkCooling.registerRecipes(registration);
        registration.addRecipes(MYSTERY_CONVERSION, List.of(
            ConversionRecipe.create(com.simibubi.create.AllBlocks.BLAZE_BURNER.asStack(),
                AllBlocks.BLAZE_COOLER.asStack()),
            ConversionRecipe.create(AllBlocks.BLAZE_COOLER.asStack(),
                com.simibubi.create.AllBlocks.BLAZE_BURNER.asStack())
        ));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        bulkCooling.registerCatalysts(registration);
        for (RecipeType<RecipeHolder<BasinRecipe>> recipeType : BASIN_RECIPE_TYPES)
            registration.addRecipeCatalyst(AllBlocks.COPPER_BASIN.asStack(), recipeType);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        if (!CompatMods.emiLoaded()) {
            registration.addGhostIngredientHandler(AbstractFilterScreen.class, FILTER_FLUID_HANDLER);
            registration.addGhostIngredientHandler(ResourceFactoryGaugeSetFilterScreen.class,
                RESOURCE_GAUGE_SET_FILTER_FLUID_HANDLER);
            registration.addGhostIngredientHandler(RedstoneRequesterScreen.class, REDSTONE_REQUESTER_FLUID_HANDLER);
            registration.addGhostIngredientHandler(HandPointerFilterScreen.class, HandPointerFilterGhostHandler.INSTANCE);
        }
        registration.addGuiContainerHandler(StockKeeperRequestScreen.class, new StockKeeperRequestFluidGuiHandler());
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
