package com.yision.fluidlogistics.mixin.logistics;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.yision.fluidlogistics.content.processing.snowGolemFreezer.SnowGolemFreezerBlockEntity;

import net.createmod.catnip.data.Iterate;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(StockTickerBlockEntity.class)
public abstract class StockTickerBlockEntityMixin {

	@Inject(method = "isKeeperPresent", at = @At("RETURN"), cancellable = true, remap = false)
	private void fluidlogistics$recognizeSnowGolemKeeper(CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValue())
			return;

		StockTickerBlockEntity ticker = (StockTickerBlockEntity) (Object) this;
		Level level = ticker.getLevel();
		if (level == null)
			return;

		for (Direction direction : Iterate.horizontalDirections) {
			BlockEntity keeper = level.getBlockEntity(ticker.getBlockPos().relative(direction));
			if (keeper instanceof SnowGolemFreezerBlockEntity) {
				cir.setReturnValue(true);
				return;
			}
		}
	}
}
