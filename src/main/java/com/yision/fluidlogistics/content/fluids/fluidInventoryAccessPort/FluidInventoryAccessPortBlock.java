package com.yision.fluidlogistics.content.fluids.fluidInventoryAccessPort;

import java.util.function.Predicate;

import com.yision.fluidlogistics.content.fluids.AbstractFluidPortBlock;
import com.yision.fluidlogistics.registry.AllBlockEntities;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.fluids.FluidStack;

public class FluidInventoryAccessPortBlock extends AbstractFluidPortBlock<FluidInventoryAccessPortBlockEntity> {

    private static final Predicate<FluidStack> ACCEPT_ALL = fluid -> true;

    public FluidInventoryAccessPortBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected Predicate<FluidStack> getTransferFilter(FluidInventoryAccessPortBlockEntity blockEntity,
            Direction side) {
        return ACCEPT_ALL;
    }

    @Override
    public Class<FluidInventoryAccessPortBlockEntity> getBlockEntityClass() {
        return FluidInventoryAccessPortBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FluidInventoryAccessPortBlockEntity> getBlockEntityType() {
        return AllBlockEntities.FLUID_INVENTORY_ACCESS_PORT.get();
    }
}
