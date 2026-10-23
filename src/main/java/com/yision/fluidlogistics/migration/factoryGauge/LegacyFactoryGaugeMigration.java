package com.yision.fluidlogistics.migration.factoryGauge;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.api.packager.PackageResourceTypes;
import com.yision.fluidlogistics.api.packager.PackageResources;
import com.yision.fluidlogistics.content.logistics.factoryGauge.ResourceFactoryGaugeNbt;
import com.yision.fluidlogistics.content.logistics.factoryGauge.ResourceGaugeHelper;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class LegacyFactoryGaugeMigration {
	public static final String KIND_KEY = "CFLGaugeKind";
	public static final String FLUID_KIND = "fluid";

	private LegacyFactoryGaugeMigration() {
	}

	@Nullable
	public static ResourceLocation determineTypeId(CompoundTag rawSlotTag, HolderLookup.Provider registries) {
		if (FLUID_KIND.equals(rawSlotTag.getString(KIND_KEY))) {
			return FluidLogistics.asResource("fluid");
		}

		ItemStack legacyFilter = FilterItemStack.of(registries, rawSlotTag.getCompound("Filter")).item();
		if (!legacyFilter.isEmpty() && PackageResources.findType(legacyFilter)
			.map(type -> type.id().equals(PackageResourceTypes.FLUID))
			.orElse(false)) {
			return FluidLogistics.asResource("fluid");
		}

		return null;
	}

	public static CompoundTag readSettings(CompoundTag rawSlotTag) {
		CompoundTag stateTag = rawSlotTag.getCompound(ResourceFactoryGaugeNbt.STATE_KEY).copy();
		if (rawSlotTag.contains(ResourceGaugeHelper.RESTOCK_THRESHOLD_KEY, Tag.TAG_INT)) {
			stateTag.putInt("RestockThreshold", rawSlotTag.getInt(ResourceGaugeHelper.RESTOCK_THRESHOLD_KEY));
		}
		if (rawSlotTag.contains(ResourceGaugeHelper.PROMISE_LIMIT_KEY, Tag.TAG_INT)) {
			stateTag.putInt("PromiseLimit", rawSlotTag.getInt(ResourceGaugeHelper.PROMISE_LIMIT_KEY));
		}
		if (rawSlotTag.contains(ResourceGaugeHelper.ADDITIONAL_STOCK_KEY, Tag.TAG_INT)) {
			stateTag.putInt("AdditionalStock", rawSlotTag.getInt(ResourceGaugeHelper.ADDITIONAL_STOCK_KEY));
		}
		if (!stateTag.contains("RemainingAdditionalStock", Tag.TAG_INT)
			&& rawSlotTag.contains(ResourceGaugeHelper.REMAINING_ADDITIONAL_STOCK_KEY, Tag.TAG_INT)) {
			stateTag.putInt("RemainingAdditionalStock",
				rawSlotTag.getInt(ResourceGaugeHelper.REMAINING_ADDITIONAL_STOCK_KEY));
		}
		return stateTag;
	}
}
