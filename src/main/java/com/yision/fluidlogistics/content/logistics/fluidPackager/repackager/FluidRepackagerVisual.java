package com.yision.fluidlogistics.content.logistics.fluidPackager.repackager;

import com.yision.fluidlogistics.content.logistics.fluidPackager.FluidPackagerVisual;
import com.yision.fluidlogistics.registry.AllPartialModels;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;

public class FluidRepackagerVisual extends FluidPackagerVisual<FluidRepackagerBlockEntity> {

	public FluidRepackagerVisual(VisualizationContext ctx, FluidRepackagerBlockEntity blockEntity, float partialTick) {
		super(ctx, blockEntity, partialTick, AllPartialModels.FLUID_REPACKAGER_TRAY, false);
	}
}
