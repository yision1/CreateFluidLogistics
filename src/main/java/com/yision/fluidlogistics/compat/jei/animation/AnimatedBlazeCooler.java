package com.yision.fluidlogistics.compat.jei.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.yision.fluidlogistics.registry.AllBlocks;
import com.yision.fluidlogistics.registry.AllPartialModels;
import com.yision.fluidlogistics.registry.AllSpriteShifts;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;

public class AnimatedBlazeCooler extends AnimatedKinetics {

    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        draw(graphics, xOffset, yOffset, false);
    }

    public void draw(GuiGraphics graphics, int xOffset, int yOffset, boolean supercooled) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(xOffset, yOffset, 200);
        poseStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
        poseStack.mulPose(Axis.YP.rotationDegrees(22.5f));

        int scale = 23;
        float rodOffset = (Mth.sin(AnimationTickHolder.getRenderTime() / 16f) + .5f) / 16f;

        blockElement(AllBlocks.BLAZE_COOLER.getDefaultState()).atLocal(0, 1.65, 0)
            .scale(scale)
            .render(graphics);
        blockElement(supercooled ? AllPartialModels.BLAZE_COOLER_SUPER_ACTIVE : AllPartialModels.BLAZE_COOLER_ACTIVE)
            .atLocal(1, 1.8, 1)
            .rotate(0, 180, 0)
            .scale(scale)
            .render(graphics);
        blockElement(supercooled ? AllPartialModels.BLAZE_COOLER_SUPER_RODS_2 : AllPartialModels.BLAZE_COOLER_RODS_2)
            .atLocal(1, 1.7 + rodOffset, 1)
            .rotate(0, 180, 0)
            .scale(scale)
            .render(graphics);

        poseStack.scale(scale, -scale, scale);
        poseStack.translate(0, -1.8, 0);
        renderColdFlame(graphics, poseStack, supercooled);
        poseStack.popPose();
    }

    private static void renderColdFlame(GuiGraphics graphics, PoseStack poseStack, boolean supercooled) {
        SpriteShiftEntry spriteShift = supercooled
            ? AllSpriteShifts.BLAZE_COOLER_SUPER_FLAME
            : AllSpriteShifts.BLAZE_COOLER_FLAME;
        float spriteWidth = spriteShift.getTarget().getU1() - spriteShift.getTarget().getU0();
        float spriteHeight = spriteShift.getTarget().getV1() - spriteShift.getTarget().getV0();
        float time = AnimationTickHolder.getRenderTime(Minecraft.getInstance().level);
        float speed = 1 / 32f + 1 / 64f * 4;

        double vScroll = speed * time;
        vScroll = (vScroll - Math.floor(vScroll)) * spriteHeight / 2;
        double uScroll = speed * time / 2;
        uScroll = (uScroll - Math.floor(uScroll)) * spriteWidth / 2;

        CachedBuffers.partial(AllPartialModels.BLAZE_COOLER_FLAME, Blocks.AIR.defaultBlockState())
            .shiftUVScrolling(spriteShift, (float) uScroll, (float) vScroll)
            .light(LightTexture.FULL_BRIGHT)
            .renderInto(poseStack, graphics.bufferSource().getBuffer(RenderType.cutoutMipped()));
    }
}
