package com.yision.fluidlogistics.client;

import com.yision.fluidlogistics.FluidLogistics;

import net.createmod.catnip.gui.TextureSheetSegment;
import net.createmod.catnip.gui.element.ScreenElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public enum FluidLogisticsGuiTextures implements ScreenElement, TextureSheetSegment {

	FACTORY_GAUGE_RECIPE("fluid_factory_gauge/factory_gauge", 32, 0, 192, 96, 256, 256),
	FACTORY_GAUGE_RESTOCK("fluid_factory_gauge/factory_gauge", 32, 112, 192, 40, 256, 256),
	FACTORY_GAUGE_BOTTOM("fluid_factory_gauge/factory_gauge", 32, 176, 200, 64, 256, 256),
	FACTORY_GAUGE_SET_FILTER("fluid_factory_gauge/requester", 16, 160, 184, 88, 256, 256),
	FLUID_THRESHOLD_RESTOCK("fluid_factory_gauge/fluid_threshold_restock", 0, 0, 192, 28, 192, 32),
	FLUID_THRESHOLD_RECIPE("fluid_factory_gauge/fluid_threshold_recipe", 0, 0, 192, 28, 192, 32),

	SCHEMATIC_OVERLAY("fluid_schematic/schematic_overlay", 0, 0, 16, 16, 16, 16),
	FLUID_SCHEMATIC_SLOT_OVERLAY("fluid_schematic/fluid_schematic_slot_overlay", 0, 0, 16, 16, 16, 16),
	FLUID_SCHEMATIC("fluid_schematic/fluid_schematics", 10, 8, 192, 123, 256, 256),

	COPPER_SCHEMATICANNON_TITLE("copper_schematicannon/copper_schematicannon", 0, 0, 205, 15, 256, 256),
	COPPER_SCHEMATICANNON_TOP("copper_schematicannon/copper_schematicannon", 0, 77, 213, 42, 256, 256),
	COPPER_SCHEMATICANNON_BOTTOM("copper_schematicannon/copper_schematicannon", 0, 119, 213, 99, 256, 256),
	COPPER_SCHEMATICANNON_PROGRESS("copper_schematicannon/copper_schematicannon", 76, 239, 114, 16, 256, 256),
	COPPER_SCHEMATICANNON_CHECKLIST_PROGRESS("copper_schematicannon/copper_schematicannon", 191, 240, 16, 14, 256, 256),
	COPPER_SCHEMATICANNON_HIGHLIGHT("copper_schematicannon/copper_schematicannon", 1, 229, 26, 26, 256, 256),
	COPPER_SCHEMATICANNON_FUEL("copper_schematicannon/copper_schematicannon", 28, 222, 47, 16, 256, 256),
	COPPER_SCHEMATICANNON_FUEL_CREATIVE("copper_schematicannon/copper_schematicannon", 28, 239, 47, 16, 256, 256),

	FROGPORT_HEADER("copper_frogport/frogport_and_mailbox", 0, 0, 214, 17, 256, 256),
	FROGPORT_SLOT("copper_frogport/frogport_and_mailbox", 26, 55, 18, 18, 256, 256),
	FROGPORT_EDIT_NAME("copper_frogport/frogport_and_mailbox", 230, 3, 13, 13, 256, 256),
	FROGPORT_BG("copper_frogport/frogport_and_mailbox", 0, 47, 220, 82, 256, 256);

	public final ResourceLocation location;
	private final int width;
	private final int height;
	private final int startX;
	private final int startY;
	private final int textureWidth;
	private final int textureHeight;

	FluidLogisticsGuiTextures(String location, int startX, int startY, int width, int height, int textureWidth,
		int textureHeight) {
		this.location = FluidLogistics.asResource("textures/gui/" + location + ".png");
		this.width = width;
		this.height = height;
		this.startX = startX;
		this.startY = startY;
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
	}

	@Override
	public ResourceLocation getLocation() {
		return location;
	}

	@OnlyIn(Dist.CLIENT)
	public void render(GuiGraphics graphics, int x, int y) {
		graphics.blit(location, x, y, startX, startY, width, height, textureWidth, textureHeight);
	}

	@Override
	public int getStartX() {
		return startX;
	}

	@Override
	public int getStartY() {
		return startY;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}
}
