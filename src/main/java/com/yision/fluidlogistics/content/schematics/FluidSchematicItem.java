package com.yision.fluidlogistics.content.schematics;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.schematics.SchematicItem;
import com.yision.fluidlogistics.content.schematics.client.FluidSchematicEditScreen;
import com.yision.fluidlogistics.registry.AllItems;

import net.createmod.catnip.gui.ScreenOpener;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class FluidSchematicItem extends SchematicItem {

    public FluidSchematicItem(Properties properties) {
        super(properties);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void displayBlueprintScreen() {
        ScreenOpener.open(new FluidSchematicEditScreen());
    }

    public static ItemStack create(Level level, String schematic, String owner) {
        ItemStack blueprint = AllItems.FLUID_SCHEMATIC.asStack();
        blueprint.set(AllDataComponents.SCHEMATIC_DEPLOYED, false);
        blueprint.set(AllDataComponents.SCHEMATIC_OWNER, owner);
        blueprint.set(AllDataComponents.SCHEMATIC_FILE, schematic);
        blueprint.set(AllDataComponents.SCHEMATIC_ANCHOR, BlockPos.ZERO);
        blueprint.set(AllDataComponents.SCHEMATIC_ROTATION, Rotation.NONE);
        blueprint.set(AllDataComponents.SCHEMATIC_MIRROR, Mirror.NONE);
        writeSize(level, blueprint);
        return blueprint;
    }
}
