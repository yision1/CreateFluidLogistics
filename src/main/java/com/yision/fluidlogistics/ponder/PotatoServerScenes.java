package com.yision.fluidlogistics.ponder;

import com.simibubi.create.AllShapes;
import com.simibubi.create.content.trains.display.FlapDisplayBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.yision.fluidlogistics.registry.AllBlocks;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

public class PotatoServerScenes {
    public static final String POTATO_SERVER = "potato_server/potato_server";

    public static void potatoServer(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title(POTATO_SERVER, "Reading Game Information Through the Potato Server");
        scene.configureBasePlate(0, 0, 7);
        scene.world().showSection(util.select().fromTo(0, 0, 0, 6, 0, 6)
            .substract(util.select().position(5, 0, 5)), Direction.UP);

        BlockPos center = util.grid().at(3, 1, 2);
        Selection single = util.select().position(center);
        Selection links = util.select().fromTo(4, 1, 1, 4, 2, 1)
            .add(util.select().position(5, 2, 2))
            .add(util.select().position(5, 3, 1));
        Selection board = util.select().fromTo(1, 1, 3, 3, 2, 3);
        Selection kinetics = util.select().fromTo(4, 1, 3, 4, 1, 5)
            .add(util.select().position(5, 0, 5));

        scene.world().setBlock(center, AllBlocks.POTATO_SERVER.getDefaultState(), false);
        scene.world().showSection(single, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showOutlineWithText(single, 70)
            .colored(PonderPalette.OUTPUT)
            .text("The Potato Server can read information about the game")
            .pointAt(util.vector().centerOf(center))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(80);

        scene.world().hideSection(single, Direction.UP);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(5, 1, 1, 5, 2, 1), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(links, Direction.DOWN);
        scene.idle(15);
        BlockPos[] linkPositions = { util.grid().at(4, 1, 1), util.grid().at(4, 2, 1),
            util.grid().at(5, 2, 2), util.grid().at(5, 3, 1) };
        Direction[] linkFaces = { Direction.WEST, Direction.WEST, Direction.SOUTH, Direction.UP };
        for (int i = 0; i < linkPositions.length; i++)
            scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, "link_" + i,
                AllShapes.DATA_GATHERER.get(linkFaces[i]).bounds().move(linkPositions[i]), 70);
        scene.overlay().showText(70)
            .text("Connect it with Display Links")
            .pointAt(util.vector().centerOf(4, 2, 1))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(80);

        scene.world().showSection(kinetics, Direction.DOWN);
        scene.world().showSection(board, Direction.DOWN);
        scene.idle(20);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.OUTPUT, "board",
            AllShapes.FLAP_DISPLAY.get(Direction.NORTH).bounds()
                .move(util.grid().at(1, 1, 3)).expandTowards(2, 1, 0), 70);
        scene.overlay().showText(70)
            .colored(PonderPalette.OUTPUT)
            .text("Visualize more information dynamically")
            .pointAt(util.vector().centerOf(2, 2, 3))
            .placeNearTarget()
            .attachKeyFrame();
        scene.idle(80);

        BlockPos controller = util.grid().at(3, 2, 3);
        String[] data = { "TPS : 20.0", "MSPT : 50.0", "CPU : 12.5%", "MEM : 25.0%" };
        scene.world().modifyBlockEntity(controller, FlapDisplayBlockEntity.class, display -> {
            for (int line = 0; line < data.length; line++)
                display.applyTextManually(line, Component.literal(data[line]));
        });
        scene.idle(100);
        scene.markAsFinished();
    }
}
