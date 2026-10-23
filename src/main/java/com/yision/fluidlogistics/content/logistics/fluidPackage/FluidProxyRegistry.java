package com.yision.fluidlogistics.content.logistics.fluidPackage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.registry.AllDataComponents;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.RegisterEvent;

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
				.sorted(ResourceLocation::compareNamespaced).toList()) {
				Fluid fluid = BuiltInRegistries.FLUID.get(fluidId);
				if (fluid == Fluids.EMPTY) {
					continue;
				}
				Fluid source = canonicalFluid(fluid);
				FluidProxyItem proxy = sources.get(source);
				if (proxy == null) {
					ResourceLocation sourceId = BuiltInRegistries.FLUID.getKey(source);
					proxy = new FluidProxyItem(source);
					helper.register(FluidLogistics.asResource(
						"fluid_proxy/" + sourceId.getNamespace() + "/" + sourceId.getPath()), proxy);
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
			throw new IllegalArgumentException("No fluid proxy registered for "
				+ BuiltInRegistries.FLUID.getKey(fluid.getFluid()));
		}
		FluidStack normalized = new FluidStack(item.getFluid(), 1);
		normalized.applyComponents(fluid.getComponents());
		DataComponentPatch components = normalized.getComponentsPatch();
		ItemStack key = new ItemStack(item);
		if (!components.isEmpty()) {
			key.set(AllDataComponents.FLUID_PROXY_COMPONENTS, components);
		}
		return key;
	}

	public static FluidStack getFluid(ItemStack stack) {
		if (!isProxy(stack)) {
			return FluidStack.EMPTY;
		}
		FluidProxyItem item = (FluidProxyItem) stack.getItem();
		FluidStack fluid = new FluidStack(item.getFluid(), 1);
		fluid.applyComponents(stack.getOrDefault(AllDataComponents.FLUID_PROXY_COMPONENTS, DataComponentPatch.EMPTY));
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
		if (source instanceof FlowingFluid sourceFlow
			&& source != Fluids.EMPTY
			&& BuiltInRegistries.FLUID.containsValue(source)
			&& BuiltInRegistries.FLUID.containsValue(flow)
			&& sourceFlow.getSource() == source
			&& sourceFlow.getFlowing() == flow
			&& (fluid == source || fluid == flow)) {
			return source;
		}
		return fluid;
	}
}
