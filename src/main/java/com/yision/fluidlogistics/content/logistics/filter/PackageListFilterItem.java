package com.yision.fluidlogistics.content.logistics.filter;

import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.logistics.filter.ListFilterItem;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemStackHandler;

public class PackageListFilterItem extends ListFilterItem {
	public PackageListFilterItem(Properties properties) {
		super(properties);
	}

	@Override
	public FilterItemStack makeStackWrapper(ItemStack filter) {
		return new PackageListFilterItemStack(filter);
	}

	private static class PackageListFilterItemStack extends FilterItemStack.ListFilterItemStack {
		private PackageListFilterItemStack(ItemStack filter) {
			super(filter);
		}

		@Override
		public boolean test(Level world, ItemStack stack, boolean matchNBT) {
			if (!PackageItem.isPackage(stack)) {
				return false;
			}

			ItemStackHandler contents = PackageItem.getContents(stack);
			for (int slot = 0; slot < contents.getSlots(); slot++) {
				ItemStack content = contents.getStackInSlot(slot);
				if (content.isEmpty()) {
					continue;
				}
				if (super.test(world, content, matchNBT) != isBlacklist) {
					return !isBlacklist;
				}
			}
			return isBlacklist;
		}

		@Override
		public boolean test(Level world, FluidStack stack, boolean matchNBT) {
			return false;
		}
	}
}
