package com.yision.fluidlogistics.content.fluids.fluidPort;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public interface FluidPort {

    @Nullable
    IFluidHandler getFluidDisplayCapability(@Nullable Direction side);

    boolean blocksFluidPackagerPlacement(Direction side);
}
