package com.yision.fluidlogistics.content.schematics.client;

import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.yision.fluidlogistics.client.FluidLogisticsGuiTextures;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

public class FluidSchematicGuiGraphics extends GuiGraphics {

    public FluidSchematicGuiGraphics(GuiGraphics source) {
        super(Minecraft.getInstance(), source.bufferSource());
        pose().mulPose(source.pose().last().pose());
    }

    @Override
    public void blit(ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
        AllGuiTextures slot = AllGuiTextures.SCHEMATIC_SLOT;
        if (texture.equals(slot.location) && u == slot.getStartX() && v == slot.getStartY()
                && width == slot.getWidth() && height == slot.getHeight()) {
            FluidLogisticsGuiTextures.FLUID_SCHEMATIC_SLOT_OVERLAY.render(this, x, y);
            return;
        }
        super.blit(texture, x, y, u, v, width, height);
    }

    @Override
    public void blit(ResourceLocation texture, int x, int y, float u, float v, int width, int height,
            int textureWidth, int textureHeight) {
        ResourceLocation renderedTexture = texture.equals(AllGuiTextures.HUD_BACKGROUND.location)
            ? FluidLogisticsGuiTextures.SCHEMATIC_OVERLAY.location
            : texture;
        super.blit(renderedTexture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    @Override
    public int drawString(Font font, String text, int x, int y, int color, boolean shadow) {
        int renderedColor = FluidSchematicColors.replaceCreateBlueText(color);
        return super.drawString(font, text, x, y, renderedColor, shadow || renderedColor != color);
    }

    @Override
    public int drawString(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        int renderedColor = FluidSchematicColors.replaceCreateBlueText(color);
        return super.drawString(font, text, x, y, renderedColor, shadow || renderedColor != color);
    }
}
