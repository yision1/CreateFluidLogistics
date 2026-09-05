package com.yision.fluidlogistics.content.logistics.factoryGauge.client;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.AddressEditBox;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnectionHandler;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;
import com.yision.fluidlogistics.api.packager.PackageResourceDisplay;
import com.yision.fluidlogistics.api.packager.PackageResources;
import com.yision.fluidlogistics.api.packager.PackageResourceTypes;
import com.yision.fluidlogistics.api.packager.client.PackageResourceClient;
import com.yision.fluidlogistics.client.FluidLogisticsGuiTextures;
import com.yision.fluidlogistics.client.ResourceAmountScrollInput;
import com.yision.fluidlogistics.config.Config;
import com.yision.fluidlogistics.content.logistics.factoryGauge.ResourceFactoryPanelBehaviour;
import com.yision.fluidlogistics.content.logistics.packageResource.client.TableClothResourceDisplay;
import com.yision.fluidlogistics.network.factoryPanel.ResourceFactoryGaugeConfigurePacket;
import com.yision.fluidlogistics.registry.AllItems;
import com.yision.fluidlogistics.util.ResourceGaugeHelper;

import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ResourceFactoryGaugeScreen extends AbstractSimiScreen {

    private static final int FOOTER_OVERLAP = 3;
    private static final int FOOTER_EXTRA_HEIGHT = 24;

    private final ResourceFactoryPanelBehaviour behaviour;
    private final boolean restocker;
    private final boolean emptyFilter;

    private boolean sendRedstoneReset;
    private boolean sendClearPromises;
    private boolean sendReset;

    private AddressEditBox addressBox;
    private IconButton confirmButton;
    private IconButton deleteButton;
    private IconButton newInputButton;
    private IconButton relocateButton;
    private IconButton enhancementToggleButton;
    private ScrollInput promiseExpiration;

    private ResourceAmountScrollInput restockThresholdInput;
    private ResourceAmountScrollInput additionalStockInput;
    private ResourceAmountScrollInput promiseLimitInput;
    private ResourceAmountScrollInput targetAmountInput;

    private final ResourceFactoryGaugeScreenState state = new ResourceFactoryGaugeScreenState();

    public ResourceFactoryGaugeScreen(ResourceFactoryPanelBehaviour behaviour) {
        this.behaviour = behaviour;
        minecraft = Minecraft.getInstance();
        restocker = behaviour.panelBE().restocker;
        emptyFilter = behaviour.getFilter().isEmpty();
        updateConfigs();
    }

    private void updateConfigs() {
        state.refresh(behaviour, minecraft.level);
    }

    private FluidLogisticsGuiTextures gaugeTopTexture() {
        return restocker
            ? FluidLogisticsGuiTextures.FACTORY_GAUGE_RESTOCK
            : FluidLogisticsGuiTextures.FACTORY_GAUGE_RECIPE;
    }

    private int baseWindowHeight() {
        FluidLogisticsGuiTextures top = gaugeTopTexture();
        return emptyFilter
            ? top.getHeight() + FluidLogisticsGuiTextures.FACTORY_GAUGE_BOTTOM.getHeight() - 32
            : top.getHeight() + FluidLogisticsGuiTextures.FACTORY_GAUGE_BOTTOM.getHeight();
    }

    private boolean hasEnhancementControls() {
        if (emptyFilter)
            return false;
        var policy = ResourceGaugeHelper.policy(behaviour);
        return restocker ? policy.hasConfigurableSettings() : policy.configurablePromiseLimit();
    }

    private boolean isFluidGauge() {
        return behaviour.registeredType()
            .map(type -> PackageResourceTypes.FLUID.equals(type.resourceTypeId()))
            .orElse(false);
    }

    private boolean hasEnhancementToggle() {
        return isFluidGauge() && hasEnhancementControls();
    }

    private boolean hasFooter() {
        if (!hasEnhancementControls())
            return false;
        return !isFluidGauge() || behaviour.fluidlogistics$enhancementsVisible();
    }

    private int footerExtraHeight() {
        return hasFooter() ? FOOTER_EXTRA_HEIGHT : 0;
    }

    @Override
    protected void init() {
        int sizeX = FluidLogisticsGuiTextures.FACTORY_GAUGE_BOTTOM.getWidth();
        int baseHeight = baseWindowHeight();
        int sizeY = baseHeight + footerExtraHeight();

        setWindowSize(sizeX, sizeY);
        super.init();
        clearWidgets();

        int x = guiLeft;
        int y = guiTop;

        if (emptyFilter) {
            confirmButton = new IconButton(x + sizeX - 33, y + sizeY - 25, AllIcons.I_CONFIRM);
            confirmButton.withCallback(() -> minecraft.setScreen(null));
            confirmButton.setToolTip(CreateLang.translate("gui.factory_panel.save_and_close")
                .component());
            addRenderableWidget(confirmButton);
            return;
        }

        if (addressBox == null) {
            String frogAddress = behaviour.getFrogAddress();
            addressBox = new AddressEditBox(this, new NoShadowFontWrapper(font), x + 36, y + baseHeight - 51,
                108, 10, false, frogAddress);
            addressBox.setValue(behaviour.recipeAddress);
            addressBox.setTextColor(0x555555);
        }
        addressBox.setX(x + 36);
        addressBox.setY(y + baseHeight - 51);
        addRenderableWidget(addressBox);

        confirmButton = new IconButton(x + sizeX - 33, y + baseHeight - 25, AllIcons.I_CONFIRM);
        confirmButton.withCallback(() -> minecraft.setScreen(null));
        confirmButton.setToolTip(CreateLang.translate("gui.factory_panel.save_and_close")
            .component());
        addRenderableWidget(confirmButton);

        deleteButton = new IconButton(x + sizeX - 55, y + baseHeight - 25, AllIcons.I_TRASH);
        deleteButton.withCallback(() -> {
            sendReset = true;
            minecraft.setScreen(null);
        });
        deleteButton.setToolTip(CreateLang.translate("gui.factory_panel.reset")
            .component());
        addRenderableWidget(deleteButton);

        promiseExpiration = new ScrollInput(x + 97, y + baseHeight - 24, 28, 16).withRange(-1, 31)
            .titled(CreateLang.translate("gui.factory_panel.promises_expire_title")
                .component());
        promiseExpiration.setState(behaviour.promiseClearingInterval);
        addRenderableWidget(promiseExpiration);

        newInputButton = new IconButton(x + 31, y + 47, AllIcons.I_ADD);
        newInputButton.withCallback(() -> {
            FactoryPanelConnectionHandler.startConnection(behaviour);
            minecraft.setScreen(null);
        });
        newInputButton.setToolTip(CreateLang.translate("gui.factory_panel.connect_input")
            .component());

        relocateButton = new IconButton(x + 31, y + 67, AllIcons.I_MOVE_GAUGE);
        relocateButton.withCallback(() -> {
            FactoryPanelConnectionHandler.startRelocating(behaviour);
            minecraft.setScreen(null);
        });
        relocateButton.setToolTip(CreateLang.translate("gui.factory_panel.relocate")
            .component());

        if (!restocker) {
            addRenderableWidget(newInputButton);
            addRenderableWidget(relocateButton);
        }

        initEnhancementToggleButton(x, y, baseHeight);
        initResourceAmountInputs(x, y, baseHeight);
    }

    private void initEnhancementToggleButton(int x, int y, int baseHeight) {
        enhancementToggleButton = null;
        if (!hasEnhancementToggle())
            return;

        enhancementToggleButton = new IconButton(
            enhancementToggleX(x), y + baseHeight - 25,
            behaviour.fluidlogistics$enhancementsVisible()
                ? AllIcons.I_PRIORITY_HIGH : AllIcons.I_PRIORITY_LOW);
        enhancementToggleButton.withCallback(this::toggleEnhancementControls);
        enhancementToggleButton.setToolTip(enhancementToggleTooltip());
        addRenderableWidget(enhancementToggleButton);
    }

    private int enhancementToggleX(int x) {
        return x + 8 + (behaviour.targetedByLinks.isEmpty() ? 0 :25);
    }

    private void toggleEnhancementControls() {
        behaviour.fluidlogistics$setEnhancementsVisible(!behaviour.fluidlogistics$enhancementsVisible());
        sendIt();
        init();
    }

    private void updateEnhancementToggleButton() {
        if (enhancementToggleButton == null)
            return;
        enhancementToggleButton.setX(enhancementToggleX(guiLeft));
        enhancementToggleButton.setY(guiTop + baseWindowHeight() - 25);
        enhancementToggleButton.setIcon(behaviour.fluidlogistics$enhancementsVisible()
            ? AllIcons.I_PRIORITY_HIGH : AllIcons.I_PRIORITY_LOW);
        enhancementToggleButton.setToolTip(enhancementToggleTooltip());
    }

    private Component enhancementToggleTooltip() {
        return CreateLang.translateDirect(behaviour.fluidlogistics$enhancementsVisible()
            ? "fluidlogistics.gauge.hide_enhanced_settings"
            : "fluidlogistics.gauge.expand_enhanced_settings");
    }

    private PackageResourceDisplay.FactoryPanelRestockPolicy policy() {
        return ResourceGaugeHelper.policy(behaviour);
    }

    private boolean hasRestockThresholdControl() {
        return restocker && policy().configurableThreshold();
    }

    private boolean hasAdditionalStockControl() {
        return restocker && policy().configurableAdditionalStock();
    }

    private boolean hasPromiseLimitControl() {
        return policy().configurablePromiseLimit();
    }

    private void initResourceAmountInputs(int x, int y, int baseHeight) {
        restockThresholdInput = null;
        additionalStockInput = null;
        promiseLimitInput = null;
        targetAmountInput = null;

        if (!hasFooter())
            return;

        int inputY = y + baseHeight;
        if (hasRestockThresholdControl()) {
            restockThresholdInput = createResourceAmountInput(
                x + 8, inputY, 32, 17, 0, false,
                behaviour.fluidlogistics$getRestockThreshold(),
                CreateLang.translateDirect("fluidlogistics.gauge.restock_threshold"), true);
            addRenderableWidget(restockThresholdInput);
        }

        if (hasAdditionalStockControl()) {
            additionalStockInput = createResourceAmountInput(
                x + 52, inputY, 32, 17, 0, false,
                behaviour.fluidlogistics$getAdditionalStock(),
                CreateLang.translateDirect("fluidlogistics.gauge.request_additional"), true);
            addRenderableWidget(additionalStockInput);
        }

        if (hasPromiseLimitControl()) {
            int promiseX = restocker ? x + 92 : x + 67;
            int promiseWidth = restocker ? 32 : 57;
            promiseLimitInput = createResourceAmountInput(
                promiseX, inputY, promiseWidth, 17, -1, true,
                behaviour.fluidlogistics$getPromiseLimit(),
                CreateLang.translateDirect("fluidlogistics.gauge.promise_limit"), false);
            addRenderableWidget(promiseLimitInput);
        }

        if (isFluidGauge()) {
            targetAmountInput = createTargetAmountInput(x + 145, inputY);
            addRenderableWidget(targetAmountInput);
        }
    }

    private ResourceAmountScrollInput createTargetAmountInput(int x, int y) {
        ResourceAmountScrollInput input = new ResourceAmountScrollInput(x, y, 40, 18);
        int maximum = behaviour.fluidlogistics$getTargetAmountMaximum();
        ItemStack key = behaviour.getFilter();
        input.withRange(0, maximum + 1)
            .withShiftStep(1)
            .withStepFunction(context -> {
                int next = PackageResources.adjustAmount(key, new PackageResourceDisplay.Adjustment(
                    context.currentValue,
                    context.forward,
                    context.shift,
                    context.control,
                    0,
                    maximum,
                    1,
                    PackageResourceDisplay.Interaction.STOCK_KEEPER))
                    .orElse(context.currentValue);
                return Math.max(1, Math.abs(next - context.currentValue));
                })
            .calling(behaviour::fluidlogistics$setTargetAmount);
        input.withSecondaryHeader(() -> CreateLang.text(formatResourceAmount(
            input.getState(), true, false)).component());
        input.setState(behaviour.getAmount());
        input.titled(CreateLang.translateDirect("factory_panel.target_amount"));
        return input;
    }

    private ResourceAmountScrollInput createResourceAmountInput(
        int x, int y, int width, int height, int minValue, boolean allowUnlimited,
        int state, Component title, boolean zeroIsInactive) {
        ResourceAmountScrollInput input = new ResourceAmountScrollInput(x, y, width, height);
        configureResourceAmountInput(input, minValue, allowUnlimited);
        input.withSecondaryHeader(() -> CreateLang.text(formatResourceAmount(
            input.getState(), zeroIsInactive, false)).component());
        input.setState(state);
        input.titled(title.copy());
        return input;
    }

    private void configureResourceAmountInput(ResourceAmountScrollInput input, int minValue,
        boolean allowUnlimited) {
        int maximum = policy().maxSettingAmount();
        ItemStack key = behaviour.getFilter();
        input.withRange(minValue, maximum + 1)
            .withShiftStep(1)
            .withStepFunction(context -> {
                if (allowUnlimited && context.currentValue < 0)
                    return 1;
                int next = PackageResources.adjustAmount(key, new PackageResourceDisplay.Adjustment(
                    context.currentValue,
                    context.forward,
                    context.shift,
                    context.control,
                    minValue,
                    maximum,
                    1,
                    PackageResourceDisplay.Interaction.FACTORY_PANEL))
                    .orElse(context.currentValue);
                return Math.max(1, Math.abs(next - context.currentValue));
            });
    }

    private String formatResourceAmount(int amount, boolean zeroIsInactive, boolean multiplier) {
        if (amount < 0 || zeroIsInactive && amount == 0)
            return "---";
        String formatted = PackageResources.formatAmount(behaviour.getFilter(), amount,
            PackageResourceDisplay.Format.PRECISE)
            .orElse(Integer.toString(amount));
        return multiplier ? "x" + formatted : formatted;
    }

    @Override
    public void tick() {
        super.tick();
        if (emptyFilter)
            return;
        if (!state.matchesConnectionCount(behaviour.targetedBy.size())) {
            updateConfigs();
            init();
        }
        updateEnhancementToggleButton();
        addressBox.tick();
        promiseExpiration.titled(CreateLang
            .translate(promiseExpiration.getState() == -1 ? "gui.factory_panel.promises_do_not_expire"
                : "gui.factory_panel.promises_expire_title")
            .component());
        if (additionalStockInput != null)
            additionalStockInput.titled(CreateLang.translateDirect(
                additionalStockInput.getState() <= 0 ? "fluidlogistics.gauge.request_additional.none"
                    : "fluidlogistics.gauge.request_additional"));
        if (promiseLimitInput != null)
            promiseLimitInput.titled(CreateLang.translateDirect(
                promiseLimitInput.getState() < 0 ? "fluidlogistics.gauge.promise_limit.none"
                    : "fluidlogistics.gauge.promise_limit"));
    }

    @Override
    protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x = guiLeft;
        int y = guiTop;

        FluidLogisticsGuiTextures bg = restocker
            ? FluidLogisticsGuiTextures.FACTORY_GAUGE_RESTOCK
            : FluidLogisticsGuiTextures.FACTORY_GAUGE_RECIPE;
        if (restocker)
            FluidLogisticsGuiTextures.FACTORY_GAUGE_RECIPE.render(graphics, x, y - 16);
        bg.render(graphics, x, y);
        FluidLogisticsGuiTextures.FACTORY_GAUGE_BOTTOM.render(graphics, x, y + bg.getHeight());

        if (emptyFilter) {
            renderEmptyState(graphics, mouseX, mouseY, x, y);
            return;
        }

        int slot = 0;
        for (BigItemStack itemStack : state.inputConfig())
            renderInputItem(graphics, slot++, itemStack, mouseX, mouseY);

        if (restocker)
            renderInputItem(graphics, slot, new BigItemStack(behaviour.getFilter(), 1), mouseX, mouseY);

        if (!restocker) {
            int outputX = x + 160;
            int outputY = y + 48;
            BigItemStack outputConfig = state.outputConfig();
            graphics.renderItem(iconFor(outputConfig.stack), outputX, outputY);
            String outputText = PackageResources.formatAmount(outputConfig.stack, outputConfig.count,
                PackageResourceDisplay.Format.COMPACT)
                .orElse(Integer.toString(outputConfig.count));
            graphics.renderItemDecorations(font, iconFor(outputConfig.stack), outputX, outputY, outputText);

            if (mouseX >= outputX - 1 && mouseX < outputX + 17 && mouseY >= outputY - 1 && mouseY < outputY + 17) {
                Component resourceName = PackageResources.nameOf(outputConfig.stack)
                    .orElse(outputConfig.stack.getHoverName());
                String amountText = PackageResources.formatAmount(outputConfig.stack, outputConfig.count,
                    PackageResourceDisplay.Format.PRECISE)
                    .orElse(Integer.toString(outputConfig.count));
                graphics.renderComponentTooltip(font, List.of(
                    CreateLang.translate("gui.factory_panel.expected_output",
                        resourceName.getString() + " x" + amountText)
                        .color(ScrollInput.HEADER_RGB)
                        .component(),
                    CreateLang.translate("gui.factory_panel.expected_output_tip")
                        .style(ChatFormatting.GRAY)
                        .component(),
                    CreateLang.translate("gui.factory_panel.expected_output_tip_1")
                        .style(ChatFormatting.GRAY)
                        .component(),
                    CreateLang.translate("gui.factory_panel.expected_output_tip_2")
                        .style(ChatFormatting.DARK_GRAY)
                        .style(ChatFormatting.ITALIC)
                        .component()),
                    mouseX, mouseY);
            }
        }

        PoseStack ms = graphics.pose();
        ms.pushPose();
        ms.translate(0, 0, 10);

        if (addressBox.isHovered() && !addressBox.isFocused())
            showAddressBoxTooltip(graphics, mouseX, mouseY);

        Component title = CreateLang
            .translate(restocker ? "gui.factory_panel.title_as_restocker" : "gui.factory_panel.title_as_recipe")
            .component();
        graphics.drawString(font, title, x + 97 - font.width(title) / 2, y + (restocker ? -12 : 4), 0x3D3C48, false);

        int previewY = restocker ? 0 : 60;
        ms.pushPose();
        ms.translate(0, previewY, 0);
        GuiGameElement.of(behaviour.registeredType()
            .map(type -> new ItemStack(type.item()
                .get()))
            .orElseGet(com.simibubi.create.AllBlocks.FACTORY_GAUGE::asStack))
            .scale(4)
            .at(0, 0, -200)
            .render(graphics, x + 195, y + 55);
        if (!behaviour.getFilter()
            .isEmpty()
            && !PackageResourceClient.tryRenderFactoryPanelPreview(graphics, behaviour.getFilter(), x + 214,
                y + 68)) {
            GuiGameElement.of(iconFor(behaviour.getFilter()))
                .scale(1.625)
                .at(0, 0, 100)
                .render(graphics, x + 214, y + 68);
        }
        ms.popPose();

        if (!behaviour.targetedByLinks.isEmpty()) {
            ItemStack asStack = AllBlocks.REDSTONE_LINK.asStack();
            int itemX = x + 9;
            int itemY = y + baseWindowHeight() - 24;
            AllGuiTextures.FROGPORT_SLOT.render(graphics, itemX - 1, itemY - 1);
            graphics.renderItem(asStack, itemX, itemY);

            if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
                graphics.renderComponentTooltip(font, List.of(
                    CreateLang.translate("gui.factory_panel.has_link_connections")
                        .color(ScrollInput.HEADER_RGB)
                        .component(),
                    CreateLang.translate("gui.factory_panel.left_click_disconnect")
                        .style(ChatFormatting.DARK_GRAY)
                        .style(ChatFormatting.ITALIC)
                        .component()),
                    mouseX, mouseY);
            }
        }

        int state = promiseExpiration.getState();
        graphics.drawString(font,
            CreateLang.text(state == -1 ? " /" : state == 0 ? "30s" : state + "m")
                .component(),
            promiseExpiration.getX() + 3, promiseExpiration.getY() + 4, 0xffeeeeee, true);

        ItemStack packageStack = isFluidGauge()
            ? TableClothResourceDisplay.createPackage(behaviour.getFilter(), Config.getFluidPerPackage())
                .orElseGet(AllItems.FLUID_PACKAGE::asStack)
            : com.simibubi.create.content.logistics.box.PackageStyles.getDefaultBox();
        int promiseX = x + 68;
        int promiseY = y + baseWindowHeight() - 24;
        graphics.renderItem(packageStack, promiseX, promiseY);
        int promised = behaviour.getPromised();
        String promisedText = PackageResources.formatAmount(behaviour.getFilter(), promised,
            PackageResourceDisplay.Format.COMPACT)
            .orElse(Integer.toString(promised));
        graphics.renderItemDecorations(font, packageStack, promiseX, promiseY, promisedText);

        if (mouseX >= promiseX && mouseX < promiseX + 16 && mouseY >= promiseY && mouseY < promiseY + 16) {
            List<Component> promiseTip;
            if (promised == 0) {
                promiseTip = List.of(
                    CreateLang.translate("gui.factory_panel.no_open_promises")
                        .color(ScrollInput.HEADER_RGB)
                        .component(),
                    CreateLang
                        .translate(restocker ? "gui.factory_panel.restocker_promises_tip"
                            : "gui.factory_panel.recipe_promises_tip")
                        .style(ChatFormatting.GRAY)
                        .component(),
                    CreateLang
                        .translate(restocker ? "gui.factory_panel.restocker_promises_tip_1"
                            : "gui.factory_panel.recipe_promises_tip_1")
                        .style(ChatFormatting.GRAY)
                        .component(),
                    CreateLang.translate("gui.factory_panel.promise_prevents_oversending")
                        .style(ChatFormatting.GRAY)
                        .component());
            } else {
                Component resourceName = PackageResources.nameOf(behaviour.getFilter())
                    .orElse(behaviour.getFilter()
                        .getHoverName());
                String amountText = PackageResources.formatAmount(behaviour.getFilter(), promised,
                    PackageResourceDisplay.Format.PRECISE)
                    .orElse(Integer.toString(promised));
                promiseTip = List.of(
                    CreateLang.translate(isFluidGauge()
                        ? "fluidlogistics.gauge.promised_fluid"
                        : "gui.factory_panel.promised_items")
                        .color(ScrollInput.HEADER_RGB)
                        .component(),
                    Component.literal(resourceName.getString() + " x" + amountText),
                    CreateLang.translate("gui.factory_panel.left_click_reset")
                        .style(ChatFormatting.DARK_GRAY)
                        .style(ChatFormatting.ITALIC)
                        .component());
            }
            graphics.renderComponentTooltip(font, promiseTip, mouseX, mouseY);
        }

        ms.popPose();
        renderResourceAmountControls(graphics, x, y);
    }

    private void renderResourceAmountControls(GuiGraphics graphics, int x, int y) {
        if (!hasFooter())
            return;

        int footerY = y + baseWindowHeight() - FOOTER_OVERLAP;
        (restocker ? FluidLogisticsGuiTextures.FLUID_THRESHOLD_RESTOCK
            : FluidLogisticsGuiTextures.FLUID_THRESHOLD_RECIPE).render(graphics, x, footerY);

        if (restockThresholdInput != null)
            renderResourceAmount(graphics, restockThresholdInput, true);
        if (additionalStockInput != null)
            renderResourceAmount(graphics, additionalStockInput, true);
        if (promiseLimitInput != null)
            renderResourceAmount(graphics, promiseLimitInput, false);
        if (targetAmountInput != null)
            renderResourceAmount(graphics, targetAmountInput, true);
    }

    private void renderResourceAmount(GuiGraphics graphics, ResourceAmountScrollInput input,
        boolean zeroIsInactive) {
        graphics.drawCenteredString(font,
            formatResourceAmount(input.getState(), zeroIsInactive, false),
            input.getX() + input.getWidth() / 2 + 1,
            input.getY() + 5,
            0xffeeeeee);
    }

    private void renderEmptyState(GuiGraphics graphics, int mouseX, int mouseY, int x, int y) {
        Component itemName = behaviour.registeredType()
            .map(type -> new ItemStack(type.item()
                .get()))
            .orElseGet(com.simibubi.create.AllBlocks.FACTORY_GAUGE::asStack)
            .getHoverName();
        graphics.drawCenteredString(font,
            CreateLang.translate("fluidlogistics.factory_gauge.empty_filter_title", itemName.getString())
                .component(),
            x + FluidLogisticsGuiTextures.FACTORY_GAUGE_BOTTOM.getWidth() / 2, y + 20, 0x3D3C48);
        graphics.drawCenteredString(font,
            CreateLang.translate("logistics.filter.click_to_set")
                .component(),
            x + FluidLogisticsGuiTextures.FACTORY_GAUGE_BOTTOM.getWidth() / 2, y + 40, 0x7A7A8C);
    }

    private ItemStack iconFor(ItemStack stack) {
        return PackageResources.iconOf(stack)
            .orElse(stack);
    }

    private void renderInputItem(GuiGraphics graphics, int slot, BigItemStack itemStack, int mouseX, int mouseY) {
        int inputX = guiLeft + (restocker ? 88 : 68 + (slot % 3 * 20));
        int inputY = guiTop + (restocker ? 12 : 28) + (slot / 3 * 20);

        graphics.renderItem(iconFor(itemStack.stack), inputX, inputY);
        if (!restocker && !itemStack.stack.isEmpty()) {
            String amountText = PackageResources.formatAmount(itemStack.stack, itemStack.count,
                PackageResourceDisplay.Format.COMPACT)
                .orElse(Integer.toString(itemStack.count));
            graphics.renderItemDecorations(font, iconFor(itemStack.stack), inputX, inputY, amountText);
        }

        if (mouseX < inputX - 2 || mouseX >= inputX - 2 + 20 || mouseY < inputY - 2 || mouseY >= inputY - 2 + 20)
            return;

        if (itemStack.stack.isEmpty()) {
            graphics.renderComponentTooltip(font, List.of(
                CreateLang.translate("gui.factory_panel.empty_panel")
                    .color(ScrollInput.HEADER_RGB)
                    .component(),
                CreateLang.translate("gui.factory_panel.left_click_disconnect")
                    .style(ChatFormatting.DARK_GRAY)
                    .style(ChatFormatting.ITALIC)
                    .component()),
                mouseX, mouseY);
            return;
        }

        Component resourceName = PackageResources.nameOf(itemStack.stack)
            .orElse(itemStack.stack.getHoverName());
        String amountText = PackageResources.formatAmount(itemStack.stack, itemStack.count,
            PackageResourceDisplay.Format.PRECISE)
            .orElse(Integer.toString(itemStack.count));

        if (restocker) {
            graphics.renderComponentTooltip(font,
                List.of(CreateLang.translate("gui.factory_panel.sending_item", resourceName.getString())
                    .color(ScrollInput.HEADER_RGB)
                    .component(),
                    CreateLang.translate("gui.factory_panel.sending_item_tip")
                        .style(ChatFormatting.GRAY)
                        .component(),
                    CreateLang.translate("gui.factory_panel.sending_item_tip_1")
                        .style(ChatFormatting.GRAY)
                        .component()),
                mouseX, mouseY);
            return;
        }

        graphics.renderComponentTooltip(font,
            List.of(CreateLang.translate("gui.factory_panel.sending_item",
                resourceName.getString() + " x" + amountText)
                .color(ScrollInput.HEADER_RGB)
                .component(),
                CreateLang.translate("gui.factory_panel.scroll_to_change_amount")
                    .style(ChatFormatting.DARK_GRAY)
                    .style(ChatFormatting.ITALIC)
                    .component(),
                CreateLang.translate("gui.factory_panel.left_click_disconnect")
                    .style(ChatFormatting.DARK_GRAY)
                    .style(ChatFormatting.ITALIC)
                    .component()),
            mouseX, mouseY);
    }

    private void showAddressBoxTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (addressBox.getValue()
            .isBlank()) {
            graphics.renderComponentTooltip(font,
                List.of(CreateLang
                    .translate(restocker ? "gui.factory_panel.restocker_address"
                        : "gui.factory_panel.recipe_address")
                    .color(ScrollInput.HEADER_RGB)
                    .component(),
                    CreateLang
                        .translate(restocker ? "gui.factory_panel.restocker_address_tip"
                            : "gui.factory_panel.recipe_address_tip")
                        .style(ChatFormatting.GRAY)
                        .component(),
                    CreateLang
                        .translate(restocker ? "gui.factory_panel.restocker_address_tip_1"
                            : "gui.factory_panel.recipe_address_tip_1")
                        .style(ChatFormatting.GRAY)
                        .component(),
                    CreateLang.translate("gui.schedule.lmb_edit")
                        .style(ChatFormatting.DARK_GRAY)
                        .style(ChatFormatting.ITALIC)
                        .component()),
                mouseX, mouseY);
        } else {
            graphics.renderComponentTooltip(font,
                List.of(CreateLang
                    .translate(restocker ? "gui.factory_panel.restocker_address_given"
                        : "gui.factory_panel.recipe_address_given")
                    .color(ScrollInput.HEADER_RGB)
                    .component(),
                    CreateLang.text("'" + addressBox.getValue() + "'")
                        .style(ChatFormatting.GRAY)
                        .component()),
                mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int pButton) {
        if (getFocused() != null && !getFocused().isMouseOver(mouseX, mouseY))
            setFocused(null);

        if (emptyFilter)
            return super.mouseClicked(mouseX, mouseY, pButton);

        int x = guiLeft;
        int y = guiTop;

        if (!restocker)
            for (int i = 0; i < state.connections().size(); i++) {
                int inputX = x + 68 + (i % 3 * 20);
                int inputY = y + 28 + (i / 3 * 20);
                if (mouseX >= inputX && mouseX < inputX + 16 && mouseY >= inputY && mouseY < inputY + 16) {
                    removeConnection = state.connections().get(i).from;
                    playButtonSound();
                    minecraft.setScreen(null);
                    return true;
                }
            }

        int itemX = x + 68;
        int itemY = y + baseWindowHeight() - 24;
        if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
            sendClearPromises = true;
            sendIt();
            sendClearPromises = false;
            playButtonSound();
            return true;
        }

        itemX = x + 9;
        itemY = y + baseWindowHeight() - 24;
        if (!behaviour.targetedByLinks.isEmpty()
            && mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
            sendRedstoneReset = true;
            playButtonSound();
            minecraft.setScreen(null);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, pButton);
    }

    @Nullable
    private FactoryPanelPosition removeConnection;

    public void playButtonSound() {
        Minecraft.getInstance()
            .getSoundManager()
            .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.25f));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (emptyFilter || restocker)
            return super.mouseScrolled(mouseX, mouseY, scrollDelta);

        if (addressBox.mouseScrolled(mouseX, mouseY, scrollDelta))
            return true;

        int x = guiLeft;
        int y = guiTop;

        for (int i = 0; i < state.inputConfig().size(); i++) {
            int inputX = x + 68 + (i % 3 * 20);
            int inputY = y + 26 + (i / 3 * 20);
            if (mouseX >= inputX && mouseX < inputX + 16 && mouseY >= inputY && mouseY < inputY + 16) {
                BigItemStack itemStack = state.inputConfig().get(i);
                if (itemStack.stack.isEmpty())
                    return true;
                itemStack.count = adjustedAmount(itemStack, itemStack.count, scrollDelta);
                return true;
            }
        }

        int outputX = x + 160;
        int outputY = y + 48;
        if (mouseX >= outputX && mouseX < outputX + 16 && mouseY >= outputY && mouseY < outputY + 16) {
            BigItemStack outputConfig = state.outputConfig();
            outputConfig.count = adjustedAmount(outputConfig, outputConfig.count, scrollDelta);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollDelta);
    }

    private int adjustedAmount(BigItemStack itemStack, int current, double scrollY) {
        int maximum = PackageResources.displayOf(itemStack.stack)
            .map(display -> display.factoryPanelRestockPolicy(itemStack.stack)
                .maxRequestPerBatch())
            .orElse(64);
        int adjusted = PackageResources.adjustAmount(itemStack.stack,
            new PackageResourceDisplay.Adjustment(
                current,
                scrollY > 0,
                hasShiftDown(),
                hasControlDown(),
                1,
                maximum,
                1,
                PackageResourceDisplay.Interaction.FACTORY_PANEL))
            .orElse(current + (int) Math.signum(scrollY) * (hasShiftDown() ? 10 : 1));
        return Mth.clamp(adjusted, 1, maximum);
    }

    @Override
    public void removed() {
        if (!emptyFilter)
            sendIt();
        super.removed();
    }

    private void sendIt() {
        FactoryPanelPosition pos = behaviour.getPanelPosition();
        int promiseExp = promiseExpiration.getState();
        String address = addressBox.getValue();

        int restockThreshold = restockThresholdInput == null
            ? behaviour.fluidlogistics$getRestockThreshold() : restockThresholdInput.getState();
        int promiseLimit = promiseLimitInput == null
            ? behaviour.fluidlogistics$getPromiseLimit() : promiseLimitInput.getState();
        int additionalStock = additionalStockInput == null
            ? behaviour.fluidlogistics$getAdditionalStock() : additionalStockInput.getState();
        int targetAmount = targetAmountInput == null
            ? behaviour.getAmount() : targetAmountInput.getState();

        ResourceFactoryGaugeConfigurePacket packet = new ResourceFactoryGaugeConfigurePacket(
            pos, address, state.inputAmounts(), state.outputConfig().count, targetAmount, promiseExp,
            restockThreshold, promiseLimit, additionalStock, behaviour.fluidlogistics$enhancementsVisible(),
            removeConnection, sendClearPromises, sendReset, sendRedstoneReset);
        com.yision.fluidlogistics.network.FluidLogisticsPackets.getChannel().sendToServer(packet);
    }
}
