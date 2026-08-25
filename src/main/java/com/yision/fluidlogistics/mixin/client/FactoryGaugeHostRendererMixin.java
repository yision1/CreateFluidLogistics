package com.yision.fluidlogistics.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelRenderer;
import com.yision.fluidlogistics.api.factorygauge.client.FactoryGaugeClient;
import com.yision.fluidlogistics.api.factorygauge.client.FactoryGaugeModelSet;
import com.yision.fluidlogistics.content.logistics.factoryGauge.ResourceFactoryPanelBehaviour;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.world.level.block.state.BlockState;

@OnlyIn(Dist.CLIENT)
@Mixin(FactoryPanelRenderer.class)
public abstract class FactoryGaugeHostRendererMixin {

    @WrapOperation(
        method = "renderBulb",
        at = @At(
            value = "INVOKE",
            target = "Lnet/createmod/catnip/render/CachedBuffers;partial("
                + "Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"
                + "Lnet/minecraft/world/level/block/state/BlockState;)"
                + "Lnet/createmod/catnip/render/SuperByteBuffer;",
            remap = false
        ),
        remap = false
    )
    private static SuperByteBuffer fluidlogistics$useTypedBulbModel(PartialModel partial, BlockState state,
        Operation<SuperByteBuffer> original, @Local(argsOnly = true) FactoryPanelBehaviour behaviour) {
        if (behaviour instanceof ResourceFactoryPanelBehaviour resource && resource.isResourceGauge()) {
            FactoryGaugeModelSet models = resource.registeredType()
                .flatMap(type -> FactoryGaugeClient.modelsFor(type.id()))
                .orElse(null);
            if (models != null)
                partial = resource.redstonePowered || resource.isMissingAddress()
                    ? models.bulbRed()
                    : models.bulbLight();
        }
        return original.call(partial, state);
    }
}

