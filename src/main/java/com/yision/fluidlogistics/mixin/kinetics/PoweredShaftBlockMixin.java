package com.yision.fluidlogistics.mixin.kinetics;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlock;
import com.yision.fluidlogistics.content.kinetics.oscillatingSteamEngine.OscillatingSteamEngineBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(PoweredShaftBlock.class)
public class PoweredShaftBlockMixin {
	@WrapOperation(method = "stillValid", at = @At(value = "INVOKE",
		target = "Lcom/simibubi/create/content/kinetics/steamEngine/SteamEngineBlock;getShaftPos"
			+ "(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)"
			+ "Lnet/minecraft/core/BlockPos;"))
	private static BlockPos fluidlogistics$engineShaftPosition(BlockState state, BlockPos pos,
		Operation<BlockPos> original) {
		return state.getBlock() instanceof OscillatingSteamEngineBlock
			? OscillatingSteamEngineBlock.getShaftPos(state, pos) : original.call(state, pos);
	}

	@Inject(method = "stillValid", at = @At("RETURN"), cancellable = true)
	private static void fluidlogistics$findAdjacentEngine(BlockState state, LevelReader level, BlockPos pos,
		CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValue())
			return;
		for (Direction direction : Direction.values()) {
			if (direction.getAxis() == state.getValue(ShaftBlock.AXIS))
				continue;
			BlockPos enginePos = pos.relative(direction);
			BlockState engine = level.getBlockState(enginePos);
			if (engine.getBlock() instanceof OscillatingSteamEngineBlock
				&& OscillatingSteamEngineBlock.getShaftPos(engine, enginePos).equals(pos)
				&& OscillatingSteamEngineBlock.isShaftValid(engine, state)) {
				cir.setReturnValue(true);
				return;
			}
		}
	}
}
