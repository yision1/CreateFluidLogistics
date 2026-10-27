package com.yision.fluidlogistics.compat;

import net.minecraftforge.fml.ModList;

public final class CompatMods {
	public static final String JEI = "jei";
	public static final String EMI = "emi";
	public static final String CREATE_ENCHANTMENT_INDUSTRY = "create_enchantment_industry";
	public static final String CREATE_DRAGONS_PLUS = "create_dragons_plus";
	public static final String FARMERS_DELIGHT = "farmersdelight";

	private CompatMods() {
	}

	public static boolean farmersDelightLoaded() {
		return ModList.get().isLoaded(FARMERS_DELIGHT);
	}

	public static boolean jeiLoaded() {
		return ModList.get().isLoaded(JEI);
	}

	public static boolean emiLoaded() {
		return ModList.get().isLoaded(EMI);
	}

	public static boolean createEnchantmentIndustryLoaded() {
		return ModList.get().isLoaded(CREATE_ENCHANTMENT_INDUSTRY);
	}

	public static boolean createDragonsPlusLoaded() {
		return ModList.get().isLoaded(CREATE_DRAGONS_PLUS);
	}
}
