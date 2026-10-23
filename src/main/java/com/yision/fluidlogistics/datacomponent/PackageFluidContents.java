package com.yision.fluidlogistics.datacomponent;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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

	private static final Codec<PackageFluidContents> LIST_CODEC = RecordCodecBuilder.create(instance -> instance
		.group(FluidTankContent.CODEC.listOf().fieldOf("fluids").forGetter(PackageFluidContents::tanks))
		.apply(instance, PackageFluidContents::new));

	public static final Codec<PackageFluidContents> CODEC = Codec.withAlternative(
		LIST_CODEC, FluidTankContent.CODEC.xmap(content -> new PackageFluidContents(List.of(content)),
			contents -> contents.tanks().getFirst()));

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
