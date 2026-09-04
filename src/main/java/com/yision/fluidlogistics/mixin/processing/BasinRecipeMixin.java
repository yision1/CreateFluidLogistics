package com.yision.fluidlogistics.mixin.processing;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.yision.fluidlogistics.content.processing.blazeCooler.BlazeCoolerBlockEntity;
import com.yision.fluidlogistics.content.processing.cooling.CoolingRecipe;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BasinRecipe.class)
public abstract class BasinRecipeMixin {

    @Inject(
        method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;"
            + "Lnet/minecraft/world/item/crafting/Recipe;Z)Z",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void fluidlogistics$requireRegularCooling(BasinBlockEntity basin, Recipe<?> recipe,
            boolean simulate, CallbackInfoReturnable<Boolean> cir) {
        if (!(recipe instanceof CoolingRecipe coolingRecipe))
            return;

        BlockEntity source = basin.getLevel().getBlockEntity(basin.getBlockPos().below());
        if (!(source instanceof BlazeCoolerBlockEntity cooler)) {
            cir.setReturnValue(false);
            return;
        }

        HeatLevel coolingLevel = cooler.getHeatLevelFromBlock();
        boolean validCooling = coolingRecipe.requiresSupercooling()
            ? coolingLevel == HeatLevel.SEETHING
            : coolingLevel.isAtLeast(HeatLevel.FADING);
        if (!validCooling)
            cir.setReturnValue(false);
    }
}
