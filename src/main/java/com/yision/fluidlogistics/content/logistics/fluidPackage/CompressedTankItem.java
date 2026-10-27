package com.yision.fluidlogistics.content.logistics.fluidPackage;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import com.yision.fluidlogistics.FluidLogistics;
import com.yision.fluidlogistics.config.Config;
import com.yision.fluidlogistics.content.logistics.fluidPackage.client.CompressedTankItemRenderer;
import com.yision.fluidlogistics.migration.item.LegacyCompressedTankMigration;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

public class CompressedTankItem extends Item {

	private static final String TAG_FLUID = "Fluid";

	public CompressedTankItem(Properties properties) {
		super(properties);
	}

	public static FluidStack getFluid(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		if (tag == null || !tag.contains(TAG_FLUID)) {
			return FluidStack.EMPTY;
		}
		return FluidStack.loadFluidStackFromNBT(tag.getCompound(TAG_FLUID));
	}

	public static void setFluid(ItemStack stack, FluidStack fluid) {
		int capacity = matchesFluid(stack, fluid) ? Math.max(getCapacity(), getFluid(stack).getAmount())
			: getCapacity();
		if (!fluid.isEmpty() && fluid.getAmount() > capacity) {
			throw new IllegalArgumentException("compressed tank capacity is " + getCapacity() + " mB");
		}
		stack.getOrCreateTag().put(TAG_FLUID, fluid.writeToNBT(new CompoundTag()));
	}

	public static boolean isFluidStack(ItemStack stack) {
		return stack.getItem() instanceof CompressedTankItem && !getFluid(stack).isEmpty();
	}

	public static boolean matchesFluid(ItemStack stack, FluidStack fluid) {
		if (fluid.isEmpty() || !isFluidStack(stack)) {
			return false;
		}
		FluidStack stored = getFluid(stack);
		return stored.isFluidEqual(fluid) && FluidStack.areFluidStackTagsEqual(stored, fluid);
	}

	public static int getCapacity() {
		return Config.getFluidPerPackage();
	}

	@Override
	public Component getName(ItemStack stack) {
		FluidStack fluid = getFluid(stack);
		if (!fluid.isEmpty()) {
			return fluid.getDisplayName();
		}
		return super.getName(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents,
		TooltipFlag tooltipFlag) {
		super.appendHoverText(stack, level, tooltipComponents, tooltipFlag);
		FluidStack fluid = getFluid(stack);
		if (fluid.isEmpty()) {
			return;
		}

		tooltipComponents
			.add(Component.literal(fluid.getAmount() + " / " + getCapacity() + " mB").withStyle(ChatFormatting.GRAY));
	}

	@Override
	public String getCreatorModId(ItemStack stack) {
		FluidStack fluid = getFluid(stack);
		if (!fluid.isEmpty()) {
			ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
			if (fluidId != null) {
				return fluidId.getNamespace();
			}
		}
		return FluidLogistics.MODID;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(SimpleCustomRenderer.create(this, new CompressedTankItemRenderer()));
	}

	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
		return new ICapabilityProvider() {
			private final LazyOptional<IFluidHandlerItem> fluidHandler = LazyOptional
				.of(() -> new CompressedTankFluidHandler(stack));

			@Override
			public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
				return cap == ForgeCapabilities.FLUID_HANDLER_ITEM ? fluidHandler.cast() : LazyOptional.empty();
			}
		};
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
		return LegacyCompressedTankMigration.use(level, player, usedHand);
	}
}
