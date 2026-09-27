package com.yision.fluidlogistics.ponder;

import com.simibubi.create.AllFluids;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.yision.fluidlogistics.content.fluids.copperBucket.CopperBucketItem;
import com.yision.fluidlogistics.content.fluids.multiFluidTank.MultiFluidTankBlockEntity;
import com.yision.fluidlogistics.registry.AllItems;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public class CopperBucketScenes {
    public static final String INTERACTION = "copper_bucket/copper_bucket";

    public static void interaction(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title(INTERACTION, "Using Copper Buckets with Fluid Containers");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        BlockPos honeyPos = util.grid().at(2, 1, 2);
        BlockPos tankPos = util.grid().at(0, 1, 4);
        Selection honey = util.select().position(honeyPos);
        Selection tank = util.select().fromTo(tankPos, tankPos.above());

        ElementLink<WorldSectionElement> tankLink = scene.world().showIndependentSection(tank, Direction.DOWN);
        scene.world().moveSection(tankLink, util.vector().of(2, 0, -2), 0);
        scene.idle(15);

        scene.overlay().showControls(util.vector().blockSurface(honeyPos, Direction.NORTH), Pointing.RIGHT, 40)
            .rightClick().withItem(AllItems.COPPER_BUCKET.asStack());
        scene.idle(7);
        scene.overlay().showOutlineWithText(util.select().fromTo(honeyPos, honeyPos.above()), 70)
            .colored(PonderPalette.GREEN)
            .text("Copper Buckets can interact directly with fluid containers")
            .attachKeyFrame()
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(honeyPos, Direction.WEST));
        scene.idle(40);

        FluidStack honeyStack = new FluidStack(AllFluids.HONEY.get().getSource(), CopperBucketItem.CAPACITY);
        scene.world().modifyBlockEntity(tankPos, MultiFluidTankBlockEntity.class,
            be -> be.getTankInventory().drain(honeyStack, FluidAction.EXECUTE));
        scene.idle(50);

        scene.world().hideIndependentSection(tankLink, null);
        scene.idle(15);
        scene.world().showSection(honey, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showControls(util.vector().topOf(honeyPos), Pointing.DOWN, 40)
            .rightClick().withItem(AllItems.COPPER_BUCKET.asStack())
            .showing(AllIcons.I_MTD_CLOSE);
        scene.idle(7);
        scene.overlay().showOutlineWithText(honey, 70)
            .colored(PonderPalette.RED)
            .text("However, Copper Buckets cannot interact directly with fluids in the world")
            .attachKeyFrame()
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(honeyPos, Direction.WEST));
        scene.idle(80);
        scene.markAsFinished();
    }
}
