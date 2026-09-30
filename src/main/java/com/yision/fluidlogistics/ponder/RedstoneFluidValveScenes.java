package com.yision.fluidlogistics.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.yision.fluidlogistics.content.fluids.redstoneFluidValve.RedstoneFluidValveBlock;
import com.yision.fluidlogistics.content.fluids.redstoneFluidValve.RedstoneFluidValveBlockEntity;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class RedstoneFluidValveScenes {
    public static final String REDSTONE_FLUID_VALVE = "redstone_fluid_valve/redstone_fluid_valve";

    public static void redstoneFluidValve(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title(REDSTONE_FLUID_VALVE, "Controlling Fluid flow using Redstone Fluid Valves");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        Selection cogs = util.select().fromTo(5, 0, 2, 5, 1, 2);
        Selection tank1 = util.select().fromTo(3, 1, 3, 3, 2, 3);
        Selection tank2 = util.select().fromTo(1, 1, 3, 1, 2, 3);
        BlockPos valvePos = util.grid().at(2, 1, 1);
        BlockPos leverPos = util.grid().at(2, 1, 0);
        BlockPos pumpPos = util.grid().at(4, 1, 2);
        Selection pipes1 = util.select().fromTo(4, 1, 3, 4, 1, 1);
        Selection pipes2 = util.select().fromTo(3, 1, 1, 1, 1, 1);
        Selection pipes3 = util.select().fromTo(0, 1, 1, 0, 1, 3);

        scene.world().setKineticSpeed(pipes1, 0);
        scene.world().propagatePipeChange(pumpPos);

        scene.idle(5);
        scene.world().showSection(tank1, Direction.NORTH);
        scene.idle(5);
        scene.world().showSection(tank2, Direction.NORTH);
        scene.idle(10);
        scene.world().showSection(pipes1, Direction.WEST);
        scene.idle(5);
        scene.world().showSection(pipes2, Direction.SOUTH);
        scene.idle(5);
        scene.world().showSection(pipes3, Direction.EAST);
        scene.idle(15);

        scene.world().destroyBlock(valvePos);
        scene.world().restoreBlocks(util.select().position(valvePos));

        scene.overlay().showText(60)
                .placeNearTarget()
                .text("Redstone Fluid Valves help control fluids propagating through pipe networks")
                .attachKeyFrame()
                .pointAt(util.vector().blockSurface(valvePos, Direction.WEST));
        scene.idle(75);

        scene.world().showSection(cogs, Direction.WEST);
        scene.idle(10);
        scene.world().setKineticSpeed(util.select().position(5, 0, 2), 64);
        scene.world().setKineticSpeed(util.select().position(5, 1, 2), -64);
        scene.world().setKineticSpeed(util.select().position(pumpPos), 64);
        scene.world().propagatePipeChange(pumpPos);
        scene.world().showSection(util.select().position(leverPos), Direction.SOUTH);
        scene.idle(15);
        scene.overlay().showText(60)
                .placeNearTarget()
                .colored(PonderPalette.RED)
                .text("Redstone Signals control whether the valve is open or closed")
                .attachKeyFrame()
                .pointAt(util.vector().blockSurface(valvePos, Direction.WEST));
        scene.idle(75);

        scene.world().toggleRedstonePower(util.select().position(leverPos));
        scene.effects().indicateRedstone(leverPos);
        scene.world().modifyBlockEntity(valvePos, RedstoneFluidValveBlockEntity.class,
                blockEntity -> blockEntity.setPowered(true));
        scene.idle(5);
        scene.world().modifyBlock(valvePos,
                state -> state.setValue(RedstoneFluidValveBlock.ENABLED, false), false);
        scene.world().propagatePipeChange(pumpPos);
        scene.idle(60);

        scene.world().toggleRedstonePower(util.select().position(leverPos));
        scene.world().modifyBlockEntity(valvePos, RedstoneFluidValveBlockEntity.class,
                blockEntity -> blockEntity.setPowered(false));
        scene.idle(5);
        scene.world().modifyBlock(valvePos,
                state -> state.setValue(RedstoneFluidValveBlock.ENABLED, true), false);
        scene.world().propagatePipeChange(pumpPos);
        scene.effects().indicateSuccess(valvePos);
        scene.idle(40);
        scene.markAsFinished();
    }
}
