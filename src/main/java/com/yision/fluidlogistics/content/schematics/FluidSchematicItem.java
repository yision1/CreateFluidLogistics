package com.yision.fluidlogistics.content.schematics;

import com.simibubi.create.content.schematics.SchematicItem;
import com.yision.fluidlogistics.content.schematics.client.FluidSchematicEditScreen;
import com.yision.fluidlogistics.registry.AllItems;

import net.createmod.catnip.gui.ScreenOpener;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

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
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Deployed", false);
        tag.putString("Owner", owner);
        tag.putString("File", schematic);
        tag.put("Anchor", NbtUtils.writeBlockPos(BlockPos.ZERO));
        tag.putString("Rotation", Rotation.NONE.name());
        tag.putString("Mirror", Mirror.NONE.name());
        blueprint.setTag(tag);
        writeSize(level, blueprint);
        return blueprint;
    }
}
