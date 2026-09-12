package com.yision.fluidlogistics.render;

import com.yision.fluidlogistics.content.logistics.fluidPackage.client.FluidPackageItemRenderer;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.vertex.MutableVertexList;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.QuadMesh;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Vector4f;
import org.joml.Vector4fc;

record FluidPackageFluidMesh(TextureAtlasSprite texture, Direction side, float minY, float maxY) implements QuadMesh {
    Model model() {
        return new SingleMeshModel(this, SimpleMaterial.builder()
            .cardinalLightingMode(CardinalLightingMode.OFF)
            .texture(texture.atlasLocation())
            .transparency(Transparency.ORDER_INDEPENDENT)
            .build());
    }

    @Override
    public int vertexCount() {
        return 4;
    }

    @Override
    public void write(MutableVertexList vertices) {
        float min = FluidPackageItemRenderer.FLUID_MIN_XZ;
        float max = FluidPackageItemRenderer.FLUID_MAX_XZ;
        boolean horizontal = side.getAxis().isHorizontal();
        float down = horizontal ? minY : min;
        float up = horizontal ? maxY : max;
        boolean reverseU = side == Direction.NORTH || side == Direction.EAST;
        float shrink = texture.uvShrinkRatio() * 0.25f;
        float centerU = (texture.getU0() + texture.getU1()) * 0.5f;
        float centerV = (texture.getV0() + texture.getV1()) * 0.5f;
        float u1 = Mth.lerp(shrink, texture.getU(reverseU ? 1 - max : min), centerU);
        float u2 = Mth.lerp(shrink, texture.getU(reverseU ? 1 - min : max), centerU);
        float v1 = Mth.lerp(shrink, texture.getV(side == Direction.UP ? down : 1 - up), centerV);
        float v2 = Mth.lerp(shrink, texture.getV(side == Direction.UP ? up : 1 - down), centerV);

        switch (side) {
            case WEST -> {
                vertex(vertices, 0, min, maxY, min, u1, v1);
                vertex(vertices, 1, min, minY, min, u1, v2);
                vertex(vertices, 2, min, minY, max, u2, v2);
                vertex(vertices, 3, min, maxY, max, u2, v1);
            }
            case EAST -> {
                vertex(vertices, 0, max, maxY, max, u1, v1);
                vertex(vertices, 1, max, minY, max, u1, v2);
                vertex(vertices, 2, max, minY, min, u2, v2);
                vertex(vertices, 3, max, maxY, min, u2, v1);
            }
            case NORTH -> {
                vertex(vertices, 0, max, maxY, min, u1, v1);
                vertex(vertices, 1, max, minY, min, u1, v2);
                vertex(vertices, 2, min, minY, min, u2, v2);
                vertex(vertices, 3, min, maxY, min, u2, v1);
            }
            case SOUTH -> {
                vertex(vertices, 0, min, maxY, max, u1, v1);
                vertex(vertices, 1, min, minY, max, u1, v2);
                vertex(vertices, 2, max, minY, max, u2, v2);
                vertex(vertices, 3, max, maxY, max, u2, v1);
            }
            case UP -> {
                vertex(vertices, 0, min, maxY, min, u1, v1);
                vertex(vertices, 1, min, maxY, max, u1, v2);
                vertex(vertices, 2, max, maxY, max, u2, v2);
                vertex(vertices, 3, max, maxY, min, u2, v1);
            }
            case DOWN -> {
                vertex(vertices, 0, min, minY, max, u1, v1);
                vertex(vertices, 1, min, minY, min, u1, v2);
                vertex(vertices, 2, max, minY, min, u2, v2);
                vertex(vertices, 3, max, minY, max, u2, v1);
            }
        }
    }

    private void vertex(MutableVertexList vertices, int index, float x, float y, float z, float u, float v) {
        vertices.x(index, x);
        vertices.y(index, y);
        vertices.z(index, z);
        vertices.u(index, u);
        vertices.v(index, v);
        vertices.r(index, 1);
        vertices.g(index, 1);
        vertices.b(index, 1);
        vertices.a(index, 1);
        vertices.light(index, 0);
        vertices.overlay(index, OverlayTexture.NO_OVERLAY);
        vertices.normalX(index, side.getStepX());
        vertices.normalY(index, side.getStepY());
        vertices.normalZ(index, side.getStepZ());
    }

    @Override
    public Vector4fc boundingSphere() {
        float width = FluidPackageItemRenderer.FLUID_WIDTH;
        float height = maxY - minY;
        return new Vector4f(0.5f, (minY + maxY) * 0.5f, 0.5f,
            Mth.sqrt(2 * width * width + height * height) * 0.5f);
    }
}
