package com.yision.fluidlogistics.api.factorygauge;

import java.util.Optional;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@FunctionalInterface
public interface FactoryGaugeFilterResolver {

    Optional<ItemStack> resolve(Level level, ItemStack candidateCopy);
}
