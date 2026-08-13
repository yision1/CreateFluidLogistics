package com.yision.fluidlogistics.content.schematics.cannon;

import com.simibubi.create.content.schematics.cannon.SchematicannonInventory;
import com.yision.fluidlogistics.registry.AllItems;

import net.minecraft.world.item.ItemStack;

public class CopperSchematicannonInventory extends SchematicannonInventory {

    public CopperSchematicannonInventory(CopperSchematicannonBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot == 0) {
            return AllItems.FLUID_SCHEMATIC.isIn(stack);
        }
        return super.isItemValid(slot, stack);
    }
}
