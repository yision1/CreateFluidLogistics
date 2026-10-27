package com.yision.fluidlogistics.mixin.logistics;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.logistics.box.PackageEntity;
import com.yision.fluidlogistics.api.packager.PackageDestroyContext;
import com.yision.fluidlogistics.api.packager.PackageResources;
import com.yision.fluidlogistics.content.logistics.packageResource.PackageResourceGoggleDispatcher;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(value = PackageEntity.class, remap = false)
public abstract class PackageEntityMixin implements IHaveGoggleInformation {
	@Shadow
	public ItemStack box;

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		return PackageResourceGoggleDispatcher.append(box, tooltip, isPlayerSneaking);
	}

	@Inject(method = "onInsideBlock", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/box/PackageEntity;destroy(Lnet/minecraft/world/damagesource/DamageSource;)V", remap = false), cancellable = true, remap = true)
	private void fluidlogistics$onWaterDestroy(BlockState state, CallbackInfo ci) {
		if (PackageResources.isBootstrapped() && PackageResources.survivesWater(box)) {
			ci.cancel();
		}
	}

	@Inject(method = "dropAllDeathLoot", at = @At("HEAD"), remap = true)
	private void fluidlogistics$handleDestroyedResources(DamageSource source, CallbackInfo ci) {
		PackageEntity self = (PackageEntity) (Object) this;
		if (PackageResources.isBootstrapped() && self.level() instanceof ServerLevel serverLevel) {
			PackageResources.handleDestroyed(new PackageDestroyContext(serverLevel, self, source, box));
		}
	}
}
