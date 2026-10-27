package com.yision.fluidlogistics.content.logistics.fluidPackage;

import java.util.function.Consumer;

import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.client.FluidTooltipHelper;
import com.yision.fluidlogistics.content.logistics.fluidPackage.client.FluidProxyItemRenderer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

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
		return FluidProxyRegistry.getFluid(stack).getDisplayName();
	}

	@Override
	public String getCreatorModId(ItemStack stack) {
		return BuiltInRegistries.FLUID.getKey(fluid).getNamespace();
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new IClientItemExtensions() {
			private final FluidProxyItemRenderer renderer = new FluidProxyItemRenderer();

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				return renderer;
			}
		});
	}

	@SubscribeEvent
	public static void onTooltip(ItemTooltipEvent event) {
		ItemStack stack = event.getItemStack();
		if (!event.getFlags().isAdvanced() || !FluidProxyRegistry.isProxy(stack))
			return;
		Component itemId = Component.literal(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
			.withStyle(ChatFormatting.DARK_GRAY);
		int index = event.getToolTip().indexOf(itemId);
		if (index < 0)
			return;
		FluidStack contents = FluidProxyRegistry.getFluid(stack);
		event.getToolTip().set(index, Component.literal(BuiltInRegistries.FLUID.getKey(contents.getFluid()).toString())
			.withStyle(ChatFormatting.DARK_GRAY));
		if (stack.hasTag())
			event.getToolTip().remove(
				Component.translatable("item.nbt_tags", stack.getTag().size()).withStyle(ChatFormatting.DARK_GRAY));
		FluidTooltipHelper.addAdvancedTagLines(event.getToolTip(), contents, event.getFlags());
	}
}
