package com.yision.fluidlogistics.registry;

import static com.yision.fluidlogistics.FluidLogistics.REGISTRATE;
import com.tterrag.registrate.util.entry.FluidEntry;
import com.yision.fluidlogistics.content.fluids.powderSnow.PowderSnowFluid;
import com.yision.fluidlogistics.content.fluids.powderSnow.PowderSnowFluidType;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.common.SoundActions;

public class AllFluidLogisticsFluids {

    public static final FluidEntry<PowderSnowFluid> POWDER_SNOW = REGISTRATE.virtualFluid(
            "powder_snow",
            ResourceLocation.withDefaultNamespace("block/powder_snow"),
            ResourceLocation.withDefaultNamespace("block/powder_snow"),
            PowderSnowFluidType::new,
            PowderSnowFluid::createSource,
            PowderSnowFluid::createFlowing)
        .properties(properties -> properties
            .density(600)
            .viscosity(4000)
            .temperature(260)
            .canPushEntity(false)
            .canSwim(false)
            .canDrown(false)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_POWDER_SNOW)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_POWDER_SNOW))
        .register();

    private AllFluidLogisticsFluids() {
    }

    public static void register() {
    }
}
