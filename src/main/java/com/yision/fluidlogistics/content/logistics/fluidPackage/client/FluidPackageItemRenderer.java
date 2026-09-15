package com.yision.fluidlogistics.content.logistics.fluidPackage.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import com.yision.fluidlogistics.config.Config;
import com.yision.fluidlogistics.content.logistics.fluidPackage.FluidPackageItem;
import com.yision.fluidlogistics.render.FluidItemRenderHelper;
import com.yision.fluidlogistics.util.FluidDisplayHelper;

import net.createmod.catnip.platform.ForgeCatnipServices;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemStackHandler;

@OnlyIn(Dist.CLIENT)
public class FluidPackageItemRenderer extends CustomRenderedItemModelRenderer {

    private static final float FLUID_INSET = 1f / 128f;

    public static final float FLUID_MIN_XZ = 3f / 16f + FLUID_INSET;
    public static final float FLUID_MAX_XZ = 13f / 16f - FLUID_INSET;
    public static final float FLUID_MIN_Y = 1f / 16f + FLUID_INSET;
    public static final float FLUID_MAX_Y = 11f / 16f - FLUID_INSET;

    public static final float FLUID_WIDTH = FLUID_MAX_XZ - FLUID_MIN_XZ;
    public static final float FLUID_HEIGHT = FLUID_MAX_Y - FLUID_MIN_Y;

    public static boolean entityRendering = false;

    @Override
    protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer,
                          ItemDisplayContext displayContext, PoseStack ms, MultiBufferSource buffer,
                          int light, int overlay) {
        renderer.render(model.getOriginalModel(), light);

        if (stack.getItem() instanceof FluidPackageItem && !entityRendering) {
            renderFluidContents(stack, -1, ms, buffer, light, CoordinateMode.ITEM_MODEL, displayContext);
        }
    }

    public static void renderFluidContents(ItemStack box, PoseStack ms, MultiBufferSource buffer, int light) {
        renderFluidContents(box, -1, ms, buffer, light);
    }

    public static void renderFluidContents(ItemStack box, float fluidLevel, PoseStack ms, MultiBufferSource buffer, int light) {
        renderFluidContents(box, fluidLevel, ms, buffer, light, CoordinateMode.ITEM_MODEL, null);
    }

    public static void renderFluidContentsForEntity(ItemStack box, PoseStack ms, MultiBufferSource buffer, int light) {
        renderFluidContentsForEntity(box, -1, ms, buffer, light);
    }

    public static void renderFluidContentsForEntity(ItemStack box, float fluidLevel, PoseStack ms, MultiBufferSource buffer, int light) {
        renderFluidContents(box, fluidLevel, ms, buffer, light, CoordinateMode.CENTERED_ENTITY, null);
    }

    private static void renderFluidContents(ItemStack box, float fluidLevel, PoseStack ms,
                                            MultiBufferSource buffer, int light, CoordinateMode mode,
                                            ItemDisplayContext displayContext) {
        if (mode == CoordinateMode.ITEM_MODEL) {
            ms.pushPose();
            ms.translate(-0.5f, -0.5f, -0.5f);
        }
        renderFluidContentsLocal(getFluidDisplayData(box, fluidLevel), ms, buffer, light, displayContext,
            mode == CoordinateMode.CENTERED_ENTITY ? -0.5f : 0);
        if (mode == CoordinateMode.ITEM_MODEL) {
            ms.popPose();
        }
    }

    public static void renderFluidContentsLocal(ItemStack box, PoseStack ms, MultiBufferSource buffer, int light) {
        renderFluidContentsLocal(getFluidDisplayData(box, -1), ms, buffer, light, null, 0);
    }

    private static void renderFluidContentsLocal(FluidDisplayData data, PoseStack ms,
                                                 MultiBufferSource buffer, int light,
                                                 ItemDisplayContext displayContext, float offsetXZ) {
        if (data == null) return;

        ForgeCatnipServices.FLUID_RENDERER.renderFluidBox(
            data.fluid(),
            FLUID_MIN_XZ + offsetXZ, data.minY(), FLUID_MIN_XZ + offsetXZ,
            FLUID_MAX_XZ + offsetXZ, data.maxY(), FLUID_MAX_XZ + offsetXZ,
            FluidItemRenderHelper.getFluidBuilder(buffer, displayContext), ms, light,
            true, false
        );
    }

    public static FluidDisplayData getFluidDisplayData(ItemStack box, float fluidLevel) {
        if (!FluidPackageItem.rendersFluidContents(box)) return null;

        List<FluidStack> fluids = getContainedFluids(box);
        if (fluids.isEmpty()) return null;

        float totalFluid = 0;
        for (FluidStack fluid : fluids) {
            totalFluid += fluid.getAmount();
        }

        if (totalFluid <= 0) return null;

        if (fluidLevel < 0) {
            fluidLevel = totalFluid;
        }

        FluidStack primaryFluid = fluids.get(0);

        float fillFactor = Mth.clamp(fluidLevel / Config.getFluidPerPackage(), 0f, 1f);
        float renderedHeight = FLUID_HEIGHT * fillFactor;
        if (renderedHeight <= 0) return null;

        float yMin;
        float yMax;
        boolean gas = primaryFluid.getFluid().getFluidType().isLighterThanAir();
        if (gas) {
            yMax = FLUID_MAX_Y;
            yMin = yMax - renderedHeight;
        } else {
            yMin = FLUID_MIN_Y;
            yMax = yMin + renderedHeight;
        }

        return yMax > yMin ? new FluidDisplayData(primaryFluid, yMin, yMax, gas) : null;
    }

    public record FluidDisplayData(FluidStack fluid, float minY, float maxY, boolean gas) {
    }

    public static FluidStack getPrimaryContainedFluid(ItemStack box) {
        List<FluidStack> fluids = getContainedFluids(box);
        return fluids.isEmpty() ? FluidStack.EMPTY : fluids.get(0);
    }

    public static FluidStack getVisualContainedFluid(ItemStack box) {
        List<FluidStack> fluids = getContainedFluids(box);
        if (fluids.isEmpty()) {
            return FluidStack.EMPTY;
        }

        int totalFluid = 0;
        for (FluidStack fluid : fluids) {
            totalFluid += fluid.getAmount();
        }

        FluidStack displayedFluid = fluids.get(0).copy();
        displayedFluid.setAmount(totalFluid);
        return displayedFluid;
    }

    public static List<FluidStack> getContainedFluids(ItemStack box) {
        List<FluidStack> fluids = new ArrayList<>();

        if (!PackageItem.isPackage(box)) return fluids;

        ItemStackHandler contents = PackageItem.getContents(box);

        for (int i = 0; i < contents.getSlots(); i++) {
            ItemStack slotStack = contents.getStackInSlot(i);
        FluidStack fluid = FluidDisplayHelper.getPackageDisplayFluid(slotStack);
            if (!fluid.isEmpty()) {
                mergeFluid(fluids, fluid);
            }
        }

        return fluids;
    }

    private static void mergeFluid(List<FluidStack> fluids, FluidStack newFluid) {
        for (FluidStack existing : fluids) {
            if (existing.isFluidEqual(newFluid)) {
                existing.grow(newFluid.getAmount());
                return;
            }
        }
        fluids.add(newFluid.copy());
    }

    private enum CoordinateMode {
        ITEM_MODEL,
        CENTERED_ENTITY
    }
}
