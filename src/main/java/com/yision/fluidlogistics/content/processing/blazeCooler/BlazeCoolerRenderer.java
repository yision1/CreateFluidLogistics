package com.yision.fluidlogistics.content.processing.blazeCooler;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.yision.fluidlogistics.registry.AllPartialModels;
import com.yision.fluidlogistics.registry.AllSpriteShifts;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BlazeCoolerRenderer extends SafeBlockEntityRenderer<BlazeCoolerBlockEntity> {

    public BlazeCoolerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(BlazeCoolerBlockEntity cooler, float partialTicks, PoseStack poseStack,
            MultiBufferSource bufferSource, int light, int overlay) {
        HeatLevel heatLevel = cooler.getHeatLevelForRender();
        if (heatLevel == HeatLevel.NONE)
            return;

        BlockState blockState = cooler.getBlockState();
        float animation = cooler.headAnimation.getValue(partialTicks) * .175f;
        float horizontalAngle = AngleHelper.rad(cooler.getHeadAngle(partialTicks));
        boolean canDrawFlame = heatLevel.isAtLeast(HeatLevel.FADING);
        PartialModel drawHat = getHat(cooler);

        renderShared(poseStack, bufferSource, cooler.getLevel(), blockState, heatLevel, animation, horizontalAngle,
            canDrawFlame, cooler.goggles, drawHat, cooler.hashCode());
    }

    public static void renderInContraption(MovementContext context, ContraptionMatrices matrices,
            MultiBufferSource bufferSource, LerpedFloat headAngle, boolean conductor) {
        if (BlazeBurnerBlock.getHeatLevelOf(context.state) == HeatLevel.NONE)
            return;

        int coolingLevelIndex = Mth.clamp(context.blockEntityData.getInt(BlazeCoolerBlockEntity.COOLING_LEVEL_TAG),
            0, HeatLevel.values().length - 1);
        HeatLevel coolingLevel = HeatLevel.byIndex(coolingLevelIndex);
        if (!coolingLevel.isAtLeast(HeatLevel.FADING))
            coolingLevel = HeatLevel.FADING;

        float horizontalAngle = AngleHelper.rad(
            headAngle.getValue(AnimationTickHolder.getPartialTicks(context.world)));
        boolean drawGoggles = context.blockEntityData.contains("Goggles");
        boolean drawHat = conductor || context.blockEntityData.contains("TrainHat");

        renderShared(matrices.getViewProjection(), matrices.getModel(), bufferSource, context.world, context.state,
            coolingLevel, 0, horizontalAngle, false, drawGoggles,
            drawHat ? com.simibubi.create.AllPartialModels.TRAIN_HAT : null, context.hashCode());
    }

    public static void renderShared(PoseStack poseStack, MultiBufferSource bufferSource, Level level,
            BlockState blockState, HeatLevel heatLevel, float animation, float horizontalAngle,
            boolean canDrawFlame, boolean drawGoggles, @Nullable PartialModel drawHat, int hashCode) {
        renderShared(poseStack, null, bufferSource, level, blockState, heatLevel, animation, horizontalAngle,
            canDrawFlame, drawGoggles, drawHat, hashCode);
    }

    private static void renderShared(PoseStack poseStack, @Nullable PoseStack modelTransform,
            MultiBufferSource bufferSource, Level level, BlockState blockState, HeatLevel heatLevel, float animation,
            float horizontalAngle, boolean canDrawFlame, boolean drawGoggles, @Nullable PartialModel drawHat,
            int hashCode) {
        boolean blockAbove = animation > .125f;
        float time = AnimationTickHolder.getRenderTime(level);
        float renderTick = time + hashCode % 13 * 16f;
        float offsetMult = heatLevel.isAtLeast(HeatLevel.FADING) ? 64 : 16;
        float offset = Mth.sin((float) ((renderTick / 16f) % (2 * Math.PI))) / offsetMult;
        float headY = offset - animation * .75f;

        poseStack.pushPose();
        PartialModel blazeModel = getBlazeModel(heatLevel, blockAbove);
        SuperByteBuffer blazeBuffer = CachedBuffers.partial(blazeModel, blockState);
        transform(blazeBuffer, modelTransform);
        blazeBuffer.translate(0, headY, 0);
        draw(blazeBuffer, horizontalAngle, poseStack, bufferSource.getBuffer(RenderType.solid()));

        if (drawGoggles) {
            PartialModel goggles = blazeModel == AllPartialModels.BLAZE_COOLER_INERT
                ? com.simibubi.create.AllPartialModels.BLAZE_GOGGLES_SMALL
                : com.simibubi.create.AllPartialModels.BLAZE_GOGGLES;
            SuperByteBuffer gogglesBuffer = CachedBuffers.partial(goggles, blockState);
            transform(gogglesBuffer, modelTransform);
            gogglesBuffer.translate(0, headY + 8 / 16f, 0);
            draw(gogglesBuffer, horizontalAngle, poseStack, bufferSource.getBuffer(RenderType.solid()));
        }

        if (drawHat != null) {
            SuperByteBuffer hatBuffer = CachedBuffers.partial(drawHat, blockState);
            transform(hatBuffer, modelTransform);
            hatBuffer.translate(0, headY, 0);
            if (blazeModel == AllPartialModels.BLAZE_COOLER_INERT) {
                hatBuffer.translateY(.5f).center().scale(.75f).uncenter();
            } else {
                hatBuffer.translateY(.75f);
            }
            hatBuffer.rotateCentered(horizontalAngle + Mth.PI, Direction.UP)
                .translate(.5f, 0, .5f)
                .light(LightTexture.FULL_BRIGHT)
                .renderInto(poseStack, bufferSource.getBuffer(RenderType.cutoutMipped()));
        }

        if (heatLevel.isAtLeast(HeatLevel.FADING)) {
            float offset1 = Mth.sin((float) ((renderTick / 16f + Math.PI) % (2 * Math.PI))) / offsetMult;
            float offset2 = Mth.sin((float) ((renderTick / 16f + Math.PI / 2) % (2 * Math.PI))) / offsetMult;
            PartialModel rods = heatLevel == HeatLevel.SEETHING
                ? AllPartialModels.BLAZE_COOLER_SUPER_RODS
                : AllPartialModels.BLAZE_COOLER_RODS;
            PartialModel rods2 = heatLevel == HeatLevel.SEETHING
                ? AllPartialModels.BLAZE_COOLER_SUPER_RODS_2
                : AllPartialModels.BLAZE_COOLER_RODS_2;
            SuperByteBuffer rodsBuffer = CachedBuffers.partial(rods, blockState);
            transform(rodsBuffer, modelTransform);
            rodsBuffer.translate(0, offset1 + animation + .125f, 0)
                .light(LightTexture.FULL_BRIGHT)
                .renderInto(poseStack, bufferSource.getBuffer(RenderType.solid()));
            SuperByteBuffer rodsBuffer2 = CachedBuffers.partial(rods2, blockState);
            transform(rodsBuffer2, modelTransform);
            rodsBuffer2.translate(0, offset2 + animation - 3 / 16f, 0)
                .light(LightTexture.FULL_BRIGHT)
                .renderInto(poseStack, bufferSource.getBuffer(RenderType.solid()));
        }

        if (canDrawFlame && blockAbove) {
            renderColdFlame(blockState, horizontalAngle, time, heatLevel, poseStack, modelTransform, bufferSource);
        }
        poseStack.popPose();
    }

    private static PartialModel getBlazeModel(HeatLevel heatLevel, boolean blockAbove) {
        if (heatLevel == HeatLevel.SEETHING)
            return blockAbove
                ? AllPartialModels.BLAZE_COOLER_SUPER_ACTIVE
                : AllPartialModels.BLAZE_COOLER_SUPER;
        if (heatLevel.isAtLeast(HeatLevel.FADING))
            return blockAbove && heatLevel.isAtLeast(HeatLevel.KINDLED)
                ? AllPartialModels.BLAZE_COOLER_ACTIVE
                : AllPartialModels.BLAZE_COOLER_IDLE;
        return AllPartialModels.BLAZE_COOLER_INERT;
    }

    @Nullable
    private static PartialModel getHat(BlazeCoolerBlockEntity cooler) {
        if (cooler.stockKeeper)
            return com.simibubi.create.AllPartialModels.LOGISTICS_HAT;
        return cooler.hat ? com.simibubi.create.AllPartialModels.TRAIN_HAT : null;
    }

    private static void renderColdFlame(BlockState blockState, float horizontalAngle, float time,
            HeatLevel heatLevel, PoseStack poseStack, @Nullable PoseStack modelTransform,
            MultiBufferSource bufferSource) {
        SpriteShiftEntry spriteShift = heatLevel == HeatLevel.SEETHING
            ? AllSpriteShifts.BLAZE_COOLER_SUPER_FLAME
            : AllSpriteShifts.BLAZE_COOLER_FLAME;
        float spriteWidth = spriteShift.getTarget().getU1() - spriteShift.getTarget().getU0();
        float spriteHeight = spriteShift.getTarget().getV1() - spriteShift.getTarget().getV0();
        float speed = 1 / 32f + 1 / 64f * heatLevel.ordinal();
        double vScroll = speed * time;
        vScroll = (vScroll - Math.floor(vScroll)) * spriteHeight / 2;
        double uScroll = speed * time / 2;
        uScroll = (uScroll - Math.floor(uScroll)) * spriteWidth / 2;

        SuperByteBuffer flameBuffer = CachedBuffers.partial(AllPartialModels.BLAZE_COOLER_FLAME, blockState);
        transform(flameBuffer, modelTransform);
        flameBuffer.shiftUVScrolling(spriteShift, (float) uScroll, (float) vScroll);
        draw(flameBuffer, horizontalAngle, poseStack, bufferSource.getBuffer(RenderType.cutoutMipped()));
    }

    private static void transform(SuperByteBuffer buffer, @Nullable PoseStack modelTransform) {
        if (modelTransform != null)
            buffer.transform(modelTransform);
    }

    private static void draw(SuperByteBuffer buffer, float horizontalAngle, PoseStack poseStack, VertexConsumer consumer) {
        buffer.rotateCentered(horizontalAngle, Direction.UP)
            .light(LightTexture.FULL_BRIGHT)
            .renderInto(poseStack, consumer);
    }
}
