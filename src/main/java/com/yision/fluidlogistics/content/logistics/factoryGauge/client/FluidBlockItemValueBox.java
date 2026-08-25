package com.yision.fluidlogistics.content.logistics.factoryGauge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class FluidBlockItemValueBox extends ValueBox.ItemValueBox {
    private static final float BLOCK_COUNT_Z_OFFSET = 10.0f + 1.0f / 4.0f;

    private final ItemStack layoutStack;

    public FluidBlockItemValueBox(Component label, AABB bounds, BlockPos pos, ItemStack stack,
        MutableComponent count) {
        super(label, bounds, pos, stack, count);
        this.layoutStack = stack;
    }

    @Override
    public void renderContents(PoseStack ms, MultiBufferSource buffer) {
        boolean alreadyGui3d = Minecraft.getInstance()
            .getItemRenderer()
            .getModel(layoutStack, null, null, 0)
            .isGui3d();

        ms.pushPose();
        try {
            if (!alreadyGui3d)
                ms.translate(0, 0, BLOCK_COUNT_Z_OFFSET);
            super.renderContents(ms, buffer);
        } finally {
            ms.popPose();
        }
    }
}

