package com.yision.fluidlogistics.api.packager;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface PackageResourceGoggleInformation {
    boolean addToGoggleTooltip(
            List<Component> tooltip,
            boolean isPlayerSneaking,
            ItemStack packageStack,
            List<PackageResource> resources);
}
