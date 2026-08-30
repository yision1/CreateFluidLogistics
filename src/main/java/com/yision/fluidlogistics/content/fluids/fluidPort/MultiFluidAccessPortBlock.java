package com.yision.fluidlogistics.content.fluids.fluidPort;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.yision.fluidlogistics.content.fluids.fluidPort.AbstractFluidPortBlock;
import com.yision.fluidlogistics.registry.AllBlockEntities;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.fluids.FluidStack;

public class MultiFluidAccessPortBlock extends AbstractFluidPortBlock<MultiFluidAccessPortBlockEntity> {

    public MultiFluidAccessPortBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    protected Predicate<FluidStack> getTransferFilter(MultiFluidAccessPortBlockEntity blockEntity, Direction side) {
        FilteringBehaviour filter = blockEntity.getFilter(side);
        return filter == null ? null : filter::test;
    }

    @Override
    public Class<MultiFluidAccessPortBlockEntity> getBlockEntityClass() {
        return MultiFluidAccessPortBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends MultiFluidAccessPortBlockEntity> getBlockEntityType() {
        return AllBlockEntities.MULTI_FLUID_ACCESS_PORT.get();
    }
}
