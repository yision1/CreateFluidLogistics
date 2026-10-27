package com.yision.fluidlogistics.compat.createenchantmentindustry;

import java.util.Set;

import com.yision.fluidlogistics.compat.CompatMods;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import plus.dragons.createenchantmentindustry.common.fluids.experience.ExperienceHelper;

import net.minecraftforge.fluids.FluidStack;

public final class CreateEnchantmentIndustryCompat {
	private static final Set<ResourceLocation> EXPERIENCE_FLUID_IDS = Set.of(
		id("create_enchantment_industry", "experience"), id("cofh_core", "experience"), id("cyclic", "xpjuice"),
		id("enderio", "xpjuice"), id("industrialforegoing", "essence"), id("justdirethings", "xp_fluid_source"),
		id("mob_grinding_utils", "fluid_xp"), id("pneumaticcraft", "memory_essence"), id("reliquary", "xp_juice_still"),
		id("sophisticatedcore", "xp_still"));

	private CreateEnchantmentIndustryCompat() {
	}

	public static boolean canRepairItem(ItemStack stack) {
		if (!CompatMods.createEnchantmentIndustryLoaded() || stack.isEmpty() || !stack.isDamaged())
			return false;
		return ExperienceHelper.canRepairItem(stack);
	}

	public static int getRequiredRepairFluidAmount(Level level, ItemStack stack, FluidStack availableFluid) {
		if (!(level instanceof ServerLevel serverLevel) || !canRepairItem(stack)
			|| !isKnownExperienceFluid(availableFluid))
			return -1;
		int availableXp = ExperienceHelper.getExperienceFromFluid(availableFluid);
		if (availableXp <= 0)
			return -1;
		int requiredXp = ExperienceHelper.repairItem(availableXp, serverLevel, stack, true);
		if (requiredXp <= 0)
			return -1;
		int requiredFluid = ExperienceHelper.getFluidFromExperience(availableFluid, requiredXp);
		return requiredFluid > 0 ? requiredFluid : -1;
	}

	public static ItemStack repairItemWithFluid(Level level, int requiredAmount, ItemStack stack,
		FluidStack availableFluid) {
		if (!(level instanceof ServerLevel serverLevel) || !canRepairItem(stack) || requiredAmount <= 0
			|| requiredAmount > availableFluid.getAmount() || !isKnownExperienceFluid(availableFluid))
			return ItemStack.EMPTY;
		FluidStack toUse = availableFluid.copy();
		toUse.setAmount(requiredAmount);
		int availableXp = ExperienceHelper.getExperienceFromFluid(toUse);
		if (availableXp <= 0)
			return ItemStack.EMPTY;
		ItemStack result = stack.copyWithCount(1);
		ExperienceHelper.repairItem(availableXp, serverLevel, result, false);
		stack.shrink(1);
		availableFluid.shrink(requiredAmount);
		return result;
	}

	public static boolean tryDropExperienceFromFluid(ServerLevel level, Vec3 pos, FluidStack fluid) {
		if (!CompatMods.createEnchantmentIndustryLoaded() || !isKnownExperienceFluid(fluid))
			return false;
		int experience = ExperienceHelper.getExperienceFromFluid(fluid);
		if (experience <= 0)
			return false;
		ExperienceOrb.award(level, pos, experience);
		return true;
	}

	private static boolean isKnownExperienceFluid(FluidStack fluid) {
		return !fluid.isEmpty() && EXPERIENCE_FLUID_IDS.contains(BuiltInRegistries.FLUID.getKey(fluid.getFluid()));
	}

	private static ResourceLocation id(String namespace, String path) {
		return ResourceLocation.fromNamespaceAndPath(namespace, path);
	}
}
