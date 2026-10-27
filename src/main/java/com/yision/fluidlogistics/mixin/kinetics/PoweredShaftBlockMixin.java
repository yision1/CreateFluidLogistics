package com.yision.fluidlogistics.mixin.kinetics;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlock;
import com.yision.fluidlogistics.content.kinetics.oscillatingSteamEngine.OscillatingSteamEngineBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(value = PoweredShaftBlock.class, remap = false)
public class PoweredShaftBlockMixin {
	@WrapOperation(method = "stillValid", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/steamEngine/SteamEngineBlock;getShaftPos"
		+ "(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)"
		+ "Lnet/minecraft/core/BlockPos;"))
	private static BlockPos fluidlogistics$engineShaftPosition(BlockState state, BlockPos pos,
		Operation<BlockPos> original) {
		return state.getBlock() instanceof OscillatingSteamEngineBlock
			? OscillatingSteamEngineBlock.getShaftPos(state, pos)
			: original.call(state, pos);
	}

	@ModifyReturnValue(method = "stillValid", at = @At("RETURN"))
	private static boolean fluidlogistics$findAdjacentEngine(boolean original, BlockState state, LevelReader level,
		BlockPos pos) {
		if (original)
			return true;
		for (Direction direction : Direction.values()) {
			if (direction.getAxis() == state.getValue(ShaftBlock.AXIS))
				continue;
			BlockPos enginePos = pos.relative(direction);
			BlockState engine = level.getBlockState(enginePos);
			if (engine.getBlock() instanceof OscillatingSteamEngineBlock
				&& OscillatingSteamEngineBlock.getShaftPos(engine, enginePos).equals(pos)
				&& OscillatingSteamEngineBlock.isShaftValid(engine, state))
				return true;
		}
		return false;
	}
}
