package com.yision.fluidlogistics.content.processing.cooling;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import com.yision.fluidlogistics.content.processing.blazeCooler.BlazeCoolerBlockEntity;
import com.yision.fluidlogistics.registry.AllBlocks;
import com.yision.fluidlogistics.registry.AllFluidLogisticsRecipeTypes;

import net.createmod.catnip.theme.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BulkCoolingFanProcessingType implements FanProcessingType {

    @Override
    public boolean isValidAt(Level level, BlockPos pos) {
        return AllBlocks.BLAZE_COOLER.has(level.getBlockState(pos))
            && level.getBlockEntity(pos) instanceof BlazeCoolerBlockEntity cooler
            && isValidHeatLevel(cooler.getHeatLevelFromBlock());
    }

    @Override
    public int getPriority() {
        return 500;
    }

    @Override
    public boolean canProcess(ItemStack stack, Level level) {
        return findRecipe(stack, level).isPresent();
    }

    @Override
    @Nullable
    public List<ItemStack> process(ItemStack stack, Level level) {
        return findRecipe(stack, level)
            .map(RecipeHolder::value)
            .map(recipe -> RecipeApplier.applyRecipeOn(level, stack, recipe, true))
            .orElse(null);
    }

    @Override
    public void spawnProcessingParticles(Level level, Vec3 pos) {
        if (level.random.nextInt(8) != 0)
            return;
        int color = Color.mixColors(getLightParticleColor(), getDarkParticleColor(), level.random.nextFloat());
        level.addParticle(new DustParticleOptions(new Color(color).asVectorF(), 1),
            pos.x + (level.random.nextFloat() - .5f) * .5f,
            pos.y + .5f,
            pos.z + (level.random.nextFloat() - .5f) * .5f,
            0, 1 / 16f, 0);
        level.addParticle(ParticleTypes.SNOWFLAKE,
            pos.x + (level.random.nextFloat() - .5f) * .5f,
            pos.y + .5f,
            pos.z + (level.random.nextFloat() - .5f) * .5f,
            0, 1 / 16f, 0);
    }

    @Override
    public void morphAirFlow(AirFlowParticleAccess particleAccess, RandomSource random) {
        particleAccess.setColor(Color.mixColors(getLightParticleColor(), getDarkParticleColor(), random.nextFloat()));
        particleAccess.setAlpha(getParticleAlpha());
        if (random.nextFloat() < 1 / 16f)
            particleAccess.spawnExtraParticle(ParticleTypes.SNOWFLAKE, .125f);
    }

    @Override
    public void affectEntity(Entity entity, Level level) {
        if (level.isClientSide || !(entity instanceof Player player) || !player.canFreeze())
            return;

        player.setTicksFrozen(Math.min(player.getTicksRequiredToFreeze(), player.getTicksFrozen() + 3));
    }

    private Optional<RecipeHolder<BulkCoolingRecipe>> findRecipe(ItemStack stack, Level level) {
        return level.getRecipeManager().getRecipeFor(
            getRecipeType().getType(), new SingleRecipeInput(stack), level);
    }

    protected boolean isValidHeatLevel(HeatLevel heatLevel) {
        return heatLevel.isAtLeast(HeatLevel.FADING);
    }

    protected IRecipeTypeInfo getRecipeType() {
        return AllFluidLogisticsRecipeTypes.BULK_COOLING;
    }

    protected int getLightParticleColor() {
        return 0xB8F4FF;
    }

    protected int getDarkParticleColor() {
        return 0x3BA8C7;
    }

    protected float getParticleAlpha() {
        return .8f;
    }
}
