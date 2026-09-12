package com.yision.fluidlogistics.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorVisual;
import com.yision.fluidlogistics.content.logistics.fluidPackage.client.phantomChain.PhantomChainVisibility;
import com.yision.fluidlogistics.content.logistics.fluidPackage.FluidPackageItem;
import com.yision.fluidlogistics.content.logistics.fluidPackage.client.FluidPackageItemRenderer;
import com.yision.fluidlogistics.render.FluidVisual;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(ChainConveyorVisual.class)
public class ChainConveyorVisualMixin {

    @Unique
    private FluidVisual fluidlogistics$fluidVisual;

    @Inject(
            method = "<init>",
            at = @At("RETURN")
    )
    private void fluidlogistics$ctor(VisualizationContext context, ChainConveyorBlockEntity blockEntity, float partialTick, CallbackInfo ci) {
        fluidlogistics$fluidVisual = new FluidVisual(context);
    }

    @Inject(
            method = "beginFrame",
            at = @At("HEAD")
    )
    private void fluidlogistics$begin(DynamicVisual.Context ctx, CallbackInfo ci) {
        if (fluidlogistics$fluidVisual == null) {
            return;
        }
        fluidlogistics$fluidVisual.begin();
    }

    @Inject(
            method = "_delete",
            at = @At("RETURN")
    )
    private void fluidlogistics$delete(CallbackInfo ci) {
        if (fluidlogistics$fluidVisual == null) {
            return;
        }
        fluidlogistics$fluidVisual.delete();
    }

    @Inject(
            method = "beginFrame",
            at = @At("RETURN")
    )
    private void fluidlogistics$end(DynamicVisual.Context ctx, CallbackInfo ci) {
        if (fluidlogistics$fluidVisual == null) {
            return;
        }
        fluidlogistics$fluidVisual.end();
    }

    @WrapOperation(
            method = "beginFrame",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/chainConveyor/ChainConveyorVisual;setupBoxVisual(Lcom/simibubi/create/content/kinetics/chainConveyor/ChainConveyorBlockEntity;Lcom/simibubi/create/content/kinetics/chainConveyor/ChainConveyorPackage;F)V"
            ),
            slice = @Slice(
                    from = @At(
                            value = "FIELD",
                            target = "Lcom/simibubi/create/content/kinetics/chainConveyor/ChainConveyorBlockEntity;travellingPackages:Ljava/util/Map;"
                    )
            )
    )
    private void fluidlogistics$skipPhantomTravellingBoxVisual(ChainConveyorVisual instance,
                                                               ChainConveyorBlockEntity be, ChainConveyorPackage box,
                                                               float partialTicks, Operation<Void> original,
                                                               @Local Map.Entry<BlockPos, List<ChainConveyorPackage>> entry) {
        if (!PhantomChainVisibility.shouldRenderConnection(be, entry.getKey())) {
            return;
        }
        original.call(instance, be, box, partialTicks);
    }

    @WrapOperation(
            method = "setupBoxVisual",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/engine_room/flywheel/lib/instance/TransformedInstance;setChanged()V"
            )
    )
    private void fluidlogistics$setupFluidVisual(TransformedInstance instance, Operation<Void> original,
                                                  @Local(name = "boxBuffer") TransformedInstance boxBuffer,
                                                  @Local(argsOnly = true) ChainConveyorPackage box,
                                                  @Local(name = "light") int light) {
        original.call(instance);
        if (instance != boxBuffer || fluidlogistics$fluidVisual == null
            || !(box.item.getItem() instanceof FluidPackageItem)) {
            return;
        }

        fluidlogistics$fluidVisual.update(FluidPackageItemRenderer.getFluidDisplayData(box.item, -1),
            boxBuffer.pose, light);
    }
}
