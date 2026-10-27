package com.yision.fluidlogistics.content.processing.snowGolemFreezer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class SnowGolemFreezerParticles {

	static void spawnSmoke(Level level, Vec3 position, boolean active) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level != level)
			return;
		if (minecraft.gameRenderer.getMainCamera().getPosition().distanceToSqr(position) > 1024)
			return;

		ParticleStatus status = minecraft.options.particles().get();
		if (status == ParticleStatus.MINIMAL || status == ParticleStatus.DECREASED && level.random.nextInt(3) == 0)
			return;

		Particle smoke = minecraft.particleEngine.createParticle(ParticleTypes.LARGE_SMOKE, position.x, position.y,
			position.z, 0, 0, 0);
		if (smoke == null)
			return;
		smoke.setColor(.7294118f, .69411767f, .7607843f);
		if (!active)
			smoke.setLifetime(14);
	}
}
