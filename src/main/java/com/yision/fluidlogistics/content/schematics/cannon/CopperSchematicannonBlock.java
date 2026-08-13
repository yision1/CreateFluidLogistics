package com.yision.fluidlogistics.content.schematics.cannon;

import com.simibubi.create.content.schematics.cannon.SchematicannonBlock;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.yision.fluidlogistics.registry.AllBlockEntities;

import net.minecraft.world.level.block.entity.BlockEntityType;

public class CopperSchematicannonBlock extends SchematicannonBlock {

    public CopperSchematicannonBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends SchematicannonBlockEntity> getBlockEntityType() {
        return AllBlockEntities.COPPER_SCHEMATICANNON.get();
    }
}
