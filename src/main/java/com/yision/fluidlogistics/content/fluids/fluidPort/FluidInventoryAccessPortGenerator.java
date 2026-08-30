package com.yision.fluidlogistics.content.fluids.fluidPort;

import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.generators.ModelFile;

public class FluidInventoryAccessPortGenerator extends SpecialBlockStateGen {

    @Override
    protected int getXRotation(BlockState state) {
        return 0;
    }

    @Override
    protected int getYRotation(BlockState state) {
        return horizontalAngle(state.getValue(FluidInventoryAccessPortBlock.FACING)) + 180;
    }

    @Override
    public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov,
        BlockState state) {
        boolean attached = state.getValue(FluidInventoryAccessPortBlock.ATTACHED);
        ResourceLocation path = prov.modLoc(
            "block/fluid_inventory_access_port/block_"
                + state.getValue(FluidInventoryAccessPortBlock.TARGET).getSerializedName());
        return prov.models().withExistingParent(path + (attached ? "_on" : "_off"), path)
            .texture("side", prov.modLoc("block/fluid_inventory_access_port/fluid_inventory_access_port_side"
                + (attached ? "_on" : "")));
    }
}
