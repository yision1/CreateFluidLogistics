package com.yision.fluidlogistics.content.kinetics.oscillatingSteamEngine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.yision.fluidlogistics.content.kinetics.oscillatingSteamEngine.OscillatingSteamEngineRenderState.Part;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class OscillatingSteamEngineRenderer extends SafeBlockEntityRenderer<OscillatingSteamEngineBlockEntity> {
	public OscillatingSteamEngineRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	protected void renderSafe(OscillatingSteamEngineBlockEntity be, float partialTick, PoseStack pose,
		MultiBufferSource buffers, int light, int overlay) {
		if (VisualizationManager.supportsVisualization(be.getLevel()))
			return;
		OscillatingSteamEngineRenderState state = OscillatingSteamEngineRenderState.of(be);
		VertexConsumer buffer = buffers.getBuffer(RenderType.solid());
		for (Part part : Part.values()) {
			if (!state.isVisible(part))
				continue;
			pose.pushPose();
			state.transform(pose, part);
			CachedBuffers.partial(part.model(), be.getBlockState()).light(light).renderInto(pose, buffer);
			pose.popPose();
		}
	}

	@Override
	public int getViewDistance() {
		return 128;
	}
}
