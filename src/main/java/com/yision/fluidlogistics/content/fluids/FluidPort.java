package com.yision.fluidlogistics.content.fluids;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraftforge.fluids.capability.IFluidHandler;

public interface FluidPort {

    @Nullable
    IFluidHandler getFluidDisplayCapability(@Nullable Direction side);

    boolean blocksFluidPackagerPlacement(Direction side);
}
