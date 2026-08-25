package com.yision.fluidlogistics.content.logistics.factoryGauge;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.foundation.utility.CreateLang;
import com.yision.fluidlogistics.api.packager.PackageResources;
import com.yision.fluidlogistics.api.packager.PackageResourceTypes;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@ApiStatus.Internal
public final class ResourceFactoryGaugeNbt {

    public static final String TYPE_KEY = "CFLGaugeType";
    public static final String STATE_KEY = "CFLResourceGauge";
    public static final String ENHANCEMENTS_VISIBLE_KEY = "EnhancementsVisible";
    public static final String LEGACY_KIND_KEY = "CFLGaugeKind";
    public static final String LEGACY_FLUID_KIND = "fluid";

    private ResourceFactoryGaugeNbt() {
        throw new AssertionError("This class should not be instantiated");
    }

    public static String slotTagName(PanelSlot slot) {
        return CreateLang.asId(slot.name());
    }

    @Nullable
    public static ResourceLocation determineTypeId(CompoundTag rawSlotTag, HolderLookup.Provider registries) {
        String typeIdString = rawSlotTag.getString(TYPE_KEY);
        if (!typeIdString.isEmpty()) {
            ResourceLocation typeId = ResourceLocation.tryParse(typeIdString);
            if (typeId != null)
                return typeId;
        }

        if (LEGACY_FLUID_KIND.equals(rawSlotTag.getString(LEGACY_KIND_KEY)))
            return builtinFluidGaugeId();

        ItemStack legacyFilter = FilterItemStack.of(registries, rawSlotTag.getCompound("Filter"))
            .item();
        if (!legacyFilter.isEmpty() && PackageResources.findType(legacyFilter)
            .map(type -> type.id()
                .equals(PackageResourceTypes.FLUID))
            .orElse(false))
            return builtinFluidGaugeId();

        return null;
    }

    public static ResourceLocation builtinFluidGaugeId() {
        return com.yision.fluidlogistics.FluidLogistics.asResource("fluid");
    }

    public static void writeTypeAndSettings(CompoundTag panelTag, ResourceLocation typeId,
        int restockThreshold, int promiseLimit, int additionalStock, boolean enhancementsVisible) {
        panelTag.putString(TYPE_KEY, typeId.toString());
        CompoundTag stateTag = new CompoundTag();
        stateTag.putInt("RestockThreshold", restockThreshold);
        stateTag.putInt("PromiseLimit", promiseLimit);
        stateTag.putInt("AdditionalStock", additionalStock);
        stateTag.putBoolean(ENHANCEMENTS_VISIBLE_KEY, enhancementsVisible);
        panelTag.put(STATE_KEY, stateTag);
    }

    public static void writeRuntimeState(CompoundTag panelTag, ResourceFactoryGaugeRuntime runtime) {
        CompoundTag stateTag = panelTag.getCompound(STATE_KEY);
        runtime.write(stateTag);
        panelTag.put(STATE_KEY, stateTag);
    }
}
