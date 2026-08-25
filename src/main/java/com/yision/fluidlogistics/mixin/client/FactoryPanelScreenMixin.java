package com.yision.fluidlogistics.mixin.client;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelScreen;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;
import com.yision.fluidlogistics.api.packager.PackageResourceDisplay;
import com.yision.fluidlogistics.api.packager.PackageResources;

import net.createmod.catnip.gui.AbstractSimiScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@Mixin(FactoryPanelScreen.class)
public abstract class FactoryPanelScreenMixin extends AbstractSimiScreen {

    @Shadow(remap = false)
    private FactoryPanelBehaviour behaviour;

    @Shadow(remap = false)
    private boolean restocker;

    protected FactoryPanelScreenMixin(Component title) {
        super(title);
    }

    @WrapOperation(
        method = {"renderInputItem", "renderWindow"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;renderItem("
                + "Lnet/minecraft/world/item/ItemStack;II)V",
            remap = true),
        remap = false)
    private void fluidlogistics$renderResourceIcon(
        GuiGraphics graphics, ItemStack stack, int x, int y, Operation<Void> original) {
        original.call(graphics, PackageResources.iconOf(stack)
            .orElse(stack), x, y);
    }

    @WrapOperation(
        method = "renderInputItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;renderItemDecorations("
                + "Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            remap = true),
        remap = false)
    private void fluidlogistics$renderResourceAmount(
        GuiGraphics graphics, net.minecraft.client.gui.Font font, ItemStack stack, int x, int y, String text,
        Operation<Void> original, @Local(argsOnly = true) BigItemStack itemStack) {
        String amountText = PackageResources.formatAmount(itemStack.stack, itemStack.count,
            PackageResourceDisplay.Format.COMPACT)
            .orElse(text);
        original.call(graphics, font, stack, x, y, amountText);
    }

    @WrapOperation(
        method = "renderInputItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;renderComponentTooltip("
                + "Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V",
            remap = true),
        remap = false)
    private void fluidlogistics$renderResourceTooltip(
        GuiGraphics graphics, net.minecraft.client.gui.Font font, List<Component> tooltips,
        int mouseX, int mouseY, Operation<Void> original,
        @Local(argsOnly = true) BigItemStack itemStack) {
        Component resourceName = PackageResources.nameOf(itemStack.stack)
            .orElse(null);
        if (resourceName == null) {
            original.call(graphics, font, tooltips, mouseX, mouseY);
            return;
        }

        String amountText = PackageResources.formatAmount(itemStack.stack, itemStack.count,
            PackageResourceDisplay.Format.PRECISE)
            .orElse(Integer.toString(itemStack.count));
        List<Component> resourceTooltips = new ArrayList<>();
        String header = restocker ? resourceName.getString() : resourceName.getString() + " x" + amountText;
        resourceTooltips.add(CreateLang.translate("gui.factory_panel.sending_item", header)
            .color(ScrollInput.HEADER_RGB)
            .component());
        if (restocker) {
            resourceTooltips.add(CreateLang.translate("gui.factory_panel.sending_item_tip")
                .style(ChatFormatting.GRAY)
                .component());
            resourceTooltips.add(CreateLang.translate("gui.factory_panel.sending_item_tip_1")
                .style(ChatFormatting.GRAY)
                .component());
        } else {
            resourceTooltips.add(CreateLang.translate("gui.factory_panel.scroll_to_change_amount")
                .style(ChatFormatting.DARK_GRAY)
                .style(ChatFormatting.ITALIC)
                .component());
            resourceTooltips.add(CreateLang.translate("gui.factory_panel.left_click_disconnect")
                .style(ChatFormatting.DARK_GRAY)
                .style(ChatFormatting.ITALIC)
                .component());
        }
        original.call(graphics, font, resourceTooltips, mouseX, mouseY);
    }

    @WrapOperation(
        method = "mouseScrolled",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/Mth;clamp(III)I",
            remap = true),
        remap = true)
    private int fluidlogistics$adjustResourceAmount(
        int value, int min, int max, Operation<Integer> original,
        @Local BigItemStack itemStack) {
        PackageResourceDisplay display = PackageResources.displayOf(itemStack.stack)
            .orElse(null);
        if (display == null)
            return original.call(value, min, max);

        int maximum = display.factoryPanelRestockPolicy(itemStack.stack)
            .maxRequestPerBatch();
        FactoryPanelScreen screen = (FactoryPanelScreen) (Object) this;
        int adjusted = PackageResources.adjustAmount(itemStack.stack,
            new PackageResourceDisplay.Adjustment(
                itemStack.count,
                value > itemStack.count,
                screen.hasShiftDown(),
                screen.hasControlDown(),
                1,
                maximum,
                1,
                PackageResourceDisplay.Interaction.FACTORY_PANEL))
            .orElse(Integer.MIN_VALUE);
        return adjusted == Integer.MIN_VALUE ? original.call(value, min, max) : adjusted;
    }
}
