package com.yision.fluidlogistics.mixin.fluids;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.yision.fluidlogistics.content.fluids.fluidPump.FluidPumpNetworkUpdater;
import com.yision.fluidlogistics.content.fluids.fluidPump.FluidPumpNetworkUpdater.PropagationContext;

import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(value = FluidPropagator.class, remap = false)
public class FluidPropagatorMixin {

	@Inject(
		method = "propagateChangedPipe",
		at = @At(
			value = "INVOKE",
			target = "Lcom/simibubi/create/content/fluids/FluidTransportBehaviour;wipePressure()V",
			remap = false
		),
		remap = false
	)
	private static void fluidlogistics$recordCreateTraversal(LevelAccessor world, BlockPos pipePos,
		BlockState pipeState, CallbackInfo ci, @Local Pair<Integer, BlockPos> pair,
		@Local FluidTransportBehaviour pipe,
		@Share("fluidPumpPropagation") LocalRef<PropagationContext> contextRef,
		@Share("fluidPumpPropagationInitialized") LocalBooleanRef initialized) {
		if (!initialized.get()) {
			contextRef.set(FluidPumpNetworkUpdater.createContext(world));
			initialized.set(true);
		}
		PropagationContext context = contextRef.get();
		if (context != null)
			context.recordCreatePipe(pair.getSecond(), pair.getFirst(), pipe);
	}

	@ModifyExpressionValue(
		method = "propagateChangedPipe",
		at = @At(
			value = "INVOKE",
			target = "Lcom/simibubi/create/content/fluids/FluidTransportBehaviour;hasAnyPressure()Z",
			remap = false
		),
		remap = false
	)
	private static boolean fluidlogistics$captureFluidPumpFrontier(boolean original, LevelAccessor world,
		BlockPos pipePos, BlockState pipeState, @Local(ordinal = 2) BlockPos target,
		@Local(ordinal = 2) BlockState targetState, @Local(ordinal = 1) FluidTransportBehaviour targetPipe,
		@Local Integer distance, @Local Direction direction,
		@Share("fluidPumpPropagation") LocalRef<PropagationContext> contextRef) {
		if (original)
			return original;
		PropagationContext context = contextRef.get();
		if (context == null || !context.shouldContinueFromCreateCutoff(distance))
			return original;
		if (!targetPipe.canHaveFlowToward(targetState, direction.getOpposite()))
			return original;
		context.addCutoff(target, distance + 1);
		return original;
	}

	@Inject(method = "propagateChangedPipe", at = @At("TAIL"))
	private static void fluidlogistics$propagateChangedPipeToFluidPumps(LevelAccessor world, BlockPos pipePos,
		BlockState pipeState, CallbackInfo ci,
		@Share("fluidPumpPropagation") LocalRef<PropagationContext> contextRef) {
		FluidPumpNetworkUpdater.finishPropagationForFluidPumps(world, contextRef.get());
	}
}
