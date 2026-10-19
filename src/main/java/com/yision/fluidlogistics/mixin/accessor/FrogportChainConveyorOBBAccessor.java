package com.yision.fluidlogistics.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorShape;

import net.minecraft.core.BlockPos;

@Mixin(ChainConveyorShape.ChainConveyorOBB.class)
public interface FrogportChainConveyorOBBAccessor {

	@Accessor("connection")
	BlockPos fluidlogistics$getConnection();
}
