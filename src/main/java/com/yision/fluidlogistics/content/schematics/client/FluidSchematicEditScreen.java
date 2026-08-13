package com.yision.fluidlogistics.content.schematics.client;

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.schematics.client.SchematicEditScreen;
import com.yision.fluidlogistics.client.FluidLogisticsGuiTextures;
import com.yision.fluidlogistics.registry.AllItems;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FluidSchematicEditScreen extends SchematicEditScreen {

    private static final FluidLogisticsGuiTextures BACKGROUND = FluidLogisticsGuiTextures.FLUID_SCHEMATIC;
    private final ItemStack renderedItem = AllItems.FLUID_SCHEMATIC.asStack();

    @Override
    protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x = guiLeft;
        int y = guiTop;

        BACKGROUND.render(graphics, x, y);

        String title = CreateClient.SCHEMATIC_HANDLER.getCurrentSchematicName();
        graphics.drawString(font, title, x + (BACKGROUND.getWidth() - 8 - font.width(title)) / 2, y + 4, 0x505050,
            false);

        GuiGameElement.of(renderedItem)
            .<GuiGameElement.GuiRenderBuilder>at(x + BACKGROUND.getWidth() + 6,
                y + BACKGROUND.getHeight() - 40, -200)
            .scale(3)
            .render(graphics);
    }
}
