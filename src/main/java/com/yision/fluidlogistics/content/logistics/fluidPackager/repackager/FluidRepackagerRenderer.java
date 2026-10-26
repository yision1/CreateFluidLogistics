package com.yision.fluidlogistics.content.logistics.fluidPackager.repackager;

import com.simibubi.create.content.logistics.packager.PackagerRenderer;
import com.yision.fluidlogistics.content.logistics.fluidPackager.FluidPackagerRenderer;
import com.yision.fluidlogistics.registry.AllPartialModels;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;

public class FluidRepackagerRenderer extends FluidPackagerRenderer<FluidRepackagerBlockEntity> {

	public FluidRepackagerRenderer(Context context) {
		super(context, AllPartialModels.FLUID_REPACKAGER_TRAY, false);
	}

	public static PartialModel getTrayModel() {
		return AllPartialModels.FLUID_REPACKAGER_TRAY;
	}

	public static PartialModel getHatchModel(FluidRepackagerBlockEntity be) {
		return getSharedHatchModel(be);
	}

	public static boolean isHatchOpen(FluidRepackagerBlockEntity be) {
		return PackagerRenderer.isHatchOpen(be);
	}
}
