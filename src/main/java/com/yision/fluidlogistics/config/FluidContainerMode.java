package com.yision.fluidlogistics.config;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public enum FluidContainerMode {
    ALLOW_BY_TAG,
    ALLOW_ALL,
    DENY_ALL;

    public boolean test(BlockState state, TagKey<Block> tag) {
        return switch (this) {
            case ALLOW_BY_TAG -> state.is(tag);
            case ALLOW_ALL -> true;
            case DENY_ALL -> false;
        };
    }
}
