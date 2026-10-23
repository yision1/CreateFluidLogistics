package com.yision.fluidlogistics.content.logistics.factoryGauge.client;

import com.yision.fluidlogistics.api.packager.PackageResourceTypes;
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
			graphics, PackageResourceTypes.getFluid(resourceKey), x, y, PREVIEW_SCALE);
	}
}
