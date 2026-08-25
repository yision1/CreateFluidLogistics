package com.yision.fluidlogistics.content.logistics.factoryGauge;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;
import org.joml.Math;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelEffectPacket;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.api.factorygauge.FactoryGaugeType;
import com.yision.fluidlogistics.api.factorygauge.FactoryGauges;
import com.yision.fluidlogistics.api.packager.PackageResourceDisplay;
import com.yision.fluidlogistics.api.packager.PackageResources;
import com.yision.fluidlogistics.api.packager.PackageResourceType;
import com.yision.fluidlogistics.content.logistics.factoryGauge.ResourceGaugeDecision.ResourceRequestPlan;
import com.yision.fluidlogistics.content.logistics.packageResource.ResourceRestockSettings;
import com.yision.fluidlogistics.registry.AllItems;
import com.yision.fluidlogistics.util.ResourceGaugeHelper;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.Tags;

public class ResourceFactoryPanelBehaviour extends FactoryPanelBehaviour
    implements ResourceRestockSettings {

    private static final ResourceFactoryGaugeController CONTROLLER = new ResourceFactoryGaugeController();
    private static final ResourceFactoryGaugeEnvironment ENVIRONMENT = ResourceFactoryGaugeEnvironment.INSTANCE;

    @Nullable
    private ResourceLocation gaugeTypeId;

    final ResourceFactoryGaugeRuntime resourceRuntime = new ResourceFactoryGaugeRuntime();

    private int fluidlogistics$lazyTickCounter = 40;
    private boolean fluidlogistics$promisePrimed;

    private int fluidlogistics$restockThreshold = ResourceGaugeHelper.DEFAULT_RESTOCK_THRESHOLD;
    private int fluidlogistics$promiseLimit = ResourceGaugeHelper.DEFAULT_PROMISE_LIMIT;
    private int fluidlogistics$additionalStock = ResourceGaugeHelper.DEFAULT_ADDITIONAL_STOCK;
    private boolean fluidlogistics$enhancementsVisible;

    public ResourceFactoryPanelBehaviour(FactoryPanelBlockEntity be, PanelSlot slot) {
        super(be, slot);
    }

    public boolean isResourceGauge() {
        return gaugeTypeId != null;
    }

    public Optional<FactoryGaugeType> registeredType() {
        return gaugeTypeId == null ? Optional.empty() : FactoryGauges.get(gaugeTypeId);
    }

    @Nullable
    public ResourceLocation gaugeTypeId() {
        return gaugeTypeId;
    }

    public void setGaugeTypeId(ResourceLocation typeId) {
        this.gaugeTypeId = typeId;
        this.resourceRuntime.reset();
        blockEntity.setChanged();
    }

    public void resetResourceSettings() {
        fluidlogistics$setRestockThreshold(ResourceGaugeHelper.DEFAULT_RESTOCK_THRESHOLD);
        fluidlogistics$setPromiseLimit(ResourceGaugeHelper.DEFAULT_PROMISE_LIMIT);
        fluidlogistics$setAdditionalStock(ResourceGaugeHelper.DEFAULT_ADDITIONAL_STOCK);
        fluidlogistics$enhancementsVisible = false;
        resourceRuntime.reset();
    }

    public static ResourceFactoryPanelBehaviour migrateRuntimeState(
        FactoryPanelBlockEntity be, PanelSlot slot, FactoryPanelBehaviour old) {
        ResourceFactoryPanelBehaviour replacement = new ResourceFactoryPanelBehaviour(be, slot);
        if (be.getLevel() == null || !old.active)
            return replacement;
        CompoundTag carried = new CompoundTag();
        old.write(carried, be.getLevel().registryAccess(), false);
        replacement.read(carried, be.getLevel().registryAccess(), false);
        return replacement;
    }

    @Override
    public void tick() {
        if (!isResourceGauge()) {
            super.tick();
            return;
        }

        if (fluidlogistics$lazyTickCounter-- <= 0) {
            fluidlogistics$lazyTickCounter = 40;
            lazyTick();
        }

        if (getWorld().isClientSide()) {
            if (blockEntity.isVirtual())
                tickResourceStorageMonitor();
            bulb.updateChaseTarget(redstonePowered || satisfied ? 1 : 0);
            bulb.tickChaser();
            if (active)
                CatnipServices.PLATFORM.executeOnClientOnly(
                    () -> () -> com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedClientHandler
                        .tickPanel(this));
            return;
        }

        if (!fluidlogistics$promisePrimed) {
            restockerPromises.setOnChanged(blockEntity::setChanged);
            fluidlogistics$promisePrimed = true;
        }

        tickResourceStorageMonitor();
        tickResourceRequests();
    }

    private void tickResourceStorageMonitor() {
        ResourceGaugeSnapshot snapshot = ENVIRONMENT.captureMonitor(this, resourceRuntime);
        ResourceGaugeDecision decision = CONTROLLER.evaluate(config(), resourceRuntime, snapshot);
        ENVIRONMENT.apply(this, decision, snapshot);
    }

    private void tickResourceRequests() {
        FactoryPanelBlockEntity panelBE = panelBE();
        if (targetedBy.isEmpty() && !panelBE.restocker)
            return;
        if (panelBE.restocker)
            restockerPromises.tick();
        if (satisfied || promisedSatisfied || waitingForNetwork || redstonePowered)
            return;
        if (resourceRuntime.requestTimer > 0) {
            resourceRuntime.requestTimer = Math.min(resourceRuntime.requestTimer, configRequestIntervalInTicks());
            resourceRuntime.requestTimer--;
            return;
        }

        resetTimer();

        if (recipeAddress.isBlank())
            return;

        if (panelBE.restocker) {
            tryResourceRestock();
            return;
        }

        PackageResourceDisplay.FactoryPanelRestockPolicy policy = ResourceGaugeHelper.policy(this);
        if (policy.configurablePromiseLimit() && fluidlogistics$promiseLimit >= 0) {
            int capacity = policy.remainingPromiseCapacity(fluidlogistics$promiseLimit, getPromised());
            if (capacity < Math.max(1, recipeOutput))
                return;
        }

        ENVIRONMENT.executeRecipeRequest(this);
    }

    private void tryResourceRestock() {
        if (getFilter().isEmpty())
            return;

        ResourceGaugeSnapshot snapshot = ENVIRONMENT.captureRestock(this);
        if (!snapshot.storageAvailable() || snapshot.storageIdentity() == null) {
            sendResourceEffect(getPanelPosition(), false);
            return;
        }
        if (snapshot.availableOnNetwork() == 0) {
            sendResourceEffect(getPanelPosition(), false);
            return;
        }

        Optional<ResourceRequestPlan> plan = CONTROLLER.evaluateRestockPlan(config(), resourceRuntime, snapshot);
        if (plan.isEmpty())
            return;
        ENVIRONMENT.executeRequest(this, plan.get());
    }

    private ResourceFactoryGaugeConfig config() {
        ItemStack key = getFilter();
        return new ResourceFactoryGaugeConfig(key.isEmpty() ? ItemStack.EMPTY : key, getAmount(),
            fluidlogistics$restockThreshold, fluidlogistics$promiseLimit, fluidlogistics$additionalStock);
    }

    private int configRequestIntervalInTicks() {
        return AllConfigs.server().logistics.factoryGaugeTimer.get();
    }

    @Override
    public void resetTimer() {
        if (!isResourceGauge()) {
            super.resetTimer();
            return;
        }
        resourceRuntime.requestTimer = configRequestIntervalInTicks();
    }

    @Override
    public void resetTimerSlightly() {
        if (!isResourceGauge()) {
            super.resetTimerSlightly();
            return;
        }
        resourceRuntime.requestTimer = configRequestIntervalInTicks() / 2;
    }

    @Override
    public void checkForRedstoneInput() {
        boolean wasPowered = redstonePowered;
        super.checkForRedstoneInput();
        if (isResourceGauge() && wasPowered != redstonePowered)
            resourceRuntime.requestTimer = 1;
    }

    public void sendResourceEffect(FactoryPanelPosition fromPos, boolean success) {
        if (getWorld() instanceof ServerLevel serverLevel)
            CatnipServices.NETWORK.sendToClientsAround(serverLevel, getPos(), 64,
                new FactoryPanelEffectPacket(fromPos, getPanelPosition(), success));
    }

    @Override
    public void disable() {
        super.disable();
        gaugeTypeId = null;
        fluidlogistics$enhancementsVisible = false;
        resourceRuntime.reset();
    }

    @Override
    public int getLevelInStorage() {
        if (!isResourceGauge())
            return super.getLevelInStorage();
        if (blockEntity.isVirtual())
            return 1;
        if (getWorld().isClientSide())
            return resourceRuntime.lastStored;
        if (getFilter().isEmpty())
            return 0;
        return ENVIRONMENT.relevantSummary(this)
            .getCountOf(getFilter());
    }

    @Override
    public int getPromised() {
        if (!isResourceGauge())
            return super.getPromised();
        if (getWorld().isClientSide())
            return resourceRuntime.lastPromised;
        return super.getPromised();
    }

    @Override
    public int getUnloadedLinks() {
        if (!isResourceGauge())
            return super.getUnloadedLinks();
        if (getWorld().isClientSide())
            return resourceRuntime.lastUnloadedLinks;
        return ENVIRONMENT.unloadedLinks(this);
    }

    @Override
    public boolean setFilter(ItemStack candidate) {
        if (!isResourceGauge()) {
            if (!isNativeGaugeFilter(candidate))
                return false;
            return super.setFilter(candidate);
        }
        FactoryGaugeType type = registeredType().orElse(null);
        if (type == null)
            return false;
        if (candidate.isEmpty())
            return writeValidatedResourceFilter(type, ItemStack.EMPTY);
        if (candidate.getItem() instanceof FilterItem)
            return false;

        ItemStack candidateCopy = candidate.copyWithCount(1);

        PackageResourceType candidateType = PackageResources.findType(candidateCopy)
            .orElse(null);
        if (candidateType != null && candidateType.id()
            .equals(type.resourceTypeId()))
            return writeValidatedResourceFilter(type, candidateCopy);

        Optional<ItemStack> resolved;
        try {
            resolved = type.filterResolver()
                .resolve(getWorld(), candidateCopy);
        } catch (RuntimeException e) {
            FluidLogistics.LOGGER
                .error("factory gauge filter resolver {} threw for candidate {}", type.id(),
                    candidate.getItem(), e);
            return false;
        }
        if (resolved == null || resolved.isEmpty())
            return false;
        ItemStack key = resolved.get();
        if (key == null || key.isEmpty())
            return false;
        return writeValidatedResourceFilter(type, key);
    }

    private boolean writeValidatedResourceFilter(FactoryGaugeType gaugeType, ItemStack key) {
        if (!key.isEmpty()) {
            PackageResourceType type = PackageResources.findType(key)
                .orElse(null);
            if (type == null || !type.id()
                .equals(gaugeType.resourceTypeId()))
                return false;
            key = type.normalizeKey(key.copy());
            if (!PackageResources.findType(key)
                .map(normalized -> normalized.id()
                    .equals(gaugeType.resourceTypeId()))
                .orElse(false))
                return false;
        }
        return super.setFilter(key);
    }

    public static boolean isNativeGaugeFilter(ItemStack candidate) {
        return candidate.isEmpty() || PackageResources.findType(candidate)
            .isEmpty();
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId,
        net.minecraft.world.entity.player.Inventory playerInventory, Player player) {
        if (isResourceGauge())
            return ResourceFactoryGaugeSetFilterMenu.create(containerId, playerInventory, this);
        return super.createMenu(containerId, playerInventory, player);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void displayScreen(Player player) {
        if (!isResourceGauge()) {
            super.displayScreen(player);
            return;
        }
        if (player instanceof LocalPlayer)
            net.createmod.catnip.gui.ScreenOpener
                .open(new com.yision.fluidlogistics.content.logistics.factoryGauge.client.ResourceFactoryGaugeScreen(this));
    }

    @Override
    public MutableComponent getLabel() {
        if (!isResourceGauge())
            return super.getLabel();
        Component resourceName = PackageResources.nameOf(getFilter())
            .orElse(null);
        if (resourceName == null)
            return super.getLabel();

        if (!targetedBy.isEmpty() && getAmount() == 0)
            return CreateLang.translate("gui.factory_panel.no_target_amount_set")
                .style(ChatFormatting.RED)
                .component();

        if (isMissingAddress())
            return CreateLang.translate("gui.factory_panel.address_missing")
                .style(ChatFormatting.RED)
                .component();

        if (waitingForNetwork)
            return CreateLang.translate("factory_panel.some_links_unloaded")
                .component();

        if (getAmount() == 0 || targetedBy.isEmpty())
            return resourceName.copy();

        String label = resourceName.getString();
        if (redstonePowered)
            label += " " + CreateLang.translate("factory_panel.redstone_paused")
                .string();
        else if (!satisfied)
            label += " " + CreateLang.translate("factory_panel.in_progress")
                .string();
        return CreateLang.text(label)
            .component();
    }

    @Override
    public MutableComponent getCountLabelForValueBox() {
        if (!isResourceGauge())
            return super.getCountLabelForValueBox();

        ItemStack filter = getFilter();
        if (filter.isEmpty())
            return Component.empty();

        var levelText = PackageResources.formatAmount(filter, getLevelInStorage(),
            PackageResourceDisplay.Format.COMPACT);
        if (levelText.isEmpty())
            return super.getCountLabelForValueBox();

        if (waitingForNetwork)
            return Component.literal("?");

        int count = getAmount();
        if (count == 0)
            return CreateLang.text("  " + levelText.orElseThrow())
                .color(0xF1EFE8)
                .component();

        String countText = PackageResources.formatAmount(filter, count, PackageResourceDisplay.Format.COMPACT)
            .orElse(Integer.toString(count));
        return CreateLang.text("   " + levelText.orElseThrow())
            .color(satisfied ? 0xD7FFA8 : promisedSatisfied ? 0xffcd75 : 0xFFBFA8)
            .add(CreateLang.text("/")
                .style(ChatFormatting.WHITE))
            .add(CreateLang.text(countText + "  ")
                .color(0xF1EFE8))
            .component();
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        if (!isResourceGauge())
            return super.createBoard(player, hitResult);
        ItemStack filter = getFilter();
        PackageResourceDisplay display = PackageResources.displayOf(filter)
            .orElse(null);
        if (display == null)
            return super.createBoard(player, hitResult);
        return CONTROLLER.createValueSettingsBoard(filter, display, super.createBoard(player, hitResult));
    }

    @Override
    public MutableComponent formatValue(ValueSettingsBehaviour.ValueSettings value) {
        if (!isResourceGauge())
            return super.formatValue(value);
        ItemStack filter = getFilter();
        PackageResourceDisplay display = PackageResources.displayOf(filter)
            .orElse(null);
        if (display == null)
            return super.formatValue(value);
        return CONTROLLER.formatValue(registeredType().map(FactoryGaugeType::resourceTypeId).orElse(null), filter,
            display, getAmount(), upTo, value);
    }

    @Override
    public ValueSettingsBehaviour.ValueSettings getValueSettings() {
        if (!isResourceGauge())
            return super.getValueSettings();
        ItemStack filter = getFilter();
        PackageResourceDisplay display = PackageResources.displayOf(filter)
            .orElse(null);
        if (display == null)
            return super.getValueSettings();
        return CONTROLLER.valueSettings(filter, display, getAmount(), upTo);
    }

    @Override
    public void setValueSettings(Player player, ValueSettingsBehaviour.ValueSettings settings, boolean ctrlDown) {
        if (!isResourceGauge()) {
            super.setValueSettings(player, settings, ctrlDown);
            return;
        }
        ItemStack filter = getFilter();
        PackageResourceDisplay display = PackageResources.displayOf(filter)
            .orElse(null);
        if (display == null) {
            super.setValueSettings(player, settings, ctrlDown);
            return;
        }
        if (getValueSettings().equals(settings))
            return;
        int amount = CONTROLLER.amountForSettings(filter, display, settings);
        super.setValueSettings(player, new ValueSettingsBehaviour.ValueSettings(settings.row(), amount), ctrlDown);
    }

    public int fluidlogistics$getTargetAmountMaximum() {
        if (!isResourceGauge())
            return Integer.MAX_VALUE;
        ItemStack filter = getFilter();
        return CONTROLLER.targetAmountMaximum(
            registeredType().map(FactoryGaugeType::resourceTypeId).orElse(null), filter,
            PackageResources.displayOf(filter).orElse(null));
    }

    public void fluidlogistics$setTargetAmount(int amount) {
        ItemStack filter = getFilter();
        PackageResourceDisplay display = PackageResources.displayOf(filter).orElse(null);
        int maximum = fluidlogistics$getTargetAmountMaximum();
        count = Math.clamp(amount, 0, maximum);
        upTo = display == null || display.factoryPanelRow(filter, count) == 0;
        panelBE().redraw = true;
    }

    @Override
    public ItemRequirement getRequiredItems() {
        if (!isResourceGauge() || !isActive())
            return super.getRequiredItems();
        FactoryGaugeType type = registeredType()
            .orElse(null);
        ItemStack requiredItem = type == null
            ? new ItemStack(com.simibubi.create.AllBlocks.FACTORY_GAUGE.asItem())
            : new ItemStack(type.item()
                .get());
        return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, requiredItem);
    }

    @Override
    public String getFrogAddress() {
        String superAddress = super.getFrogAddress();
        if (superAddress != null || !isResourceGauge())
            return superAddress;

        if (!panelBE().restocker)
            return null;

        PackagerBlockEntity owner = panelBE().getRestockedPackager();
        if (owner == null || com.yision.fluidlogistics.api.packager.ResourcePackagers.ownerOf(owner)
            .isEmpty())
            return null;

        if (owner.getLevel()
            .getBlockEntity(owner.getBlockPos()
                .above()) instanceof com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity fpbe) {
            if (fpbe.addressFilter != null && !fpbe.addressFilter.isBlank())
                return fpbe.addressFilter + "";
        }
        return null;
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        if (!isResourceGauge()) {
            super.write(nbt, registries, clientPacket);
            return;
        }

        super.write(nbt, registries, clientPacket);
        if (!active)
            return;

        CompoundTag panelTag = nbt.getCompound(ResourceFactoryGaugeNbt.slotTagName(slot));
        fluidlogistics$writeResourceSettings(panelTag);
        ResourceFactoryGaugeNbt.writeRuntimeState(panelTag, resourceRuntime);
        nbt.put(ResourceFactoryGaugeNbt.slotTagName(slot), panelTag);
    }

    @Override
    public void writeSafe(CompoundTag nbt, HolderLookup.Provider registries) {
        if (!isResourceGauge()) {
            super.writeSafe(nbt, registries);
            return;
        }

        super.writeSafe(nbt, registries);
        if (!active)
            return;

        CompoundTag panelTag = nbt.getCompound(ResourceFactoryGaugeNbt.slotTagName(slot));
        fluidlogistics$writeResourceSettings(panelTag);
        nbt.put(ResourceFactoryGaugeNbt.slotTagName(slot), panelTag);
    }

    private void fluidlogistics$writeResourceSettings(CompoundTag panelTag) {
        ResourceFactoryGaugeNbt.writeTypeAndSettings(panelTag, gaugeTypeId, fluidlogistics$restockThreshold,
            fluidlogistics$promiseLimit, fluidlogistics$additionalStock, fluidlogistics$enhancementsVisible);
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        CompoundTag rawSlotTag = nbt.getCompound(ResourceFactoryGaugeNbt.slotTagName(slot))
            .copy();

        gaugeTypeId = ResourceFactoryGaugeNbt.determineTypeId(rawSlotTag, registries);
        if (gaugeTypeId != null && FactoryGauges.get(gaugeTypeId)
            .isEmpty())
            gaugeTypeId = null;

        super.read(nbt, registries, clientPacket);

        if (!active) {
            gaugeTypeId = null;
            fluidlogistics$enhancementsVisible = false;
            return;
        }

        fluidlogistics$readResourceState(rawSlotTag);
        fluidlogistics$normalizeLoadedFilter();
    }

    private void fluidlogistics$readResourceState(CompoundTag rawSlotTag) {
        CompoundTag stateTag = rawSlotTag.getCompound(ResourceFactoryGaugeNbt.STATE_KEY);

        int threshold = rawSlotTag.contains(ResourceGaugeHelper.RESTOCK_THRESHOLD_KEY, Tag.TAG_INT)
            ? rawSlotTag.getInt(ResourceGaugeHelper.RESTOCK_THRESHOLD_KEY)
            : stateTag.contains("RestockThreshold", Tag.TAG_INT) ? stateTag.getInt("RestockThreshold")
                : ResourceGaugeHelper.DEFAULT_RESTOCK_THRESHOLD;
        int promiseLimit = rawSlotTag.contains(ResourceGaugeHelper.PROMISE_LIMIT_KEY, Tag.TAG_INT)
            ? rawSlotTag.getInt(ResourceGaugeHelper.PROMISE_LIMIT_KEY)
            : stateTag.contains("PromiseLimit", Tag.TAG_INT) ? stateTag.getInt("PromiseLimit")
                : ResourceGaugeHelper.DEFAULT_PROMISE_LIMIT;
        int additionalStock = rawSlotTag.contains(ResourceGaugeHelper.ADDITIONAL_STOCK_KEY, Tag.TAG_INT)
            ? rawSlotTag.getInt(ResourceGaugeHelper.ADDITIONAL_STOCK_KEY)
            : stateTag.contains("AdditionalStock", Tag.TAG_INT) ? stateTag.getInt("AdditionalStock")
                : ResourceGaugeHelper.DEFAULT_ADDITIONAL_STOCK;

        fluidlogistics$setRestockThreshold(threshold);
        fluidlogistics$setPromiseLimit(promiseLimit);
        fluidlogistics$setAdditionalStock(additionalStock);
        fluidlogistics$enhancementsVisible = stateTag.getBoolean(ResourceFactoryGaugeNbt.ENHANCEMENTS_VISIBLE_KEY);

        int remainingAdditionalStock = stateTag.contains("RemainingAdditionalStock", Tag.TAG_INT)
            ? stateTag.getInt("RemainingAdditionalStock")
            : rawSlotTag.contains(ResourceGaugeHelper.REMAINING_ADDITIONAL_STOCK_KEY, Tag.TAG_INT)
                ? rawSlotTag.getInt(ResourceGaugeHelper.REMAINING_ADDITIONAL_STOCK_KEY)
                : ResourceGaugeHelper.DEFAULT_ADDITIONAL_STOCK;
        resourceRuntime.remainingAdditionalStock = ResourceGaugeHelper.policy(this)
            .clampAdditionalStock(remainingAdditionalStock);
        resourceRuntime.lastStored = stateTag.getInt("LastStored");
        resourceRuntime.lastPromised = stateTag.getInt("LastPromised");
        resourceRuntime.lastUnloadedLinks = stateTag.getInt("LastUnloadedLinks");
        resourceRuntime.requestTimer = stateTag.getInt("RequestTimer");
    }

    private void fluidlogistics$normalizeLoadedFilter() {
        FactoryGaugeType type = registeredType()
            .orElse(null);
        if (type == null)
            return;
        ItemStack loadedFilter = getFilter();
        if (loadedFilter.isEmpty())
            return;
        PackageResourceType resourceType = PackageResources.findType(loadedFilter)
            .orElse(null);
        if (resourceType == null || !resourceType.id()
            .equals(type.resourceTypeId())) {
            this.filter = FilterItemStack.of(ItemStack.EMPTY);
            return;
        }
        ItemStack normalized = resourceType.normalizeKey(loadedFilter.copy());
        if (normalized.isEmpty() || !PackageResources.findType(normalized)
            .map(normalizedType -> normalizedType.id()
                .equals(type.resourceTypeId()))
            .orElse(false)) {
            this.filter = FilterItemStack.of(ItemStack.EMPTY);
            return;
        }
        this.filter = FilterItemStack.of(normalized);
    }

    @Override
    public int fluidlogistics$getRestockThreshold() {
        return fluidlogistics$restockThreshold;
    }

    public boolean fluidlogistics$enhancementsVisible() {
        return fluidlogistics$enhancementsVisible;
    }

    public void fluidlogistics$setEnhancementsVisible(boolean visible) {
        fluidlogistics$enhancementsVisible = visible;
    }

    @Override
    public void fluidlogistics$setRestockThreshold(int threshold) {
        fluidlogistics$restockThreshold = ResourceGaugeHelper.policy(this)
            .clampThreshold(threshold);
    }

    @Override
    public int fluidlogistics$getPromiseLimit() {
        return fluidlogistics$promiseLimit;
    }

    @Override
    public void fluidlogistics$setPromiseLimit(int limit) {
        fluidlogistics$promiseLimit = ResourceGaugeHelper.policy(this)
            .clampPromiseLimit(limit);
    }

    @Override
    public int fluidlogistics$getAdditionalStock() {
        return fluidlogistics$additionalStock;
    }

    @Override
    public void fluidlogistics$setAdditionalStock(int amount) {
        fluidlogistics$additionalStock = ResourceGaugeHelper.policy(this)
            .clampAdditionalStock(amount);
        if (resourceRuntime.remainingAdditionalStock > fluidlogistics$additionalStock)
            resourceRuntime.remainingAdditionalStock = fluidlogistics$additionalStock;
    }

}
