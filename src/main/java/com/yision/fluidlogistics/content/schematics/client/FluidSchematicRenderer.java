package com.yision.fluidlogistics.content.schematics.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.schematics.client.SchematicRenderer;
import com.yision.fluidlogistics.content.schematics.FluidSchematicPlan;
import com.yision.fluidlogistics.content.schematics.FluidSchematicPlan.Kind;

import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.createmod.catnip.render.ShadedBlockSbbBuilder;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class FluidSchematicRenderer extends SchematicRenderer {

    private static final ThreadLocal<ShadedBlockSbbBuilder> FLUID_BUFFER_BUILDER =
        ThreadLocal.withInitial(ShadedBlockSbbBuilder::create);

    private final Map<RenderType, List<FluidSectionBuffer>> fluidBufferCache = new LinkedHashMap<>();
    private boolean fluidChanged = true;

    public FluidSchematicRenderer(SchematicLevel world) {
        super(createFluidLevel(world));
    }

    private static SchematicLevel createFluidLevel(SchematicLevel source) {
        SchematicLevel fluidLevel = new SchematicLevel(source.anchor, Minecraft.getInstance().level);
        fluidLevel.setBounds(source.getBounds());
        for (BlockPos localPos : source.getAllPositions()) {
            BlockPos pos = localPos.offset(source.anchor);
            BlockState state = source.getBlockState(pos);
            if (FluidSchematicPlan.classify(state) != Kind.AIR) {
                fluidLevel.setBlock(pos, state, Block.UPDATE_CLIENTS);
                fluidLevel.getBlockEntity(pos);
            }
        }
        return fluidLevel;
    }

    @Override
    public void update() {
        super.update();
        fluidChanged = true;
    }

    @Override
    public void render(PoseStack poseStack, SuperRenderTypeBuffer buffers) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        if (fluidChanged) {
            redrawFluids();
            fluidChanged = false;
        }

        super.render(poseStack, buffers);
        fluidBufferCache.forEach((layer, sectionBuffers) -> {
            for (FluidSectionBuffer sectionBuffer : sectionBuffers) {
                BlockPos offset = sectionBuffer.offset();
                poseStack.pushPose();
                poseStack.translate(offset.getX(), offset.getY(), offset.getZ());
                sectionBuffer.buffer().renderInto(poseStack, buffers.getBuffer(layer));
                poseStack.popPose();
            }
        });
    }

    private void redrawFluids() {
        fluidBufferCache.clear();
        Map<RenderType, Map<FluidSectionKey, List<BlockPos>>> positionsByLayer = new LinkedHashMap<>();

        for (BlockPos localPos : schematic.getAllPositions()) {
            BlockPos pos = localPos.offset(schematic.anchor);
            BlockState state = schematic.getBlockState(pos);
            if (FluidSchematicPlan.classify(state) == Kind.AIR) {
                continue;
            }
            FluidState fluidState = state.getFluidState();
            RenderType layer = ItemBlockRenderTypes.getRenderLayer(fluidState);
            FluidSectionKey section = FluidSectionKey.of(pos);
            positionsByLayer.computeIfAbsent(layer, ignored -> new LinkedHashMap<>())
                .computeIfAbsent(section, ignored -> new ArrayList<>())
                .add(pos);
        }

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        ShadedBlockSbbBuilder builder = FLUID_BUFFER_BUILDER.get();
        schematic.renderMode = true;
        try {
            for (RenderType layer : RenderType.chunkBufferLayers()) {
                Map<FluidSectionKey, List<BlockPos>> sections = positionsByLayer.get(layer);
                if (sections == null) {
                    continue;
                }

                List<FluidSectionBuffer> sectionBuffers = new ArrayList<>();
                for (Map.Entry<FluidSectionKey, List<BlockPos>> entry : sections.entrySet()) {
                    builder.begin();
                    VertexConsumer liquidBuffer = builder.unwrap(false);
                    if (layer == RenderType.solid()) {
                        liquidBuffer = new SolidFluidVertexConsumer(liquidBuffer);
                    }
                    for (BlockPos pos : entry.getValue()) {
                        BlockState state = schematic.getBlockState(pos);
                        dispatcher.renderLiquid(pos, schematic, liquidBuffer, state, state.getFluidState());
                    }
                    SuperByteBuffer buffer = builder.end();
                    if (!buffer.isEmpty()) {
                        sectionBuffers.add(new FluidSectionBuffer(entry.getKey().offsetFrom(schematic.anchor), buffer));
                    }
                }
                if (!sectionBuffers.isEmpty()) {
                    fluidBufferCache.put(layer, sectionBuffers);
                }
            }
        } finally {
            schematic.renderMode = false;
        }
    }

    private record FluidSectionKey(int x, int y, int z) {

        private static FluidSectionKey of(BlockPos pos) {
            return new FluidSectionKey(
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getY()),
                SectionPos.blockToSectionCoord(pos.getZ())
            );
        }

        private BlockPos offsetFrom(BlockPos anchor) {
            return new BlockPos(
                SectionPos.sectionToBlockCoord(x) - anchor.getX(),
                SectionPos.sectionToBlockCoord(y) - anchor.getY(),
                SectionPos.sectionToBlockCoord(z) - anchor.getZ()
            );
        }
    }

    private record FluidSectionBuffer(BlockPos offset, SuperByteBuffer buffer) {
    }

    private record SolidFluidVertexConsumer(VertexConsumer delegate) implements VertexConsumer {

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            delegate.setColor(red, green, blue, alpha == 0 ? 255 : alpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
            delegate.setNormal(normalX, normalY, normalZ);
            return this;
        }
    }
}
