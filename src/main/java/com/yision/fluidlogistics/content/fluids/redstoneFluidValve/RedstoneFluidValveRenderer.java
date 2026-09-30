package com.yision.fluidlogistics.content.fluids.redstoneFluidValve;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.yision.fluidlogistics.registry.AllPartialModels;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class RedstoneFluidValveRenderer extends SmartBlockEntityRenderer<RedstoneFluidValveBlockEntity> {

    public RedstoneFluidValveRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(RedstoneFluidValveBlockEntity blockEntity, float partialTicks, PoseStack poseStack,
                              MultiBufferSource buffer, int light, int overlay) {
        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(RedstoneFluidValveBlock.FACING);
        Axis pipeAxis = RedstoneFluidValveBlock.getPipeAxis(state);
        Axis shaftAxis = RedstoneFluidValveBlock.getCoverAxis(state, pipeAxis);
        int pointerRotationOffset = pipeAxis.isHorizontal() && shaftAxis == Axis.X || pipeAxis.isVertical() ? 90 : 0;
        float pointerRotation = Mth.lerp(blockEntity.pointer.getValue(partialTicks), 0, -90);
        SuperByteBuffer pointer = CachedBuffers.partial(AllPartialModels.REDSTONE_FLUID_VALVE_POINTER, state);
        pointer.center()
                .rotateYDegrees(AngleHelper.horizontalAngle(facing))
                .rotateXDegrees(facing == Direction.UP ? 0 : facing == Direction.DOWN ? 180 : 90)
                .rotateYDegrees(pointerRotationOffset + pointerRotation)
                .uncenter()
                .light(light)
                .overlay(overlay)
                .renderInto(poseStack, buffer.getBuffer(RenderType.cutoutMipped()));
    }

}
