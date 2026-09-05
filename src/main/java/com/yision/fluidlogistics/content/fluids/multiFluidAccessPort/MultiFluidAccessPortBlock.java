package com.yision.fluidlogistics.content.fluids.multiFluidAccessPort;

import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.yision.fluidlogistics.content.fluids.AbstractFluidPortBlock;
import com.yision.fluidlogistics.registry.AllBlockEntities;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.fluids.FluidStack;

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
