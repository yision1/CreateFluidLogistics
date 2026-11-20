package com.yision.fluidlogistics.util;

import java.util.EnumMap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public final class SidedCapabilityCache<T> {
	private final EnumMap<Direction, CacheEntry<T>> caches = new EnumMap<>(Direction.class);
	private final Capability<T> capability;

	public SidedCapabilityCache(Capability<T> capability) {
		this.capability = capability;
	}

	@Nullable
	public T get(Level level, BlockPos targetPos, Direction side) {
		if (!level.isLoaded(targetPos)) {
			caches.remove(side);
			return null;
		}
		BlockEntity blockEntity = level.getBlockEntity(targetPos);
		if (blockEntity == null || blockEntity.isRemoved()) {
			caches.remove(side);
			return null;
		}
		CacheEntry<T> cache = caches.get(side);
		if (cache == null || cache.level() != level || !cache.pos().equals(targetPos)
			|| cache.blockEntity() != blockEntity || !cache.capability().isPresent()) {
			LazyOptional<T> resolved = blockEntity.getCapability(capability, side.getOpposite());
			if (!resolved.isPresent()) {
				caches.remove(side);
				return null;
			}
			CacheEntry<T> entry = new CacheEntry<>(level, targetPos.immutable(), blockEntity, resolved);
			caches.put(side, entry);
			resolved.addListener(opt -> caches.remove(side, entry));
			cache = entry;
		}
		return cache.capability().orElse(null);
	}

	public void clear() {
		caches.clear();
	}

	private record CacheEntry<T>(Level level, BlockPos pos, BlockEntity blockEntity, LazyOptional<T> capability) {
	}
}
