package com.yision.fluidlogistics.content.logistics.factoryGauge.client;

import com.yision.fluidlogistics.content.logistics.fluidPackage.CompressedTankItem;
import com.yision.fluidlogistics.foundation.render.GuiFluidBlockRenderer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public final class FactoryPanelFluidPreviewRenderer {
	private static final float PREVIEW_SCALE = 1.625f;

	private FactoryPanelFluidPreviewRenderer() {
		throw new AssertionError("This class should not be instantiated");
	}

	public static void render(GuiGraphics graphics, ItemStack resourceKey, int x, int y) {
		GuiFluidBlockRenderer.render(
			graphics, CompressedTankItem.getFluid(resourceKey), x, y, PREVIEW_SCALE);
	}
}
