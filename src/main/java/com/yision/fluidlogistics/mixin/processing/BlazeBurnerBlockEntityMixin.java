package com.yision.fluidlogistics.mixin.processing;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.yision.fluidlogistics.content.processing.blazeCooler.BlazeCoolerBlockEntity;
import com.yision.fluidlogistics.content.processing.blazeCooler.BlazeCoolerConversion;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlazeBurnerBlockEntity.class)
public abstract class BlazeBurnerBlockEntityMixin {

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void fluidlogistics$tickCoolerConversion(CallbackInfo ci) {
        BlazeBurnerBlockEntity burner = (BlazeBurnerBlockEntity) (Object) this;
        if (burner instanceof BlazeCoolerBlockEntity)
            return;
        BlazeCoolerConversion.tickConversion(burner, BlazeCoolerConversion.shouldCool(burner));
    }
}
