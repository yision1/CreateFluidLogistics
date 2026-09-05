package com.yision.fluidlogistics.ponder;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.yision.fluidlogistics.registry.AllBlocks;
import com.yision.fluidlogistics.registry.AllItems;
import com.yision.fluidlogistics.content.processing.blazeCooler.BlazeCoolerBlockEntity;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BlazeCoolerScenes {

    public static final String CONVERSION = "blaze_cooler/conversion";
    public static final String FUELING = "blaze_cooler/fueling";

    public static void blazeCooler(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title(CONVERSION, "Cooling a Blaze Burner");
        scene.configureBasePlate(0, 0, 5);

        BlockPos burnerPos = util.grid().at(2, 1, 2);
        Selection burner = util.select().position(burnerPos);

        scene.world().showSection(util.select().everywhere().substract(burner), Direction.UP);
        scene.idle(15);

        scene.world().showSection(burner, Direction.DOWN);
        scene.overlay()
            .showText(70)
            .text("Place a Blaze Burner in a cold biome")
            .attachKeyFrame()
            .placeNearTarget()
            .pointAt(util.vector().centerOf(burnerPos));
        scene.idle(80);

        scene.overlay()
            .showText(40)
            .text("After some time")
            .attachKeyFrame()
            .placeNearTarget()
            .pointAt(util.vector().centerOf(burnerPos));
        scene.idle(80);

        scene.world().setBlock(burnerPos, AllBlocks.BLAZE_COOLER.getDefaultState()
            .setValue(BlazeBurnerBlock.FACING, Direction.SOUTH)
            .setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SMOULDERING), false);
        scene.idle(10);

        scene.overlay()
            .showOutlineWithText(burner, 80)
            .colored(PonderPalette.BLUE)
            .text("A Blaze Burner turns into a Blaze Cooler when it gets cold")
            .attachKeyFrame()
            .placeNearTarget()
            .pointAt(util.vector().centerOf(burnerPos));
        scene.idle(90);

        scene.markAsFinished();
    }

    public static void fueling(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title(FUELING, "Fueling Blaze Coolers");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos cooler = util.grid().at(2, 1, 2);
        scene.world().showSection(util.select().position(cooler), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().position(cooler.above()), Direction.DOWN);
        scene.idle(10);

        scene.overlay()
            .showText(70)
            .attachKeyFrame()
            .text("Blaze Coolers can provide cooling to items processed in a Copper Basin")
            .pointAt(util.vector().blockSurface(cooler, Direction.WEST))
            .placeNearTarget();
        scene.idle(80);

        scene.world().hideSection(util.select().position(cooler.above()), Direction.UP);
        scene.idle(20);
        scene.world().setBlock(cooler.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), false);
        scene.overlay().showControls(util.vector().topOf(cooler), Pointing.DOWN, 15).rightClick()
            .withItem(new ItemStack(Items.ICE));
        scene.idle(7);
        setCoolingLevel(scene, util, cooler, 1, 2160, BlazeBurnerBlock.HeatLevel.KINDLED);
        scene.idle(20);

        scene.overlay()
            .showText(70)
            .attachKeyFrame()
            .text("For this, the Blaze has to be fed with cooling items")
            .pointAt(util.vector().blockSurface(cooler, Direction.WEST))
            .placeNearTarget();
        scene.idle(80);

        scene.idle(20);
        scene.overlay().showControls(util.vector().topOf(cooler), Pointing.DOWN, 30).rightClick()
            .withItem(AllItems.FROST_CAKE.asStack());
        scene.idle(7);
        setCoolingLevel(scene, util, cooler, 2, 3200, BlazeBurnerBlock.HeatLevel.SEETHING);
        scene.idle(20);

        scene.overlay()
            .showText(80)
            .attachKeyFrame()
            .colored(PonderPalette.MEDIUM)
            .text("With a Frost Cake, the Cooler can reach an even stronger level of cooling")
            .pointAt(util.vector().blockSurface(cooler, Direction.WEST))
            .placeNearTarget();
        scene.idle(90);

        scene.world().modifyBlockEntityNBT(util.select().position(4, 1, 2), DeployerBlockEntity.class,
            nbt -> nbt.put("HeldItem", AllItems.FROST_CAKE.asStack()
                .save(new CompoundTag())));
        scene.world().showSection(util.select().fromTo(3, 0, 5, 2, 0, 5), Direction.UP);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(4, 1, 2, 4, 1, 5), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(2, 1, 4, 2, 1, 5), Direction.DOWN);
        scene.idle(10);

        scene.overlay()
            .showText(80)
            .attachKeyFrame()
            .text("The feeding process can be automated using Deployers or Mechanical Arms")
            .pointAt(util.vector().blockSurface(cooler.east(2), Direction.UP));
        scene.idle(90);

        scene.markAsFinished();
    }

    private static void setCoolingLevel(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos cooler,
            int fuelLevel, int burnTime, BlazeBurnerBlock.HeatLevel coolingLevel) {
        scene.world().modifyBlockEntityNBT(util.select().position(cooler), BlazeCoolerBlockEntity.class, nbt -> {
            nbt.putInt("fuelLevel", fuelLevel);
            nbt.putInt("burnTimeRemaining", burnTime);
            nbt.putInt("CFLBlazeCoolerLevel", coolingLevel.ordinal());
        }, true);
    }
}
