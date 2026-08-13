package com.yision.fluidlogistics.mixin.client;

import java.util.ArrayList;
import java.util.List;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlockEntity;
import com.yision.fluidlogistics.content.logistics.packageResource.client.TableClothResourceDisplay;

import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TableClothBlockEntity.class)
public class TableClothBlockEntityMixin {
    @Unique
    private List<ItemStack> fluidlogistics$sourceItemsForRender;

    @Unique
    private List<ItemStack> fluidlogistics$resourceItemsForRender;

    @ModifyReturnValue(method = "getItemsForRender", at = @At("RETURN"), remap = false)
    private List<ItemStack> fluidlogistics$renderResourcePackages(List<ItemStack> original) {
        TableClothBlockEntity blockEntity = (TableClothBlockEntity) (Object) this;
        if (!blockEntity.isShop() || original.isEmpty()) {
            fluidlogistics$sourceItemsForRender = null;
            fluidlogistics$resourceItemsForRender = null;
            return original;
        }
        if (original == fluidlogistics$sourceItemsForRender) {
            return fluidlogistics$resourceItemsForRender;
        }

        List<BigItemStack> entries = blockEntity.requestData.encodedRequest.stacks();
        List<ItemStack> rendered = new ArrayList<>(original.size());
        boolean changed = false;
        for (int i = 0; i < original.size(); i++) {
            ItemStack source = original.get(i);
            ItemStack display = source;
            if (i < entries.size()) {
                BigItemStack entry = entries.get(i);
                display = TableClothResourceDisplay.createPackage(entry.stack, entry.count).orElse(source);
            }
            rendered.add(display);
            changed |= display != source;
        }

        fluidlogistics$sourceItemsForRender = original;
        fluidlogistics$resourceItemsForRender = changed ? List.copyOf(rendered) : original;
        return fluidlogistics$resourceItemsForRender;
    }
}
