package com.yision.fluidlogistics.ponder;

import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.yision.fluidlogistics.content.processing.snowGolemFreezer.SnowGolemFreezerBlockEntity;
import com.yision.fluidlogistics.registry.AllItems;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class SnowGolemFreezerScenes {

	public static final String CAPTURING = "snow_golem_freezer/capturing";
	public static final String FUELING = "snow_golem_freezer/fueling";

	public static void capturing(SceneBuilder scene, SceneBuildingUtil util) {
		scene.title(CAPTURING, "Using Empty Snow Golem Freezers");
		scene.configureBasePlate(0, 0, 5);
		scene.world().showSection(util.select().layer(0), Direction.UP);
		scene.idle(10);
		BlockPos center = util.grid().at(2, 0, 2);

		scene.world().createEntity(w -> {
			SnowGolem snowGolem = EntityType.SNOW_GOLEM.create(w);
			Vec3 v = util.vector().topOf(center);
			snowGolem.setPosRaw(v.x, v.y, v.z);
			snowGolem.setYRot(snowGolem.yRotO = 180);
			snowGolem.yHeadRot = snowGolem.yHeadRotO = 180;
			snowGolem.yBodyRot = snowGolem.yBodyRotO = 180;
			snowGolem.setNoAi(true);
			return snowGolem;
		});

		scene.idle(20);
		scene.overlay().showControls(util.vector().centerOf(center.above(2)), Pointing.DOWN, 40).rightClick()
				.withItem(AllItems.EMPTY_SNOW_GOLEM_FREEZER.asStack());
		scene.idle(10);
		scene.overlay().showText(60)
			.text("Right-click a Snow Golem with the empty freezer to capture it")
			.attachKeyFrame()
			.pointAt(util.vector().blockSurface(center.above(2), Direction.WEST))
			.placeNearTarget();
		scene.idle(50);

		scene.world().modifyEntities(SnowGolem.class, Entity::discard);
		scene.idle(20);

		scene.world().showSection(util.select().position(2, 1, 2), Direction.DOWN);
		scene.idle(20);
		scene.overlay().showControls(util.vector().topOf(center.above()), Pointing.DOWN, 40).rightClick()
			.withItem(AllItems.EMPTY_SNOW_GOLEM_FREEZER.asStack());
		scene.idle(10);
		scene.overlay().showText(60)
			.text("Alternatively, Snow Golems can be collected from their Spawners directly")
			.attachKeyFrame()
			.pointAt(util.vector().blockSurface(center.above(), Direction.WEST))
			.placeNearTarget();
		scene.idle(50);
		scene.world().hideSection(util.select().position(2, 1, 2), Direction.UP);
		scene.idle(20);
		scene.world().showSection(util.select().position(1, 1, 2), Direction.DOWN);
		scene.idle(20);

		setFreezingLevel(scene, util, util.grid().at(1, 1, 2), 1, 600, BlazeBurnerBlock.HeatLevel.KINDLED);
		scene.overlay().showText(70)
			.text("You now have an ideal cooling source for various machines")
			.attachKeyFrame()
			.pointAt(util.vector().blockSurface(center.west()
				.above(), Direction.WEST))
			.placeNearTarget();
		scene.idle(80);
	}

	public static void fueling(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title(FUELING, "Feeding Snow Golem Freezers");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(10);

		BlockPos freezer = util.grid().at(2, 1, 2);
		scene.world().showSection(util.select().position(freezer), Direction.DOWN);
		scene.idle(10);
		scene.world().showSection(util.select().position(freezer.above()), Direction.DOWN);
		scene.idle(10);

		scene.overlay()
			.showText(70)
			.attachKeyFrame()
			.text("Snow Golem Freezers can provide a cooling source for items processed in a Copper Basin")
			.pointAt(util.vector().blockSurface(freezer, Direction.WEST))
			.placeNearTarget();
		scene.idle(80);

		scene.world().hideSection(util.select().position(freezer.above()), Direction.UP);
		scene.idle(20);
		scene.world().setBlock(freezer.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), false);
		scene.overlay().showControls(util.vector().topOf(freezer), Pointing.DOWN, 15).rightClick()
			.withItem(new ItemStack(Items.SNOW_BLOCK));
		scene.idle(7);
		setFreezingLevel(scene, util, freezer, 1, 400, BlazeBurnerBlock.HeatLevel.KINDLED);
		scene.idle(20);

		scene.overlay()
			.showText(70)
			.attachKeyFrame()
			.text("Feed the Snow Golem Freezer with freezing fuel")
			.pointAt(util.vector().blockSurface(freezer, Direction.WEST))
			.placeNearTarget();
		scene.idle(80);

		scene.world().modifyBlockEntityNBT(util.select().position(4, 1, 2), DeployerBlockEntity.class,
			nbt -> nbt.put("HeldItem", new ItemStack(Items.SNOW_BLOCK)
				.saveOptional(scene.world().getHolderLookupProvider())), true);
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
			.pointAt(util.vector().blockSurface(freezer.east(2), Direction.UP));
		scene.idle(90);

		scene.overlay().showControls(util.vector().topOf(freezer), Pointing.DOWN, 40).rightClick()
			.withItem(new ItemStack(Items.CARVED_PUMPKIN));
		scene.idle(10);
		scene.overlay()
			.showText(80)
			.attachKeyFrame()
			.text("Right-click with a Carved Pumpkin to give the Snow Golem a pumpkin head")
			.pointAt(util.vector().blockSurface(freezer, Direction.WEST))
			.placeNearTarget();
		scene.idle(20);
		scene.world().modifyBlockEntityNBT(util.select().position(freezer), SnowGolemFreezerBlockEntity.class,
			nbt -> nbt.putBoolean("PumpkinHead", true), true);
		scene.idle(70);

		scene.markAsFinished();
	}

	private static void setFreezingLevel(SceneBuilder scene, SceneBuildingUtil util, BlockPos freezer,
		int fuelLevel, int burnTime, BlazeBurnerBlock.HeatLevel freezingLevel) {
		scene.world().modifyBlockEntityNBT(util.select().position(freezer), SnowGolemFreezerBlockEntity.class, nbt -> {
			nbt.putInt("fuelLevel", fuelLevel);
			nbt.putInt("burnTimeRemaining", burnTime);
			nbt.putInt("CFLBlazeCoolerLevel", freezingLevel.ordinal());
		}, true);
	}
}
