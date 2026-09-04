package com.yision.fluidlogistics.content.processing.cooling;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class CoolingRecipeParams extends ProcessingRecipeParams {

    private static final MapCodec<CoolingRecipeParams> BASE_CODEC = codec(CoolingRecipeParams::new);
    private static final StreamCodec<RegistryFriendlyByteBuf, CoolingRecipeParams> BASE_STREAM_CODEC =
        streamCodec(CoolingRecipeParams::new);

    public static final MapCodec<CoolingRecipeParams> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        BASE_CODEC.forGetter(params -> params),
        Codec.BOOL.optionalFieldOf("supercooled", false).forGetter(CoolingRecipeParams::supercooled)
    ).apply(instance, CoolingRecipeParams::withSupercooled));

    public static final StreamCodec<RegistryFriendlyByteBuf, CoolingRecipeParams> STREAM_CODEC = StreamCodec.composite(
        BASE_STREAM_CODEC, params -> params,
        ByteBufCodecs.BOOL, CoolingRecipeParams::supercooled,
        CoolingRecipeParams::withSupercooled
    );

    private boolean supercooled;

    private CoolingRecipeParams() {
    }

    public boolean supercooled() {
        return supercooled;
    }

    private static CoolingRecipeParams withSupercooled(CoolingRecipeParams params, boolean supercooled) {
        params.supercooled = supercooled;
        return params;
    }
}
