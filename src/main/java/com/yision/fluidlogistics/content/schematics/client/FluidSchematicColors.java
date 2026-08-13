package com.yision.fluidlogistics.content.schematics.client;

public final class FluidSchematicColors {

    public static final int COPPER = 0xD37A5A;
    public static final int TEXT_HIGHLIGHT = 0xFFE8DC;

    private static final int CREATE_BLUE_TEXT = 0xCCDDFF;
    private static final int CREATE_MUTED_BLUE_TEXT = 0xCCCCDD;

    private FluidSchematicColors() {
    }

    public static int replaceCreateBlueText(int color) {
        int rgb = color & 0xFFFFFF;
        if (rgb != CREATE_BLUE_TEXT && rgb != CREATE_MUTED_BLUE_TEXT) {
            return color;
        }
        return (color & 0xFF000000) | TEXT_HIGHLIGHT;
    }
}
