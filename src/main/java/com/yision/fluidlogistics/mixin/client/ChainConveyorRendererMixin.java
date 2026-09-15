package com.yision.fluidlogistics.mixin.client;

import java.util.List;
import java.util.Map;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRenderer;
import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.content.logistics.fluidPackage.client.phantomChain.PhantomChainVisibility;
import com.yision.fluidlogistics.content.logistics.fluidPackage.FluidPackageItem;
import com.yision.fluidlogistics.content.logistics.fluidPackage.client.FluidPackageItemRenderer;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(value = ChainConveyorRenderer.class, remap = false)
public class ChainConveyorRendererMixin {

    @Unique
    private static final ThreadLocal<ResourceLocation> fluidlogistics$phantomChainTextureOverride = new ThreadLocal<>();

    @Unique
    private static final ResourceLocation fluidlogistics$PHANTOM_CHAIN_LOCATION =
        FluidLogistics.asResource("textures/block/phantom_chain.png");

    @ModifyExpressionValue(
            method = "renderChain",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/simibubi/create/content/kinetics/chainConveyor/ChainConveyorRenderer;CHAIN_LOCATION:Lnet/minecraft/resources/ResourceLocation;",
                    remap = false
            ),
            remap = false
    )
    private static ResourceLocation fluidlogistics$overrideChainTexture(ResourceLocation original) {
        ResourceLocation override = fluidlogistics$phantomChainTextureOverride.get();
        return override != null ? override : original;
    }

    @WrapOperation(
            method = "renderChains",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/chainConveyor/ChainConveyorRenderer;renderChain(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;FFIIZ)V",
                    remap = false
            ),
            remap = false
    )
    private void fluidlogistics$renderPhantomAwareChain(PoseStack ms, MultiBufferSource buffer,
                                                        float animation, float length, int light1, int light2, boolean far,
                                                        Operation<Void> original,
                                                        @Local(argsOnly = true) ChainConveyorBlockEntity be,
                                                        @Local(ordinal = 0) BlockPos blockPos) {
        if (!PhantomChainVisibility.shouldRenderConnection(be, blockPos)) {
            return;
        }

        boolean phantomRevealed = PhantomChainVisibility.isRevealing()
            && ((com.yision.fluidlogistics.util.PhantomChainConveyorAccess) be)
                .fluidlogistics$isPhantomConnection(blockPos);
        if (phantomRevealed) {
            fluidlogistics$phantomChainTextureOverride.set(fluidlogistics$PHANTOM_CHAIN_LOCATION);
        }
        try {
            original.call(ms, buffer, animation, length, light1, light2, far);
        } finally {
            if (phantomRevealed) {
                fluidlogistics$phantomChainTextureOverride.remove();
            }
        }
    }

    @WrapOperation(
            method = "renderSafe",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/chainConveyor/ChainConveyorRenderer;renderBox(Lcom/simibubi/create/content/kinetics/chainConveyor/ChainConveyorBlockEntity;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/core/BlockPos;Lcom/simibubi/create/content/kinetics/chainConveyor/ChainConveyorPackage;F)V",
                    remap = false
            ),
            slice = @Slice(
                    from = @At(
                            value = "FIELD",
                            target = "Lcom/simibubi/create/content/kinetics/chainConveyor/ChainConveyorBlockEntity;travellingPackages:Ljava/util/Map;",
                            remap = false
                    )
            ),
            remap = false
    )
    private void fluidlogistics$hidePhantomTravellingBoxes(ChainConveyorRenderer instance, ChainConveyorBlockEntity be,
                                                           PoseStack ms, MultiBufferSource buffer, int overlay,
                                                           BlockPos pos, ChainConveyorPackage box, float partialTicks,
                                                           Operation<Void> original,
                                                           @Local Map.Entry<BlockPos, List<ChainConveyorPackage>> entry) {
        if (!PhantomChainVisibility.shouldRenderConnection(be, entry.getKey())) {
            return;
        }
        original.call(instance, be, ms, buffer, overlay, pos, box, partialTicks);
    }

    @WrapOperation(
            method = "renderBox",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/createmod/catnip/render/SuperByteBuffer;renderInto(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V",
                    remap = false
            ),
            remap = false
    )
    private void fluidlogistics$renderFluid(SuperByteBuffer instance, PoseStack ms, VertexConsumer consumer,
                                            Operation<Void> original,
                                            @Local(name = "boxBuffer") SuperByteBuffer boxBuffer,
                                            @Local(argsOnly = true) ChainConveyorPackage box,
                                            @Local(argsOnly = true) MultiBufferSource buffer,
                                            @Local(name = "light") int light) {
        if (instance != boxBuffer || !(box.item.getItem() instanceof FluidPackageItem)) {
            original.call(instance, ms, consumer);
            return;
        }

        Matrix4f pose = new Matrix4f(boxBuffer.getTransforms().last().pose());
        Matrix3f normal = new Matrix3f(boxBuffer.getTransforms().last().normal());
        original.call(instance, ms, consumer);

        ms.pushPose();
        ms.last().pose().mul(pose);
        ms.last().normal().mul(normal);
        FluidPackageItemRenderer.renderFluidContentsLocal(box.item, ms, buffer, light);
        ms.popPose();
    }
}
