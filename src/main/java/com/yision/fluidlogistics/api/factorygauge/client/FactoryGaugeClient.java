package com.yision.fluidlogistics.api.factorygauge.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.ApiStatus;

import com.yision.fluidlogistics.api.factorygauge.FactoryGauges;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class FactoryGaugeClient {

    private static final Object LOCK = new Object();
    private static final Map<ResourceLocation, FactoryGaugeModelSet> MODEL_SETS = new LinkedHashMap<>();
    private static volatile boolean frozen;

    private FactoryGaugeClient() {
        throw new AssertionError("This class should not be instantiated");
    }

    public static void registerModels(ResourceLocation typeId, FactoryGaugeModelSet set) {
        synchronized (LOCK) {
            if (frozen)
                throw new IllegalStateException(
                    "factory gauge model registration is frozen; cannot register " + typeId);
            if (MODEL_SETS.putIfAbsent(typeId, set) != null)
                throw new IllegalStateException("duplicate factory gauge model set for " + typeId);
        }
    }

    public static Optional<FactoryGaugeModelSet> modelsFor(ResourceLocation typeId) {
        if (frozen)
            return Optional.ofNullable(MODEL_SETS.get(typeId));
        FactoryGaugeModelSet set;
        synchronized (LOCK) {
            set = MODEL_SETS.get(typeId);
        }
        return Optional.ofNullable(set);
    }

    @ApiStatus.Internal
    public static List<ResourceLocation> customModelLocations() {
        List<ResourceLocation> locations = new ArrayList<>();
        synchronized (LOCK) {
            for (FactoryGaugeModelSet set : MODEL_SETS.values())
                for (var partial : set.all())
                    locations.add(partial.modelLocation());
        }
        return locations;
    }

    @ApiStatus.Internal
    public static void freezeModels() {
        synchronized (LOCK) {
            if (frozen)
                return;
            for (ResourceLocation typeId : FactoryGauges.registeredTypeIds()) {
                if (!MODEL_SETS.containsKey(typeId))
                    throw new IllegalStateException(
                        "registered factory gauge type " + typeId + " has no model set");
            }
            for (ResourceLocation typeId : MODEL_SETS.keySet()) {
                if (FactoryGauges.get(typeId)
                    .isEmpty())
                    throw new IllegalStateException(
                        "model set registered for unknown factory gauge type " + typeId);
            }
            frozen = true;
        }
    }
}
