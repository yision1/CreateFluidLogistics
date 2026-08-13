package com.yision.fluidlogistics.mixin.schematics;

import com.yision.fluidlogistics.registry.AllItems;

import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.simibubi.create.content.schematics.table.SchematicTableMenu$1", remap = false)
public class SchematicTableInputSlotMixin {

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true, remap = true)
    private void fluidlogistics$acceptFluidSchematics(
        ItemStack stack, CallbackInfoReturnable<Boolean> cir
    ) {
        if (AllItems.EMPTY_FLUID_SCHEMATIC.isIn(stack) || AllItems.FLUID_SCHEMATIC.isIn(stack)) {
            cir.setReturnValue(true);
        }
    }
}
