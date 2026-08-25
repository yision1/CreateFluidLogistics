package com.yision.fluidlogistics.content.logistics.factoryGauge;

import java.util.Optional;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import com.yision.fluidlogistics.api.packager.PackageResourceDisplay;
import com.yision.fluidlogistics.api.packager.PackageResources;
import com.yision.fluidlogistics.api.packager.PackageResourceTypes;
import com.yision.fluidlogistics.util.FluidAmountHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@ApiStatus.Internal
public final class ResourceFactoryGaugeController {

    public ResourceGaugeDecision evaluate(
        ResourceFactoryGaugeConfig config,
        ResourceFactoryGaugeRuntime runtime,
        ResourceGaugeSnapshot snapshot) {

        int nextRemaining = remainingAdditionalStockAfterConsumption(runtime, snapshot);
        boolean shouldSatisfy;
        boolean shouldPromiseSatisfy;
        int demand;

        if (config.normalizedResourceKey().isEmpty()) {
            demand = 0;
            shouldSatisfy = true;
            shouldPromiseSatisfy = true;
        } else {
            PackageResourceDisplay.FactoryPanelRestockPolicy policy = policyOf(config);
            demand = policy.restockDemand(config.targetAmount(), nextRemaining);
            int threshold = policy.effectiveThreshold(config.restockThreshold());
            shouldSatisfy = demand - snapshot.stored() < threshold;
            shouldPromiseSatisfy = demand - snapshot.stored() - snapshot.promised() < threshold;
        }

        boolean shouldWait = snapshot.unloadedLinks() > 0;
        if (!shouldSatisfy && nextRemaining <= 0 && snapshot.restocker()
            && config.hasAdditionalStock())
            nextRemaining = config.additionalStock();
        else if (shouldSatisfy)
            nextRemaining = 0;

        return new ResourceGaugeDecision(
            shouldSatisfy,
            shouldPromiseSatisfy,
            shouldWait,
            nextRemaining,
            shouldSatisfy != snapshot.satisfiedLike(),
            demand > 0 && !snapshot.satisfiedLike() && shouldSatisfy);
    }

    public Optional<ResourceGaugeDecision.ResourceRequestPlan> evaluateRestockPlan(
        ResourceFactoryGaugeConfig config,
        ResourceFactoryGaugeRuntime runtime,
        ResourceGaugeSnapshot snapshot) {

        if (!snapshot.restocker() || config.normalizedResourceKey().isEmpty() || !snapshot.storageAvailable())
            return Optional.empty();

        PackageResourceDisplay.FactoryPanelRestockPolicy policy = policyOf(config);
        int demand = policy.restockDemand(config.targetAmount(), runtime.remainingAdditionalStock);
        long shortageValue = (long) demand - snapshot.promised() - snapshot.stored();
        if (shortageValue < policy.effectiveThreshold(config.restockThreshold()))
            return Optional.empty();

        int shortage = (int) Math.min(com.simibubi.create.content.logistics.BigItemStack.INF, shortageValue);
        int amountToOrder = Math.min(shortage, snapshot.availableOnNetwork());
        amountToOrder = Math.min(amountToOrder, policy.maxRequestPerBatch());
        amountToOrder = Math.min(amountToOrder,
            policy.remainingPromiseCapacity(config.promiseLimit(), snapshot.promised()));
        if (amountToOrder <= 0 || snapshot.availableOnNetwork() == 0)
            return Optional.empty();

        return Optional.of(new ResourceGaugeDecision.ResourceRequestPlan(
            config.normalizedResourceKey().copy(),
            amountToOrder,
            snapshot.network(),
            snapshot.storageIdentity(),
            snapshot.address()));
    }

    public ValueSettingsBoard createValueSettingsBoard(ItemStack filter, PackageResourceDisplay display,
        ValueSettingsBoard original) {
        return new ValueSettingsBoard(
            CreateLang.translate("factory_panel.target_amount")
                .component(),
            display.factoryPanelMaxValue(filter),
            display.factoryPanelMilestoneInterval(filter),
            display.factoryPanelUnits(filter)
                .stream()
                .<Component>map(unit -> Component.literal(unit.label()))
                .toList(),
            original.formatter());
    }

    public MutableComponent formatValue(@Nullable ResourceLocation resourceTypeId, ItemStack filter,
        PackageResourceDisplay display, int currentAmount, boolean upTo,
        ValueSettingsBehaviour.ValueSettings value) {
        if (value.value() == 0)
            return CreateLang.translateDirect("gui.factory_panel.inactive");

        if (PackageResourceTypes.FLUID.equals(resourceTypeId)) {
            int amount = display.factoryPanelAmount(filter, value.row(), value.value());
            ValueSettingsBehaviour.ValueSettings current = valueSettings(filter, display, currentAmount, upTo);
            if (current.equals(value) && currentAmount != amount)
                return Component.literal(display.format(filter, currentAmount,
                    PackageResourceDisplay.Format.PRECISE));
            return Component.literal(display.format(filter, amount, PackageResourceDisplay.Format.PRECISE));
        }

        var units = display.factoryPanelUnits(filter);
        int row = Math.max(0, Math.min(units.size() - 1, value.row()));
        int displayedValue = display.factoryPanelDisplayedValue(filter, row, value.value());
        return Component.literal(displayedValue + units.get(row)
            .label());
    }

    public ValueSettingsBehaviour.ValueSettings valueSettings(ItemStack filter, PackageResourceDisplay display,
        int amount, boolean upTo) {
        int unitCount = display.factoryPanelUnits(filter)
            .size();
        int row = upTo || unitCount == 1 ? 0
            : Math.max(1, Math.min(unitCount - 1, display.factoryPanelRow(filter, amount)));
        int value = display.factoryPanelValue(filter, row, amount);
        return new ValueSettingsBehaviour.ValueSettings(row, value);
    }

    public int amountForSettings(ItemStack filter, PackageResourceDisplay display,
        ValueSettingsBehaviour.ValueSettings settings) {
        return display.factoryPanelAmount(filter, settings.row(), settings.value());
    }

    public int targetAmountMaximum(@Nullable ResourceLocation resourceTypeId, ItemStack filter,
        @Nullable PackageResourceDisplay display) {
        if (PackageResourceTypes.FLUID.equals(resourceTypeId))
            return FluidAmountHelper.MAX_FACTORY_GAUGE_TARGET_AMOUNT;
        if (display == null)
            return 0;
        long maximum = display.factoryPanelUnits(filter)
            .stream()
            .mapToLong(unit -> (long) unit.amountPerStep() * display.factoryPanelMaxValue(filter))
            .max()
            .orElse(0L);
        return (int) Math.min(Integer.MAX_VALUE, maximum);
    }

    private PackageResourceDisplay.FactoryPanelRestockPolicy policyOf(ResourceFactoryGaugeConfig config) {
        ItemStack key = config.normalizedResourceKey();
        return PackageResources.displayOf(key)
            .map(display -> display.factoryPanelRestockPolicy(key))
            .orElseGet(PackageResourceDisplay.FactoryPanelRestockPolicy::standard);
    }

    private int remainingAdditionalStockAfterConsumption(ResourceFactoryGaugeRuntime runtime,
        ResourceGaugeSnapshot snapshot) {
        if (!snapshot.restocker())
            return 0;
        int consumed = Math.max(0, runtime.lastStored - snapshot.stored());
        return Math.max(0, runtime.remainingAdditionalStock - consumed);
    }
}
