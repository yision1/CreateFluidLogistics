package com.yision.fluidlogistics.mixin.logistics;

import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.yision.fluidlogistics.content.processing.blazeCooler.BlazeCoolerBlockEntity;

import net.createmod.catnip.data.Iterate;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StockTickerBlockEntity.class)
public abstract class StockTickerBlockEntityMixin {

    @Inject(method = "isKeeperPresent", at = @At("RETURN"), cancellable = true, remap = false)
    private void fluidlogistics$recognizeBlazeCooler(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue())
            return;

        StockTickerBlockEntity ticker = (StockTickerBlockEntity) (Object) this;
        Level level = ticker.getLevel();
        if (level == null)
            return;

        for (Direction direction : Iterate.horizontalDirections) {
            if (level.getBlockEntity(ticker.getBlockPos().relative(direction)) instanceof BlazeCoolerBlockEntity) {
                cir.setReturnValue(true);
                return;
            }
        }
    }
}
