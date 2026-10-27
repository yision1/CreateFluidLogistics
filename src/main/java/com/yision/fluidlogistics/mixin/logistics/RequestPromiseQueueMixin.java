package com.yision.fluidlogistics.mixin.logistics;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packagerLink.RequestPromise;
import com.simibubi.create.content.logistics.packagerLink.RequestPromiseQueue;
import com.yision.fluidlogistics.api.packager.PackageResources;
import com.yision.fluidlogistics.migration.item.LegacyFluidKeyMigration;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

@Mixin(RequestPromiseQueue.class)
public abstract class RequestPromiseQueueMixin {
	@Shadow(remap = false)
	private Map<Item, List<RequestPromise>> promisesByItem;

	@Shadow(remap = false)
	private Runnable onChanged;

	@Inject(method = "add", at = @At("HEAD"), remap = false)
	private void fluidlogistics$normalizeAddedPromise(RequestPromise promise, CallbackInfo ci) {
		if (PackageResources.isBootstrapped()) {
			promise.promisedStack.stack = LegacyFluidKeyMigration.normalizeKey(promise.promisedStack.stack);
		}
	}

	@Inject(method = "getTotalPromisedAndRemoveExpired", at = @At("HEAD"), cancellable = true, remap = false)
	private void fluidlogistics$getTotalPromisedForResource(ItemStack stack, int expiryTime,
		CallbackInfoReturnable<Integer> cir) {
		if (!PackageResources.isBootstrapped()) {
			return;
		}
		var keyResult = PackageResources.normalizeKey(stack);
		if (keyResult.isEmpty()) {
			return;
		}
		ItemStack key = keyResult.orElseThrow();
		List<RequestPromise> list = promisesByItem.get(key.getItem());
		if (list == null) {
			cir.setReturnValue(0);
			return;
		}

		long promised = 0;
		boolean changed = false;
		for (Iterator<RequestPromise> iterator = list.iterator(); iterator.hasNext();) {
			RequestPromise promise = iterator.next();
			if (!PackageResources.sameResource(promise.promisedStack.stack, key)) {
				continue;
			}
			if (expiryTime != -1 && promise.ticksExisted >= expiryTime) {
				iterator.remove();
				changed = true;
			} else {
				promised += promise.promisedStack.count;
			}
		}
		if (list.isEmpty()) {
			promisesByItem.remove(key.getItem());
		}
		if (changed) {
			onChanged.run();
		}
		cir.setReturnValue((int) Math.min(BigItemStack.INF, promised));
	}

	@ModifyVariable(method = "forceClear", at = @At("HEAD"), argsOnly = true, remap = false)
	private ItemStack fluidlogistics$normalizeClearedResource(ItemStack stack) {
		if (!PackageResources.isBootstrapped()) {
			return stack;
		}
		return PackageResources.normalizeKey(stack).orElse(stack);
	}

	@WrapOperation(method = "forceClear", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/items/ItemHandlerHelper;canItemStacksStack("
		+ "Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z", remap = false), remap = false)
	private boolean fluidlogistics$matchClearedResource(ItemStack promised, ItemStack stack,
		Operation<Boolean> original) {
		if (PackageResources.isBootstrapped() && PackageResources.findType(stack).isPresent()) {
			return PackageResources.sameResource(promised, stack);
		}
		return original.call(promised, stack);
	}

	@Inject(method = "itemEnteredSystem", at = @At("HEAD"), cancellable = true, remap = false)
	private void fluidlogistics$itemEnteredSystemResource(ItemStack stack, int amount, CallbackInfo ci) {
		if (!PackageResources.isBootstrapped()) {
			return;
		}
		var keyResult = PackageResources.normalizeKey(stack);
		if (keyResult.isEmpty()) {
			return;
		}
		ci.cancel();
		if (amount <= 0) {
			return;
		}
		ItemStack key = keyResult.orElseThrow();
		List<RequestPromise> list = promisesByItem.get(key.getItem());
		if (list == null) {
			return;
		}

		boolean changed = false;
		for (Iterator<RequestPromise> iterator = list.iterator(); iterator.hasNext();) {
			RequestPromise promise = iterator.next();
			if (!PackageResources.sameResource(promise.promisedStack.stack, key)) {
				continue;
			}
			int transferred = Math.min(amount, promise.promisedStack.count);
			amount -= transferred;
			promise.promisedStack.count -= transferred;
			changed |= transferred > 0;
			if (promise.promisedStack.count <= 0) {
				iterator.remove();
				changed = true;
			}
			if (amount <= 0) {
				break;
			}
		}
		if (list.isEmpty()) {
			promisesByItem.remove(key.getItem());
		}
		if (changed) {
			onChanged.run();
		}
	}
}
