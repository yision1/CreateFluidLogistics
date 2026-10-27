package com.yision.fluidlogistics.mixin.logistics;

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
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.yision.fluidlogistics.api.packager.PackageResources;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

@Mixin(InventorySummary.class)
public abstract class InventorySummaryMixin {
	@Shadow(remap = false)
	private Map<Item, List<BigItemStack>> items;

	@Shadow(remap = false)
	private int totalCount;

	@Shadow(remap = false)
	private List<BigItemStack> stacksByCount;

	@Inject(method = "add(Lnet/minecraft/world/item/ItemStack;I)V", at = @At("HEAD"), cancellable = true, remap = false)
	private void fluidlogistics$addResource(ItemStack stack, int count, CallbackInfo ci) {
		if (count == 0 || !PackageResources.isBootstrapped()) {
			return;
		}
		if (PackageResources.findType(stack).isEmpty()) {
			return;
		}
		stack = PackageResources.normalizeKey(stack).orElseThrow();
		stacksByCount = null;

		if (totalCount < BigItemStack.INF) {
			totalCount = (int) Math.min(BigItemStack.INF, (long) totalCount + count);
		}

		List<BigItemStack> stacks = items.computeIfAbsent(stack.getItem(),
			$ -> com.google.common.collect.Lists.newArrayList());
		for (BigItemStack existing : stacks) {
			if (!ItemStack.isSameItemSameTags(existing.stack, stack)) {
				continue;
			}
			if (existing.count < BigItemStack.INF) {
				existing.count = (int) Math.min(BigItemStack.INF, (long) existing.count + count);
			}
			ci.cancel();
			return;
		}

		stacks.add(new BigItemStack(stack, count));
		ci.cancel();
	}

	@Inject(method = "getCountOf", at = @At("HEAD"), cancellable = true, remap = false)
	private void fluidlogistics$getCountOfResource(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
		if (!PackageResources.isBootstrapped()) {
			return;
		}
		var keyResult = PackageResources.normalizeKey(stack);
		if (keyResult.isEmpty()) {
			return;
		}
		ItemStack key = keyResult.orElseThrow();
		List<BigItemStack> stacks = items.get(key.getItem());
		long count = 0;
		if (stacks != null) {
			for (BigItemStack entry : stacks) {
				if (PackageResources.sameResource(entry.stack, key)) {
					count += entry.count;
				}
			}
		}
		cir.setReturnValue((int) Math.min(BigItemStack.INF, count));
	}

	@ModifyVariable(method = "erase", at = @At("HEAD"), argsOnly = true, remap = false)
	private ItemStack fluidlogistics$normalizeErasedResource(ItemStack stack) {
		if (!PackageResources.isBootstrapped()) {
			return stack;
		}
		var keyResult = PackageResources.normalizeKey(stack);
		if (keyResult.isEmpty()) {
			return stack;
		}
		stacksByCount = null;
		return keyResult.orElseThrow();
	}

	@WrapOperation(method = "erase", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/items/ItemHandlerHelper;canItemStacksStack("
		+ "Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z", remap = false), remap = false)
	private boolean fluidlogistics$matchErasedResource(ItemStack existing, ItemStack stack,
		Operation<Boolean> original) {
		if (PackageResources.isBootstrapped() && PackageResources.findType(stack).isPresent()) {
			return PackageResources.sameResource(existing, stack);
		}
		return original.call(existing, stack);
	}
}
