package com.yision.fluidlogistics.ponder;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity;
import com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity.CreativeSmartFluidTank;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.yision.fluidlogistics.content.fluids.pressureGauge.PressureGaugeBlock;
import com.yision.fluidlogistics.content.fluids.pressureGauge.PressureGaugeBlockEntity;
import com.yision.fluidlogistics.registry.AllBlocks;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;

public class FlowMeterScenes {
    public static final String FLOW_METER = "flow_meter/flow_meter";

    public static void flowMeter(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title(FLOW_METER, "Monitoring Flow Rate using the Flow Meter");
        scene.configureBasePlate(1, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos meterPos = util.grid().at(2, 1, 3);
        BlockPos pumpPos = util.grid().at(5, 1, 3);
        BlockPos targetTankPos = util.grid().at(0, 0, 4);
        BlockPos sourceTankPos = util.grid().at(6, 0, 4);
        Selection pipeNetwork = util.select().fromTo(0, 0, 3, 0, 1, 3)
            .add(util.select().fromTo(0, 1, 3, 6, 1, 3))
            .add(util.select().fromTo(6, 0, 3, 6, 1, 3));
        Selection kinetics = util.select().fromTo(4, 0, 5, 5, 1, 5)
            .add(util.select().position(5, 1, 4));

        scene.world().setBlock(targetTankPos,
            com.simibubi.create.AllBlocks.CREATIVE_FLUID_TANK.getDefaultState(), false);
        scene.world().setBlock(sourceTankPos,
            com.simibubi.create.AllBlocks.CREATIVE_FLUID_TANK.getDefaultState(), false);
        scene.world().modifyBlock(targetTankPos.north(), state -> state
            .setValue(FluidPipeBlock.DOWN, false)
            .setValue(FluidPipeBlock.SOUTH, true), false);
        scene.world().modifyBlock(sourceTankPos.north(), state -> state
            .setValue(FluidPipeBlock.DOWN, false)
            .setValue(FluidPipeBlock.SOUTH, true), false);
        scene.world().modifyBlockEntity(sourceTankPos, CreativeFluidTankBlockEntity.class,
            blockEntity -> ((CreativeSmartFluidTank) blockEntity.getTankInventory())
                .setContainedFluid(new FluidStack(Fluids.WATER, 1000)));

        scene.world().showSection(pipeNetwork, Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(kinetics, Direction.NORTH);
        scene.idle(10);

        scene.world().setBlock(meterPos, AllBlocks.FLOW_METER.getDefaultState()
            .setValue(PressureGaugeBlock.AXIS, Axis.X)
            .setValue(PressureGaugeBlock.FACING, Direction.UP), true);
        setFlowRate(scene, meterPos, 32);
        scene.world().propagatePipeChange(pumpPos);
        scene.idle(10);

        scene.overlay().showText(80)
            .text("The Flow Meter displays the current Flow Rate of attached components")
            .attachKeyFrame()
            .pointAt(util.vector().topOf(meterPos))
            .placeNearTarget();
        scene.idle(90);

        scene.world().multiplyKineticSpeed(kinetics.copy().add(util.select().position(pumpPos)), 4);
        scene.world().propagatePipeChange(pumpPos);
        scene.effects().rotationSpeedIndicator(pumpPos);
        setFlowRate(scene, meterPos, 128);
        scene.effects().indicateSuccess(meterPos);
        scene.idle(30);

        Vec3 meterSurface = util.vector().blockSurface(meterPos, Direction.NORTH);
        scene.overlay().showControls(meterSurface, Pointing.RIGHT, 80)
            .withItem(AllItems.GOGGLES.asStack());
        scene.idle(7);
        scene.overlay().showText(80)
            .text("When wearing Engineers' Goggles, the player can get more detailed information from the Gauge")
            .attachKeyFrame()
            .colored(PonderPalette.MEDIUM)
            .pointAt(meterSurface)
            .placeNearTarget();
        scene.idle(100);

        Selection comparator = util.select().fromTo(2, 1, 1, 2, 1, 2);
        scene.world().showSection(comparator, Direction.SOUTH);
        scene.idle(10);
        scene.world().toggleRedstonePower(comparator);
        scene.effects().indicateRedstone(util.grid().at(2, 1, 2));
        scene.idle(20);

        scene.overlay().showText(120)
            .text("Comparators can emit analog Redstone Signals relative to the Flow Meter's measurements")
            .attachKeyFrame()
            .colored(PonderPalette.RED)
            .pointAt(util.vector().centerOf(2, 1, 2).add(0, -0.35, 0))
            .placeNearTarget();
        scene.idle(130);
        scene.markAsFinished();
    }

    private static void setFlowRate(CreateSceneBuilder scene, BlockPos pos, double flowRate) {
        scene.world().modifyBlockEntity(pos, PressureGaugeBlockEntity.class,
            blockEntity -> blockEntity.setTheoreticalFlowRate(flowRate));
    }
}
