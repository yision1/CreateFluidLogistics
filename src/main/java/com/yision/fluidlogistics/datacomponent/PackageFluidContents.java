package com.yision.fluidlogistics.datacomponent;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.fluids.FluidStack;

public record PackageFluidContents(List<FluidTankContent> tanks) {
	public static final int MAX_FLUIDS = 10;
	public static final String TAG_KEY = "Fluids";

	public PackageFluidContents {
		tanks = List.copyOf(tanks);
		if (tanks.stream().anyMatch(FluidTankContent::isEmpty)) {
			throw new IllegalArgumentException("package fluid slots must not be empty");
		}
	}

	public static PackageFluidContents single(FluidStack fluid) {
		return new PackageFluidContents(List.of(new FluidTankContent(fluid)));
	}

	public boolean isEmpty() {
		return tanks.isEmpty();
	}

	public static boolean has(ItemStack stack) {
		return stack.hasTag() && stack.getTag().contains(TAG_KEY);
	}

	public static PackageFluidContents get(ItemStack stack) {
		if (!has(stack)) {
			return null;
		}
		ListTag list = stack.getTag().getCompound(TAG_KEY).getList("Fluids", Tag.TAG_COMPOUND);
		List<FluidTankContent> tanks = new ArrayList<>(list.size());
		for (int i = 0; i < list.size(); i++) {
			tanks.add(new FluidTankContent(FluidStack.loadFluidStackFromNBT(list.getCompound(i))));
		}
		return new PackageFluidContents(tanks);
	}

	public static void set(ItemStack stack, PackageFluidContents contents) {
		ListTag list = new ListTag();
		for (FluidTankContent tank : contents.tanks()) {
			list.add(tank.fluid().writeToNBT(new CompoundTag()));
		}
		CompoundTag fluids = new CompoundTag();
		fluids.put("Fluids", list);
		stack.getOrCreateTag().put(TAG_KEY, fluids);
	}

	public static void remove(ItemStack stack) {
		stack.removeTagKey(TAG_KEY);
	}
}
