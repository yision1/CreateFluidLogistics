package com.yision.fluidlogistics.content.logistics.fluidPackage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.yision.fluidlogistics.FluidLogistics;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.RegisterEvent;

public final class FluidProxyRegistry {
	private static final Map<Fluid, FluidProxyItem> PROXIES = new IdentityHashMap<>();
	private static Collection<Item> items = List.of();

	private FluidProxyRegistry() {
	}

	public static void register(RegisterEvent event) {
		event.register(Registries.ITEM, helper -> {
			Map<Fluid, FluidProxyItem> sources = new IdentityHashMap<>();
			List<Item> registeredItems = new ArrayList<>();
			for (ResourceLocation fluidId : BuiltInRegistries.FLUID.keySet().stream()
				.sorted(java.util.Comparator.comparing(ResourceLocation::toString)).toList()) {
				Fluid fluid = BuiltInRegistries.FLUID.get(fluidId);
				if (fluid == Fluids.EMPTY) {
					continue;
				}
				Fluid source = canonicalFluid(fluid);
				FluidProxyItem proxy = sources.get(source);
				if (proxy == null) {
					ResourceLocation sourceId = BuiltInRegistries.FLUID.getKey(source);
					proxy = new FluidProxyItem(source);
					helper.register(
						FluidLogistics.asResource("fluid_proxy/" + sourceId.getNamespace() + "/" + sourceId.getPath()),
						proxy);
					sources.put(source, proxy);
					registeredItems.add(proxy);
				}
				PROXIES.put(fluid, proxy);
			}
			items = List.copyOf(registeredItems);
		});
	}

	public static ItemStack createKey(FluidStack fluid) {
		if (fluid.isEmpty()) {
			return ItemStack.EMPTY;
		}
		FluidProxyItem item = PROXIES.get(fluid.getFluid());
		if (item == null) {
			throw new IllegalArgumentException(
				"No fluid proxy registered for " + BuiltInRegistries.FLUID.getKey(fluid.getFluid()));
		}
		ItemStack key = new ItemStack(item);
		if (fluid.hasTag()) {
			key.getOrCreateTag().put("FluidProxyTag", fluid.getTag().copy());
		}
		return key;
	}

	public static FluidStack getFluid(ItemStack stack) {
		if (!isProxy(stack)) {
			return FluidStack.EMPTY;
		}
		FluidProxyItem item = (FluidProxyItem) stack.getItem();
		FluidStack fluid = new FluidStack(item.getFluid(), 1);
		if (stack.hasTag() && stack.getTag().contains("FluidProxyTag")) {
			fluid.setTag(stack.getTag().getCompound("FluidProxyTag").copy());
		}
		return fluid;
	}

	public static boolean isProxy(ItemStack stack) {
		return !stack.isEmpty() && stack.getItem() instanceof FluidProxyItem;
	}

	public static Collection<Item> items() {
		return items;
	}

	private static Fluid canonicalFluid(Fluid fluid) {
		if (!(fluid instanceof FlowingFluid flowing)) {
			return fluid;
		}
		Fluid source = flowing.getSource();
		Fluid flow = flowing.getFlowing();
		if (source instanceof FlowingFluid sourceFlow && source != Fluids.EMPTY
			&& BuiltInRegistries.FLUID.getResourceKey(source).isPresent()
			&& BuiltInRegistries.FLUID.getResourceKey(flow).isPresent() && sourceFlow.getSource() == source
			&& sourceFlow.getFlowing() == flow && (fluid == source || fluid == flow)) {
			return source;
		}
		return fluid;
	}
}
