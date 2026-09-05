package com.yision.fluidlogistics.registry;

import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.CTType;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SpriteShifter;

import static com.yision.fluidlogistics.FluidLogistics.asResource;

public class AllSpriteShifts {
    public static final SpriteShiftEntry BLAZE_COOLER_FLAME = SpriteShifter.get(
            asResource("block/blaze_cooler/flame"), asResource("block/blaze_cooler/flame_scroll"));
    public static final SpriteShiftEntry BLAZE_COOLER_SUPER_FLAME = SpriteShifter.get(
            asResource("block/blaze_cooler/flame"), asResource("block/blaze_cooler/super_flame_scroll"));

    public static final CTSpriteShiftEntry
            MULTI_FLUID_TANK = getCT(AllCTTypes.RECTANGLE, "multi_fluid_tank/multi_fluid_tank"),
            MULTI_FLUID_TANK_TOP = getCT(AllCTTypes.RECTANGLE, "multi_fluid_tank/multi_fluid_tank_top"),
            MULTI_FLUID_TANK_INNER = getCT(AllCTTypes.RECTANGLE, "multi_fluid_tank/multi_fluid_tank_inner");

    private static CTSpriteShiftEntry getCT(CTType type, String blockTextureName, String connectedTextureName) {
        return CTSpriteShifter.getCT(type, asResource("block/" + blockTextureName),
                asResource("block/" + connectedTextureName + "_connected"));
    }

    private static CTSpriteShiftEntry getCT(CTType type, String blockTextureName) {
        return getCT(type, blockTextureName, blockTextureName);
    }

    public static void register() {
    }
}
