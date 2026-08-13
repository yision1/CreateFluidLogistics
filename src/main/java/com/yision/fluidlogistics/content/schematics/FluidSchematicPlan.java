package com.yision.fluidlogistics.content.schematics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.foundation.fluid.FluidHelper;

import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.createmod.catnip.math.BBHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

public final class FluidSchematicPlan {

    public enum Kind {
        WATERLOGGED,
        FREE_SOURCE,
        AIR
    }

    public record Cell(int index, BlockPos relativePos, Kind kind, BlockState previewState, FluidStack fluid) {

        public Block expectedHost() {
            return previewState.getBlock();
        }
    }

    private static final Comparator<Cell> PRINT_ORDER = Comparator
        .comparingInt((Cell cell) -> cell.relativePos().getY())
        .thenComparingInt(cell -> cell.relativePos().getZ())
        .thenComparingInt(cell -> cell.relativePos().getX());

    private final BlockPos anchor;
    private final List<Cell> cells;
    private final BoundingBox bounds;
    private final boolean errored;
    private final int hash;

    private FluidSchematicPlan(BlockPos anchor, List<Cell> cells, boolean errored) {
        this.anchor = anchor;
        List<Cell> sorted = new ArrayList<>(cells);
        sorted.sort(PRINT_ORDER);
        List<Cell> indexed = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            Cell cell = sorted.get(i);
            indexed.add(new Cell(i, cell.relativePos(), cell.kind(), cell.previewState(), cell.fluid()));
        }
        this.cells = List.copyOf(indexed);
        this.bounds = BoundingBox.encapsulatingPositions(indexed.stream().map(Cell::relativePos).toList())
            .orElse(null);
        this.errored = errored;
        this.hash = calculateHash(this.cells);
    }

    public static Kind classify(BlockState state) {
        FluidState fluidState = state.getFluidState();
        if (fluidState.isEmpty() || !fluidState.isSource()) {
            return Kind.AIR;
        }
        Fluid fluid = FluidHelper.convertToStill(fluidState.getType());
        if (!(fluid instanceof FlowingFluid)) {
            return Kind.AIR;
        }
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)
            && state.getValue(BlockStateProperties.WATERLOGGED)
            && fluidState.is(FluidTags.WATER)) {
            return Kind.WATERLOGGED;
        }
        return state.getBlock() == fluidState.createLegacyBlock().getBlock()
            ? Kind.FREE_SOURCE
            : Kind.AIR;
    }

    public static FluidSchematicPlan load(ItemStack blueprint, Level level) {
        BlockPos anchor = blueprint.get(AllDataComponents.SCHEMATIC_ANCHOR);
        if (anchor == null || !blueprint.getOrDefault(AllDataComponents.SCHEMATIC_DEPLOYED, false)) {
            return new FluidSchematicPlan(BlockPos.ZERO, List.of(), false);
        }

        StructureTemplate template = SchematicItem.loadSchematic(level, blueprint);
        StructurePlaceSettings settings = SchematicItem.getSettings(blueprint, false);
        SchematicLevel schematic = new SchematicLevel(anchor, level);
        try {
            template.placeInWorld(schematic, anchor, anchor, settings, schematic.getRandom(), Block.UPDATE_CLIENTS);
        } catch (Exception exception) {
            com.yision.fluidlogistics.FluidLogistics.LOGGER.error("Failed to load fluid schematic", exception);
            return new FluidSchematicPlan(anchor, List.of(), true);
        }
        BlockPos extraBounds = StructureTemplate.calculateRelativePosition(
            settings, new BlockPos(template.getSize()).offset(-1, -1, -1));
        schematic.setBounds(BBHelper.encapsulate(schematic.getBounds(), extraBounds));

        List<Cell> cells = new ArrayList<>();
        BoundingBox bounds = schematic.getBounds();
        for (BlockPos relativePos : BlockPos.betweenClosed(
            bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
            BlockState state = schematic.getBlockState(relativePos.offset(anchor));
            Kind kind = classify(state);
            if (kind != Kind.AIR) {
                Fluid fluid = FluidHelper.convertToStill(state.getFluidState().getType());
                FluidStack required = new FluidStack(fluid, FluidType.BUCKET_VOLUME);
                cells.add(new Cell(-1, relativePos.immutable(), kind, state, required));
                continue;
            }
            cells.add(new Cell(-1, relativePos.immutable(), Kind.AIR,
                Blocks.AIR.defaultBlockState(), FluidStack.EMPTY));
        }
        return new FluidSchematicPlan(anchor, cells, false);
    }

    public BlockPos anchor() {
        return anchor;
    }

    public List<Cell> cells() {
        return cells;
    }

    public Cell cell(int index) {
        return cells.get(index);
    }

    public BoundingBox bounds() {
        return bounds;
    }

    public boolean isEmpty() {
        return cells.isEmpty();
    }

    public boolean isErrored() {
        return errored;
    }

    public int hash() {
        return hash;
    }

    private static int calculateHash(List<Cell> cells) {
        int result = 1;
        for (Cell cell : cells) {
            result = 31 * result + cell.relativePos().hashCode();
            result = 31 * result + cell.kind().ordinal();
            if (cell.kind() == Kind.AIR) {
                continue;
            }
            result = 31 * result + BuiltInRegistries.BLOCK.getKey(cell.expectedHost()).hashCode();
            result = 31 * result + BuiltInRegistries.FLUID.getKey(cell.fluid().getFluid()).hashCode();
        }
        return result;
    }
}
