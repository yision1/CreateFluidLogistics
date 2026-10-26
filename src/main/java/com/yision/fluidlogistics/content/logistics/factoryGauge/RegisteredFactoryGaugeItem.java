package com.yision.fluidlogistics.content.logistics.factoryGauge;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockItem;
import com.yision.fluidlogistics.api.factorygauge.FactoryGauges;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class RegisteredFactoryGaugeItem extends FactoryPanelBlockItem {

	private final ResourceLocation gaugeTypeId;

	public RegisteredFactoryGaugeItem(ResourceLocation gaugeTypeId, Item.Properties properties) {
		super(getFactoryGaugeBlock(), properties);
		this.gaugeTypeId = gaugeTypeId;
	}

	public ResourceLocation gaugeTypeId() {
		return gaugeTypeId;
	}

	@Override
	@Nullable
	protected BlockState getPlacementState(BlockPlaceContext context) {
		BlockState state = context.getLevel().getBlockState(context.getClickedPos());
		if (state.is(getBlock()))
			return canPlace(context, state) ? state : null;
		return super.getPlacementState(context);
	}

	@Override
	public InteractionResult place(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		if (context.getLevel().getBlockEntity(pos) instanceof FactoryPanelBlockEntity)
			return FactoryGauges.get(gaugeTypeId)
				.map(type -> FactoryGaugeHostHooks.useRegisteredGaugeOnExistingHost(type, context, pos)
					.result())
				.orElse(InteractionResult.FAIL);

		InteractionResult result = super.place(context);
		if (!result.consumesAction())
			return result;

		if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof FactoryPanelBlockEntity be))
			return result;

		for (PanelSlot slot : PanelSlot.values()) {
			FactoryPanelBehaviour behaviour = be.panels.get(slot);
			if (behaviour == null || !behaviour.isActive())
				continue;

			ResourceFactoryPanelBehaviour resource = FactoryGaugeHostHooks.ensureResourceBehaviour(be, slot);
			if (resource.isResourceGauge())
				continue;

			resource.setGaugeTypeId(gaugeTypeId);
			be.notifyUpdate();
			break;
		}

		return result;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		InteractionResult result = super.useOn(context);
		BlockPos hostPos = FactoryGaugeHostHooks.existingHostAfterFailedPlacement(result, context);
		if (hostPos == null)
			return result;

		return FactoryGauges.get(gaugeTypeId)
			.map(type -> FactoryGaugeHostHooks.useRegisteredGaugeOnExistingHost(type, context, hostPos)
				.result())
			.orElse(result);
	}

	@Override
	public void registerBlocks(Map<Block, Item> blockToItem, Item self) {
	}

	@Override
	public String getDescriptionId() {
		return Util.makeDescriptionId("item", BuiltInRegistries.ITEM.getKey(this));
	}

	private static Block getFactoryGaugeBlock() {
		return AllBlocks.FACTORY_GAUGE.get();
	}
}
