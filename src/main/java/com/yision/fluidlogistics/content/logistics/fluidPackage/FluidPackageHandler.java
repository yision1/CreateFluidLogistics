package com.yision.fluidlogistics.content.logistics.fluidPackage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.api.packager.PackageInspection;
import com.yision.fluidlogistics.api.packager.PackageResource;
import com.yision.fluidlogistics.api.packager.PackageResourceDisplay;
import com.yision.fluidlogistics.api.packager.PackageResources;
import com.yision.fluidlogistics.migration.packageData.LegacyFluidPackageMigration;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = FluidLogistics.MODID)
public final class FluidPackageHandler {
	private FluidPackageHandler() {
	}

	@SubscribeEvent
	public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
		if (!event.getEntity().isShiftKeyDown() || !shouldBlockOpen(event.getItemStack())) {
			return;
		}
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.PASS);
	}

	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if (!event.getEntity().isShiftKeyDown() || !shouldBlockOpen(event.getItemStack())) {
			return;
		}
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.PASS);
	}

	@SubscribeEvent
	public static void onTooltip(ItemTooltipEvent event) {
		ItemStack box = event.getItemStack();
		if (!(box.getItem() instanceof PackageItem packageItem) || !PackageResources.isBootstrapped()) {
			return;
		}
		PackageInspection inspection = PackageResources.inspectPackage(box);
		if (!inspection.hasResources()) {
			return;
		}
		List<Component> nativeLines = new ArrayList<>();
		packageItem.appendHoverText(box, event.getContext(), nativeLines, event.getFlags());
		int nativeIndex = nativeLines.isEmpty() ? -1 : Collections.indexOfSubList(event.getToolTip(), nativeLines);
		int insertionIndex;
		if (nativeIndex >= 0) {
			if (LegacyFluidPackageMigration.hasLegacyContents(box)) {
				ItemStack displayBox = box.copy();
				displayBox.set(AllDataComponents.PACKAGE_CONTENTS,
					ItemContainerContents.fromItems(inspection.ordinaryItems()));
				List<Component> itemLines = new ArrayList<>();
				packageItem.appendHoverText(displayBox, event.getContext(), itemLines, event.getFlags());
				event.getToolTip().subList(nativeIndex, nativeIndex + nativeLines.size()).clear();
				event.getToolTip().addAll(nativeIndex, itemLines);
				insertionIndex = nativeIndex + itemLines.size();
			} else {
				insertionIndex = nativeIndex + nativeLines.size();
			}
		} else {
			Component itemIdLine = Component.literal(BuiltInRegistries.ITEM.getKey(box.getItem()).toString())
				.withStyle(ChatFormatting.DARK_GRAY);
			int itemIdIndex = event.getToolTip().indexOf(itemIdLine);
			insertionIndex = event.getFlags().isAdvanced() && itemIdIndex >= 0
				? itemIdIndex
				: event.getToolTip().size();
		}
		event.getToolTip().addAll(insertionIndex, resourceTooltip(inspection.resources()));
	}

	private static List<Component> resourceTooltip(List<PackageResource> resources) {
		List<Component> tooltip = new ArrayList<>();
		for (PackageResource resource : resources.subList(0, Math.min(3, resources.size()))) {
			ItemStack key = resource.key();
			Component name = PackageResources.nameOf(key).orElseGet(key::getHoverName);
			String amount = PackageResources.formatAmount(key, resource.amount(), PackageResourceDisplay.Format.COMPACT)
				.orElse(String.valueOf(resource.amount()));
			tooltip.add(Component.literal("").append(name).append(" " + amount).withStyle(ChatFormatting.GRAY));
		}
		int skipped = resources.size() - tooltip.size();
		if (skipped > 0) {
			tooltip.add(Component.translatable("container.shulkerBox.more", skipped)
				.withStyle(ChatFormatting.ITALIC));
		}
		return tooltip;
	}

	private static boolean shouldBlockOpen(ItemStack box) {
		return !(box.getItem() instanceof FluidPackageItem)
			&& PackageResources.isBootstrapped()
			&& PackageResources.blocksManualOpen(box);
	}
}
