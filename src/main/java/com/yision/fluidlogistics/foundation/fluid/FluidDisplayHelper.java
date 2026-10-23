package com.yision.fluidlogistics.foundation.fluid;

import com.yision.fluidlogistics.content.logistics.fluidPackage.CompressedTankItem;
import com.yision.fluidlogistics.content.logistics.fluidPackage.FluidProxyRegistry;

import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.fluids.FluidStack;

public final class FluidDisplayHelper {
	private FluidDisplayHelper() {
	}

	public static FluidStack getDisplayFluid(ItemStack stack) {
		if (FluidProxyRegistry.isProxy(stack)) {
			return FluidProxyRegistry.getFluid(stack);
		}
		return CompressedTankItem.isFluidStack(stack) ? CompressedTankItem.getFluid(stack) : FluidStack.EMPTY;
	}
}
