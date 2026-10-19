package com.yision.fluidlogistics.infrastructure.config;

import org.jetbrains.annotations.NotNull;

import com.mojang.serialization.MapCodec;

import net.neoforged.neoforge.common.conditions.ICondition;

public record FluidHatchAdvertisedCondition() implements ICondition {
	public static final MapCodec<FluidHatchAdvertisedCondition> CODEC = MapCodec
		.unit(new FluidHatchAdvertisedCondition());

	@Override
	public boolean test(@NotNull IContext context) {
		return FeatureToggle.isFluidHatchAdvertised();
	}

	@Override
	public @NotNull MapCodec<? extends ICondition> codec() {
		return CODEC;
	}
}
