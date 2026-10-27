package com.yision.fluidlogistics.content.processing.snowGolemFreezer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.network.FluidLogisticsPackets;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = FluidLogistics.MODID)
public final class SnowGolemFreezerFuelManager extends SimpleJsonResourceReloadListener {

	private static final Gson GSON = new Gson();
	private static final String DIRECTORY = "snow_golem_freezer_fuels";
	public static final SnowGolemFreezerFuelManager INSTANCE = new SnowGolemFreezerFuelManager();

	private static volatile Snapshot fuels = Snapshot.EMPTY;

	private SnowGolemFreezerFuelManager() {
		super(GSON, DIRECTORY);
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager,
		ProfilerFiller profiler) {
		Map<Item, Fuel> items = new HashMap<>();
		Map<Fluid, Fuel> fluids = new HashMap<>();
		List<ItemTagFuel> itemTags = new ArrayList<>();
		List<FluidTagFuel> fluidTags = new ArrayList<>();

		resources.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
			try {
				readRule(entry.getKey(), entry.getValue(), items, fluids, itemTags, fluidTags);
			} catch (RuntimeException exception) {
				FluidLogistics.LOGGER.error("Invalid snow golem freezer fuel definition {}: {}", entry.getKey(),
					exception.getMessage());
			}
		});

		fuels = new Snapshot(Map.copyOf(items), Map.copyOf(fluids), List.copyOf(itemTags), List.copyOf(fluidTags));
		FluidLogistics.LOGGER.info("Loaded {} snow golem freezer fuel definitions", resources.size());
	}

	public static @Nullable Fuel find(ItemStack stack) {
		if (stack.isEmpty())
			return null;

		Snapshot snapshot = fuels;
		Fuel direct = snapshot.items().get(stack.getItem());
		if (direct != null)
			return direct;
		for (ItemTagFuel tagged : snapshot.itemTags())
			if (stack.is(tagged.tag()))
				return tagged.fuel();
		return null;
	}

	public static @Nullable Fuel find(FluidStack stack) {
		if (stack.isEmpty())
			return null;

		Snapshot snapshot = fuels;
		Fluid fluid = stack.getFluid();
		Fuel direct = snapshot.fluids().get(fluid);
		if (direct != null)
			return direct;
		for (FluidTagFuel tagged : snapshot.fluidTags())
			if (fluid.is(tagged.tag()))
				return tagged.fuel();
		return null;
	}

	private static void readRule(ResourceLocation definition, JsonElement element, Map<Item, Fuel> items,
		Map<Fluid, Fuel> fluids, List<ItemTagFuel> itemTags, List<FluidTagFuel> fluidTags) {
		if (!element.isJsonObject())
			throw new IllegalArgumentException("root must be an object");

		JsonObject object = element.getAsJsonObject();
		if (!object.has("type"))
			throw new IllegalArgumentException("missing type");

		boolean hasId = object.has("id");
		boolean hasTag = object.has("tag");
		if (hasId == hasTag)
			throw new IllegalArgumentException("exactly one id or tag is required");

		String type = object.get("type").getAsString();
		ResourceLocation target = new ResourceLocation(object.get(hasId ? "id" : "tag").getAsString());
		int coolTime = positiveInt(object, "cool_time");
		boolean fluidRule = type.equals("fluid");
		int amount = fluidRule ? positiveInt(object, "amount") : 1;
		if (amount > FluidType.BUCKET_VOLUME)
			throw new IllegalArgumentException("amount exceeds the 1B tank capacity");

		Fuel fuel = new Fuel(coolTime, amount);
		switch (type) {
		case "item" -> {
			if (hasId)
				items.put(requiredItem(definition, target), fuel);
			else
				itemTags.add(new ItemTagFuel(TagKey.create(Registries.ITEM, target), fuel));
		}
		case "fluid" -> {
			if (hasId)
				fluids.put(requiredFluid(definition, target), fuel);
			else
				fluidTags.add(new FluidTagFuel(TagKey.create(Registries.FLUID, target), fuel));
		}
		default -> throw new IllegalArgumentException("type must be item or fluid");
		}
	}

	private static int positiveInt(JsonObject object, String name) {
		if (!object.has(name))
			throw new IllegalArgumentException("missing " + name);
		int value = object.get(name).getAsInt();
		if (value <= 0)
			throw new IllegalArgumentException(name + " must be positive");
		return value;
	}

	private static Item requiredItem(ResourceLocation definition, ResourceLocation target) {
		return BuiltInRegistries.ITEM.getOptional(target)
			.orElseThrow(() -> new IllegalArgumentException(definition + " references unknown item " + target));
	}

	private static Fluid requiredFluid(ResourceLocation definition, ResourceLocation target) {
		return BuiltInRegistries.FLUID.getOptional(target)
			.orElseThrow(() -> new IllegalArgumentException(definition + " references unknown fluid " + target));
	}

	public record Fuel(int coolTime, int amount) {
	}

	private record ItemTagFuel(TagKey<Item> tag, Fuel fuel) {
	}

	private record FluidTagFuel(TagKey<Fluid> tag, Fuel fuel) {
	}

	private record Snapshot(Map<Item, Fuel> items, Map<Fluid, Fuel> fluids, List<ItemTagFuel> itemTags,
		List<FluidTagFuel> fluidTags) {
		private static final Snapshot EMPTY = new Snapshot(Map.of(), Map.of(), List.of(), List.of());
	}

	@SubscribeEvent
	public static void synchronizeFuels(OnDatapackSyncEvent event) {
		var target = event.getPlayer() == null ? PacketDistributor.ALL.noArg()
			: PacketDistributor.PLAYER.with(event::getPlayer);
		FluidLogisticsPackets.getChannel().send(target, new SyncPacket(fuels));
	}

	public static final class SyncPacket extends SimplePacketBase {
		private final Snapshot snapshot;

		private SyncPacket(Snapshot snapshot) {
			this.snapshot = snapshot;
		}

		public SyncPacket(FriendlyByteBuf buffer) {
			Map<Item, Fuel> items = buffer.readMap(buf -> BuiltInRegistries.ITEM.get(buf.readResourceLocation()),
				SyncPacket::readFuel);
			Map<Fluid, Fuel> fluids = buffer.readMap(buf -> BuiltInRegistries.FLUID.get(buf.readResourceLocation()),
				SyncPacket::readFuel);
			List<ItemTagFuel> itemTags = buffer.readList(
				buf -> new ItemTagFuel(TagKey.create(Registries.ITEM, buf.readResourceLocation()), readFuel(buf)));
			List<FluidTagFuel> fluidTags = buffer.readList(
				buf -> new FluidTagFuel(TagKey.create(Registries.FLUID, buf.readResourceLocation()), readFuel(buf)));
			snapshot = new Snapshot(Map.copyOf(items), Map.copyOf(fluids), List.copyOf(itemTags),
				List.copyOf(fluidTags));
		}

		@Override
		public void write(FriendlyByteBuf buffer) {
			buffer.writeMap(snapshot.items(),
				(buf, item) -> buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(item)), SyncPacket::writeFuel);
			buffer.writeMap(snapshot.fluids(),
				(buf, fluid) -> buf.writeResourceLocation(BuiltInRegistries.FLUID.getKey(fluid)),
				SyncPacket::writeFuel);
			buffer.writeCollection(snapshot.itemTags(), (buf, tagged) -> {
				buf.writeResourceLocation(tagged.tag().location());
				writeFuel(buf, tagged.fuel());
			});
			buffer.writeCollection(snapshot.fluidTags(), (buf, tagged) -> {
				buf.writeResourceLocation(tagged.tag().location());
				writeFuel(buf, tagged.fuel());
			});
		}

		@Override
		public boolean handle(NetworkEvent.Context context) {
			context.enqueueWork(() -> fuels = snapshot);
			return true;
		}

		private static Fuel readFuel(FriendlyByteBuf buffer) {
			return new Fuel(buffer.readVarInt(), buffer.readVarInt());
		}

		private static void writeFuel(FriendlyByteBuf buffer, Fuel fuel) {
			buffer.writeVarInt(fuel.coolTime());
			buffer.writeVarInt(fuel.amount());
		}
	}
}
