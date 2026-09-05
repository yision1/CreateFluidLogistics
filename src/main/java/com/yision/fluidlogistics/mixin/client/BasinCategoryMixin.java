package com.yision.fluidlogistics.mixin.client;

import com.simibubi.create.compat.jei.category.BasinCategory;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.yision.fluidlogistics.compat.jei.animation.AnimatedBlazeCooler;
import com.yision.fluidlogistics.content.processing.cooling.CoolingRecipe;
import com.yision.fluidlogistics.registry.AllBlocks;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BasinCategory.class)
public abstract class BasinCategoryMixin {

    @Unique
    private static final int FLUIDLOGISTICS$COOLING_COLOR = 0x3F76C5;

    @Unique
    private static final AnimatedBlazeCooler FLUIDLOGISTICS$COOLER = new AnimatedBlazeCooler();

    @Unique
    private boolean fluidlogistics$coolingRecipe;

    @Unique
    private boolean fluidlogistics$supercooledRecipe;

    @Inject(
        method = "setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;"
            + "Lcom/simibubi/create/content/processing/basin/BasinRecipe;"
            + "Lmezz/jei/api/recipe/IFocusGroup;)V",
        at = @At("TAIL"),
        remap = false
    )
    private void fluidlogistics$addCoolerCatalyst(IRecipeLayoutBuilder builder, BasinRecipe recipe,
            IFocusGroup focuses, CallbackInfo ci) {
        if (recipe instanceof CoolingRecipe)
            builder.addSlot(RecipeIngredientRole.CATALYST, 134, 81)
                .addItemStack(AllBlocks.BLAZE_COOLER.asStack());
    }

    @Inject(
        method = "draw(Lcom/simibubi/create/content/processing/basin/BasinRecipe;"
            + "Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;"
            + "Lnet/minecraft/client/gui/GuiGraphics;DD)V",
        at = @At("HEAD"),
        remap = false
    )
    private void fluidlogistics$trackCoolingRecipe(BasinRecipe recipe, IRecipeSlotsView recipeSlotsView,
            GuiGraphics graphics, double mouseX, double mouseY, CallbackInfo ci) {
        fluidlogistics$coolingRecipe = recipe instanceof CoolingRecipe;
        fluidlogistics$supercooledRecipe = recipe instanceof CoolingRecipe coolingRecipe
            && coolingRecipe.requiresSupercooling();
    }

    @ModifyVariable(
        method = "draw(Lcom/simibubi/create/content/processing/basin/BasinRecipe;"
            + "Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;"
            + "Lnet/minecraft/client/gui/GuiGraphics;DD)V",
        at = @At("STORE"),
        ordinal = 0,
        remap = false
    )
    private boolean fluidlogistics$useHeatLayoutForCooling(boolean noHeat) {
        return fluidlogistics$coolingRecipe ? false : noHeat;
    }

    @Inject(
        method = "draw(Lcom/simibubi/create/content/processing/basin/BasinRecipe;"
            + "Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;"
            + "Lnet/minecraft/client/gui/GuiGraphics;DD)V",
        at = @At("TAIL"),
        remap = false
    )
    private void fluidlogistics$drawBlazeCooler(BasinRecipe recipe, IRecipeSlotsView recipeSlotsView,
            GuiGraphics graphics, double mouseX, double mouseY, CallbackInfo ci) {
        if (fluidlogistics$coolingRecipe)
            FLUIDLOGISTICS$COOLER.draw(graphics, 91, 55, fluidlogistics$supercooledRecipe);
    }

    @ModifyArg(
        method = "draw(Lcom/simibubi/create/content/processing/basin/BasinRecipe;"
            + "Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;"
            + "Lnet/minecraft/client/gui/GuiGraphics;DD)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;"
                + "Lnet/minecraft/network/chat/Component;IIIZ)I",
            remap = true
        ),
        index = 1,
        remap = false
    )
    private Component fluidlogistics$replaceCoolingRequirement(Component original) {
        return fluidlogistics$coolingRecipe
            ? Component.translatable(fluidlogistics$supercooledRecipe
            ? "recipe.heat_requirement.supercooling"
            : "recipe.heat_requirement.cooling")
            : original;
    }

    @ModifyArg(
        method = "draw(Lcom/simibubi/create/content/processing/basin/BasinRecipe;"
            + "Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;"
            + "Lnet/minecraft/client/gui/GuiGraphics;DD)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;"
                + "Lnet/minecraft/network/chat/Component;IIIZ)I",
            remap = true
        ),
        index = 4,
        remap = false
    )
    private int fluidlogistics$replaceCoolingRequirementColor(int original) {
        return fluidlogistics$coolingRecipe ? FLUIDLOGISTICS$COOLING_COLOR : original;
    }
}
