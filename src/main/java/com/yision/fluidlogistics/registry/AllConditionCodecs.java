package com.yision.fluidlogistics.registry;

import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;
import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.infrastructure.config.FeatureEnabledCondition;
import com.yision.fluidlogistics.infrastructure.config.FluidHatchAdvertisedCondition;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class AllConditionCodecs {
	public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS = DeferredRegister
		.create(NeoForgeRegistries.Keys.CONDITION_CODECS, FluidLogistics.MODID);

	public static final Supplier<MapCodec<FeatureEnabledCondition>> FEATURE_ENABLED = CONDITION_CODECS
		.register("feature_enabled", () -> FeatureEnabledCondition.CODEC);

	public static final Supplier<MapCodec<FluidHatchAdvertisedCondition>> FLUID_HATCH_ADVERTISED = CONDITION_CODECS
		.register("fluid_hatch_advertised", () -> FluidHatchAdvertisedCondition.CODEC);

	private AllConditionCodecs() {
	}

	public static void register(IEventBus modEventBus) {
		CONDITION_CODECS.register(modEventBus);
	}
}
