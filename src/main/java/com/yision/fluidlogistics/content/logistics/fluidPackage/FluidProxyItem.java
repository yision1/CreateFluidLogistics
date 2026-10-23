package com.yision.fluidlogistics.content.logistics.fluidPackage;

import java.util.ArrayList;
import java.util.List;

import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.foundation.gui.FluidTooltipHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.fluids.FluidStack;

@EventBusSubscriber(modid = FluidLogistics.MODID)
public final class FluidProxyItem extends Item {
	private final Fluid fluid;

	FluidProxyItem(Fluid fluid) {
		super(new Properties().stacksTo(1));
		this.fluid = fluid;
	}

	public Fluid getFluid() {
		return fluid;
	}

	@Override
	public Component getName(ItemStack stack) {
		return FluidProxyRegistry.getFluid(stack).getHoverName();
	}

	@Override
	public String getCreatorModId(ItemStack stack) {
		return BuiltInRegistries.FLUID.getKey(fluid).getNamespace();
	}

	@SubscribeEvent
	public static void onTooltip(ItemTooltipEvent event) {
		ItemStack stack = event.getItemStack();
		if (!event.getFlags().isAdvanced() || !FluidProxyRegistry.isProxy(stack)) {
			return;
		}
		List<Component> tooltip = event.getToolTip();
		Component itemId = Component.literal(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
			.withStyle(ChatFormatting.DARK_GRAY);
		int index = tooltip.indexOf(itemId);
		if (index < 0) {
			return;
		}
		FluidStack contents = FluidProxyRegistry.getFluid(stack);
		tooltip.set(index, Component.literal(BuiltInRegistries.FLUID.getKey(contents.getFluid()).toString())
			.withStyle(ChatFormatting.DARK_GRAY));
		Component itemComponents = Component.translatable("item.components", stack.getComponents().size())
			.withStyle(ChatFormatting.DARK_GRAY);
		int componentIndex = tooltip.subList(index + 1, tooltip.size()).indexOf(itemComponents);
		if (componentIndex >= 0) {
			tooltip.remove(index + 1 + componentIndex);
		}
		List<Component> fluidComponents = new ArrayList<>();
		FluidTooltipHelper.addAdvancedComponentLines(fluidComponents, contents, true);
		tooltip.addAll(index + 1, fluidComponents);
	}
}
