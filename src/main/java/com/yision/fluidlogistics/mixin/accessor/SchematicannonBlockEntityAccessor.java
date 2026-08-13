package com.yision.fluidlogistics.mixin.accessor;

import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SchematicannonBlockEntity.class)
public interface SchematicannonBlockEntityAccessor {

    @Accessor("printerCooldown")
    int fluidlogistics$getPrinterCooldown();

    @Accessor("printerCooldown")
    void fluidlogistics$setPrinterCooldown(int cooldown);

    @Accessor("blockSkipped")
    void fluidlogistics$setBlockSkipped(boolean skipped);
}
