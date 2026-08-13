package com.yision.fluidlogistics.content.schematics.cannon;

import java.util.UUID;

import com.yision.fluidlogistics.content.schematics.FluidSchematicPlan.Cell;
import com.yision.fluidlogistics.content.schematics.FluidSchematicPlan.Kind;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;

public final class FluidShotPayload {

    public static final String KEY = "fluidlogistics:fluid_shot";

    public record Data(
        UUID jobId,
        int planHash,
        int cellIndex,
        Kind kind,
        Fluid fluid,
        boolean fluidConsumed,
        Block expectedHost
    ) {

        public FluidStack requiredFluid() {
            return new FluidStack(fluid, FluidType.BUCKET_VOLUME);
        }
    }

    private FluidShotPayload() {
    }

    public static CompoundTag create(UUID jobId, int planHash, Cell cell, boolean fluidConsumed) {
        CompoundTag shot = new CompoundTag();
        shot.putUUID("Job", jobId);
        shot.putInt("PlanHash", planHash);
        shot.putInt("Cell", cell.index());
        shot.putString("Kind", cell.kind().name());
        shot.putString("Fluid", BuiltInRegistries.FLUID.getKey(cell.fluid().getFluid()).toString());
        shot.putBoolean("FluidConsumed", fluidConsumed);
        shot.putString("Host", BuiltInRegistries.BLOCK.getKey(cell.expectedHost()).toString());
        CompoundTag data = new CompoundTag();
        data.put(KEY, shot);
        return data;
    }

    public static boolean isFluidShot(CompoundTag data) {
        return data != null && data.contains(KEY, Tag.TAG_COMPOUND);
    }

    public static Data read(CompoundTag data) {
        if (!isFluidShot(data)) {
            return null;
        }
        CompoundTag shot = data.getCompound(KEY);
        if (!shot.hasUUID("Job")) {
            return null;
        }
        try {
            Kind kind = Kind.valueOf(shot.getString("Kind"));
            ResourceLocation fluidId = ResourceLocation.tryParse(shot.getString("Fluid"));
            ResourceLocation hostId = ResourceLocation.tryParse(shot.getString("Host"));
            Fluid fluid = fluidId == null ? null : BuiltInRegistries.FLUID.getOptional(fluidId).orElse(null);
            Block host = hostId == null ? null : BuiltInRegistries.BLOCK.getOptional(hostId).orElse(null);
            if (fluid == null || fluid == Fluids.EMPTY || host == null) {
                return null;
            }
            return new Data(
                shot.getUUID("Job"),
                shot.getInt("PlanHash"),
                shot.getInt("Cell"),
                kind,
                fluid,
                shot.getBoolean("FluidConsumed"),
                host
            );
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
