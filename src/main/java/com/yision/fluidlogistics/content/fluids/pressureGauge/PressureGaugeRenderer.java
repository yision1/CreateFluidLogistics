package com.yision.fluidlogistics.content.fluids.pressureGauge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.yision.fluidlogistics.registry.AllPartialModels;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class PressureGaugeRenderer extends SmartBlockEntityRenderer<PressureGaugeBlockEntity> {
    public PressureGaugeRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(PressureGaugeBlockEntity be, float partialTicks, PoseStack pose,
        MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTicks, pose, buffer, light, overlay);
        BlockState state = be.getBlockState();
        Level level = be.getLevel();
        BlockPos pos = be.getBlockPos();
        float pivot = 5.75f / 16;
        float progress = Mth.lerp(partialTicks, be.prevDialState, be.dialState);

        for (Direction face : Iterate.directions) {
            if (!PressureGaugeBlock.shouldRenderDialOnFace(level, pos, state, face))
                continue;
            SuperByteBuffer dial = orient(CachedBuffers.partial(AllPartialModels.FLOW_METER_DIAL, state), face);
            dial.translate(0, pivot, pivot)
                .rotate(Mth.HALF_PI * (1 - progress), Direction.EAST)
                .translate(0, -pivot, -pivot)
                .light(light).renderInto(pose, buffer.getBuffer(RenderType.solid()));
            orient(CachedBuffers.partial(AllPartialModels.FLOW_METER_HEAD, state), face)
                .light(light).renderInto(pose, buffer.getBuffer(RenderType.solid()));
        }
    }

    private static SuperByteBuffer orient(SuperByteBuffer buffer, Direction face) {
        return buffer.rotateCentered((-face.toYRot() - 90) * Mth.DEG_TO_RAD, Direction.UP);
    }
}
