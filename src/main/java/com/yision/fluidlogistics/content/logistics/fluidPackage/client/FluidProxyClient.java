package com.yision.fluidlogistics.content.logistics.fluidPackage.client;

import com.yision.fluidlogistics.FluidLogistics;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = FluidLogistics.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class FluidProxyClient {
	private FluidProxyClient() {
	}

	@SubscribeEvent
	static void addModelPack(AddPackFindersEvent event) {
		if (event.getPackType() != PackType.CLIENT_RESOURCES)
			return;
		Pack pack = Pack.readMetaAndCreate("fluidlogistics:fluid_proxy_models", Component.literal("Fluid proxies"),
			true, FluidProxyModelPack::new, PackType.CLIENT_RESOURCES, Pack.Position.BOTTOM, PackSource.BUILT_IN);
		if (pack != null)
			event.addRepositorySource(consumer -> consumer.accept(pack));
	}
}
