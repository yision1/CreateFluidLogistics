package com.yision.fluidlogistics.content.logistics.factoryGauge.builtin;

import java.util.Optional;

import org.jetbrains.annotations.ApiStatus;

import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.yision.fluidlogistics.api.factorygauge.FactoryGaugeFilterResolver;
import com.yision.fluidlogistics.api.packager.PackageResources;
import com.yision.fluidlogistics.api.packager.PackageResourceTypes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;

@ApiStatus.Internal
public final class FluidContainerProbe implements FactoryGaugeFilterResolver {

    public static final FluidContainerProbe INSTANCE = new FluidContainerProbe();

    private FluidContainerProbe() {
    }

    @Override
    public Optional<ItemStack> resolve(Level level, ItemStack candidateCopy) {
        if (candidateCopy == null || candidateCopy.isEmpty()
            || candidateCopy.getItem() instanceof FilterItem)
            return Optional.empty();

        if (PackageResources.findType(candidateCopy)
            .map(type -> type.id()
                .equals(PackageResourceTypes.FLUID))
            .orElse(false))
            return Optional.of(candidateCopy.copyWithCount(1));

        ItemStack probeStack = candidateCopy.copyWithCount(1);
        if (!GenericItemEmptying.canItemBeEmptied(level, probeStack))
            return Optional.empty();

        FluidStack fluid = GenericItemEmptying.emptyItem(level, probeStack, true)
            .getFirst();
        if (fluid.isEmpty())
            return Optional.empty();

        ItemStack key = PackageResourceTypes.createFluidKey(FluidHelper.copyStackWithAmount(fluid, 1));
        return PackageResources.findType(key)
            .filter(type -> type.id()
                .equals(PackageResourceTypes.FLUID))
            .map(ignored -> key);
    }
}
