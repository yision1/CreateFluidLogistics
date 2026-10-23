package com.yision.fluidlogistics.content.processing.snowGolemFreezer;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yision.fluidlogistics.FluidLogistics;

import net.minecraft.core.registries.Registries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

public final class SnowGolemFreezerFuelManager {

	public static final DataMapType<Item, Fuel> ITEM_FUELS = DataMapType.builder(
		FluidLogistics.asResource("snow_golem_freezer_fuels"), Registries.ITEM, Fuel.ITEM_CODEC)
		.synced(Fuel.ITEM_CODEC, true)
		.build();
	public static final DataMapType<Fluid, Fuel> FLUID_FUELS = DataMapType.builder(
		FluidLogistics.asResource("snow_golem_freezer_fuels"), Registries.FLUID, Fuel.FLUID_CODEC)
		.synced(Fuel.FLUID_CODEC, true)
		.build();

	private SnowGolemFreezerFuelManager() {
	}

	public static void registerDataMaps(RegisterDataMapTypesEvent event) {
		event.register(ITEM_FUELS);
		event.register(FLUID_FUELS);
	}

	public static @Nullable Fuel find(ItemStack stack) {
		return stack.isEmpty() ? null : stack.getItemHolder().getData(ITEM_FUELS);
	}

	public static @Nullable Fuel find(FluidStack stack) {
		return stack.isEmpty() ? null : stack.getFluidHolder().getData(FLUID_FUELS);
	}

	public record Fuel(int coolTime, int amount) {
		public static final Codec<Fuel> ITEM_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ExtraCodecs.POSITIVE_INT.fieldOf("cool_time").forGetter(Fuel::coolTime))
			.apply(instance, coolTime -> new Fuel(coolTime, 1)));
		public static final Codec<Fuel> FLUID_CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ExtraCodecs.POSITIVE_INT.fieldOf("cool_time").forGetter(Fuel::coolTime),
			Codec.intRange(1, FluidType.BUCKET_VOLUME).fieldOf("amount").forGetter(Fuel::amount))
			.apply(instance, Fuel::new));
	}
}
