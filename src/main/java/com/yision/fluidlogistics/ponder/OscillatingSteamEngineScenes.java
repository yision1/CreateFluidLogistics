package com.yision.fluidlogistics.ponder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class OscillatingSteamEngineScenes {
	public static final String SETUP = "oscillating_steam_engine/setup";

	public static void setup(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title(SETUP, "Setting up Oscillating Steam Engines");
		scene.configureBasePlate(0, 0, 7);
		scene.setSceneOffsetY(-1);
		scene.scaleSceneView(.9f);
		scene.showBasePlate();

		Selection fakeGround = util.select().fromTo(4, 0, 0, 2, 0, 0);
		ElementLink<WorldSectionElement> fakeGroundElement = scene.world().showIndependentSection(fakeGround,
			Direction.UP);
		scene.world().moveSection(fakeGroundElement, util.vector().of(1, 0, 1), 0);

		Selection campfires = util.select().fromTo(4, 1, 4, 5, 1, 3);
		Selection burners = util.select().fromTo(3, 1, 3, 2, 1, 4);

		Selection tank = util.select().fromTo(4, 2, 3, 5, 2, 4);
		Selection boiler1 = util.select().fromTo(5, 4, 2, 4, 4, 1);
		Selection boiler2 = util.select().fromTo(5, 7, 6, 4, 4, 5);
		Selection boiler3 = util.select().fromTo(5, 4, 3, 4, 11, 4);

		Selection engine = util.select().position(2, 2, 3);
		Selection engine1 = util.select().position(3, 4, 1);
		BlockPos engine1ShaftPos = util.grid().at(2, 4, 1);
		Selection engine1Shaft = util.select().position(2, 4, 1);

		Selection pumpCogs = util.select().fromTo(6, 1, 7, 6, 1, 2);
		Selection largeCog = util.select().position(5, 0, 7);

		Selection pump1 = util.select().fromTo(5, 2, 2, 4, 1, 1);
		Selection pump2 = util.select().fromTo(5, 2, 7, 4, 1, 6);
		Selection pump3 = util.select().fromTo(2, 3, 7, 1, 1, 6);

		scene.world().modifyBlock(util.grid().at(4, 2, 7), s -> s.setValue(PumpBlock.FACING, Direction.SOUTH), false);
		scene.world().modifyBlock(util.grid().at(1, 2, 7), s -> s.setValue(PumpBlock.FACING, Direction.SOUTH), false);
		scene.world().modifyBlock(util.grid().at(2, 3, 7), s -> s.setValue(PumpBlock.FACING, Direction.SOUTH), false);

		scene.idle(15);
		ElementLink<WorldSectionElement> tankElement = scene.world().showIndependentSection(tank, Direction.DOWN);
		scene.world().moveSection(tankElement, util.vector().of(0, -1, 0), 0);
		scene.idle(10);
		ElementLink<WorldSectionElement> engineElement = scene.world().showIndependentSection(engine, Direction.EAST);
		scene.world().moveSection(engineElement, util.vector().of(1, -1, 0), 0);
		scene.idle(15);
		scene.world().moveSection(tankElement, util.vector().of(0, -1000, 0), 0);
		scene.world().hideIndependentSection(tankElement, null);
		ElementLink<WorldSectionElement> boilerElement = scene.world().showIndependentSectionImmediately(boiler1);
		scene.world().moveSection(boilerElement, util.vector().of(0, -3, 2), 0);
		scene.effects().indicateSuccess(util.grid().at(5, 1, 3));
		scene.effects().indicateSuccess(util.grid().at(4, 1, 3));
		scene.effects().indicateSuccess(util.grid().at(5, 1, 4));
		scene.effects().indicateSuccess(util.grid().at(4, 1, 4));
		scene.idle(25);

		scene.overlay().showText(50).attachKeyFrame().text("Oscillating Steam Engines can be placed on a Fluid Tank")
			.pointAt(util.vector().blockSurface(util.grid().at(3, 1, 3), Direction.WEST)).placeNearTarget();
		scene.idle(60);

		scene.overlay()
			.showControls(util.vector().blockSurface(util.grid().at(3, 1, 3), Direction.UP), Pointing.DOWN, 60)
			.withItem(AllBlocks.SHAFT.asStack()).rightClick();
		scene.idle(10);
		scene.world().setBlock(engine1ShaftPos, AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Axis.Z),
			false);
		ElementLink<WorldSectionElement> engineShaftElement = scene.world().showIndependentSection(engine1Shaft, null);
		scene.world().moveSection(engineShaftElement, util.vector().of(0, -3, 2), 0);
		scene.idle(5);
		scene.world().moveSection(engineElement, util.vector().of(0, -1000, 0), 0);
		scene.world().hideIndependentSection(engineElement, null);
		engineElement = scene.world().showIndependentSectionImmediately(engine1);
		scene.world().moveSection(engineElement, util.vector().of(0, -3, 2), 0);
		scene.world().setBlock(engine1ShaftPos,
			AllBlocks.POWERED_SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Axis.Z), false);
		scene.effects().indicateSuccess(util.grid().at(2, 1, 3));
		scene.idle(40);

		scene.overlay().showText(80).attachKeyFrame().colored(PonderPalette.BLUE)
			.text("Clicking the engine with a Shaft creates the Kinetic Output")
			.pointAt(util.vector().centerOf(util.grid().at(2, 1, 3))).placeNearTarget();
		scene.idle(90);

		scene.world().multiplyKineticSpeed(largeCog, -1);
		scene.world().multiplyKineticSpeed(pumpCogs, -1);
		scene.world().multiplyKineticSpeed(pump1, -1);
		scene.world().multiplyKineticSpeed(pump2, -1);

		scene.world().moveSection(boilerElement, util.vector().of(0, 1, 0), 15);
		scene.world().moveSection(engineElement, util.vector().of(0, 1, 0), 15);
		scene.world().moveSection(engineShaftElement, util.vector().of(0, 1, 0), 15);
		scene.idle(10);
		scene.world().showSection(campfires, Direction.NORTH);
		scene.idle(10);
		scene.world().hideIndependentSection(fakeGroundElement, Direction.DOWN);
		scene.world().showSection(largeCog, Direction.UP);
		scene.idle(5);
		scene.world().showSection(pump1, Direction.DOWN);
		scene.idle(5);
		scene.world().showSection(pumpCogs, Direction.WEST);
		scene.idle(25);

		scene.overlay().showText(60).attachKeyFrame().text("With sufficient Heat, Water and Boiler space...")
			.pointAt(util.vector().blockSurface(util.grid().at(4, 2, 4), Direction.UP)).placeNearTarget();
		scene.idle(30);

		scene.world().setKineticSpeed(engine1Shaft, 16);
		scene.effects().createRedstoneParticles(util.grid().at(3, 2, 3), 0xFFFFFF, 10);
		scene.idle(40);

		scene.overlay().showText(60).text("...they will generate Rotational Force")
			.pointAt(util.vector().blockSurface(util.grid().at(2, 2, 3), Direction.NORTH)).placeNearTarget();
		scene.idle(70);

		scene.overlay().showOutlineWithText(util.select().fromTo(5, 2, 3, 4, 2, 4), 50).attachKeyFrame()
			.text("The minimal setup requires 4 Fluid Tanks")
			.pointAt(util.vector().blockSurface(util.grid().at(4, 2, 4), Direction.UP)).placeNearTarget();
		scene.idle(60);

		scene.world().hideSection(campfires, Direction.SOUTH);
		scene.idle(15);
		ElementLink<WorldSectionElement> burnersElement = scene.world().showIndependentSection(burners,
			Direction.SOUTH);
		scene.world().moveSection(burnersElement, util.vector().of(2, 0, 0), 0);
		scene.idle(25);

		scene.overlay()
			.showControls(util.vector().blockSurface(util.grid().at(4, 1, 3), Direction.EAST), Pointing.RIGHT, 60)
			.withItem(new ItemStack(Items.OAK_LOG)).rightClick();
		scene.idle(10);
		scene.world().setBlocks(burners,
			AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.KINDLED), false);
		scene.idle(5);

		scene.world().setKineticSpeed(engine1Shaft, 64);
		scene.effects().createRedstoneParticles(util.grid().at(3, 2, 3), 0xFFFFFF, 10);
		scene.idle(40);

		scene.overlay().showText(80).text("With the help of Blaze Burners, the power output can be increased")
			.attachKeyFrame().pointAt(util.vector().blockSurface(util.grid().at(4, 1, 3), Direction.WEST))
			.placeNearTarget();
		scene.idle(90);

		scene.world().hideSection(pump1, Direction.UP);
		scene.idle(5);
		scene.world().hideIndependentSection(boilerElement, Direction.SOUTH);
		scene.idle(10);
		boilerElement = scene.world().showIndependentSection(boiler2, Direction.SOUTH);
		scene.world().moveSection(boilerElement, util.vector().of(0, -2, -2), 0);
		scene.idle(10);
		ElementLink<WorldSectionElement> pumpElement = scene.world().showIndependentSection(pump2, Direction.DOWN);
		scene.world().moveSection(pumpElement, util.vector().of(0, 0, -5), 0);
		scene.idle(20);

		Vec3 target = util.vector().blockSurface(util.grid().at(4, 3, 3), Direction.WEST);
		scene.overlay().showText(80).text("Higher power levels require more Water, Size and Heat").attachKeyFrame()
			.pointAt(target).placeNearTarget();
		scene.idle(90);

		scene.overlay().showControls(target.add(0, 0, 0.5), Pointing.DOWN, 60).withItem(AllItems.GOGGLES.asStack());
		scene.idle(6);
		scene.overlay().showText(80).text("The boiler's current power level can be inspected with Engineer's Goggles")
			.attachKeyFrame().colored(PonderPalette.BLUE).pointAt(target.add(0, 0, 0.5)).placeNearTarget();
		scene.idle(90);

		scene.world().showSectionAndMerge(util.select().fromTo(3, 4, 6, 2, 4, 6), Direction.EAST, boilerElement);
		scene.idle(5);
		scene.world().setKineticSpeed(util.select().position(2, 4, 6), 64);
		scene.world().showSectionAndMerge(util.select().fromTo(3, 5, 6, 2, 5, 6), Direction.EAST, boilerElement);
		scene.idle(5);
		scene.world().setKineticSpeed(util.select().position(2, 5, 6), -64);
		scene.world().showSectionAndMerge(util.select().fromTo(3, 5, 5, 2, 5, 5), Direction.EAST, boilerElement);
		scene.idle(5);
		scene.world().setKineticSpeed(util.select().position(2, 5, 5), -64);
		scene.world().showSectionAndMerge(util.select().fromTo(2, 4, 7, 2, 5, 7), Direction.NORTH, boilerElement);
		scene.idle(5);
		scene.world().setKineticSpeed(util.select().position(2, 5, 7), -64);
		scene.world().setKineticSpeed(util.select().position(2, 4, 7), 64);
		scene.idle(20);

		scene.overlay().showText(100)
			.text("With each added power level, an additional Engine can output at full capacity").attachKeyFrame()
			.colored(PonderPalette.GREEN).pointAt(util.vector().blockSurface(util.grid().at(2, 3, 3), Direction.NORTH))
			.placeNearTarget();
		scene.idle(110);

		scene.overlay().showText(30).text("Lvl 4").colored(PonderPalette.BLUE)
			.pointAt(util.vector().blockSurface(util.grid().at(4, 4, 4), Direction.WEST)).placeNearTarget();
		scene.idle(40);
		scene.overlay().showOutlineWithText(util.select().fromTo(3, 2, 3, 3, 3, 4), 30).text("4 Engines")
			.colored(PonderPalette.BLUE).pointAt(util.vector().blockSurface(util.grid().at(3, 3, 4), Direction.UP))
			.placeNearTarget();
		scene.idle(30);

		scene.world().hideIndependentSection(pumpElement, Direction.UP);
		scene.idle(5);
		scene.world().hideIndependentSection(boilerElement, Direction.SOUTH);
		scene.world().hideIndependentSection(engineElement, Direction.SOUTH);
		scene.world().hideIndependentSection(engineShaftElement, Direction.SOUTH);
		scene.idle(20);

		scene.overlay().showControls(util.vector().of(5, 2, 4), Pointing.DOWN, 10)
			.withItem(AllItems.BLAZE_CAKE.asStack()).rightClick();
		scene.idle(6);
		scene.world().setBlocks(burners,
			AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SEETHING), false);
		scene.idle(15);

		boilerElement = scene.world().showIndependentSection(boiler3, Direction.SOUTH);
		scene.world().moveSection(boilerElement, util.vector().of(0, -2, 0), 0);
		scene.idle(10);
		pumpElement = scene.world().showIndependentSection(pump3, Direction.DOWN);
		scene.world().moveSection(pumpElement, util.vector().of(3, 0, -5), 0);
		scene.idle(20);

		ElementLink<WorldSectionElement> cogsElement = scene.world()
			.showIndependentSection(util.select().position(2, 1, 7), Direction.NORTH);
		scene.world().moveSection(cogsElement, util.vector().of(0, -2, -2), 0);

		Selection previous = null;
		boolean previousForward = false;
		for (int y = 4; y < 9; y++) {
			if (y != 6)
				for (boolean left : Iterate.trueAndFalse) {
					int z = (left ^ y % 2 == 0) ? 3 : 4;
					if (previous != null)
						scene.world().setKineticSpeed(previous, previousForward ? 64 : -64);
					scene.world().showSectionAndMerge(previous = util.select().fromTo(3, y, z, 2, y, z), Direction.EAST,
						boilerElement);
					previousForward = y % 2 == 0;
					scene.idle(5);
				}

			scene.world().showSectionAndMerge(util.select().position(2, y, 7), Direction.NORTH, cogsElement);
			scene.world().setKineticSpeed(util.select().position(2, y, 7), y % 2 == 0 ? 64 : -64);
		}

		scene.world().setKineticSpeed(previous, 64);

		scene.overlay().showText(30).text("Lvl 8").colored(PonderPalette.BLUE)
			.pointAt(util.vector().blockSurface(util.grid().at(4, 4, 3), Direction.NORTH)).placeNearTarget();
		scene.idle(40);
		scene.overlay().showOutlineWithText(util.select().fromTo(3, 2, 3, 3, 6, 4), 30).text("8 Engines")
			.colored(PonderPalette.BLUE).pointAt(util.vector().blockSurface(util.grid().at(3, 3, 4), Direction.UP))
			.placeNearTarget();
		scene.idle(30);
	}
}
