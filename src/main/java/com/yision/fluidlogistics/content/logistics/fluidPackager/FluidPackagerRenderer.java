package com.yision.fluidlogistics.content.logistics.fluidPackager;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.packager.PackagerBlock;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagerRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.yision.fluidlogistics.registry.AllPartialModels;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class FluidPackagerRenderer<T extends PackagerBlockEntity> extends SmartBlockEntityRenderer<T> {

    private final PartialModel trayModel;
    private final boolean hideTrayAfterHalfway;

    public FluidPackagerRenderer(Context context) {
        this(context, AllPartialModels.FLUID_PACKAGER_TRAY, true);
    }

    protected FluidPackagerRenderer(Context context, PartialModel trayModel, boolean hideTrayAfterHalfway) {
        super(context);
        this.trayModel = trayModel;
        this.hideTrayAfterHalfway = hideTrayAfterHalfway;
    }

    @Override
    protected void renderSafe(T be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

        ItemStack renderedBox = be.getRenderedBox();
        float trayOffset = be.getTrayOffset(partialTicks);
        BlockState blockState = be.getBlockState();
        Direction facing = blockState.getValue(PackagerBlock.FACING)
                .getOpposite();

        if (!VisualizationManager.supportsVisualization(be.getLevel())) {
            var hatchModel = getSharedHatchModel(be);

            SuperByteBuffer sbb = CachedBuffers.partial(hatchModel, blockState);
            sbb.translate(Vec3.atLowerCornerOf(facing.getNormal())
                            .scale(.49999f))
                    .rotateYCenteredDegrees(AngleHelper.horizontalAngle(facing))
                    .rotateXCenteredDegrees(AngleHelper.verticalAngle(facing))
                    .light(light)
                    .renderInto(ms, buffer.getBuffer(RenderType.solid()));

            if (!hideTrayAfterHalfway || trayOffset <= 0.5f) {
                sbb = CachedBuffers.partial(trayModel, blockState);
                sbb.translate(Vec3.atLowerCornerOf(facing.getNormal())
                                .scale(trayOffset))
                        .rotateYCenteredDegrees(facing.toYRot())
                        .light(light)
                        .renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
            }
        }

        if (!renderedBox.isEmpty() && (!hideTrayAfterHalfway || trayOffset <= 0.5f)) {
            ms.pushPose();
            var msr = TransformStack.of(ms);
            msr.translate(Vec3.atLowerCornerOf(facing.getNormal())
                            .scale(trayOffset))
                    .translate(.5f, .5f, .5f)
                    .rotateYDegrees(facing.toYRot())
                    .translate(0, 2 / 16f, 0)
                    .scale(1.49f, 1.49f, 1.49f);
            Minecraft.getInstance()
                    .getItemRenderer()
                    .renderStatic(null, renderedBox, ItemDisplayContext.FIXED, false, ms, buffer, be.getLevel(), light,
                            overlay, 0);
            ms.popPose();
        }
    }

    public static PartialModel getTrayModel() {
        return AllPartialModels.FLUID_PACKAGER_TRAY;
    }

    public static PartialModel getHatchModel(FluidPackagerBlockEntity be) {
        return getSharedHatchModel(be);
    }

    public static boolean isHatchOpen(FluidPackagerBlockEntity be) {
        return PackagerRenderer.isHatchOpen(be);
    }

    protected static PartialModel getSharedHatchModel(PackagerBlockEntity be) {
        return PackagerRenderer.isHatchOpen(be)
                ? AllPartialModels.FLUID_PACKAGER_HATCH_OPEN
                : AllPartialModels.FLUID_PACKAGER_HATCH_CLOSED;
    }
}
