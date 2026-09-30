package com.yision.fluidlogistics.content.logistics.potatoServer;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.PercentOrProgressBarDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.content.trains.display.FlapDisplaySection;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.CreateLang;
import com.sun.management.OperatingSystemMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class PotatoServerDisplaySource extends PercentOrProgressBarDisplaySource {
    private static final OperatingSystemMXBean OS = ManagementFactory.getOperatingSystemMXBean()
        instanceof OperatingSystemMXBean bean ? bean : null;
    private static final List<GarbageCollectorMXBean> GC_BEANS = ManagementFactory.getGarbageCollectorMXBeans();
    private final int metric;

    public PotatoServerDisplaySource(int metric) {
        this.metric = metric;
    }

    @Override
    protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
        if (!(context.level() instanceof ServerLevel level))
            return Component.literal("--");

        if (metric == 4 || metric == 5) {
            MutableComponent line = super.provideLine(context, stats);
            return line == EMPTY_LINE && !progressBarActive(context) ? Component.literal("--") : line;
        }

        MinecraftServer server = level.getServer();
        double mspt = server.getAverageTickTimeNanos() / 1_000_000d;
        String value = switch (metric) {
            case 0 -> String.format(Locale.ROOT, "%.1f", mspt <= 0 ? 20d : Math.min(20d, 1000d / mspt));
            case 1 -> String.format(Locale.ROOT, "%.1f", mspt);
            case 2, 6 -> Integer.toString(PotatoServerStatistics.countEntities(level, metric == 6));
            case 3 -> {
                long time = level.getDayTime();
                yield String.format(Locale.ROOT, "Day %d | %02d:%02d", time / 24000 + 1,
                    (time / 1000 + 6) % 24, time % 1000 * 60 / 1000);
            }
            case 7 -> Integer.toString(PotatoServerStatistics.countChunks(level));
            case 8, 9 -> {
                long total = 0;
                boolean supported = false;
                for (GarbageCollectorMXBean gc : GC_BEANS) {
                    long measurement = metric == 8 ? gc.getCollectionCount() : gc.getCollectionTime();
                    if (measurement < 0)
                        continue;
                    total += measurement;
                    supported = true;
                }
                yield supported ? Long.toString(total) : "--";
            }
            case 10, 11 -> formatDataAmount(PotatoServerNetworkTracker.totalBytes(server, metric == 10));
            default -> "--";
        };
        return Component.literal(value);
    }

    private static String formatDataAmount(long bytes) {
        return String.format(Locale.ROOT, "%.2f MB", bytes / 1_000_000d);
    }

    @Override
    protected Float getProgress(DisplayLinkContext context) {
        if (!(context.level() instanceof ServerLevel))
            return null;
        if (metric == 4) {
            double load = OS == null ? -1 : OS.getProcessCpuLoad();
            return load < 0 ? null : (float) load;
        }
        Runtime runtime = Runtime.getRuntime();
        return (float) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory();
    }

    @Override
    protected boolean progressBarActive(DisplayLinkContext context) {
        int mode = context.sourceConfig().getInt("Mode");
        return metric == 4 ? mode == 1 : metric == 5 && mode == 2;
    }

    @Override
    protected MutableComponent formatNumeric(DisplayLinkContext context, Float progress) {
        if (metric == 5 && context.sourceConfig().getInt("Mode") == 0) {
            Runtime runtime = Runtime.getRuntime();
            long used = runtime.totalMemory() - runtime.freeMemory();
            return Component.literal((used / 1000000) + "/" + (runtime.maxMemory() / 1000000));
        }
        return Component.literal(String.format(Locale.ROOT, "%.1f%%", Mth.clamp(progress * 100, 0, 100)));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initConfigurationWidgets(DisplayLinkContext context, ModularGuiLineBuilder builder, boolean isFirstLine) {
        super.initConfigurationWidgets(context, builder, isFirstLine);
        if (isFirstLine || (metric != 4 && metric != 5))
            return;
        builder.addSelectionScrollInput(0, 120, (input, label) -> input
            .forOptions(metric == 4
                ? CreateLang.translatedOptions("display_source.fill_level", "percent", "progress_bar")
                : List.of(
                    Component.translatable("fluidlogistics.display_source.potato_memory.usage"),
                    CreateLang.translateDirect("display_source.fill_level.percent"),
                    CreateLang.translateDirect("display_source.fill_level.progress_bar")))
            .titled(CreateLang.translateDirect("display_source.fill_level.display")), "Mode");
    }

    @Override
    protected String getFlapDisplayLayoutName(DisplayLinkContext context) {
        return metric == 4 || metric == 5 && context.sourceConfig().getInt("Mode") != 0
            ? super.getFlapDisplayLayoutName(context) : "Default";
    }

    @Override
    protected FlapDisplaySection createSectionForValue(DisplayLinkContext context, int size) {
        return metric == 4 || metric == 5 && context.sourceConfig().getInt("Mode") != 0
            ? super.createSectionForValue(context, size)
            : new FlapDisplaySection(size * FlapDisplaySection.MONOSPACE, "alphabet", false, false);
    }

    @Override
    protected boolean allowsLabeling(DisplayLinkContext context) {
        return true;
    }

}
