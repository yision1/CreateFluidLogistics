package com.yision.fluidlogistics.filter.attribute;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttributeType;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.minecraftforge.fluids.FluidStack;

public final class AnyFluidAttribute implements FluidAttribute {
	private final Type type;

	private AnyFluidAttribute(Type type) {
		this.type = type;
	}

	@Override
	public boolean appliesTo(FluidStack stack, Level level) {
		return !stack.isEmpty();
	}

	@Override
	public ItemAttributeType getType() {
		return type;
	}

	@Override
	public String getTranslationKey() {
		return "is_any_fluid";
	}

	@Override
	public void save(CompoundTag nbt) {
	}

	@Override
	public void load(CompoundTag nbt) {
	}

	public static final class Type implements ItemAttributeType {
		private final AnyFluidAttribute attribute = new AnyFluidAttribute(this);

		@Override
		public @NotNull ItemAttribute createAttribute() {
			return attribute;
		}

		@Override
		public List<ItemAttribute> getAllAttributes(ItemStack stack, Level level) {
			return FluidAttributeHelper.extractFluids(stack, level).isEmpty() ? List.of() : List.of(attribute);
		}
	}
}
