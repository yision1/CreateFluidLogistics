package com.yision.fluidlogistics.content.logistics.factoryGauge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.yision.fluidlogistics.api.factorygauge.FactoryGaugeType;
import com.yision.fluidlogistics.api.packager.PackageResourceTypes;
import com.yision.fluidlogistics.config.Config;
import com.yision.fluidlogistics.content.logistics.factoryGauge.ResourceFactoryPanelBehaviour;
import com.yision.fluidlogistics.content.logistics.fluidPackage.FluidPackageResourceType;

import net.createmod.catnip.platform.ForgeCatnipServices;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;

@OnlyIn(Dist.CLIENT)
public final class FactoryGaugeWorldFluidRenderer {
    private static final float VALUE_BOX_BLOCK_SCALE = 1.0f + 1.0f / 64.0f;
    private static final float FIXED_BLOCK_SCALE = 0.5f;

    private FactoryGaugeWorldFluidRenderer() {
        throw new AssertionError("This class should not be instantiated");
    }

    private static boolean isFluidGauge(FilteringBehaviour behaviour) {
        if (!(behaviour instanceof ResourceFactoryPanelBehaviour gauge))
            return false;
        if (!gauge.isResourceGauge() || !gauge.isActive())
            return false;

        FactoryGaugeType gaugeType = gauge.registeredType().orElse(null);
        return gaugeType != null && PackageResourceTypes.FLUID.equals(gaugeType.resourceTypeId());
    }

    public static boolean usesFluidBlockRendering(FilteringBehaviour behaviour) {
        if (Config.useItemRenderingForFluidFactoryGaugeFluid() || !isFluidGauge(behaviour))
            return false;
        ItemStack key = behaviour.getFilter();
        return !key.isEmpty() && !FluidPackageResourceType.getFluid(key).isEmpty();
    }

    public static boolean tryRender(FilteringBehaviour behaviour, ItemStack key, PoseStack ms,
        MultiBufferSource buffer, int light) {
        if (Config.useItemRenderingForFluidFactoryGaugeFluid() || !isFluidGauge(behaviour))
            return false;

        FluidStack fluid = FluidPackageResourceType.getFluid(key);
        if (fluid.isEmpty())
            return false;

        ms.pushPose();
        try {
            ms.scale(VALUE_BOX_BLOCK_SCALE, VALUE_BOX_BLOCK_SCALE, VALUE_BOX_BLOCK_SCALE);
            ms.scale(FIXED_BLOCK_SCALE, FIXED_BLOCK_SCALE, FIXED_BLOCK_SCALE);
            ms.translate(-0.5f, -0.5f, -0.5f);
            ForgeCatnipServices.FLUID_RENDERER.renderFluidBox(
                fluid,
                0, 0, 0,
                1, 1, 1,
                buffer, ms, light,
                true, true
            );
        } finally {
            ms.popPose();
        }
        return true;
    }
}
