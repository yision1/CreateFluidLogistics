package com.yision.fluidlogistics.content.logistics.factoryGauge.client;

import java.util.Collections;
import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.CreateLang;
import com.yision.fluidlogistics.content.logistics.factoryGauge.ResourceFactoryGaugeSetFilterMenu;
import com.yision.fluidlogistics.content.logistics.factoryGauge.ResourceFactoryPanelBehaviour;

import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ResourceFactoryGaugeSetFilterScreen extends AbstractSimiContainerScreen<ResourceFactoryGaugeSetFilterMenu> {

    private IconButton confirmButton;
    private List<Rect2i> extraAreas = Collections.emptyList();

    public ResourceFactoryGaugeSetFilterScreen(ResourceFactoryGaugeSetFilterMenu menu, Inventory inv,
        Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        int bgHeight = AllGuiTextures.FACTORY_GAUGE_SET_ITEM.getHeight();
        int bgWidth = AllGuiTextures.FACTORY_GAUGE_SET_ITEM.getWidth();
        setWindowSize(bgWidth, bgHeight + AllGuiTextures.PLAYER_INVENTORY.getHeight());
        super.init();
        clearWidgets();
        int x = getGuiLeft();
        int y = getGuiTop();

        confirmButton = new IconButton(x + bgWidth - 40, y + bgHeight - 25, AllIcons.I_CONFIRM);
        confirmButton.withCallback(() -> minecraft.player.closeContainer());
        addRenderableWidget(confirmButton);

        extraAreas = List.of(new Rect2i(x + bgWidth, y + bgHeight - 30, 40, 20));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = getGuiLeft();
        int y = getGuiTop();
        AllGuiTextures.FACTORY_GAUGE_SET_ITEM.render(graphics, x - 5, y);
        renderPlayerInventory(graphics, x + 5, y + 94);

        Component title = CreateLang.translate("gui.factory_panel.place_item_to_monitor")
            .component();
        graphics.drawString(font, title, x + imageWidth / 2 - font.width(title) / 2 - 5, y + 4, 0x3D3C48, false);

        GuiGameElement.of(previewStack())
            .scale(3)
            .render(graphics, x + 180, y + 48);
    }

    private ItemStack previewStack() {
        if (getMenu().contentHolder instanceof ResourceFactoryPanelBehaviour behaviour) {
            return behaviour.registeredType()
                .<ItemStack>map(type -> new ItemStack(type.item()
                    .get()))
                .orElseGet(AllBlocks.FACTORY_GAUGE::asStack);
        }
        return AllBlocks.FACTORY_GAUGE.asStack();
    }

    @Override
    public List<Rect2i> getExtraAreas() {
        return extraAreas;
    }
}
