package com.yision.fluidlogistics.compat.jei;

import com.simibubi.create.compat.jei.category.ProcessingViaFanCategory;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.yision.fluidlogistics.content.processing.cooling.BulkCoolingRecipe;
import com.yision.fluidlogistics.registry.AllPartialModels;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import net.minecraft.client.gui.GuiGraphics;

public class FanBulkCoolingCategory extends ProcessingViaFanCategory.MultiOutput<BulkCoolingRecipe> {

    private boolean renderActiveCooler;

    public FanBulkCoolingCategory(Info<BulkCoolingRecipe> info) {
        super(info);
    }

    @Override
    public void draw(BulkCoolingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
            double mouseX, double mouseY) {
        renderActiveCooler = recipe.requiresActiveCooler();
        super.draw(recipe, recipeSlotsView, graphics, mouseX, mouseY);
    }

    @Override
    protected void renderAttachedBlock(GuiGraphics graphics) {
        if (renderActiveCooler) {
            AnimatedKinetics.defaultBlockElement(AllPartialModels.BLAZE_COOLER_ITEM)
                .scale(SCALE)
                .atLocal(0, 0, 2)
                .render(graphics);
            return;
        }

        AnimatedKinetics.defaultBlockElement(AllPartialModels.BLAZE_COOLER_CAGE)
            .scale(SCALE)
            .atLocal(0, 0, 2)
            .render(graphics);
        AnimatedKinetics.defaultBlockElement(AllPartialModels.BLAZE_COOLER_INERT)
            .rotate(0, 180, 0)
            .scale(SCALE)
            .atLocal(1, .15, 3)
            .render(graphics);
    }
}
