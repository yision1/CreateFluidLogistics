package com.yision.fluidlogistics.content.logistics.fluidPackage.phantomChain;

import net.minecraft.core.BlockPos;

public interface PhantomChainConveyorAccess {
	boolean fluidlogistics$isPhantomConnection(BlockPos connection);

	void fluidlogistics$setPhantomConnection(BlockPos connection, boolean phantom);
}
