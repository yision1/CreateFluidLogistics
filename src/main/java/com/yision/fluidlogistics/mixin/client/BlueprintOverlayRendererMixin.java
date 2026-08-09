package com.yision.fluidlogistics.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.equipment.blueprint.BlueprintOverlayRenderer;
import com.simibubi.create.content.logistics.tableCloth.BlueprintOverlayShopContext;
import com.yision.fluidlogistics.api.packager.PackageResourceDisplay;
import com.yision.fluidlogistics.content.logistics.packageResource.client.TableClothResourceDisplay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlueprintOverlayRenderer.class)
public class BlueprintOverlayRendererMixin {
    @Shadow(remap = false)
    private static BlueprintOverlayShopContext shopContext;

    @WrapOperation(
            method = "renderOverlay",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/equipment/blueprint/BlueprintOverlayRenderer;" +
                            "drawItemStack(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/Minecraft;" +
                            "IILnet/minecraft/world/item/ItemStack;Ljava/lang/String;)V",
                    ordinal = 1,
                    remap = false),
            remap = false)
    private static void fluidlogistics$renderTableClothResource(
            GuiGraphics graphics, Minecraft minecraft, int x, int y, ItemStack stack, String count,
            Operation<Void> original) {
        if (shopContext == null || shopContext.checkout()) {
            original.call(graphics, minecraft, x, y, stack, count);
            return;
        }

        int amount = stack.getCount();
        String formattedAmount = TableClothResourceDisplay
                .formatAmount(stack, amount, PackageResourceDisplay.Format.COMPACT)
                .orElse(null);
        if (formattedAmount == null) {
            original.call(graphics, minecraft, x, y, stack, count);
            return;
        }

        original.call(graphics, minecraft, x, y, stack.copyWithCount(1), formattedAmount);
    }
}
