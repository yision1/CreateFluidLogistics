package com.yision.fluidlogistics.migration.item;

import com.yision.fluidlogistics.content.logistics.fluidPackage.CompressedTankItem;
import com.yision.fluidlogistics.datacomponent.PackageFluidContents;
import com.yision.fluidlogistics.registry.AllItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.minecraftforge.fluids.FluidStack;

public final class LegacyCompressedTankMigration {
	private LegacyCompressedTankMigration() {
	}

	public static InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!canRecover(stack)) {
			return InteractionResultHolder.pass(stack);
		}
		return InteractionResultHolder.success(level.isClientSide ? stack : recover(stack));
	}

	public static ItemStack recover(ItemStack stack) {
		if (!canRecover(stack)) {
			return stack;
		}
		FluidStack fluid = CompressedTankItem.getFluid(stack);
		ItemStack result = AllItems.createFluidPackage();
		if (stack.hasTag()) {
			CompoundTag metadata = stack.getTag().copy();
			metadata.remove("Fluid");
			result.setTag(metadata);
		}
		PackageFluidContents.set(result, PackageFluidContents.single(fluid));
		return result;
	}

	private static boolean canRecover(ItemStack stack) {
		if (stack.getCount() != 1 || !CompressedTankItem.isFluidStack(stack) || PackageFluidContents.has(stack)) {
			return false;
		}
		FluidStack fluid = CompressedTankItem.getFluid(stack);
		return !fluid.isEmpty() && BuiltInRegistries.FLUID.getKey(fluid.getFluid()) != null;
	}
}
