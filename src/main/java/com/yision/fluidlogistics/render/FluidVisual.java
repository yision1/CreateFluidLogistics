package com.yision.fluidlogistics.render;

import com.yision.fluidlogistics.content.logistics.fluidPackage.client.FluidPackageItemRenderer.FluidDisplayData;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.util.InstanceRecycler;
import net.createmod.catnip.data.Iterate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

import org.joml.Matrix4fc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FluidVisual {
    private final VisualizationContext context;
    private final Map<FluidPackageFluidMesh, InstanceRecycler<TransformedInstance>> surfaces = new HashMap<>();
    private final Set<FluidPackageFluidMesh> usedSurfaces = new HashSet<>();
    private final Direction[] sides = Arrays.copyOfRange(Iterate.directions, 1, Iterate.directions.length);

    public FluidVisual(VisualizationContext context) {
        this.context = context;
    }

    public void update(FluidDisplayData data, Matrix4fc pose, int light) {
        if (data == null) return;

        var fluidStack = data.fluid();
        IClientFluidTypeExtensions clientFluid = IClientFluidTypeExtensions.of(fluidStack.getFluid());
        var atlas = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite stillTexture = atlas.apply(clientFluid.getStillTexture(fluidStack));
        int color = clientFluid.getTintColor(fluidStack);
        int fluidLight = LightTexture.pack(
            Math.max(LightTexture.block(light), fluidStack.getFluidType().getLightLevel(fluidStack)),
            LightTexture.sky(light));

        for (Direction side : sides) {
            Direction renderedSide = data.gas() && side == Direction.UP ? Direction.DOWN : side;
            var mesh = new FluidPackageFluidMesh(stillTexture, renderedSide, data.minY(), data.maxY());
            usedSurfaces.add(mesh);
            TransformedInstance buffer = surfaces.computeIfAbsent(mesh, key -> {
                var instancer = context.instancerProvider().instancer(InstanceTypes.TRANSFORMED, key.model());
                return new InstanceRecycler<>(instancer::createInstance);
            }).get();
            buffer.setTransform(pose);
            buffer.colorArgb(color).light(fluidLight);
            buffer.setChanged();
        }
    }

    public void begin() {
        usedSurfaces.clear();
        surfaces.values().forEach(InstanceRecycler::resetCount);
    }

    public void end() {
        surfaces.entrySet().removeIf(entry -> {
            if (!usedSurfaces.contains(entry.getKey())) {
                entry.getValue().delete();
                return true;
            }
            entry.getValue().discardExtra();
            return false;
        });
    }

    public void delete() {
        surfaces.values().forEach(InstanceRecycler::delete);
        surfaces.clear();
        usedSurfaces.clear();
    }
}
