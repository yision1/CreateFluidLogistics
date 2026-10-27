package com.yision.fluidlogistics.compat.jei;

import com.simibubi.create.compat.jei.category.ProcessingViaFanCategory;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.yision.fluidlogistics.content.processing.cooling.BulkCoolingRecipe;
import com.yision.fluidlogistics.registry.AllPartialModels;

import net.minecraft.client.gui.GuiGraphics;

public class FanBulkCoolingCategory extends ProcessingViaFanCategory.MultiOutput<BulkCoolingRecipe> {

	public FanBulkCoolingCategory(Info<BulkCoolingRecipe> info) {
		super(info);
	}

	@Override
	protected void renderAttachedBlock(GuiGraphics graphics) {
		AnimatedKinetics.defaultBlockElement(AllPartialModels.SNOW_GOLEM_FREEZER_CAGE).scale(SCALE).atLocal(0, 0, 2)
			.render(graphics);
		AnimatedKinetics.defaultBlockElement(AllPartialModels.SNOW_GOLEM_FREEZER_INERT).rotate(0, 180, 0).scale(SCALE)
			.atLocal(1, .15, 3).render(graphics);
	}
}
