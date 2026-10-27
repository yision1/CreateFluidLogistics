package com.yision.fluidlogistics.mixin.fluids;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.BoilerData;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.yision.fluidlogistics.content.kinetics.oscillatingSteamEngine.OscillatingSteamEngineBlock;

import net.minecraft.world.level.block.state.BlockState;

@Mixin(value = BoilerData.class, remap = false)
public class BoilerDataMixin {
	@WrapOperation(method = "evaluate", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has"
		+ "(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private boolean fluidlogistics$countOscillatingEngine(BlockEntry<?> entry, BlockState state,
		Operation<Boolean> original) {
		return original.call(entry, state)
			|| entry == AllBlocks.STEAM_ENGINE && state.getBlock() instanceof OscillatingSteamEngineBlock;
	}
}
