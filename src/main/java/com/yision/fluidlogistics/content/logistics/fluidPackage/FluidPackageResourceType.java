package com.yision.fluidlogistics.content.logistics.fluidPackage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.jetbrains.annotations.ApiStatus;

import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.yision.fluidlogistics.api.packager.PackageDestroyContext;
import com.yision.fluidlogistics.api.packager.PackageInspection;
import com.yision.fluidlogistics.api.packager.PackageResource;
import com.yision.fluidlogistics.api.packager.PackageResourceDisplay;
import com.yision.fluidlogistics.api.packager.PackageResourceGoggleInformation;
import com.yision.fluidlogistics.api.packager.PackageResourceType;
import com.yision.fluidlogistics.api.packager.PackageResourceTypes;
import com.yision.fluidlogistics.api.packager.PackageResources;
import com.yision.fluidlogistics.api.packager.PackageUnpackContext;
import com.yision.fluidlogistics.compat.createenchantmentindustry.CreateEnchantmentIndustryCompat;
import com.yision.fluidlogistics.config.Config;
import com.yision.fluidlogistics.datacomponent.FluidTankContent;
import com.yision.fluidlogistics.datacomponent.PackageFluidContents;
import com.yision.fluidlogistics.migration.packageData.LegacyFluidPackageMigration;
import com.yision.fluidlogistics.registry.AllItems;
import com.yision.fluidlogistics.util.FluidAmountHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandler;

@ApiStatus.Internal
public final class FluidPackageResourceType {
	private static final PackageResourceType FLUID_TYPE = new FluidResourceType();
	private static boolean builtInsRegistered;

	private FluidPackageResourceType() {
		throw new AssertionError("This class should not be instantiated");
	}

	public static PackageResourceType fluid() {
		return FLUID_TYPE;
	}

	public static ItemStack createFluidKey(FluidStack fluid) {
		if (fluid == null || fluid.isEmpty()) {
			return ItemStack.EMPTY;
		}
		return FluidProxyRegistry.createKey(fluid);
	}

	public static FluidStack getFluid(ItemStack carrierOrKey) {
		if (!FLUID_TYPE.isValidCarrier(carrierOrKey)) {
			return FluidStack.EMPTY;
		}
		return FluidProxyRegistry.isProxy(carrierOrKey) ? FluidProxyRegistry.getFluid(carrierOrKey)
			: FluidHelper.copyStackWithAmount(CompressedTankItem.getFluid(carrierOrKey), 1);
	}

	@ApiStatus.Internal
	public static synchronized void registerBuiltIns() {
		if (builtInsRegistered) {
			return;
		}
		PackageResources.register(FLUID_TYPE);
		builtInsRegistered = true;
	}

	public static int getFluidPerPackage() {
		return Config.getFluidPerPackage();
	}

	private static final class FluidResourceType implements PackageResourceType, PackageResourceGoggleInformation {
		private static final PackageResourceDisplay DISPLAY = new FluidResourceDisplay();

		@Override
		public ResourceLocation id() {
			return PackageResourceTypes.FLUID;
		}

		@Override
		public Supplier<? extends Item> carrierItem() {
			return AllItems.COMPRESSED_STORAGE_TANK;
		}

		@Override
		public ItemStack createCarrier(ItemStack key, int amount) {
			if (amount <= 0 || amount > maxPerPackage(key)) {
				throw new IllegalArgumentException("invalid fluid amount");
			}
			ItemStack carrier = new ItemStack(AllItems.COMPRESSED_STORAGE_TANK.get());
			CompressedTankItem.setFluid(carrier, FluidHelper.copyStackWithAmount(getFluid(key), amount));
			return carrier;
		}

		@Override
		public boolean unpack(PackageUnpackContext context, PackageResource resource, boolean simulate) {
			if (unpackAmount(context, resource, true) != resource.amount()) {
				return false;
			}
			return simulate || unpackAmount(context, resource, false) == resource.amount();
		}

		@Override
		public Collection<Item> keyItems() {
			List<Item> items = new ArrayList<>(FluidProxyRegistry.items());
			items.add(AllItems.COMPRESSED_STORAGE_TANK.get());
			return items;
		}

		@Override
		public boolean isValidCarrier(ItemStack stack) {
			return stack != null && (FluidProxyRegistry.isProxy(stack) || CompressedTankItem.isFluidStack(stack));
		}

		@Override
		public boolean isPhysicalCarrier(ItemStack stack) {
			return CompressedTankItem.isFluidStack(stack);
		}

		@Override
		public ItemStack normalizeKey(ItemStack stack) {
			if (!isValidCarrier(stack)) {
				throw new IllegalArgumentException("invalid fluid resource carrier");
			}
			return FluidProxyRegistry.createKey(getFluid(stack));
		}

		@Override
		public boolean matches(ItemStack firstNormalizedKey, ItemStack secondNormalizedKey) {
			if (!isValidCarrier(firstNormalizedKey) || !isValidCarrier(secondNormalizedKey)) {
				return false;
			}
			return getFluid(firstNormalizedKey).isFluidEqual(getFluid(secondNormalizedKey));
		}

		@Override
		public int identityHash(ItemStack normalizedKey) {
			ItemStack key = normalizeKey(normalizedKey);
			return Objects.hash(key.getItem(), key.getTag());
		}

		@Override
		public int amountOf(ItemStack carrierStack) {
			if (!isPhysicalCarrier(carrierStack)) {
				throw new IllegalArgumentException("invalid fluid resource carrier");
			}
			return Math.multiplyExact(CompressedTankItem.getFluid(carrierStack).getAmount(), carrierStack.getCount());
		}

		@Override
		public int maxPerPackage(ItemStack normalizedKey) {
			normalizeKey(normalizedKey);
			return Config.getFluidPerPackage();
		}

		@Override
		public ItemStack createPackage(ItemStack normalizedKey, int amount) {
			if (amount <= 0 || amount > maxPerPackage(normalizedKey)) {
				throw new IllegalArgumentException("invalid fluid resource amount");
			}
			return FluidPackageContentHelper
				.createCanonicalPackage(FluidHelper.copyStackWithAmount(getFluid(normalizedKey), amount));
		}

		@Override
		public List<PackageResource> readPackageResources(ItemStack packageStack) {
			var content = PackageFluidContents.get(packageStack);
			if (content == null || content.isEmpty()) {
				return List.of();
			}
			return content.tanks().stream()
				.map(tank -> new PackageResource(id(), createFluidKey(tank.fluid()), tank.getAmount())).toList();
		}

		@Override
		public void setPackageResources(ItemStack packageStack, List<PackageResource> resources) {
			if (!PackageItem.isPackage(packageStack)
				|| FluidPackageContentHelper.hasConflictingContents(packageStack)) {
				throw new IllegalArgumentException("Cannot replace conflicting or invalid package contents");
			}
			if (LegacyFluidPackageMigration.hasLegacyContents(packageStack)) {
				throw new IllegalArgumentException("Legacy package must be split before replacing fluid contents");
			}
			if (FluidPackageItem.isFluidPackage(packageStack)
				&& (resources.size() != 1 || FluidPackageContentHelper.hasItemContents(packageStack))) {
				throw new IllegalArgumentException(
					"Fluid package must contain exactly one fluid and no ordinary items");
			}
			List<FluidTankContent> tanks = new ArrayList<>(resources.size());
			long totalAmount = 0;
			for (PackageResource resource : resources) {
				FluidStack fluid = getFluid(resource.key());
				if (!id().equals(resource.typeId()) || fluid.isEmpty() || resource.amount() <= 0) {
					throw new IllegalArgumentException("invalid fluid package resource");
				}
				totalAmount += resource.amount();
				if (totalAmount > Config.getFluidPerPackage()) {
					throw new IllegalArgumentException("combined fluid amount exceeds package capacity");
				}
				int matchingTank = -1;
				for (int tank = 0; tank < tanks.size(); tank++) {
					if (tanks.get(tank).fluid().isFluidEqual(fluid)) {
						matchingTank = tank;
						break;
					}
				}
				if (matchingTank >= 0) {
					tanks.set(matchingTank, new FluidTankContent(FluidHelper.copyStackWithAmount(fluid,
						tanks.get(matchingTank).getAmount() + resource.amount())));
				} else {
					if (tanks.size() == PackageFluidContents.MAX_FLUIDS) {
						throw new IllegalArgumentException("too many fluids in package");
					}
					tanks.add(new FluidTankContent(FluidHelper.copyStackWithAmount(fluid, resource.amount())));
				}
			}
			if (tanks.isEmpty()) {
				PackageFluidContents.remove(packageStack);
			} else {
				PackageFluidContents.set(packageStack, new PackageFluidContents(tanks));
			}
		}

		@Override
		public Set<String> packageComponents() {
			return Set.of(PackageFluidContents.TAG_KEY);
		}

		@Override
		public void validatePackage(ItemStack packageStack) {
			if (FluidPackageContentHelper.hasConflictingContents(packageStack)) {
				throw new IllegalArgumentException(
					"Package has both legacy and current fluid cargo; retain for recovery");
			}
		}

		@Override
		public ItemStack migratePackage(ItemStack packageStack) {
			return FluidPackageContentHelper.migrateCopy(packageStack);
		}

		@Override
		public ItemStack withPackageAmount(ItemStack packageStack, ItemStack key, int amount) {
			FluidPackageContentHelper.setCanonicalContents(packageStack,
				FluidHelper.copyStackWithAmount(getFluid(key), amount));
			return packageStack;
		}

		@Override
		public Optional<PackageResource> readCanonicalPackage(ItemStack packageStack) {
			if (packageStack == null || !FluidPackageItem.isFluidPackage(packageStack)) {
				return Optional.empty();
			}
			FluidStack fluid = FluidPackageContentHelper.getSingleContainedFluid(packageStack);
			if (fluid.isEmpty()) {
				return Optional.empty();
			}
			ItemStack key = createFluidKey(fluid);
			return Optional.of(new PackageResource(PackageResourceTypes.FLUID, key, fluid.getAmount()));
		}

		@Override
		public int unpackAmount(PackageUnpackContext context, PackageResource resource, boolean simulate) {
			if (!resource.typeId().equals(PackageResourceTypes.FLUID)) {
				return 0;
			}
			IFluidHandler handler = context.targetBlockEntity() == null ? null
				: context.targetBlockEntity().getCapability(ForgeCapabilities.FLUID_HANDLER, context.side())
					.orElse(null);
			if (handler == null) {
				return 0;
			}
			FluidStack fluid = FluidHelper.copyStackWithAmount(getFluid(resource.key()), resource.amount());
			return handler.fill(fluid, simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE);
		}

		@Override
		public PackageResourceDisplay display() {
			return DISPLAY;
		}

		@Override
		public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking, ItemStack packageStack,
			List<PackageResource> resources) {
			List<FluidStack> fluids = resources.stream()
				.map(resource -> FluidHelper.copyStackWithAmount(getFluid(resource.key()), resource.amount()))
				.filter(fluid -> !fluid.isEmpty()).toList();
			return FluidPackageGoggleInfo.append(tooltip, fluids, Config.getFluidPerPackage());
		}

		@Override
		public boolean survivesWater(ItemStack packageStack, PackageInspection inspection) {
			return inspection.canonical();
		}

		@Override
		public DropAction onDestroyed(PackageDestroyContext context, List<PackageResource> resources) {
			ServerLevel level = context.level();
			PackageEntity entity = context.entity();
			boolean selectedFluid = false;
			for (PackageResource resource : resources) {
				FluidStack fluid = FluidHelper.copyStackWithAmount(getFluid(resource.key()), resource.amount());

				if (CreateEnchantmentIndustryCompat.tryDropExperienceFromFluid(level, entity.position(), fluid))
					continue;
				if (!selectedFluid && tryPlaceFluid(level, entity.blockPosition(), fluid)) {
					selectedFluid = true;
				}
			}
			return DropAction.CONSUME_CARRIERS;
		}

		private static boolean tryPlaceFluid(ServerLevel level, BlockPos pos, FluidStack fluid) {
			if (fluid.getAmount() < FluidAmountHelper.MB_PER_BUCKET) {
				return false;
			}
			if (level.dimensionType().ultraWarm() && fluid.getFluid().is(FluidTags.WATER)) {
				level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F,
					2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F);
				for (int i = 0; i < 8; i++) {
					level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + level.random.nextDouble(),
						pos.getY() + level.random.nextDouble(), pos.getZ() + level.random.nextDouble(), 1, 0.0D, 0.0D,
						0.0D, 0.0D);
				}
				return true;
			}
			BlockState fluidState = fluid.getFluid().defaultFluidState().createLegacyBlock();
			if (fluidState.isAir()) {
				return true;
			}
			BlockState existingState = level.getBlockState(pos);
			if (!existingState.canBeReplaced() || !fluidState.canSurvive(level, pos)) {
				return true;
			}
			level.setBlock(pos, fluidState, Block.UPDATE_ALL);
			return true;
		}
	}

	private static final class FluidResourceDisplay implements PackageResourceDisplay {
		private static final int MAX_FACTORY_PANEL_AMOUNT = 100 * FluidAmountHelper.MB_PER_BUCKET;
		private static final int MAX_FACTORY_PANEL_SETTING_AMOUNT = 1000 * FluidAmountHelper.MB_PER_BUCKET;
		private static final List<FactoryPanelUnit> FACTORY_PANEL_UNITS = List.of(new FactoryPanelUnit("mB", 10, 10),
			new FactoryPanelUnit(" B", 10 * FluidAmountHelper.MB_PER_BUCKET, 10));

		@Override
		public String baseUnit() {
			return "mB";
		}

		@Override
		public String format(ItemStack normalizedKey, int amount, Format format) {
			Objects.requireNonNull(format, "format");
			return switch (format) {
			case COMPACT -> FluidAmountHelper.format(amount);
			case PRECISE -> FluidAmountHelper.formatPrecise(amount);
			case DETAILED -> FluidAmountHelper.formatDetailed(amount);
			};
		}

		@Override
		public Component name(ItemStack normalizedKey) {
			return getFluid(normalizedKey).getDisplayName().copy();
		}

		@Override
		public boolean showsAmountInTooltip(TooltipContext context) {
			return context == TooltipContext.STOCK_KEEPER_ORDER || context == TooltipContext.STOCK_KEEPER_CRAFTABLE;
		}

		@Override
		public List<FactoryPanelUnit> factoryPanelUnits(ItemStack normalizedKey) {
			return FACTORY_PANEL_UNITS;
		}

		@Override
		public int factoryPanelMaxValue(ItemStack normalizedKey) {
			return 100;
		}

		@Override
		public int factoryPanelMilestoneInterval(ItemStack normalizedKey) {
			return 10;
		}

		@Override
		public FactoryPanelRestockPolicy factoryPanelRestockPolicy(ItemStack normalizedKey) {
			long configuredBatch = (long) Config.getFluidPerPackage() * 100L;
			int batchLimit = (int) Math.max(1L, Math.min(MAX_FACTORY_PANEL_AMOUNT, configuredBatch));
			return new FactoryPanelRestockPolicy(true, true, true, MAX_FACTORY_PANEL_SETTING_AMOUNT, batchLimit);
		}

		@Override
		public int adjust(ItemStack normalizedKey, Adjustment adjustment) {
			Objects.requireNonNull(adjustment, "adjustment");
			if (adjustment.interaction() == Interaction.STOCK_KEEPER_INVENTORY) {
				return FluidAmountHelper.adjustStockKeeperFluidInventoryAmount(adjustment.currentAmount(),
					adjustment.forward(), adjustment.shift(), adjustment.control(), adjustment.minAmount(),
					adjustment.maxAmount(), adjustment.steps());
			}
			if (adjustment.interaction() == Interaction.STOCK_KEEPER) {
				return FluidAmountHelper.adjustStockKeeperFluidRequestAmount(adjustment.currentAmount(),
					adjustment.forward(), adjustment.shift(), adjustment.control(), adjustment.minAmount(),
					adjustment.maxAmount(), adjustment.steps());
			}
			return FluidAmountHelper.adjustFluidRequestAmount(adjustment.currentAmount(), adjustment.forward(),
				adjustment.shift(), adjustment.control(), adjustment.minAmount(), adjustment.maxAmount(),
				adjustment.steps());
		}
	}
}
