package com.yision.fluidlogistics.content.fluids.powderSnow;

import com.yision.fluidlogistics.registry.AllFluidLogisticsFluids;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.wrappers.FluidBucketWrapper;

public class PowderSnowBucketFluidHandler extends FluidBucketWrapper {

    public PowderSnowBucketFluidHandler(ItemStack container) {
        super(container);
    }

    @Override
    public FluidStack getFluid() {
        return container.is(Items.POWDER_SNOW_BUCKET)
            ? new FluidStack(AllFluidLogisticsFluids.POWDER_SNOW.get().getSource(), FluidType.BUCKET_VOLUME)
            : FluidStack.EMPTY;
    }
}
