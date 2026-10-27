package com.yision.fluidlogistics.mixin.logistics;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.logistics.packagePort.PackagePortTarget;
import com.yision.fluidlogistics.content.logistics.copperFrogport.CopperFrogportBlock;
import com.yision.fluidlogistics.content.logistics.copperFrogport.CopperFrogportBlockEntity;
import com.yision.fluidlogistics.content.logistics.copperFrogport.CopperFrogportRules;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(value = PackagePortTarget.ChainConveyorFrogportTarget.class, remap = false)
public class ChainConveyorFrogportTargetMixin {

	@Inject(method = "canSupport", at = @At("HEAD"), cancellable = true, remap = false)
	private void fluidlogistics$supportCopperFrogport(BlockEntity blockEntity, CallbackInfoReturnable<Boolean> cir) {
		if (!(blockEntity instanceof CopperFrogportBlockEntity frogport) || frogport.getLevel() == null) {
			return;
		}

		PackagePortTarget.ChainConveyorFrogportTarget target = (PackagePortTarget.ChainConveyorFrogportTarget) (Object) this;
		Vec3 targetLocation = target.getExactTargetLocation(frogport, frogport.getLevel(), frogport.getBlockPos());
		boolean valid = !Vec3.ZERO.equals(targetLocation) && CopperFrogportRules.isChainPositionValid(
			CopperFrogportBlock.getAttachedDirection(frogport.getBlockState()), targetLocation,
			CopperFrogportBlock.getConnectionSource(frogport.getBlockPos(), frogport.getBlockState()));
		cir.setReturnValue(valid);
	}
}
