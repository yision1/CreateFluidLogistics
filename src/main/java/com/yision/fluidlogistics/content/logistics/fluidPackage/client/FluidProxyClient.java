package com.yision.fluidlogistics.content.logistics.fluidPackage.client;

import java.util.Optional;

import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.content.logistics.fluidPackage.FluidProxyRegistry;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.item.Item;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;

@EventBusSubscriber(modid = FluidLogistics.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class FluidProxyClient {
	private FluidProxyClient() {
	}

	@SubscribeEvent
	static void registerExtensions(RegisterClientExtensionsEvent event) {
		FluidProxyItemRenderer renderer = new FluidProxyItemRenderer();
		event.registerItem(new IClientItemExtensions() {
			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				return renderer;
			}
		}, FluidProxyRegistry.items().toArray(Item[]::new));
	}

	@SubscribeEvent
	static void addModelPack(AddPackFindersEvent event) {
		if (event.getPackType() != PackType.CLIENT_RESOURCES) {
			return;
		}
		PackLocationInfo location = new PackLocationInfo("fluidlogistics:fluid_proxy_models",
			Component.literal("Fluid proxies"), PackSource.BUILT_IN, Optional.empty());
		Pack.ResourcesSupplier resources = new Pack.ResourcesSupplier() {
			@Override
			public PackResources openPrimary(PackLocationInfo info) {
				return new FluidProxyModelPack(info);
			}

			@Override
			public PackResources openFull(PackLocationInfo info, Pack.Metadata metadata) {
				return new FluidProxyModelPack(info);
			}
		};
		Pack pack = Pack.readMetaAndCreate(location, resources, PackType.CLIENT_RESOURCES,
			new PackSelectionConfig(true, Pack.Position.BOTTOM, false));
		if (pack != null) {
			event.addRepositorySource(consumer -> consumer.accept(pack.hidden()));
		}
	}
}
