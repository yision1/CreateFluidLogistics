package com.yision.fluidlogistics.mixin.logistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.repackager.PackageRepackageHelper;
import com.yision.fluidlogistics.api.packager.PackageInspection;
import com.yision.fluidlogistics.api.packager.PackageResources;
import com.yision.fluidlogistics.content.logistics.packageResource.ResourceRepackagingPlan;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.items.ItemStackHandler;

@Mixin(PackageRepackageHelper.class)
public abstract class PackageRepackageHelperMixin {

	@Shadow
	protected Map<Integer, List<ItemStack>> collectedPackages;

	@Shadow
	public abstract List<BigItemStack> repack(int orderId, RandomSource random);

	@Unique
	private final Map<Integer, List<BigItemStack>> fluidlogistics$preparedOrders = new HashMap<>();
	@Unique
	private final Set<Integer> fluidlogistics$rejectedOrders = new HashSet<>();
	@Unique
	private ResourceRepackagingPlan.Group fluidlogistics$group;

	@Inject(method = "clear", at = @At("RETURN"))
	private void fluidlogistics$clear(CallbackInfo ci) {
		fluidlogistics$preparedOrders.clear();
		fluidlogistics$rejectedOrders.clear();
	}

	@Inject(method = "addPackageFragment", at = @At("RETURN"), cancellable = true)
	private void fluidlogistics$prepareOrder(ItemStack box, CallbackInfoReturnable<Integer> cir) {
		int orderId = cir.getReturnValueI();
		if (orderId == -1) {
			return;
		}
		if (!PackageResources.isBootstrapped() || fluidlogistics$rejectedOrders.contains(orderId)) {
			cir.setReturnValue(-1);
			return;
		}
		try {
			if (collectedPackages.get(orderId).stream().map(PackageResources::inspectPackage)
				.anyMatch(PackageInspection::hasResources)) {
				fluidlogistics$preparedOrders.put(orderId, repack(orderId, RandomSource.create()));
			}
		} catch (RuntimeException exception) {
			fluidlogistics$rejectedOrders.add(orderId);
			cir.setReturnValue(-1);
		}
	}

	@WrapMethod(method = "repack")
	private List<BigItemStack> fluidlogistics$repack(int orderId, RandomSource random,
		Operation<List<BigItemStack>> original) {
		if (fluidlogistics$rejectedOrders.contains(orderId)) {
			throw new IllegalStateException("resource order could not be prepared: " + orderId);
		}
		List<BigItemStack> prepared = fluidlogistics$preparedOrders.get(orderId);
		if (prepared != null) {
			return prepared.stream().map(entry -> new BigItemStack(entry.stack.copy(), entry.count)).toList();
		}
		if (!PackageResources.isBootstrapped()) {
			return original.call(orderId, random);
		}
		List<ItemStack> packages = collectedPackages.get(orderId);
		List<PackageInspection> inspections = packages.stream().map(PackageResources::inspectPackage).toList();
		if (inspections.stream().noneMatch(PackageInspection::hasResources)) {
			return original.call(orderId, random);
		}
		ResourceRepackagingPlan plan = new ResourceRepackagingPlan(orderId, inspections);
		ResourceRepackagingPlan.Group previousGroup = fluidlogistics$group;
		List<BigItemStack> recipes = new ArrayList<>();
		List<BigItemStack> remainder = new ArrayList<>();
		try {
			for (ResourceRepackagingPlan.Group group : plan.groups()) {
				fluidlogistics$group = group;
				collectedPackages.put(orderId, group.packages());
				group.collect(original.call(orderId, random), recipes, remainder);
			}
			return plan.finish(orderId, recipes, remainder);
		} finally {
			collectedPackages.put(orderId, packages);
			fluidlogistics$group = previousGroup;
		}
	}

	@WrapOperation(method = "repack", at = @At(value = "INVOKE",
		target = "Lcom/simibubi/create/content/logistics/box/PackageItem;containing(Lnet/neoforged/neoforge/items/ItemStackHandler;)Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack fluidlogistics$packResources(ItemStackHandler contents, Operation<ItemStack> original) {
		ItemStack box = original.call(contents);
		return fluidlogistics$group == null ? box : fluidlogistics$group.decorate(box, false);
	}

	@WrapOperation(method = "repackBasedOnRecipes", at = @At(value = "INVOKE",
		target = "Lcom/simibubi/create/content/logistics/box/PackageItem;containing(Lnet/neoforged/neoforge/items/ItemStackHandler;)Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack fluidlogistics$preserveRecipeMetadata(ItemStackHandler contents, Operation<ItemStack> original) {
		ItemStack box = original.call(contents);
		return fluidlogistics$group == null ? box : fluidlogistics$group.decorate(box, true);
	}

	@Inject(method = "repack", slice = @Slice(from = @At(value = "NEW",
		target = "net/neoforged/neoforge/items/ItemStackHandler", ordinal = 0)),
		at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;", ordinal = 1))
	private void fluidlogistics$appendResources(int orderId, RandomSource random,
		CallbackInfoReturnable<List<BigItemStack>> cir,
		@Local(name = "exportingPackages") List<BigItemStack> output) {
		if (fluidlogistics$group != null) {
			fluidlogistics$group.appendRemaining(output);
		}
	}
}
