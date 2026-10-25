package com.yision.fluidlogistics.datacomponent;

import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import net.neoforged.neoforge.fluids.FluidStack;

public record PackageFluidContents(List<FluidTankContent> tanks) {
	public static final int MAX_FLUIDS = 10;

	public PackageFluidContents {
		tanks = List.copyOf(tanks);
		if (tanks.stream().anyMatch(FluidTankContent::isEmpty)) {
			throw new IllegalArgumentException("package fluid slots must not be empty");
		}
	}

	public static final Codec<PackageFluidContents> CODEC = FluidTankContent.CODEC.listOf()
		.xmap(PackageFluidContents::new, PackageFluidContents::tanks);

	public static final StreamCodec<RegistryFriendlyByteBuf, PackageFluidContents> STREAM_CODEC
		= FluidTankContent.STREAM_CODEC.apply(ByteBufCodecs.list())
			.map(PackageFluidContents::new, PackageFluidContents::tanks);

	public static PackageFluidContents single(FluidStack fluid) {
		return new PackageFluidContents(List.of(new FluidTankContent(fluid)));
	}

	public boolean isEmpty() {
		return tanks.isEmpty();
	}
}
