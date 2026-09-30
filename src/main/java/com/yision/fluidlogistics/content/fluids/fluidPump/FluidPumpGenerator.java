package com.yision.fluidlogistics.content.fluids.fluidPump;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;

public class FluidPumpGenerator {

	public void generate(DataGenContext<Block, ?> ctx, RegistrateBlockstateProvider prov) {
		prov.getVariantBuilder(ctx.getEntry())
			.forAllStatesExcept(state -> ConfiguredModel.builder()
				.modelFile(getModel(prov, FluidPumpBlock.isVerticalModel(state)))
				.rotationX(FluidPumpBlock.getModelXRotation(state))
				.rotationY(FluidPumpBlock.getModelYRotation(state))
				.build(), net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED);
	}

	private ModelFile getModel(RegistrateBlockstateProvider prov, boolean vertical) {
		return prov.models()
			.getExistingFile(prov.modLoc("block/fluid_pump/" + (vertical ? "block_vertical" : "block_horizontal")));
	}
}
