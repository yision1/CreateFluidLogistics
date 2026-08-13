package com.yision.fluidlogistics.foundation.fluid;

import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.config.Config;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class FluidContainerPolicies {
    public static final TagKey<Block> FAUCET_FILLABLE = TagKey.create(
        Registries.BLOCK, FluidLogistics.asResource("faucet_fillable"));

    private FluidContainerPolicies() {
    }

    public static boolean allowsFaucet(BlockState state) {
        return Config.getFaucetFluidContainerMode().test(state, FAUCET_FILLABLE);
    }
}
