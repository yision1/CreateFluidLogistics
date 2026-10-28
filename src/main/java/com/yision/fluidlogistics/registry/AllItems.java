package com.yision.fluidlogistics.registry;

import static com.yision.fluidlogistics.FluidLogistics.REGISTRATE;

import java.util.concurrent.ThreadLocalRandom;

import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.foundation.data.AssetLookup;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.api.factorygauge.FactoryGauges;
import com.yision.fluidlogistics.content.equipment.handPointer.HandPointerItem;
import com.yision.fluidlogistics.content.fluids.copperBucket.CopperBucketItem;
import com.yision.fluidlogistics.content.logistics.filter.PackageListFilterItem;
import com.yision.fluidlogistics.content.logistics.fluidPackage.FluidPackageItem;
import com.yision.fluidlogistics.content.processing.snowGolemFreezer.SnowGolemFreezerBlockItem;
import com.yision.fluidlogistics.content.schematics.FluidSchematicItem;
import com.yision.fluidlogistics.item.CompressedTankItem;
import com.yision.fluidlogistics.migration.item.LegacyItemAliases;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.common.Tags.Items;
import net.neoforged.neoforge.registries.RegisterEvent;

public class AllItems {
	public static final ResourceLocation FLUID_GAUGE_TYPE_ID = FluidLogistics.asResource("fluid");
	private static final int CREATE_RARE_PACKAGE_CHANCE = 7500;

	public static final ItemEntry<Item> FLUID_FACTORY_GAUGE = REGISTRATE
		.item("fluid_factory_gauge", properties -> FactoryGauges.createItem(FLUID_GAUGE_TYPE_ID, properties))
		.model(AssetLookup.itemModelWithPartials())
		.setData(ProviderType.LANG, NonNullBiConsumer.noop())
		.register();

	public static final ItemEntry<Item> EMPTY_FLUID_SCHEMATIC = REGISTRATE
		.item("empty_fluid_schematic", Item::new)
		.properties(p -> p.stacksTo(1))
		.model((context, provider) -> provider.withExistingParent(context.getName(),
			provider.modLoc("item/fluid_schematic/" + context.getName())))
		.setData(ProviderType.LANG, NonNullBiConsumer.noop())
		.register();

	public static final ItemEntry<FluidSchematicItem> FLUID_SCHEMATIC = REGISTRATE
		.item("fluid_schematic", FluidSchematicItem::new)
		.properties(p -> p.stacksTo(1))
		.model((context, provider) -> provider.withExistingParent(context.getName(),
			provider.modLoc("item/fluid_schematic/" + context.getName())))
		.setData(ProviderType.LANG, NonNullBiConsumer.noop())
		.register();

	public static final ItemEntry<CopperBucketItem> COPPER_BUCKET = REGISTRATE
		.item("copper_bucket", CopperBucketItem::new)
		.properties(p -> p.stacksTo(1))
		.model(AssetLookup.itemModelWithPartials())
		.setData(ProviderType.LANG, NonNullBiConsumer.noop())
		.register();

	public static final ItemEntry<CompressedTankItem> COMPRESSED_STORAGE_TANK = REGISTRATE
		.item("compressed_storage_tank", CompressedTankItem::new)
		.removeTab(ResourceKey.create(Registries.CREATIVE_MODE_TAB, FluidLogistics.asResource("fluidlogistics_tab")))
		.properties(p -> p.stacksTo(1))
		.model(AssetLookup.itemModelWithPartials())
		.setData(ProviderType.LANG, NonNullBiConsumer.noop())
		.register();

	public static final ItemEntry<FluidPackageItem> FLUID_PACKAGE = REGISTRATE
		.item("fluid_package", FluidPackageItem::new)
		.properties(p -> p.stacksTo(1))
		.tag(AllItemTags.PACKAGES.tag, AllItemTags.NOT_UPRIGHT_ON_BELT.tag)
		.model((context, provider) -> provider.withExistingParent(context.getName(),
			provider.modLoc("item/package/" + context.getName())))
		.setData(ProviderType.LANG, NonNullBiConsumer.noop())
		.register();

	public static final ItemEntry<FluidPackageItem> FLUID_PACKAGE_EXPOSED = REGISTRATE
		.item("fluid_package_exposed",
			properties -> new FluidPackageItem(properties, FluidPackageItem.FLUID_EXPOSED_STYLE))
		.removeTab(ResourceKey.create(Registries.CREATIVE_MODE_TAB, FluidLogistics.asResource("fluidlogistics_tab")))
		.properties(p -> p.stacksTo(1))
		.tag(AllItemTags.PACKAGES.tag, AllItemTags.NOT_UPRIGHT_ON_BELT.tag)
		.model((context, provider) -> provider.withExistingParent(context.getName(),
			provider.modLoc("item/package/" + context.getName())))
		.setData(ProviderType.LANG, NonNullBiConsumer.noop())
		.register();

	public static final ItemEntry<FluidPackageItem> FLUID_PACKAGE_OXIDIZED = REGISTRATE
		.item("fluid_package_oxidized",
			properties -> new FluidPackageItem(properties, FluidPackageItem.FLUID_OXIDIZED_STYLE))
		.removeTab(ResourceKey.create(Registries.CREATIVE_MODE_TAB, FluidLogistics.asResource("fluidlogistics_tab")))
		.properties(p -> p.stacksTo(1))
		.tag(AllItemTags.PACKAGES.tag, AllItemTags.NOT_UPRIGHT_ON_BELT.tag)
		.model((context, provider) -> provider.withExistingParent(context.getName(),
			provider.modLoc("item/package/" + context.getName())))
		.setData(ProviderType.LANG, NonNullBiConsumer.noop())
		.register();

	public static final ItemEntry<FluidPackageItem> FLUID_PACKAGE_WEATHERED = REGISTRATE
		.item("fluid_package_weathered",
			properties -> new FluidPackageItem(properties, FluidPackageItem.FLUID_WEATHERED_STYLE))
		.removeTab(ResourceKey.create(Registries.CREATIVE_MODE_TAB, FluidLogistics.asResource("fluidlogistics_tab")))
		.properties(p -> p.stacksTo(1))
		.tag(AllItemTags.PACKAGES.tag, AllItemTags.NOT_UPRIGHT_ON_BELT.tag)
		.model((context, provider) -> provider.withExistingParent(context.getName(),
			provider.modLoc("item/package/" + context.getName())))
		.setData(ProviderType.LANG, NonNullBiConsumer.noop())
		.register();

	public static final ItemEntry<FluidPackageItem> RARE_PIG_PACKAGE = REGISTRATE
		.item("rare_pig_package",
			properties -> new FluidPackageItem(properties, FluidPackageItem.RARE_PIG_STYLE, false))
		.removeTab(ResourceKey.create(Registries.CREATIVE_MODE_TAB, FluidLogistics.asResource("fluidlogistics_tab")))
		.properties(p -> p.stacksTo(1))
		.tag(AllItemTags.PACKAGES.tag, AllItemTags.NOT_UPRIGHT_ON_BELT.tag)
		.model((context, provider) -> provider.withExistingParent(context.getName(),
			provider.modLoc("item/package/" + context.getName())))
		.setData(ProviderType.LANG, NonNullBiConsumer.noop())
		.register();

	public static final ItemEntry<FluidPackageItem> RARE_FOX_PACKAGE = REGISTRATE
		.item("rare_fox_package",
			properties -> new FluidPackageItem(properties, FluidPackageItem.RARE_FOX_STYLE, false))
		.removeTab(ResourceKey.create(Registries.CREATIVE_MODE_TAB, FluidLogistics.asResource("fluidlogistics_tab")))
		.properties(p -> p.stacksTo(1))
		.tag(AllItemTags.PACKAGES.tag, AllItemTags.NOT_UPRIGHT_ON_BELT.tag)
		.model((context, provider) -> provider.withExistingParent(context.getName(),
			provider.modLoc("item/package/" + context.getName())))
		.setData(ProviderType.LANG, NonNullBiConsumer.noop())
		.register();

	public static final ItemEntry<PackageListFilterItem> PACKAGE_LIST_FILTER = REGISTRATE
		.item("package_list_filter", PackageListFilterItem::new)
		.model(AssetLookup.itemModelWithPartials())
		.setData(ProviderType.LANG, NonNullBiConsumer.noop())
		.register();

	public static final ItemEntry<HandPointerItem> HAND_POINTER = REGISTRATE
		.item("hand_pointer", HandPointerItem::new)
		.properties(p -> p.stacksTo(1))
		.tag(Items.TOOLS)
		.model(AssetLookup.itemModelWithPartials())
		.setData(ProviderType.LANG, NonNullBiConsumer.noop())
		.register();

	public static final ItemEntry<Item> PHANTOM_CHAIN = REGISTRATE
		.item("phantom_chain", Item::new)
		.model(AssetLookup.itemModelWithPartials())
		.setData(ProviderType.LANG, NonNullBiConsumer.noop())
		.register();

	public static final ItemEntry<SnowGolemFreezerBlockItem> EMPTY_SNOW_GOLEM_FREEZER = REGISTRATE
		.item("empty_snow_golem_freezer", SnowGolemFreezerBlockItem::empty)
		.model((context, provider) -> provider.withExistingParent(context.getName(),
			provider.modLoc("block/snow_golem_freezer/block")))
		.setData(ProviderType.LANG, NonNullBiConsumer.noop())
		.register();

	public static ItemStack createFluidPackage() {
		if (ThreadLocalRandom.current().nextInt(CREATE_RARE_PACKAGE_CHANCE) == 0) {
			return new ItemStack(ThreadLocalRandom.current().nextBoolean()
				? RARE_PIG_PACKAGE.get()
				: RARE_FOX_PACKAGE.get());
		}

		int roll = ThreadLocalRandom.current().nextInt(200);
		Item fluidPackage = roll < 180 ? FLUID_PACKAGE.get()
			: roll < 190 ? FLUID_PACKAGE_EXPOSED.get()
				: roll < 195 ? FLUID_PACKAGE_WEATHERED.get()
					: FLUID_PACKAGE_OXIDIZED.get();
		return new ItemStack(fluidPackage);
	}

	public static void registerAliases(RegisterEvent event) {
		LegacyItemAliases.register(event);
	}

	public static void register() {
	}
}
