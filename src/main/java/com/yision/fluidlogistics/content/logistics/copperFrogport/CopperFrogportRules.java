package com.yision.fluidlogistics.content.logistics.copperFrogport;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class CopperFrogportRules {

    private CopperFrogportRules() {
    }

    public static boolean isChainPositionValid(Direction attachedDirection, Vec3 target, Vec3 source) {
        return switch (attachedDirection) {
            case UP -> target.y <= source.y;
            case DOWN -> target.y >= source.y;
            case NORTH -> target.z >= source.z;
            case SOUTH -> target.z <= source.z;
            case WEST -> target.x >= source.x;
            case EAST -> target.x <= source.x;
        };
    }

    public static Direction inventorySide(Direction requestedSide, Direction attachedDirection) {
        return requestedSide == Direction.DOWN ? attachedDirection : requestedSide;
    }
}
