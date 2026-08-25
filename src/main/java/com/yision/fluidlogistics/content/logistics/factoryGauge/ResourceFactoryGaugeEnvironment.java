package com.yision.fluidlogistics.content.logistics.factoryGauge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSupportBehaviour;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour.RequestType;
import com.simibubi.create.content.logistics.packagerLink.LogisticsManager;
import com.simibubi.create.content.logistics.packagerLink.RequestPromise;
import com.simibubi.create.content.logistics.packagerLink.RequestPromiseQueue;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.yision.fluidlogistics.api.packager.PackageResourceType;
import com.yision.fluidlogistics.api.packager.PackageResources;
import com.yision.fluidlogistics.api.packager.ResourcePackager;
import com.yision.fluidlogistics.api.packager.ResourcePackagers;
import com.yision.fluidlogistics.content.logistics.packageResource.ResourcePackagerEngine;
import com.yision.fluidlogistics.content.logistics.packageResource.ResourcePackagerInventoryIdentifier;
import com.yision.fluidlogistics.content.logistics.factoryGauge.ResourceGaugeDecision.ResourceRequestPlan;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraftforge.items.ItemStackHandler;

@ApiStatus.Internal
public final class ResourceFactoryGaugeEnvironment {

    public static final ResourceFactoryGaugeEnvironment INSTANCE = new ResourceFactoryGaugeEnvironment();

    private static final ItemStackHandler EMPTY_RESOURCE_HANDLER = new ItemStackHandler(0);
    private static final Map<UUID, ResourceLinkAvailability> RESOURCE_LINK_AVAILABILITY = new WeakHashMap<>();

    private ResourceFactoryGaugeEnvironment() {
    }

    public InventorySummary relevantSummary(ResourceFactoryPanelBehaviour behaviour) {
        if (!behaviour.panelBE().restocker)
            return LogisticsManager.getSummaryOfNetwork(behaviour.network, false);
        PackagerBlockEntity owner = behaviour.panelBE()
            .getRestockedPackager();
        if (owner == null)
            return InventorySummary.EMPTY;
        ResourcePackager packager = ResourcePackagers.ownerOf(owner)
            .orElse(null);
        if (packager == null)
            return owner.getAvailableItems();
        ItemStack normalizedKey = normalizedKeyOf(behaviour);
        if (normalizedKey.isEmpty() || !ResourcePackagers.supports(packager, resourceTypeOf(behaviour), normalizedKey))
            return owner.getAvailableItems();
        return ResourcePackagerEngine.getLastKnownResources(packager);
    }

    public int unloadedLinks(ResourceFactoryPanelBehaviour behaviour) {
        if (!behaviour.panelBE().restocker) {
            int unavailable = com.simibubi.create.Create.LOGISTICS
                .getUnloadedLinkCount(behaviour.network);
            return unavailable + unavailableResourceLinks(behaviour);
        }

        PackagerBlockEntity owner = behaviour.panelBE()
            .getRestockedPackager();
        if (owner == null)
            return 1;
        ResourcePackager packager = ResourcePackagers.ownerOf(owner)
            .orElse(null);
        return packager != null && ResourcePackagers.storageIdentity(packager) == null ? 1 : 0;
    }

    private int unavailableResourceLinks(ResourceFactoryPanelBehaviour behaviour) {
        long gameTime = behaviour.getWorld()
            .getGameTime();
        ResourceLinkAvailability cached = RESOURCE_LINK_AVAILABILITY.get(behaviour.network);
        if (cached != null && cached.gameTime() == gameTime)
            return cached.unavailableLinks();

        int unavailable = 0;
        for (LogisticallyLinkedBehaviour link : LogisticallyLinkedBehaviour.getAllPresent(behaviour.network,
            false)) {
            ResourcePackager packager = ResourcePackagers.fromLink(link)
                .orElse(null);
            if (packager != null && ResourcePackagers.storageIdentity(packager) == null)
                unavailable++;
        }

        RESOURCE_LINK_AVAILABILITY.put(behaviour.network,
            new ResourceLinkAvailability(gameTime, unavailable));
        return unavailable;
    }

    public ResourceGaugeSnapshot captureMonitor(ResourceFactoryPanelBehaviour behaviour,
        ResourceFactoryGaugeRuntime runtime) {
        int unloaded = behaviour.getUnloadedLinks();
        if (!behaviour.getWorld().isClientSide && !behaviour.panelBE().restocker
            && unloaded == 0 && runtime.lastUnloadedLinks != 0)
            LogisticsManager.SUMMARIES.invalidate(behaviour.network);
        int stored = behaviour.getLevelInStorage();
        int promised = behaviour.getPromised();
        return ResourceGaugeSnapshot.monitor(stored, promised, unloaded,
            behaviour.panelBE().restocker, behaviour.satisfied);
    }

    public ResourceGaugeSnapshot captureRestock(ResourceFactoryPanelBehaviour behaviour) {
        Objects.requireNonNull(behaviour, "behaviour");
        ItemStack normalizedKey = normalizedKeyOf(behaviour);
        int stored = behaviour.getLevelInStorage();
        int promised = behaviour.getPromised();

        PackagerBlockEntity owner = behaviour.panelBE()
            .getRestockedPackager();
        ResourcePackager packager = owner == null ? null
            : ResourcePackagers.ownerOf(owner)
                .orElse(null);
        boolean storageAvailable = packager != null && !normalizedKey.isEmpty()
            && ResourcePackagers.supports(packager, resourceTypeOf(behaviour), normalizedKey.copy());
        Object storageIdentity = storageAvailable ? ResourcePackagers.storageIdentity(packager) : null;
        int availableOnNetwork = 0;
        if (storageIdentity != null) {
            IdentifiedInventory identifiedInventory = new IdentifiedInventory(
                new ResourcePackagerInventoryIdentifier(storageIdentity), EMPTY_RESOURCE_HANDLER);
            availableOnNetwork = LogisticsManager.getStockOf(behaviour.network, normalizedKey, identifiedInventory);
        }

        return new ResourceGaugeSnapshot(stored, promised, behaviour.getUnloadedLinks(),
            true, availableOnNetwork, storageIdentity != null, behaviour.satisfied,
            behaviour.network, storageIdentity, behaviour.recipeAddress);
    }

    public void executeRecipeRequest(ResourceFactoryPanelBehaviour behaviour) {
        boolean failed = false;
        Map<UUID, Map<ItemStack, FactoryPanelBehaviour.ItemStackConnections>> consolidated = new HashMap<>();

        for (FactoryPanelConnection connection : behaviour.targetedBy.values()) {
            FactoryPanelBehaviour source = FactoryPanelBehaviour.at(behaviour.getWorld(), connection);
            if (source == null)
                return;

            ItemStack item = source.getFilter();
            Map<ItemStack, FactoryPanelBehaviour.ItemStackConnections> networkItemCounts = consolidated
                .computeIfAbsent(source.network,
                    $ -> new Object2ObjectOpenCustomHashMap<>(ItemStackLinkedSet.TYPE_AND_TAG));
            networkItemCounts.computeIfAbsent(item, $ -> new FactoryPanelBehaviour.ItemStackConnections(item));
            FactoryPanelBehaviour.ItemStackConnections existingConnections = networkItemCounts.get(item);
            existingConnections.add(connection);
            existingConnections.totalAmount += connection.amount;
        }

        Multimap<UUID, BigItemStack> toRequest = HashMultimap.create();
        for (Entry<UUID, Map<ItemStack, FactoryPanelBehaviour.ItemStackConnections>> entry : consolidated.entrySet()) {
            UUID network = entry.getKey();
            InventorySummary summary = LogisticsManager.getSummaryOfNetwork(network, true);

            for (FactoryPanelBehaviour.ItemStackConnections connections : entry.getValue().values()) {
                if (connections.totalAmount == 0 || connections.item.isEmpty()
                    || summary.getCountOf(connections.item) < connections.totalAmount) {
                    for (FactoryPanelConnection connection : connections)
                        behaviour.sendResourceEffect(connection.from, false);
                    failed = true;
                    continue;
                }

                BigItemStack stack = new BigItemStack(connections.item, connections.totalAmount);
                toRequest.put(network, stack);
                for (FactoryPanelConnection connection : connections)
                    behaviour.sendResourceEffect(connection.from, true);
            }
        }

        if (failed)
            return;

        Map<UUID, Collection<BigItemStack>> asMap = toRequest.asMap();
        PackageOrderWithCrafts craftingContext = PackageOrderWithCrafts.empty();
        List<Multimap<PackagerBlockEntity, PackagingRequest>> requests = new ArrayList<>();

        if (!behaviour.activeCraftingArrangement.isEmpty())
            craftingContext = PackageOrderWithCrafts.singleRecipe(behaviour.activeCraftingArrangement.stream()
                .map(stack -> new BigItemStack(stack.copyWithCount(1)))
                .toList());

        for (Entry<UUID, Collection<BigItemStack>> entry : asMap.entrySet()) {
            PackageOrderWithCrafts order = new PackageOrderWithCrafts(
                new PackageOrder(new ArrayList<>(entry.getValue())), craftingContext.orderedCrafts());
            Multimap<PackagerBlockEntity, PackagingRequest> request = LogisticsManager
                .findPackagersForRequest(entry.getKey(), order, null, behaviour.recipeAddress);
            requests.add(request);
        }

        for (Multimap<PackagerBlockEntity, PackagingRequest> entry : requests)
            for (PackagerBlockEntity packager : entry.keySet())
                if (packager.isTooBusyFor(RequestType.RESTOCK))
                    return;

        for (Multimap<PackagerBlockEntity, PackagingRequest> entry : requests)
            LogisticsManager.performPackageRequests(entry);

        RequestPromiseQueue promises = Create.LOGISTICS.getQueuedPromises(behaviour.network);
        if (promises != null)
            promises.add(new RequestPromise(new BigItemStack(behaviour.getFilter(), behaviour.recipeOutput)));

        behaviour.panelBE().advancements.awardPlayer(AllAdvancements.FACTORY_GAUGE);
    }

    public void apply(ResourceFactoryPanelBehaviour behaviour, ResourceGaugeDecision decision,
        ResourceGaugeSnapshot snapshot) {
        ResourceFactoryGaugeRuntime runtime = behaviour.resourceRuntime;

        if (behaviour.satisfied && !decision.satisfied() && runtime.requestTimer > 1)
            runtime.requestTimer = 1;

        boolean changed = runtime.lastStored != snapshot.stored()
            || runtime.lastPromised != snapshot.promised()
            || runtime.lastUnloadedLinks != snapshot.unloadedLinks()
            || behaviour.satisfied != decision.satisfied()
            || behaviour.promisedSatisfied != decision.promisedSatisfied()
            || behaviour.waitingForNetwork != decision.waitingForNetwork()
            || runtime.remainingAdditionalStock != decision.nextRemainingAdditionalStock();
        if (!changed)
            return;

        if (decision.playConfirmSound()) {
            AllSoundEvents.CONFIRM.playOnServer(behaviour.getWorld(), behaviour.getPos(), 0.075f, 1f);
            AllSoundEvents.CONFIRM_2.playOnServer(behaviour.getWorld(), behaviour.getPos(), 0.125f, 0.575f);
        }

        boolean notifyOutputs = decision.notifyRedstoneOutputs();
        runtime.lastStored = snapshot.stored();
        runtime.lastPromised = snapshot.promised();
        runtime.lastUnloadedLinks = snapshot.unloadedLinks();
        behaviour.satisfied = decision.satisfied();
        behaviour.promisedSatisfied = decision.promisedSatisfied();
        behaviour.waitingForNetwork = decision.waitingForNetwork();
        runtime.remainingAdditionalStock = decision.nextRemainingAdditionalStock();

        if (!behaviour.getWorld().isClientSide)
            behaviour.blockEntity.sendData();
        if (notifyOutputs)
            notifyRedstoneOutputs(behaviour);
    }

    public void executeRequest(ResourceFactoryPanelBehaviour behaviour, ResourceRequestPlan plan) {
        if (plan.network() == null || plan.storageIdentity() == null)
            return;
        IdentifiedInventory identifiedInventory = new IdentifiedInventory(
            new ResourcePackagerInventoryIdentifier(plan.storageIdentity()), EMPTY_RESOURCE_HANDLER);
        BigItemStack orderedItem = new BigItemStack(plan.resourceKey(), plan.amount());
        PackageOrderWithCrafts order = PackageOrderWithCrafts.simple(List.of(orderedItem));

        behaviour.sendResourceEffect(behaviour.getPanelPosition(), true);

        if (!LogisticsManager.broadcastPackageRequest(plan.network(),
            com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour.RequestType.RESTOCK,
            order, identifiedInventory, plan.address()))
            return;

        behaviour.restockerPromises.add(new RequestPromise(orderedItem));
    }

    public void notifyRedstoneOutputs(ResourceFactoryPanelBehaviour behaviour) {
        for (FactoryPanelConnection connection : behaviour.targetedByLinks.values()) {
            if (!behaviour.getWorld()
                .isLoaded(connection.from.pos()))
                return;
            FactoryPanelSupportBehaviour linkAt = FactoryPanelBehaviour
                .linkAt(behaviour.getWorld(), connection);
            if (linkAt == null || linkAt.isOutput())
                return;
            linkAt.notifyLink();
        }
    }

    @Nullable
    private PackageResourceType resourceTypeOf(ResourceFactoryPanelBehaviour behaviour) {
        return PackageResources.findType(behaviour.getFilter())
            .orElse(null);
    }

    private ItemStack normalizedKeyOf(ResourceFactoryPanelBehaviour behaviour) {
        ItemStack filter = behaviour.getFilter();
        if (filter.isEmpty())
            return ItemStack.EMPTY;
        PackageResourceType type = resourceTypeOf(behaviour);
        return type == null ? ItemStack.EMPTY : type.normalizeKey(filter.copy());
    }

    private record ResourceLinkAvailability(long gameTime, int unavailableLinks) {
    }
}

