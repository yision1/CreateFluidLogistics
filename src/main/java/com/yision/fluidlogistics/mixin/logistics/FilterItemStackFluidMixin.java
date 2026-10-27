package com.yision.fluidlogistics.mixin.logistics;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.yision.fluidlogistics.content.logistics.fluidPackage.FluidProxyRegistry;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.minecraftforge.fluids.FluidStack;

@Mixin(value = FilterItemStack.class, remap = false)
public abstract class FilterItemStackFluidMixin {
	@Shadow
	@Final
	private ItemStack filterItemStack;
	@Shadow
	private boolean fluidExtracted;
	@Shadow
	private FluidStack filterFluidStack;

	@Inject(method = "resolveFluid", at = @At("HEAD"), cancellable = true)
	private void fluidlogistics$resolveProxyFluid(Level world, CallbackInfo ci) {
		if (!FluidProxyRegistry.isProxy(filterItemStack)) {
			return;
		}
		if (!fluidExtracted) {
			filterFluidStack = FluidProxyRegistry.getFluid(filterItemStack);
			fluidExtracted = true;
		}
		ci.cancel();
	}
}
