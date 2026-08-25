package com.yision.fluidlogistics.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox.ItemValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import com.yision.fluidlogistics.content.logistics.factoryGauge.client.FactoryGaugeWorldFluidRenderer;
import com.yision.fluidlogistics.content.logistics.factoryGauge.client.FluidBlockItemValueBox;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@Mixin(FilteringRenderer.class)
public abstract class FactoryGaugeFilteringRendererMixin {

    @WrapOperation(
        method = "renderOnBlockEntity",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/foundation/blockEntity/behaviour/ValueBoxRenderer;"
                + "renderItemIntoValueBox(Lnet/minecraft/world/item/ItemStack;"
                + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                + "Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            ordinal = 1,
            remap = false
        ),
        remap = false
    )
    private static void fluidlogistics$renderFluidGaugeAsBlock(ItemStack filter, PoseStack ms,
        MultiBufferSource buffer, int light, int overlay, Operation<Void> original,
        @Local FilteringBehaviour behaviour) {
        if (FactoryGaugeWorldFluidRenderer.tryRender(behaviour, filter, ms, buffer, light))
            return;

        original.call(filter, ms, buffer, light, overlay);
    }

    @WrapOperation(
        method = "tick",
        at = @At(
            value = "NEW",
            target = "Lcom/simibubi/create/foundation/blockEntity/behaviour/ValueBox$ItemValueBox;",
            remap = false
        ),
        remap = false
    )
    private static ItemValueBox fluidlogistics$useFluidBlockCountLayout(Component label, AABB bounds,
        BlockPos pos, ItemStack stack, MutableComponent count, Operation<ItemValueBox> original,
        @Local FilteringBehaviour behaviour) {
        if (!FactoryGaugeWorldFluidRenderer.usesFluidBlockRendering(behaviour))
            return original.call(label, bounds, pos, stack, count);

        return new FluidBlockItemValueBox(label, bounds, pos, stack, count);
    }
}

