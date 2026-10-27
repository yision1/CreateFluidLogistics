package com.yision.fluidlogistics.mixin.client;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import com.yision.fluidlogistics.api.packager.PackageResourceCrafting;
import com.yision.fluidlogistics.api.packager.PackageResourceCraftingData;
import com.yision.fluidlogistics.api.packager.PackageResourceDisplay;
import com.yision.fluidlogistics.api.packager.PackageResources;
import com.yision.fluidlogistics.api.packager.client.StockKeeperAmountRenderer;
import com.yision.fluidlogistics.content.logistics.packageResource.client.PackageResourceClientRegistry;
import com.yision.fluidlogistics.content.processing.snowGolemFreezer.SnowGolemFreezerBlockEntity;
import com.yision.fluidlogistics.content.processing.snowGolemFreezer.SnowGolemFreezerRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(StockKeeperRequestScreen.class)
public abstract class StockKeeperRequestScreenMixin {

	@Shadow(remap = false)
	public List<BigItemStack> itemsToOrder;

	@Shadow(remap = false)
	public List<CraftableBigItemStack> recipesToOrder;

	@Shadow(remap = false)
	public List<List<BigItemStack>> displayedItems;

	@Shadow(remap = false)
	StockTickerBlockEntity blockEntity;

	@Shadow(remap = false)
	private boolean canRequestCraftingPackage;

	@Shadow(remap = false)
	protected abstract BigItemStack getOrderForItem(ItemStack stack);

	@Shadow(remap = false)
	protected abstract Pair<Integer, List<List<BigItemStack>>> maxCraftable(CraftableBigItemStack cbis,
		InventorySummary summary, java.util.function.Function<ItemStack, Integer> countModifier, int newTypeLimit);

	@Shadow(remap = false)
	protected abstract Couple<Integer> getHoveredSlot(int mouseX, int mouseY);

	@Shadow(remap = false)
	private int getMaxScroll() {
		return 0;
	}

	@Shadow(remap = false)
	private void drawItemCount(GuiGraphics graphics, int count, int customCount) {
	}

	@Unique
	private StockKeeperAmountRenderer fluidlogistics$customAmountRenderer;

	@WrapOperation(method = "renderBg", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/render/CachedBuffers;partial("
		+ "Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"
		+ "Lnet/minecraft/world/level/block/state/BlockState;)"
		+ "Lnet/createmod/catnip/render/SuperByteBuffer;", remap = false), remap = true)
	private SuperByteBuffer fluidlogistics$useCoolerCageInWarehouse(PartialModel model, BlockState state,
		Operation<SuperByteBuffer> original) {
		if (com.yision.fluidlogistics.registry.AllBlocks.SNOW_GOLEM_FREEZER.has(state))
			model = com.yision.fluidlogistics.registry.AllPartialModels.SNOW_GOLEM_FREEZER_CAGE;
		return original.call(model, state);
	}

	@WrapOperation(method = "renderBg", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/processing/burner/BlazeBurnerRenderer;renderShared("
		+ "Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/PoseStack;"
		+ "Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;"
		+ "Lnet/minecraft/world/level/block/state/BlockState;"
		+ "Lcom/simibubi/create/content/processing/burner/BlazeBurnerBlock$HeatLevel;FFZZ"
		+ "Ldev/engine_room/flywheel/lib/model/baked/PartialModel;I)V", remap = false), remap = true)
	private void fluidlogistics$renderCoolerInWarehouse(PoseStack poseStack, PoseStack modelTransform,
		MultiBufferSource bufferSource, Level level, BlockState state, HeatLevel heatLevel, float animation,
		float horizontalAngle, boolean canDrawFlame, boolean drawGoggles, PartialModel drawHat, int hashCode,
		Operation<Void> original, @Local BlazeBurnerBlockEntity keeperBE) {
		if (com.yision.fluidlogistics.registry.AllBlocks.SNOW_GOLEM_FREEZER.has(state)) {
			SnowGolemFreezerRenderer.renderShared(poseStack, bufferSource, level, state, heatLevel, animation,
				horizontalAngle, canDrawFlame, drawGoggles, drawHat, hashCode,
				keeperBE instanceof SnowGolemFreezerBlockEntity cooler && cooler.hasPumpkinHead());
			return;
		}
		original.call(poseStack, modelTransform, bufferSource, level, state, heatLevel, animation, horizontalAngle,
			canDrawFlame, drawGoggles, drawHat, hashCode);
	}

	@Inject(method = "renderItemEntry", at = @At("HEAD"), remap = false, cancellable = true)
	private void fluidlogistics$onRenderItemEntryHead(GuiGraphics graphics, float scale, BigItemStack entry,
		boolean isStackHovered, boolean isRenderingOrders, CallbackInfo ci) {
		fluidlogistics$customAmountRenderer = PackageResourceClientRegistry.stockKeeperRendererFor(entry.stack);
	}

	@WrapOperation(method = "renderItemEntry", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/stockTicker/StockKeeperRequestScreen;"
		+ "drawItemCount(Lnet/minecraft/client/gui/GuiGraphics;II)V", remap = false), remap = false)
	private void fluidlogistics$redirectDrawItemCount(StockKeeperRequestScreen instance, GuiGraphics graphics,
		int count, int customCount, Operation<Void> original) {
		if (fluidlogistics$customAmountRenderer != null) {
			return;
		}
		original.call(instance, graphics, count, customCount);
	}

	@WrapOperation(method = "renderItemEntry", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItemDecorations("
		+ "Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", remap = true), remap = false)
	private void fluidlogistics$redirectRenderItemDecorations(GuiGraphics graphics, Font font, ItemStack stack, int x,
		int y, String text, Operation<Void> original, @Local(ordinal = 0) int customCount) {
		if (fluidlogistics$customAmountRenderer != null && customCount > 0) {
			fluidlogistics$customAmountRenderer.render(graphics, customCount);
			return;
		}
		original.call(graphics, font, stack, x, y, text);
	}

	@Inject(method = "renderItemEntry", at = @At("RETURN"), remap = false)
	private void fluidlogistics$afterRenderItemEntry(GuiGraphics graphics, float scale, BigItemStack entry,
		boolean isStackHovered, boolean isRenderingOrders, CallbackInfo ci) {
		fluidlogistics$customAmountRenderer = null;
	}

	@Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true, remap = true)
	private void fluidlogistics$handleDirectResourceClick(double mouseX, double mouseY, int button,
		CallbackInfoReturnable<Boolean> cir) {
		boolean lmb = button == 0;
		boolean rmb = button == 1;
		if (!lmb && !rmb) {
			return;
		}

		Couple<Integer> hoveredSlot = getHoveredSlot((int) mouseX, (int) mouseY);
		if (fluidlogistics$isNoHoveredSlot(hoveredSlot) || hoveredSlot.getFirst() == -2) {
			return;
		}

		boolean orderClicked = hoveredSlot.getFirst() == -1;
		BigItemStack entry = orderClicked ? itemsToOrder.get(hoveredSlot.getSecond())
			: displayedItems.get(hoveredSlot.getFirst()).get(hoveredSlot.getSecond());
		if (!fluidlogistics$changeDirectResourceOrder(entry, orderClicked, !(rmb || orderClicked), entry.count)) {
			return;
		}

		cir.setReturnValue(true);
	}

	@Inject(method = "mouseScrolled(DDD)Z", at = @At("HEAD"), cancellable = true, remap = true)
	private void fluidlogistics$handleDirectResourceScroll(double mouseX, double mouseY, double scrollDelta,
		CallbackInfoReturnable<Boolean> cir) {
		Couple<Integer> hoveredSlot = getHoveredSlot((int) mouseX, (int) mouseY);
		if (fluidlogistics$isNoHoveredSlot(hoveredSlot)) {
			return;
		}
		if (hoveredSlot.getFirst() >= 0 && !Screen.hasShiftDown() && getMaxScroll() != 0) {
			return;
		}
		if (hoveredSlot.getFirst() == -2) {
			return;
		}

		boolean orderClicked = hoveredSlot.getFirst() == -1;
		BigItemStack entry = orderClicked ? itemsToOrder.get(hoveredSlot.getSecond())
			: displayedItems.get(hoveredSlot.getFirst()).get(hoveredSlot.getSecond());
		int steps = Mth.ceil(Math.abs(scrollDelta));
		if (steps <= 0) {
			if (PackageResources.findType(entry.stack).isPresent()) {
				cir.setReturnValue(true);
			}
			return;
		}

		boolean forward = scrollDelta > 0;
		int maxAvailable = blockEntity.getLastClientsideStockSnapshotAsSummary().getCountOf(entry.stack);
		if (!fluidlogistics$changeDirectResourceOrder(entry, orderClicked, forward, maxAvailable, steps)) {
			return;
		}
		cir.setReturnValue(true);
	}

	@Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true, remap = true)
	private void fluidlogistics$handleCustomResourceRecipeClick(double mouseX, double mouseY, int button,
		CallbackInfoReturnable<Boolean> cir) {
		if (button != 0 && button != 1) {
			return;
		}

		Couple<Integer> hoveredSlot = getHoveredSlot((int) mouseX, (int) mouseY);
		if (hoveredSlot.getFirst() != -2) {
			return;
		}

		CraftableBigItemStack cbis = recipesToOrder.get(hoveredSlot.getSecond());
		if (!fluidlogistics$isCustomResourceCraftable(cbis)) {
			return;
		}

		if (button == 1 && cbis.count == 0) {
			recipesToOrder.remove(cbis);
			cir.setReturnValue(true);
			return;
		}

		int delta = fluidlogistics$getResourceRecipeStepAmount(cbis);
		if (button == 1) {
			delta = -delta;
		}

		fluidlogistics$handleCustomResourceCraftableRequest(cbis, delta);
		cir.setReturnValue(true);
	}

	@Inject(method = "mouseScrolled(DDD)Z", at = @At("HEAD"), cancellable = true, remap = true)
	private void fluidlogistics$handleCustomResourceRecipeScroll(double mouseX, double mouseY, double scrollDelta,
		CallbackInfoReturnable<Boolean> cir) {
		Couple<Integer> hoveredSlot = getHoveredSlot((int) mouseX, (int) mouseY);
		if (hoveredSlot.getFirst() != -2) {
			return;
		}

		CraftableBigItemStack cbis = recipesToOrder.get(hoveredSlot.getSecond());
		if (!fluidlogistics$isCustomResourceCraftable(cbis)) {
			return;
		}

		int steps = Mth.ceil(Math.abs(scrollDelta));
		if (steps <= 0) {
			cir.setReturnValue(true);
			return;
		}

		int delta = fluidlogistics$getResourceRecipeStepAmount(cbis) * steps;
		if (scrollDelta < 0) {
			delta = -delta;
		}

		fluidlogistics$handleCustomResourceCraftableRequest(cbis, delta);
		cir.setReturnValue(true);
	}

	@Inject(method = "requestCraftable", at = @At("HEAD"), cancellable = true, remap = false)
	private void fluidlogistics$requestCustomResourceRecipe(CraftableBigItemStack cbis, int requestedDifference,
		CallbackInfo ci) {
		if (!fluidlogistics$hasCustomRecipeData(cbis)) {
			return;
		}

		fluidlogistics$handleCustomResourceCraftableRequest(cbis, requestedDifference);
		ci.cancel();
	}

	@Inject(method = "updateCraftableAmounts", at = @At("HEAD"), cancellable = true, remap = false)
	private void fluidlogistics$handleCustomRecipeAmountUpdates(CallbackInfo ci) {
		for (CraftableBigItemStack cbis : recipesToOrder) {
			if (PackageResourceCrafting.has(cbis)) {
				fluidlogistics$updateCraftableAmountsWithCustomEntries();
				ci.cancel();
				return;
			}
		}
	}

	@WrapOperation(method = "renderForeground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderComponentTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V", ordinal = 0))

	private void fluidlogistics$recipeTooltip(GuiGraphics graphics, Font font, List<Component> tooltipLines, int mouseX,
		int mouseY, Operation<Void> original, @Local(name = "entry") BigItemStack entry) {
		List<Component> lines = PackageResources
			.formatAmount(entry.stack, entry.count, PackageResourceDisplay.Format.PRECISE)
			.map(amount -> fluidlogistics$withResourceAmount(tooltipLines, amount)).orElse(tooltipLines);
		original.call(graphics, font, lines, mouseX, mouseY);
	}

	@WrapOperation(method = "renderForeground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V", ordinal = 0))

	private void fluidlogistics$itemTooltip(GuiGraphics graphics, Font font, ItemStack stack, int mouseX, int mouseY,
		Operation<Void> original, @Local(name = "entry") BigItemStack entry) {
		if (getHoveredSlot(mouseX, mouseY).getFirst() == -1) {
			var amount = PackageResources.formatAmount(entry.stack, entry.count, PackageResourceDisplay.Format.PRECISE);
			if (amount.isPresent()) {
				List<Component> lines = Screen.getTooltipFromItem(Minecraft.getInstance(), stack);
				graphics.renderTooltip(font, fluidlogistics$withResourceAmount(lines, amount.get()),
					stack.getTooltipImage(), stack, mouseX, mouseY);
				return;
			}
		}
		original.call(graphics, font, stack, mouseX, mouseY);
	}

	@Unique
	private void fluidlogistics$updateCraftableAmountsWithCustomEntries() {
		InventorySummary usedItems = new InventorySummary();
		InventorySummary availableItems = new InventorySummary();

		for (BigItemStack ordered : itemsToOrder) {
			availableItems.add(ordered.stack, ordered.count);
		}

		boolean hasCustomEntries = false;

		for (CraftableBigItemStack cbis : recipesToOrder) {
			PackageResourceCraftingData data = PackageResourceCrafting.get(cbis).orElse(null);
			if (data != null) {
				hasCustomEntries = true;

				int outputCount = data.outputCount();
				if (outputCount <= 0) {
					cbis.count = 0;
					continue;
				}

				int maxSets = fluidlogistics$getCustomCraftableSets(availableItems, usedItems, data.requirements());
				cbis.count = Math.min(cbis.count, maxSets * outputCount);

				int committedSets = cbis.count / outputCount;
				for (BigItemStack requirement : data.requirements()) {
					usedItems.add(requirement.stack, requirement.count * committedSets);
				}
				continue;
			}

			Pair<Integer, List<List<BigItemStack>>> craftingResult = maxCraftable(cbis, availableItems,
				stack -> -usedItems.getCountOf(stack), -1);
			int maxCraftable = craftingResult.getFirst();
			List<List<BigItemStack>> validEntriesByIngredient = craftingResult.getSecond();
			int outputCount = cbis.getOutputCount(blockEntity.getLevel());

			cbis.count = Math.min(cbis.count, maxCraftable);

			for (List<BigItemStack> list : validEntriesByIngredient) {
				int remaining = cbis.count / outputCount;
				for (BigItemStack entry : list) {
					if (remaining <= 0) {
						break;
					}
					usedItems.add(entry.stack, Math.min(remaining, entry.count));
					remaining -= entry.count;
				}
			}
		}

		canRequestCraftingPackage = false;
		if (hasCustomEntries) {
			return;
		}

		for (BigItemStack ordered : itemsToOrder) {
			if (usedItems.getCountOf(ordered.stack) != ordered.count) {
				return;
			}
		}
		canRequestCraftingPackage = true;
	}

	@Unique
	private static int fluidlogistics$getCustomCraftableSets(InventorySummary availableItems,
		List<BigItemStack> existingOrders, List<BigItemStack> requirements) {
		int craftableSets = Integer.MAX_VALUE;
		for (BigItemStack requirement : requirements) {
			int orderedCount = fluidlogistics$getMatchingCount(existingOrders, requirement.stack);
			int available = availableItems.getCountOf(requirement.stack) - orderedCount;
			craftableSets = Math.min(craftableSets, available / requirement.count);
		}
		return craftableSets == Integer.MAX_VALUE ? 0 : Math.max(0, craftableSets);
	}

	@Unique
	private static int fluidlogistics$getCustomCraftableSets(InventorySummary availableItems,
		InventorySummary usedItems, List<BigItemStack> requirements) {
		int craftableSets = Integer.MAX_VALUE;
		for (BigItemStack requirement : requirements) {
			int available = availableItems.getCountOf(requirement.stack) - usedItems.getCountOf(requirement.stack);
			craftableSets = Math.min(craftableSets, available / requirement.count);
		}
		return craftableSets == Integer.MAX_VALUE ? 0 : Math.max(0, craftableSets);
	}

	@Unique
	private static boolean fluidlogistics$canFitCustomRecipe(List<BigItemStack> existingOrders,
		List<BigItemStack> requirements) {
		int totalTypes = existingOrders.size();
		List<ItemStack> newTypes = new java.util.ArrayList<>();

		for (BigItemStack requirement : requirements) {
			if (fluidlogistics$hasMatchingStack(existingOrders, requirement.stack)
				|| fluidlogistics$hasMatchingStack(newTypes, requirement.stack)) {
				continue;
			}
			newTypes.add(requirement.stack);
			totalTypes++;
			if (totalTypes > 9) {
				return false;
			}
		}

		return true;
	}

	@Unique
	private static int fluidlogistics$getMatchingCount(List<BigItemStack> stacks, ItemStack target) {
		int total = 0;
		for (BigItemStack entry : stacks) {
			if (ItemStack.isSameItemSameTags(entry.stack, target)) {
				total += entry.count;
			}
		}
		return total;
	}

	@Unique
	private static boolean fluidlogistics$hasMatchingStack(List<?> stacks, ItemStack target) {
		for (Object entry : stacks) {
			ItemStack stack = entry instanceof BigItemStack bigItemStack ? bigItemStack.stack : (ItemStack) entry;
			if (ItemStack.isSameItemSameTags(stack, target)) {
				return true;
			}
		}
		return false;
	}

	@Unique
	private boolean fluidlogistics$hasCustomRecipeData(CraftableBigItemStack cbis) {
		return PackageResourceCrafting.has(cbis);
	}

	@Unique
	private boolean fluidlogistics$isCustomResourceCraftable(CraftableBigItemStack cbis) {
		return fluidlogistics$hasCustomRecipeData(cbis) && PackageResources.findType(cbis.stack).isPresent();
	}

	@Unique
	private static boolean fluidlogistics$isNoHoveredSlot(Couple<Integer> hoveredSlot) {
		return hoveredSlot.getFirst() == -1 && hoveredSlot.getSecond() == -1;
	}

	@Unique

	private static List<Component> fluidlogistics$withResourceAmount(List<Component> nativeLines, String amount) {
		if (nativeLines.isEmpty()) {
			return nativeLines;
		}
		List<Component> lines = new ArrayList<>(nativeLines);
		lines.add(1, Component.literal(amount).withStyle(ChatFormatting.GRAY));
		return lines;
	}

	@Unique
	private boolean fluidlogistics$changeDirectResourceOrder(BigItemStack entry, boolean orderClicked, boolean forward,
		int maxAvailable) {
		return fluidlogistics$changeDirectResourceOrder(entry, orderClicked, forward, maxAvailable, 1);
	}

	@Unique
	private boolean fluidlogistics$changeDirectResourceOrder(BigItemStack entry, boolean orderClicked, boolean forward,
		int maxAvailable, int steps) {
		BigItemStack existingOrder = orderClicked ? entry : getOrderForItem(entry.stack);
		var adjusted = PackageResources.adjustAmount(entry.stack,
			new PackageResourceDisplay.Adjustment(existingOrder == null ? 0 : existingOrder.count, forward,
				Screen.hasShiftDown(), Screen.hasControlDown(), 0, Math.max(0, maxAvailable), steps,
				orderClicked ? PackageResourceDisplay.Interaction.STOCK_KEEPER_ORDER
					: PackageResourceDisplay.Interaction.STOCK_KEEPER_INVENTORY));
		if (adjusted.isEmpty()) {
			return false;
		}
		int newAmount = adjusted.getAsInt();
		if (existingOrder == null) {
			if (!forward || itemsToOrder.size() >= 9 || newAmount <= 0) {
				return true;
			}
			existingOrder = new BigItemStack(fluidlogistics$copyOne(entry.stack), 0);
			itemsToOrder.add(existingOrder);
		}
		if (newAmount <= 0) {
			itemsToOrder.remove(existingOrder);
			return true;
		}

		existingOrder.count = newAmount;
		return true;
	}

	@Unique
	private int fluidlogistics$getResourceRecipeStepAmount(CraftableBigItemStack cbis) {
		if (!Screen.hasShiftDown() && !Screen.hasControlDown()) {
			return PackageResourceCrafting.get(cbis).map(PackageResourceCraftingData::outputCount).orElse(1);
		}
		return PackageResources.adjustAmount(cbis.stack,
			new PackageResourceDisplay.Adjustment(0, true, Screen.hasShiftDown(), Screen.hasControlDown(), 0,
				BigItemStack.INF, 1, PackageResourceDisplay.Interaction.STOCK_KEEPER_INVENTORY))
			.orElse(1);
	}

	@Unique
	private void fluidlogistics$handleCustomResourceCraftableRequest(CraftableBigItemStack cbis,
		int requestedDifference) {
		PackageResourceCraftingData data = PackageResourceCrafting.get(cbis).orElse(null);
		if (data == null) {
			return;
		}
		int outputCount = data.outputCount();
		if (outputCount <= 0) {
			return;
		}

		boolean takeOrdersAway = requestedDifference < 0;
		if (takeOrdersAway) {
			requestedDifference = Math.max(-cbis.count, requestedDifference);
		}
		if (requestedDifference == 0) {
			return;
		}

		int requestedSets = Mth.ceil(Math.abs(requestedDifference) / (float) outputCount);
		int applicableSets;

		if (takeOrdersAway) {
			applicableSets = Math.min(requestedSets, cbis.count / outputCount);
		} else {
			InventorySummary availableItems = blockEntity.getLastClientsideStockSnapshotAsSummary();
			if (availableItems == null) {
				return;
			}

			if (!fluidlogistics$canFitCustomRecipe(itemsToOrder, data.requirements())) {
				return;
			}

			applicableSets = fluidlogistics$getCustomCraftableSets(availableItems, itemsToOrder, data.requirements());
			applicableSets = Math.min(requestedSets, applicableSets);
		}

		if (applicableSets <= 0) {
			return;
		}

		int amountDelta = applicableSets * outputCount;
		cbis.count += takeOrdersAway ? -amountDelta : amountDelta;

		for (BigItemStack requirement : data.requirements()) {
			int delta = requirement.count * applicableSets;
			BigItemStack existingOrder = getOrderForItem(requirement.stack);

			if (takeOrdersAway) {
				if (existingOrder == null) {
					continue;
				}
				existingOrder.count -= delta;
				if (existingOrder.count <= 0) {
					itemsToOrder.remove(existingOrder);
				}
				continue;
			}

			if (existingOrder == null) {
				existingOrder = new BigItemStack(fluidlogistics$copyOne(requirement.stack), 0);
				itemsToOrder.add(existingOrder);
			}
			existingOrder.count += delta;
		}

		fluidlogistics$updateCraftableAmountsWithCustomEntries();
	}

	@Unique
	private static ItemStack fluidlogistics$copyOne(ItemStack stack) {
		ItemStack copy = stack.copy();
		copy.setCount(1);
		return copy;
	}
}
