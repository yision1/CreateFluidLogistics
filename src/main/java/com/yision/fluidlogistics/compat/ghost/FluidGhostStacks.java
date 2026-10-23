package com.yision.fluidlogistics.compat.ghost;

import com.yision.fluidlogistics.api.packager.PackageResourceTypes;
import com.yision.fluidlogistics.content.logistics.fluidPackage.FluidProxyRegistry;

import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.fluids.FluidStack;

public final class FluidGhostStacks {
	private FluidGhostStacks() {
	}

	public static ItemStack fromFluid(FluidStack fluid) {
		return PackageResourceTypes.createFluidKey(fluid);
	}

	public static boolean isFluidGhost(ItemStack stack) {
		return FluidProxyRegistry.isProxy(stack);
	}

	public static FluidStack getFluid(ItemStack stack) {
		if (!isFluidGhost(stack)) {
			return FluidStack.EMPTY;
		}
		return PackageResourceTypes.getFluid(stack);
	}
}
