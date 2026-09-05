package com.yision.fluidlogistics.compat.jade;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity.FuelType;
import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.content.processing.blazeCooler.BlazeCoolerBlockEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.TooltipPosition;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.IElementHelper;

public enum BlazeCoolerIconProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation JADE_BLAZE_BURNER =
        ResourceLocation.fromNamespaceAndPath("jadeaddons.create", "blaze_burner");
    private static final ResourceLocation UID =
        ResourceLocation.fromNamespaceAndPath(FluidLogistics.MODID, "blaze_cooler_icon");
    private static final String CREATIVE_TAG = "CFLBlazeCoolerCreative";
    private static final String REMAINING_TIME_TAG = "CFLBlazeCoolerRemainingTime";

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.getBoolean(CREATIVE_TAG) && !data.contains(REMAINING_TIME_TAG))
            return;

        HeatLevel heatLevel = accessor.getBlockEntity() instanceof BlazeCoolerBlockEntity cooler
            ? cooler.getHeatLevelForRender()
            : BlazeBurnerBlock.getHeatLevelOf(accessor.getBlockState());
        ItemStack iconStack = new ItemStack(heatLevel == HeatLevel.SEETHING ? Blocks.BLUE_ICE : Blocks.SNOW_BLOCK);

        tooltip.remove(JADE_BLAZE_BURNER);
        tooltip.add(IElementHelper.get().smallItem(iconStack));
        tooltip.append(data.getBoolean(CREATIVE_TAG)
            ? IThemeHelper.get().info(Component.translatable("jade.infinity"))
            : IThemeHelper.get().seconds(data.getInt(REMAINING_TIME_TAG)));
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlazeCoolerBlockEntity cooler = (BlazeCoolerBlockEntity) accessor.getBlockEntity();
        if (cooler.isCreative())
            data.putBoolean(CREATIVE_TAG, true);
        else if (cooler.getActiveFuel() != FuelType.NONE)
            data.putInt(REMAINING_TIME_TAG, cooler.getRemainingBurnTime());
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public int getDefaultPriority() {
        return TooltipPosition.BODY + 999;
    }
}
