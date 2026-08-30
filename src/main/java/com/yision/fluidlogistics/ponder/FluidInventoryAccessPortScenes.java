package com.yision.fluidlogistics.ponder;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.yision.fluidlogistics.content.fluids.fluidPort.FluidInventoryAccessPortBlock;
import com.yision.fluidlogistics.content.fluids.multiFluidTank.MultiFluidTankBlockEntity;
import com.yision.fluidlogistics.registry.AllBlocks;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public class FluidInventoryAccessPortScenes {

	public static final String FLUID_INVENTORY_ACCESS_PORT = "fluid_inventory_access_port";

	public static void fluidInventoryAccessPort(SceneBuilder builder, SceneBuildingUtil util) {
		CreateSceneBuilder scene = new CreateSceneBuilder(builder);
		scene.title(FLUID_INVENTORY_ACCESS_PORT, "Accessing Fluid Inventories with Fluid Inventory Access Ports");
		scene.configureBasePlate(0, 0, 7);
		scene.scaleSceneView(0.9f);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos sourceTank = util.grid().at(5, 1, 3);
		BlockPos portPos = util.grid().at(4, 1, 3);
		BlockPos northTank = util.grid().at(4, 1, 1);
		BlockPos westTank = util.grid().at(2, 1, 3);
		BlockPos southTank = util.grid().at(4, 1, 5);

		Selection sourceTankS = util.select().fromTo(5, 1, 3, 5, 2, 3);
		Selection portS = util.select().position(portPos);
		Selection northBranchS = util.select().fromTo(4, 1, 1, 4, 1, 2);
		Selection westBranchS = util.select().fromTo(2, 1, 3, 3, 1, 3);
		Selection southBranchS = util.select().fromTo(4, 1, 4, 4, 2, 5);
		Selection pipesS = util.select().fromTo(6, 1, 3, 7, 1, 3);

		scene.world().showSection(util.select().position(7, 0, 3), Direction.UP);
		scene.idle(3);
		scene.world().showSection(pipesS, Direction.WEST);
		scene.idle(5);
		scene.world().showSection(sourceTankS, Direction.DOWN);
		scene.idle(10);
		fillSource(scene, sourceTank, Fluids.WATER);
		fillSource(scene, sourceTank, Fluids.LAVA);
		scene.idle(10);

		scene.overlay()
			.showText(90)
			.text("Accessing fluid containers can sometimes be troublesome")
			.placeNearTarget()
			.pointAt(util.vector().topOf(sourceTank));
		scene.idle(100);

		scene.world().showSection(portS, Direction.EAST);
		scene.idle(20);
		scene.overlay()
			.showText(90)
			.text("Fluid Inventory Access Ports can extend fluid containers for easier access")
			.placeNearTarget()
			.pointAt(util.vector().blockSurface(portPos, Direction.WEST));
		scene.idle(100);

		scene.world().showSection(northBranchS, Direction.SOUTH);
		scene.idle(5);
		scene.world().showSection(westBranchS, Direction.EAST);
		scene.idle(5);
		scene.world().showSection(southBranchS, Direction.NORTH);
		scene.idle(20);
		scene.overlay().showOutline(PonderPalette.GREEN, "north", northBranchS, 80);
		scene.overlay().showOutline(PonderPalette.BLUE, "west", westBranchS, 80);
		scene.overlay().showOutline(PonderPalette.OUTPUT, "south", southBranchS, 80);
		scene.overlay()
			.showText(80)
			.text("Fluid Inventory Access Ports can access fluids from any side")
			.attachKeyFrame()
			.placeNearTarget()
			.pointAt(util.vector().topOf(portPos));
		scene.idle(90);

		for (int i = 0; i < 3; i++) {
			transferFluid(scene, sourceTank, northTank, Fluids.WATER);
			transferFluid(scene, sourceTank, westTank, Fluids.LAVA);
			scene.world().modifyBlockEntity(southTank, MultiFluidTankBlockEntity.class,
				be -> be.getTankInventory().fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE));
			scene.world().modifyBlockEntity(southTank, MultiFluidTankBlockEntity.class,
				be -> be.getTankInventory().fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE));
			scene.idle(5);
		}
		scene.idle(20);

		scene.world().hideSection(northBranchS, Direction.NORTH);
		scene.world().hideSection(westBranchS, Direction.WEST);
		scene.world().hideSection(southBranchS, Direction.SOUTH);
		scene.idle(20);

		BlockPos leverPos = util.grid().at(4, 2, 3);
		Selection leverS = util.select().position(leverPos);
		scene.world().setBlock(leverPos, Blocks.LEVER.defaultBlockState()
			.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL)
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
			.setValue(BlockStateProperties.POWERED, false), false);
		scene.world().showSection(leverS, Direction.DOWN);
		scene.idle(20);
		scene.overlay()
			.showText(80)
			.text("Fluid Inventory Access Ports can also be disabled by redstone signals")
			.attachKeyFrame()
			.placeNearTarget()
			.pointAt(util.vector().centerOf(leverPos));
		scene.idle(90);
		scene.world().modifyBlock(leverPos, state -> state.cycle(BlockStateProperties.POWERED), false);
		scene.world().modifyBlock(portPos,
			state -> state.setValue(FluidInventoryAccessPortBlock.ATTACHED, false), false);
		scene.idle(30);

		scene.world().hideSection(leverS, Direction.UP);
		scene.idle(10);
		scene.world().setBlock(leverPos, Blocks.AIR.defaultBlockState(), false);
		scene.world().modifyBlock(portPos,
			state -> state.setValue(FluidInventoryAccessPortBlock.ATTACHED, true), false);

		BlockPos chainedPortPos = util.grid().at(3, 1, 3);
		Selection chainedPortS = util.select().position(chainedPortPos);
		scene.world().setBlock(chainedPortPos, AllBlocks.FLUID_INVENTORY_ACCESS_PORT.getDefaultState()
			.setValue(FluidInventoryAccessPortBlock.FACING, Direction.EAST)
			.setValue(FluidInventoryAccessPortBlock.TARGET, AttachFace.WALL)
			.setValue(FluidInventoryAccessPortBlock.ATTACHED, false), false);
		scene.world().showSection(chainedPortS, Direction.EAST);
		scene.idle(20);
		scene.overlay()
			.showOutlineWithText(chainedPortS.add(portS), 80)
			.text("Note that Fluid Inventory Access Ports cannot be chained")
			.colored(PonderPalette.RED)
			.attachKeyFrame()
			.placeNearTarget()
			.pointAt(util.vector().centerOf(chainedPortPos));
		scene.idle(90);

		BlockPos topPortPos = util.grid().at(5, 3, 3);
		Selection topPortS = util.select().position(topPortPos);
		scene.world().setBlock(topPortPos, AllBlocks.FLUID_INVENTORY_ACCESS_PORT.getDefaultState()
			.setValue(FluidInventoryAccessPortBlock.FACING, Direction.NORTH)
			.setValue(FluidInventoryAccessPortBlock.TARGET, AttachFace.FLOOR)
			.setValue(FluidInventoryAccessPortBlock.ATTACHED, true), false);
		scene.world().showSection(topPortS, Direction.DOWN);
		scene.idle(20);
		scene.overlay().showOutline(PonderPalette.OUTPUT, "multiple_ports", topPortS.add(portS), 90);
		scene.overlay()
			.showText(90)
			.text("However, one fluid container can connect to multiple Fluid Inventory Access Ports at the same time")
			.placeNearTarget()
			.pointAt(util.vector().centerOf(topPortPos));
		scene.idle(100);

		scene.markAsFinished();
	}

	private static void fillSource(CreateSceneBuilder scene, BlockPos sourceTank, Fluid fluid) {
		for (int i = 0; i < 4; i++) {
			scene.world().modifyBlockEntity(sourceTank, MultiFluidTankBlockEntity.class,
				be -> be.getTankInventory().fill(new FluidStack(fluid, 1000), FluidAction.EXECUTE));
			scene.idle(5);
		}
	}

	private static void transferFluid(CreateSceneBuilder scene, BlockPos sourceTank, BlockPos targetTank,
		Fluid fluid) {
		scene.world().modifyBlockEntity(sourceTank, MultiFluidTankBlockEntity.class,
			be -> be.getTankInventory().drain(new FluidStack(fluid, 1000), FluidAction.EXECUTE));
		scene.world().modifyBlockEntity(targetTank, FluidTankBlockEntity.class,
			be -> be.getTankInventory().fill(new FluidStack(fluid, 1000), FluidAction.EXECUTE));
	}
}
