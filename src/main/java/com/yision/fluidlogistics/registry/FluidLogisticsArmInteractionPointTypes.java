package com.yision.fluidlogistics.registry;

import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import com.yision.fluidlogistics.FluidLogistics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import org.jetbrains.annotations.Nullable;

public class FluidLogisticsArmInteractionPointTypes {

	public static final DeferredRegister<ArmInteractionPointType> ARM_INTERACTION_POINT_TYPES =
		DeferredRegister.create(CreateBuiltInRegistries.ARM_INTERACTION_POINT_TYPE.key(), FluidLogistics.MODID);

	public static final DeferredHolder<ArmInteractionPointType, ArmInteractionPointType> FLUID_PACKAGER =
		ARM_INTERACTION_POINT_TYPES.register("fluid_packager", FluidPackagerType::new);
	public static final DeferredHolder<ArmInteractionPointType, ArmInteractionPointType> SMART_HOPPER =
		ARM_INTERACTION_POINT_TYPES.register("smart_hopper", SmartHopperType::new);
	public static final DeferredHolder<ArmInteractionPointType, ArmInteractionPointType> BLAZE_COOLER =
		ARM_INTERACTION_POINT_TYPES.register("blaze_cooler", BlazeCoolerType::new);

	private static class FluidPackagerType extends ArmInteractionPointType {
		@Override
		public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
			return AllBlocks.FLUID_PACKAGER.has(state) || AllBlocks.FLUID_REPACKAGER.has(state);
		}

		@Override
		public @Nullable ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
			return new ArmInteractionPoint(this, level, pos, state);
		}
	}

	private static class SmartHopperType extends ArmInteractionPointType {
		@Override
		public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
			return AllBlocks.SMART_HOPPER.has(state);
		}

		@Override
		public @Nullable ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
			return new ArmInteractionPoint(this, level, pos, state);
		}
	}

	private static class BlazeCoolerType extends ArmInteractionPointType {
		@Override
		public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
			return AllBlocks.BLAZE_COOLER.has(state);
		}

		@Override
		public @Nullable ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
			return new AllArmInteractionPointTypes.BlazeBurnerPoint(this, level, pos, state);
		}
	}
}
