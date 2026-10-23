package com.yision.fluidlogistics.content.processing.snowGolemFreezer;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.RisingParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SnowGolemFreezerSnowflakeParticle extends RisingParticle {

	private final SpriteSet sprites;

	private SnowGolemFreezerSnowflakeParticle(ClientLevel level, double x, double y, double z,
		double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
		super(level, x, y, z, xSpeed, ySpeed, zSpeed);
		this.sprites = sprites;
		gravity = 0;
		hasPhysics = false;
		yd = Math.max(0, yd);
		quadSize = .1f * (random.nextFloat() * random.nextFloat() + 1);
		setColor(.923f, .964f, .999f);
		setSpriteFromAge(sprites);
	}

	@Override
	public void tick() {
		super.tick();
		setSpriteFromAge(sprites);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
	}

	public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
		@Override
		public SnowGolemFreezerSnowflakeParticle createParticle(SimpleParticleType type, ClientLevel level,
			double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			return new SnowGolemFreezerSnowflakeParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
		}
	}
}
