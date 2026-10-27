package com.yision.fluidlogistics.compat.jade;

import java.util.List;

import com.yision.fluidlogistics.FluidLogistics;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

public enum SnowGolemFreezerIconProvider implements IBlockComponentProvider {
	INSTANCE;

	private static final ResourceLocation JADE_BLAZE_BURNER = ResourceLocation.fromNamespaceAndPath("jadeaddons.create",
		"blaze_burner");
	private static final ResourceLocation UID = FluidLogistics.asResource("snow_golem_freezer_icon");

	@Override
	public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
		ItemStack iconStack = new ItemStack(Blocks.SNOW_BLOCK);
		for (int line = 0; line < tooltip.size(); line++) {
			for (IElement.Align alignment : IElement.Align.values()) {
				List<IElement> elements = tooltip.get(line, alignment);
				for (int index = 0; index < elements.size(); index++) {
					if (JADE_BLAZE_BURNER.equals(elements.get(index).getTag())) {
						elements.set(index, IElementHelper.get().smallItem(iconStack).tag(JADE_BLAZE_BURNER));
						break;
					}
				}
			}
		}
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
