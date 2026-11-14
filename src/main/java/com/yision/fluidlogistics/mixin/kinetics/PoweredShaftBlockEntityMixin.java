package com.yision.fluidlogistics.mixin.kinetics;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlockEntity;
import com.yision.fluidlogistics.content.kinetics.oscillatingSteamEngine.OscillatingEngineDrive;
import com.yision.fluidlogistics.content.kinetics.oscillatingSteamEngine.OscillatingEngineShaft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

@Mixin(PoweredShaftBlockEntity.class)
public class PoweredShaftBlockEntityMixin implements OscillatingEngineShaft {
	@Unique
	private OscillatingEngineDrive fluidlogistics$oscillatingDrive;

	@Override
	public OscillatingEngineDrive fluidlogistics$getOscillatingDrive() {
		if (fluidlogistics$oscillatingDrive == null)
			fluidlogistics$oscillatingDrive = new OscillatingEngineDrive((PoweredShaftBlockEntity) (Object) this);
		return fluidlogistics$oscillatingDrive;
	}

	@Inject(method = "canBePoweredBy", at = @At("HEAD"), cancellable = true)
	private void fluidlogistics$canConnect(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		OscillatingEngineDrive drive = fluidlogistics$getOscillatingDrive();
		if (drive.isActive() || drive.isEngine(pos))
			cir.setReturnValue(drive.canConnect(pos));
	}

	@Inject(method = "isPoweredBy", at = @At("HEAD"), cancellable = true)
	private void fluidlogistics$isConnected(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		OscillatingEngineDrive drive = fluidlogistics$getOscillatingDrive();
		if (drive.isActive())
			cir.setReturnValue(drive.contains(pos));
	}

	@Inject(method = "update", at = @At("HEAD"), cancellable = true)
	private void fluidlogistics$updateEngine(BlockPos pos, int direction, float efficiency, CallbackInfo ci) {
		OscillatingEngineDrive drive = fluidlogistics$getOscillatingDrive();
		if (drive.isActive() || drive.isEngine(pos)) {
			drive.connect(pos);
			ci.cancel();
		}
	}

	@Inject(method = "remove(Lnet/minecraft/core/BlockPos;)V", at = @At("HEAD"), cancellable = true)
	private void fluidlogistics$removeEngine(BlockPos pos, CallbackInfo ci) {
		OscillatingEngineDrive drive = fluidlogistics$getOscillatingDrive();
		if (drive.isActive()) {
			drive.remove(pos);
			ci.cancel();
		}
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void fluidlogistics$tickEngines(CallbackInfo ci) {
		fluidlogistics$getOscillatingDrive().tick();
	}

	@Inject(method = "getCombinedCapacity", at = @At("HEAD"), cancellable = true)
	private void fluidlogistics$getCapacity(CallbackInfoReturnable<Float> cir) {
		OscillatingEngineDrive drive = fluidlogistics$getOscillatingDrive();
		if (drive.isActive())
			cir.setReturnValue(drive.getCombinedCapacity());
	}

	@Inject(method = "write", at = @At("RETURN"))
	private void fluidlogistics$writeEngines(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket,
		CallbackInfo ci) {
		fluidlogistics$getOscillatingDrive().write(tag);
	}

	@Inject(method = "read", at = @At("RETURN"))
	private void fluidlogistics$readEngines(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket,
		CallbackInfo ci) {
		fluidlogistics$getOscillatingDrive().read(tag);
	}
}
