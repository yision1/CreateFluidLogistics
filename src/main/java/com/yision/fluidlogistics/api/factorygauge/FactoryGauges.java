package com.yision.fluidlogistics.api.factorygauge;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.ApiStatus;

import com.simibubi.create.Create;
import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.api.packager.PackageResources;
import com.yision.fluidlogistics.content.logistics.factoryGauge.RegisteredFactoryGaugeItem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class FactoryGauges {

    private enum Lifecycle {
        OPEN, BOOTSTRAPPING, FROZEN
    }

    private static final Object LOCK = new Object();
    private static final Map<ResourceLocation, FactoryGaugeType> BY_ID = new LinkedHashMap<>();
    private static final Map<Item, FactoryGaugeType> BY_ITEM = new IdentityHashMap<>();
    private static volatile Lifecycle lifecycle = Lifecycle.OPEN;

    private FactoryGauges() {
        throw new AssertionError("This class should not be instantiated");
    }

    public static Item createItem(ResourceLocation typeId, Item.Properties properties) {
        if (typeId == null)
            throw new IllegalArgumentException("factory gauge type id must not be null");
        if (properties == null)
            throw new IllegalArgumentException("factory gauge item properties must not be null");
        synchronized (LOCK) {
            if (lifecycle != Lifecycle.OPEN)
                throw new IllegalStateException(
                    "FactoryGauges is " + lifecycle + "; cannot create item for " + typeId);
            return new RegisteredFactoryGaugeItem(typeId, properties);
        }
    }

    public static void register(FactoryGaugeType type) {
        synchronized (LOCK) {
            if (lifecycle != Lifecycle.OPEN)
                throw new IllegalStateException(
                    "FactoryGauges is " + lifecycle + "; cannot register factory gauge type " + type.id());
            if (BY_ID.putIfAbsent(type.id(), type) != null)
                throw new IllegalStateException("duplicate factory gauge type id " + type.id());
        }
        FluidLogistics.LOGGER.debug("registered factory gauge type {}", type.id());
    }

    public static Optional<FactoryGaugeType> get(ResourceLocation typeId) {
        if (lifecycle == Lifecycle.FROZEN)
            return Optional.ofNullable(BY_ID.get(typeId));
        FactoryGaugeType type;
        synchronized (LOCK) {
            type = BY_ID.get(typeId);
        }
        return Optional.ofNullable(type);
    }

    public static Optional<FactoryGaugeType> findByItem(Item item) {
        if (lifecycle != Lifecycle.FROZEN)
            return Optional.empty();
        return Optional.ofNullable(BY_ITEM.get(item));
    }

    public static boolean isFrozen() {
        return lifecycle == Lifecycle.FROZEN;
    }

    @ApiStatus.Internal
    public static Set<ResourceLocation> registeredTypeIds() {
        synchronized (LOCK) {
            return Set.copyOf(BY_ID.keySet());
        }
    }

    @ApiStatus.Internal
    public static void bootstrap() {
        synchronized (LOCK) {
            if (lifecycle == Lifecycle.FROZEN)
                return;
            lifecycle = Lifecycle.BOOTSTRAPPING;
            try {
                Map<Item, FactoryGaugeType> validatedItems = new IdentityHashMap<>();
                for (FactoryGaugeType type : BY_ID.values())
                    validate(type, validatedItems);
                BY_ITEM.putAll(validatedItems);
                lifecycle = Lifecycle.FROZEN;
                Create.LOGGER.info("factory gauge registry frozen with {} type(s)", BY_ID.size());
            } catch (Throwable t) {
                BY_ITEM.clear();
                lifecycle = Lifecycle.OPEN;
                throw t;
            }
        }
    }

    private static void validate(FactoryGaugeType type, Map<Item, FactoryGaugeType> validatedItems) {
        Item item = type.item()
            .get();
        if (item == null || item == Items.AIR)
            throw new IllegalStateException("factory gauge type " + type.id() + " has no registered item");
        if (!(item instanceof RegisteredFactoryGaugeItem registered)
            || !registered.gaugeTypeId()
                .equals(type.id()))
            throw new IllegalStateException("item of factory gauge type " + type.id()
                + " was not created by FactoryGauges.createItem() with the same type id: "
                + BuiltInRegistries.ITEM.getKey(item));
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(item);
        if (itemKey == null || !itemKey.getNamespace()
            .equals(type.id()
                .getNamespace()))
            throw new IllegalStateException("item " + itemKey + " of factory gauge type " + type.id()
                + " must live in the type id namespace");
        if (validatedItems.putIfAbsent(item, type) != null)
            throw new IllegalStateException(
                "item " + itemKey + " is already used by another factory gauge type");
        if (PackageResources.get(type.resourceTypeId())
            .isEmpty())
            throw new IllegalStateException("factory gauge type " + type.id() + " references unknown package "
                + "resource type " + type.resourceTypeId());
    }
}
