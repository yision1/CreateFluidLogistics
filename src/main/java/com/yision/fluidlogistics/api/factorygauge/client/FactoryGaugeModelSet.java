package com.yision.fluidlogistics.api.factorygauge.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

public record FactoryGaugeModelSet(
    PartialModel panel,
    PartialModel panelWithBulb,
    PartialModel panelRestocker,
    PartialModel panelRestockerWithBulb,
    PartialModel bulbLight,
    PartialModel bulbRed) {

    public static FactoryGaugeModelSet fromRoot(ResourceLocation root) {
        return new FactoryGaugeModelSet(
            PartialModel.of(suffix(root, "panel")),
            PartialModel.of(suffix(root, "panel_with_bulb")),
            PartialModel.of(suffix(root, "panel_restocker")),
            PartialModel.of(suffix(root, "panel_restocker_with_bulb")),
            PartialModel.of(suffix(root, "bulb_light")),
            PartialModel.of(suffix(root, "bulb_red")));
    }

    private static ResourceLocation suffix(ResourceLocation root, String path) {
        return root.withPath(root.getPath() + "/" + path);
    }

    public Iterable<PartialModel> all() {
        return java.util.List.of(panel, panelWithBulb, panelRestocker, panelRestockerWithBulb, bulbLight, bulbRed);
    }
}
