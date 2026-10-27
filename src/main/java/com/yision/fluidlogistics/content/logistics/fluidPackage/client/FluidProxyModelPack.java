package com.yision.fluidlogistics.content.logistics.fluidPackage.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.content.logistics.fluidPackage.FluidProxyItem;
import com.yision.fluidlogistics.content.logistics.fluidPackage.FluidProxyRegistry;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.item.Item;

final class FluidProxyModelPack extends AbstractPackResources {
	private static final byte[] MODEL = "{\"parent\":\"fluidlogistics:item/fluid_proxy\"}"
		.getBytes(StandardCharsets.UTF_8);

	FluidProxyModelPack(String name) {
		super(name, true);
	}

	@Override
	public IoSupplier<InputStream> getRootResource(String... elements) {
		if (elements.length != 1 || !elements[0].equals(PACK_META)) {
			return null;
		}
		int format = SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES);
		byte[] metadata = ("{\"pack\":{\"pack_format\":" + format + ",\"description\":\"Fluid proxies\"}}")
			.getBytes(StandardCharsets.UTF_8);
		return () -> new ByteArrayInputStream(metadata);
	}

	@Override
	public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
		if (type != PackType.CLIENT_RESOURCES || !location.getNamespace().equals(FluidLogistics.MODID)
			|| !location.getPath().startsWith("models/item/fluid_proxy/") || !location.getPath().endsWith(".json")) {
			return null;
		}
		String path = location.getPath();
		ResourceLocation itemId = FluidLogistics.asResource(path.substring("models/item/".length(), path.length() - 5));
		if (!(BuiltInRegistries.ITEM.get(itemId) instanceof FluidProxyItem)) {
			return null;
		}
		return () -> new ByteArrayInputStream(MODEL);
	}

	@Override
	public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
		if (type != PackType.CLIENT_RESOURCES || !namespace.equals(FluidLogistics.MODID)) {
			return;
		}
		for (Item item : FluidProxyRegistry.items()) {
			ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
			ResourceLocation model = itemId.withPath("models/item/" + itemId.getPath() + ".json");
			if (path.isEmpty() || model.getPath().startsWith(path + "/")) {
				output.accept(model, () -> new ByteArrayInputStream(MODEL));
			}
		}
	}

	@Override
	public Set<String> getNamespaces(PackType type) {
		return type == PackType.CLIENT_RESOURCES ? Set.of(FluidLogistics.MODID) : Set.of();
	}

	@Override
	public void close() {
	}
}
