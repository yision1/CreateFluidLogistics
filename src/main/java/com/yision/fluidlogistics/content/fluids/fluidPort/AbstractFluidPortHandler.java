package com.yision.fluidlogistics.content.fluids.fluidPort;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.yision.fluidlogistics.content.fluids.multiFluidTank.SharedCapacityFluidHandler;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public abstract class AbstractFluidPortHandler
    implements IFluidHandler, FluidPortHandler, SharedCapacityFluidHandler {

    private final ThreadLocal<Boolean> recursionGuard = ThreadLocal.withInitial(() -> false);

    @Nullable
    protected abstract IFluidHandler getSourceHandler();

    protected final <T> T preventRecursion(Supplier<T> value, T fallback) {
        if (recursionGuard.get()) {
            return fallback;
        }
        recursionGuard.set(true);
        try {
            return value.get();
        } finally {
            recursionGuard.set(false);
        }
    }

    @Override
    public int getTanks() {
        return preventRecursion(() -> {
            IFluidHandler handler = getSourceHandler();
            return handler == null ? 0 : handler.getTanks();
        }, 0);
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return preventRecursion(() -> {
            IFluidHandler handler = getSourceHandler();
            if (handler == null || tank < 0 || tank >= handler.getTanks()) {
                return FluidStack.EMPTY;
            }
            return handler.getFluidInTank(tank).copy();
        }, FluidStack.EMPTY);
    }

    @Override
    public int getTankCapacity(int tank) {
        return preventRecursion(() -> {
            IFluidHandler handler = getSourceHandler();
            return handler == null || tank < 0 || tank >= handler.getTanks()
                ? 0
                : handler.getTankCapacity(tank);
        }, 0);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return preventRecursion(() -> {
            IFluidHandler handler = getSourceHandler();
            return handler != null && tank >= 0 && tank < handler.getTanks()
                && handler.isFluidValid(tank, stack);
        }, false);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return preventRecursion(() -> {
            IFluidHandler handler = getSourceHandler();
            return handler == null ? 0 : handler.fill(resource, action);
        }, 0);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return preventRecursion(() -> {
            IFluidHandler handler = getSourceHandler();
            return handler == null ? FluidStack.EMPTY : handler.drain(resource, action);
        }, FluidStack.EMPTY);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return preventRecursion(() -> {
            IFluidHandler handler = getSourceHandler();
            return handler == null ? FluidStack.EMPTY : handler.drain(maxDrain, action);
        }, FluidStack.EMPTY);
    }

    @Override
    public boolean canFillAll(List<FluidStack> fluids) {
        return preventRecursion(() -> {
            IFluidHandler handler = getSourceHandler();
            return handler instanceof SharedCapacityFluidHandler sharedCapacityFluidHandler
                && sharedCapacityFluidHandler.canFillAll(fluids);
        }, false);
    }
}
