package com.yision.fluidlogistics.content.schematics.cannon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.clipboard.ClipboardContent;
import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import com.simibubi.create.content.equipment.clipboard.ClipboardOverrides.ClipboardType;
import com.simibubi.create.content.schematics.cannon.MaterialChecklist;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.yision.fluidlogistics.api.packager.PackageResourceDisplay;
import com.yision.fluidlogistics.api.packager.PackageResources;
import com.yision.fluidlogistics.content.logistics.fluidPackage.FluidPackageResourceType;
import com.yision.fluidlogistics.util.FluidAmountHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

public class FluidMaterialChecklist extends MaterialChecklist {

    private static final int BUCKETS_PER_GROUP = 20;

    private final Map<Fluid, Integer> fluidRequired = new HashMap<>();
    private final Map<Fluid, Integer> fluidGathered = new HashMap<>();

    public void clearFluids() {
        fluidRequired.clear();
        fluidGathered.clear();
        blocksNotLoaded = false;
    }

    public void require(FluidStack fluid) {
        Fluid still = FluidHelper.convertToStill(fluid.getFluid());
        fluidRequired.merge(still, fluid.getAmount(), FluidMaterialChecklist::saturatedAdd);
    }

    public void collect(FluidStack fluid) {
        Fluid still = FluidHelper.convertToStill(fluid.getFluid());
        if (fluidRequired.containsKey(still)) {
            fluidGathered.merge(still, fluid.getAmount(), FluidMaterialChecklist::saturatedAdd);
        }
    }

    public Set<Fluid> requiredFluids() {
        return Set.copyOf(fluidRequired.keySet());
    }

    public int requiredAmount(Fluid fluid) {
        return fluidRequired.getOrDefault(FluidHelper.convertToStill(fluid), 0);
    }

    @Override
    public ItemStack createWrittenClipboard() {
        List<List<ClipboardEntry>> pages = new ArrayList<>();
        List<ClipboardEntry> page = new ArrayList<>();
        int entries = 0;
        if (blocksNotLoaded) {
            page.add(new ClipboardEntry(false,
                Component.translatable("create.materialChecklist.blocksNotLoaded").withStyle(ChatFormatting.RED)));
        }

        List<Fluid> completed = new ArrayList<>();
        for (Fluid fluid : sortedFluids()) {
            int required = fluidRequired.getOrDefault(fluid, 0);
            int missing = Math.max(0, required - fluidGathered.getOrDefault(fluid, 0));
            if (missing == 0) {
                completed.add(fluid);
                continue;
            }
            ItemStack key = FluidPackageResourceType.createFluidKey(new FluidStack(fluid, 1));
            if (entries == MAX_ENTRIES_PER_CLIPBOARD_PAGE) {
                entries = 0;
                page.add(new ClipboardEntry(false, Component.literal(">>>").withStyle(ChatFormatting.DARK_GRAY)));
                pages.add(page);
                page = new ArrayList<>();
            }
            entries++;
            page.add(new ClipboardEntry(false, entry(key, missing, true, false))
                .displayItem(key, missing));
        }

        for (Fluid fluid : completed) {
            int required = fluidRequired.getOrDefault(fluid, 0);
            ItemStack key = FluidPackageResourceType.createFluidKey(new FluidStack(fluid, 1));
            if (entries == MAX_ENTRIES_PER_CLIPBOARD_PAGE) {
                entries = 0;
                page.add(new ClipboardEntry(true, Component.literal(">>>").withStyle(ChatFormatting.DARK_GREEN)));
                pages.add(page);
                page = new ArrayList<>();
            }
            entries++;
            page.add(new ClipboardEntry(true, entry(key, required, false, false))
                .displayItem(key, 0));
        }
        pages.add(page);

        ItemStack clipboard = AllBlocks.CLIPBOARD.asStack();
        clipboard.set(AllDataComponents.CLIPBOARD_CONTENT, new ClipboardContent(ClipboardType.WRITTEN, pages, true));
        clipboard.set(DataComponents.CUSTOM_NAME,
            Component.translatable("create.materialChecklist").withStyle(Style.EMPTY.withItalic(false)));
        return clipboard;
    }

    @Override
    public ItemStack createWrittenBook() {
        List<Filterable<Component>> pages = new ArrayList<>();
        MutableComponent page = Component.empty();
        int entries = 0;

        if (blocksNotLoaded) {
            pages.add(Filterable.passThrough(Component.literal("\n")
                .append(Component.translatable("create.materialChecklist.blocksNotLoaded")
                    .withStyle(ChatFormatting.RED))));
        }

        List<Fluid> completed = new ArrayList<>();
        for (Fluid fluid : sortedFluids()) {
            int required = fluidRequired.getOrDefault(fluid, 0);
            int missing = Math.max(0, required - fluidGathered.getOrDefault(fluid, 0));
            if (missing == 0) {
                completed.add(fluid);
                continue;
            }
            ItemStack key = FluidPackageResourceType.createFluidKey(new FluidStack(fluid, 1));
            if (entries == MAX_ENTRIES_PER_PAGE) {
                entries = 0;
                page.append(Component.literal("\n >>>").withStyle(ChatFormatting.BLUE));
                pages.add(Filterable.passThrough(page));
                page = Component.empty();
            }
            entries++;
            page.append(entry(key, missing, true, true));
        }

        for (Fluid fluid : completed) {
            int required = fluidRequired.getOrDefault(fluid, 0);
            ItemStack key = FluidPackageResourceType.createFluidKey(new FluidStack(fluid, 1));
            if (entries == MAX_ENTRIES_PER_PAGE) {
                entries = 0;
                page.append(Component.literal("\n >>>").withStyle(ChatFormatting.DARK_GREEN));
                pages.add(Filterable.passThrough(page));
                page = Component.empty();
            }
            entries++;
            page.append(entry(key, required, false, true));
        }
        pages.add(Filterable.passThrough(page));

        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
            Filterable.passThrough("Fluid Checklist"), "Schematicannon", 0, pages, true));
        book.set(DataComponents.CUSTOM_NAME,
            Component.translatable("fluidlogistics.fluid_material_checklist").withStyle(Style.EMPTY.withItalic(false)));
        return book;
    }

    private List<Fluid> sortedFluids() {
        List<Fluid> fluids = new ArrayList<>(fluidRequired.keySet());
        fluids.sort(Comparator.comparing(fluid -> {
            ItemStack key = FluidPackageResourceType.createFluidKey(new FluidStack(fluid, 1));
            return PackageResources.nameOf(key).orElse(key.getHoverName()).getString();
        }));
        return fluids;
    }

    private static MutableComponent entry(ItemStack key, int amount, boolean unfinished, boolean forBook) {
        Component name = PackageResources.nameOf(key).orElse(key.getHoverName());
        String formatted = PackageResources.formatAmount(key, amount, PackageResourceDisplay.Format.PRECISE)
            .orElse(amount + " mB");
        int buckets = amount / FluidAmountHelper.MB_PER_BUCKET;
        int groups = buckets / BUCKETS_PER_GROUP;
        int remainder = buckets % BUCKETS_PER_GROUP;
        MutableComponent text = Component.empty().append(name.copy());
        if (!unfinished && forBook) {
            text.append(" \u2714");
        }
        if (!unfinished || forBook) {
            text.withStyle(unfinished ? ChatFormatting.BLUE : ChatFormatting.DARK_GREEN);
        }
        return text
            .append(Component.literal("\n x" + formatted).withStyle(ChatFormatting.BLACK))
            .append(Component.literal(" | " + groups + "\u25A4 +" + remainder + "B" + (forBook ? "\n" : ""))
                .withStyle(ChatFormatting.GRAY));
    }

    private static int saturatedAdd(int first, int second) {
        return (int) Math.min(Integer.MAX_VALUE, (long) first + second);
    }
}
