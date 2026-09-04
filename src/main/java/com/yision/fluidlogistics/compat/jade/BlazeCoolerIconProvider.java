package com.yision.fluidlogistics.compat.jade;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.content.processing.blazeCooler.BlazeCoolerBlockEntity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

public enum BlazeCoolerIconProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final ResourceLocation JADE_BLAZE_BURNER =
        ResourceLocation.fromNamespaceAndPath("jadeaddons.create", "blaze_burner");
    private static final ResourceLocation UID =
        ResourceLocation.fromNamespaceAndPath(FluidLogistics.MODID, "blaze_cooler_icon");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        HeatLevel heatLevel = accessor.getBlockEntity() instanceof BlazeCoolerBlockEntity cooler
            ? cooler.getHeatLevelForRender()
            : BlazeBurnerBlock.getHeatLevelOf(accessor.getBlockState());
        ItemStack iconStack = new ItemStack(heatLevel == HeatLevel.SEETHING ? Blocks.BLUE_ICE : Blocks.SNOW_BLOCK);
        IElement icon = IElementHelper.get().smallItem(iconStack);

        tooltip.replace(JADE_BLAZE_BURNER, lines -> replaceFirstElement(lines, icon));
    }

    private static List<List<IElement>> replaceFirstElement(List<List<IElement>> lines, IElement icon) {
        List<List<IElement>> replacement = new ArrayList<>(lines.size());
        for (List<IElement> line : lines) {
            List<IElement> elements = new ArrayList<>(line);
            if (!elements.isEmpty())
                elements.set(0, icon);
            replacement.add(elements);
        }
        return replacement;
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public int getDefaultPriority() {
        return 10_000;
    }
}
