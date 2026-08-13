package com.yision.fluidlogistics.content.logistics.packageResource.client;

import java.util.Optional;

import org.jetbrains.annotations.ApiStatus;

import com.yision.fluidlogistics.api.packager.PackageResourceDisplay;
import com.yision.fluidlogistics.api.packager.PackageResources;
import com.yision.fluidlogistics.content.logistics.fluidPackage.FluidPackageItem;
import com.yision.fluidlogistics.registry.AllItems;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@ApiStatus.Internal
public final class TableClothResourceDisplay {
    private TableClothResourceDisplay() {
        throw new AssertionError("This class should not be instantiated");
    }

    public static Optional<ItemStack> createPackage(ItemStack carrierOrKey, int amount) {
        if (!PackageResources.isBootstrapped() || carrierOrKey == null || carrierOrKey.isEmpty() || amount <= 0) {
            return Optional.empty();
        }

        return PackageResources.normalizeKey(carrierOrKey)
                .flatMap(key -> PackageResources.findType(key)
                        .map(type -> PackageResources.createPackage(
                                key, Math.min(amount, type.maxPerPackage(key)))))
                .map(TableClothResourceDisplay::stabilizePackageStyle);
    }

    public static Optional<String> formatAmount(
            ItemStack carrierOrKey, int amount, PackageResourceDisplay.Format format) {
        if (!PackageResources.isBootstrapped() || carrierOrKey == null || carrierOrKey.isEmpty() || amount <= 0) {
            return Optional.empty();
        }
        return PackageResources.formatAmount(carrierOrKey, amount, format);
    }

    private static ItemStack stabilizePackageStyle(ItemStack packageStack) {
        if (!FluidPackageItem.isFluidPackage(packageStack)) {
            return packageStack;
        }
        ItemStack stablePackage = new ItemStack(AllItems.FLUID_PACKAGE.get());
        stablePackage.setCount(packageStack.getCount());
        if (packageStack.hasTag()) {
            stablePackage.setTag(packageStack.getTag().copy());
        }
        return stablePackage;
    }
}
