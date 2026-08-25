package com.yision.fluidlogistics.mixin.client;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelState;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelType;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelModel;
import com.yision.fluidlogistics.api.factorygauge.client.FactoryGaugeClient;
import com.yision.fluidlogistics.api.factorygauge.client.FactoryGaugeModelSet;
import com.yision.fluidlogistics.content.logistics.factoryGauge.client.ResourceFactoryGaugeModel;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;

@OnlyIn(Dist.CLIENT)
@Mixin(FactoryPanelModel.class)
public abstract class FactoryGaugeHostModelMixin {

    @Inject(
        method = "gatherModelData",
        at = @At("RETURN"),
        remap = false
    )
    private void fluidlogistics$collectGaugeTypes(ModelData.Builder builder, BlockAndTintGetter world,
        BlockPos pos, BlockState state, ModelData blockEntityData, CallbackInfoReturnable<ModelData.Builder> cir) {
        Map<PanelSlot, ResourceLocation> types = ResourceFactoryGaugeModel.collectGaugeTypes(world, pos);
        if (types.isEmpty())
            return;
        builder.with(ResourceFactoryGaugeModel.GAUGE_TYPE_PROPERTY, types);
    }

    @WrapOperation(
        method = "addPanel",
        at = @At(
            value = "INVOKE",
            target = "Ldev/engine_room/flywheel/lib/model/baked/PartialModel;get()"
                + "Lnet/minecraft/client/resources/model/BakedModel;",
            remap = false
        ),
        remap = false
    )
    private BakedModel fluidlogistics$useTypedPanelModel(PartialModel partial, Operation<BakedModel> original,
        @Local(argsOnly = true) PanelSlot slot, @Local(argsOnly = true) PanelType type,
        @Local(argsOnly = true) PanelState panelState, @Local(argsOnly = true) ModelData data) {
        Map<PanelSlot, ResourceLocation> types = data.get(ResourceFactoryGaugeModel.GAUGE_TYPE_PROPERTY);
        if (types != null) {
            ResourceLocation typeId = types.get(slot);
            if (typeId != null) {
                FactoryGaugeModelSet models = FactoryGaugeClient.modelsFor(typeId)
                    .orElse(null);
                if (models != null)
                    partial = ResourceFactoryGaugeModel.panelFor(models, type, panelState);
            }
        }
        return original.call(partial);
    }
}
