package com.yision.fluidlogistics.mixin.kinetics;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.yision.fluidlogistics.content.kinetics.oscillatingSteamEngine.OscillatingSteamEngineBlock;

import net.minecraft.world.level.block.state.BlockState;

@Mixin(targets = "com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock$PlacementHelper", remap = false)
public class SteamEnginePlacementHelperMixin {
	@ModifyReturnValue(method = "getStatePredicate", at = @At("RETURN"))
	private Predicate<BlockState> fluidlogistics$excludeOscillatingEngine(Predicate<BlockState> original) {
		return original.and(state -> !(state.getBlock() instanceof OscillatingSteamEngineBlock));
	}
}
