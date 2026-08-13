package com.yision.fluidlogistics.content.schematics.cannon;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.cannon.MaterialChecklist;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.yision.fluidlogistics.content.schematics.FluidSchematicPlacement;
import com.yision.fluidlogistics.content.schematics.FluidSchematicPlan;
import com.yision.fluidlogistics.content.schematics.FluidSchematicPlan.Cell;
import com.yision.fluidlogistics.content.schematics.FluidSchematicPlan.Kind;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluids;

public class FluidSchematicPrinter extends SchematicPrinter {

    private FluidSchematicPlan plan;
    private boolean loaded;
    private boolean errored;
    private int cursor = -1;
    private int currentIndex = -1;
    private BlockPos syncedAnchor;
    private BlockPos syncedTarget;
    private final Deque<Integer> retries = new ArrayDeque<>();
    private final Set<Integer> retrySet = new HashSet<>();

    private int savedHash;
    private boolean savedStateValid;
    private int savedCursor = -1;
    private int savedCurrent = -1;
    private int[] savedRetries = new int[0];

    @Override
    public void fromTag(CompoundTag compound, boolean clientPacket) {
        syncedAnchor = compound.contains("Anchor", Tag.TAG_COMPOUND)
            ? NbtUtils.readBlockPos(compound.getCompound("Anchor"))
            : null;
        syncedTarget = compound.contains("FluidTarget", Tag.TAG_COMPOUND)
            ? NbtUtils.readBlockPos(compound.getCompound("FluidTarget"))
            : null;
        savedStateValid = compound.contains("FluidPlanHash");
        savedHash = compound.getInt("FluidPlanHash");
        savedCursor = compound.getInt("FluidCursor");
        savedCurrent = compound.getInt("FluidCurrent");
        savedRetries = compound.getIntArray("FluidRetries");
        if (clientPacket) {
            loaded = syncedAnchor != null;
        }
    }

    @Override
    public void write(CompoundTag compound) {
        BlockPos anchor = plan == null ? syncedAnchor : plan.anchor();
        if (anchor != null) {
            compound.put("Anchor", NbtUtils.writeBlockPos(anchor));
        }
        BlockPos target = getCurrentTarget();
        if (target != null) {
            compound.put("FluidTarget", NbtUtils.writeBlockPos(target));
        }
        if (plan != null) {
            compound.putInt("FluidPlanHash", plan.hash());
        }
        compound.putInt("FluidCursor", cursor);
        compound.putInt("FluidCurrent", currentIndex);
        compound.putIntArray("FluidRetries", retries.stream().mapToInt(Integer::intValue).toArray());
    }

    @Override
    public void loadSchematic(ItemStack blueprint, Level originalWorld, boolean processNBT) {
        plan = FluidSchematicPlan.load(blueprint, originalWorld);
        loaded = true;
        errored = plan.isErrored();
        syncedAnchor = plan.anchor();
        syncedTarget = null;
        cursor = -1;
        currentIndex = -1;
        retries.clear();
        retrySet.clear();

        if (!errored && savedStateValid && savedHash == plan.hash()) {
            cursor = Mth.clamp(savedCursor, -1, plan.cells().size() - 1);
            currentIndex = validIndex(savedCurrent) ? savedCurrent : -1;
            for (int retry : savedRetries) {
                if (validIndex(retry) && retrySet.add(retry)) {
                    retries.addLast(retry);
                }
            }
        }
        savedHash = 0;
        savedStateValid = false;
        savedCursor = -1;
        savedCurrent = -1;
        savedRetries = new int[0];
    }

    @Override
    public void resetSchematic() {
        plan = null;
        loaded = false;
        errored = false;
        cursor = -1;
        currentIndex = -1;
        syncedAnchor = null;
        syncedTarget = null;
        retries.clear();
        retrySet.clear();
        savedHash = 0;
        savedStateValid = false;
        savedCursor = -1;
        savedCurrent = -1;
        savedRetries = new int[0];
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public boolean isErrored() {
        return errored;
    }

    @Override
    public BlockPos getCurrentTarget() {
        if (plan == null) {
            return syncedTarget;
        }
        return currentIndex < 0 ? syncedTarget : plan.anchor().offset(currentCell().relativePos());
    }

    @Override
    public BlockPos getAnchor() {
        return plan == null ? syncedAnchor : plan.anchor();
    }

    @Override
    public boolean isWorldEmpty() {
        return plan == null || plan.isEmpty();
    }

    @Override
    public boolean advanceCurrentPos() {
        if (currentIndex >= 0) {
            return true;
        }
        while (!retries.isEmpty()) {
            int retry = retries.removeFirst();
            retrySet.remove(retry);
            if (validIndex(retry)) {
                currentIndex = retry;
                return true;
            }
        }
        if (plan != null && cursor + 1 < plan.cells().size()) {
            currentIndex = ++cursor;
            return true;
        }
        return false;
    }

    @Override
    public ItemRequirement getCurrentRequirement() {
        return ItemRequirement.NONE;
    }

    @Override
    public boolean shouldPlaceCurrent(Level world, PlacementPredicate predicate) {
        if (currentIndex < 0) {
            return false;
        }
        BlockPos target = getCurrentTarget();
        Cell cell = currentCell();
        FluidSchematicPlacement.Result placement = FluidSchematicPlacement.evaluate(world, target, cell);
        return shouldPlaceCell(world, predicate, target, cell, placement);
    }

    boolean shouldPlaceCurrent(Level world, PlacementPredicate predicate,
            FluidSchematicPlacement.Result placement) {
        return currentIndex >= 0
            && shouldPlaceCell(world, predicate, getCurrentTarget(), currentCell(), placement);
    }

    @Override
    public void handleCurrentTarget(BlockTargetHandler blockHandler, EntityTargetHandler entityHandler) {
        if (currentIndex >= 0) {
            blockHandler.handle(getCurrentTarget(), currentCell().previewState(), null);
        }
    }

    @Override
    public int markAllBlockRequirements(MaterialChecklist checklist, Level world, PlacementPredicate predicate) {
        if (!(checklist instanceof FluidMaterialChecklist fluidChecklist) || plan == null) {
            return 0;
        }
        int blocks = 0;
        for (Cell cell : plan.cells()) {
            BlockPos target = plan.anchor().offset(cell.relativePos());
            if (!world.isLoaded(target)) {
                fluidChecklist.warnBlockNotLoaded();
                continue;
            }
            FluidSchematicPlacement.Result placement = FluidSchematicPlacement.evaluate(world, target, cell);
            if (placement == FluidSchematicPlacement.Result.SATISFIED) {
                continue;
            }
            if ((cell.kind() != Kind.WATERLOGGED || placement == FluidSchematicPlacement.Result.READY)
                && !shouldPlaceCell(world, predicate, target, cell, placement)) {
                continue;
            }
            if (cell.kind() == Kind.AIR) {
                continue;
            }
            fluidChecklist.require(cell.fluid());
            blocks++;
        }
        return blocks;
    }

    private boolean shouldPlaceCell(Level world, PlacementPredicate predicate, BlockPos target, Cell cell,
            FluidSchematicPlacement.Result placement) {
        if (world == null || !world.isLoaded(target) || !world.getWorldBorder().isWithinBounds(target)) {
            return false;
        }
        if (placement == FluidSchematicPlacement.Result.SATISFIED) {
            return false;
        }

        BlockState state = cell.previewState();
        BlockState toReplace = world.getBlockState(target);
        BlockState toReplaceOther = null;
        if (state.hasProperty(BlockStateProperties.BED_PART)
            && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
            && state.getValue(BlockStateProperties.BED_PART) == BedPart.FOOT) {
            toReplaceOther = world.getBlockState(target.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
        }
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
            && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
            toReplaceOther = world.getBlockState(target.above());
        }
        if (toReplace.getDestroySpeed(world, target) == -1
            || (toReplaceOther != null && toReplaceOther.getDestroySpeed(world, target) == -1)) {
            return false;
        }

        boolean isNormalCube = state.isRedstoneConductor(world, target);
        return predicate.shouldPlace(target, state, null, toReplace, toReplaceOther, isNormalCube);
    }

    @Override
    public void markAllEntityRequirements(MaterialChecklist checklist) {
    }

    @Override
    public void sendBlockUpdates(Level level) {
        BoundingBox bounds = plan == null ? null : plan.bounds();
        if (bounds == null) {
            return;
        }
        BlockPos.betweenClosedStream(bounds.inflatedBy(1))
            .filter(pos -> !bounds.isInside(pos))
            .map(pos -> pos.offset(plan.anchor()))
            .filter(pos -> level.isLoaded(pos) && level.getFluidState(pos).is(Fluids.WATER))
            .forEach(pos -> level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level)));
    }

    public FluidSchematicPlan plan() {
        return plan;
    }

    public Cell currentCell() {
        return plan.cell(currentIndex);
    }

    public int currentIndex() {
        return currentIndex;
    }

    public void completeCurrent() {
        syncedTarget = getCurrentTarget();
        currentIndex = -1;
    }

    public void retry(int index) {
        if (validIndex(index) && retrySet.add(index)) {
            retries.addFirst(index);
        }
    }

    public boolean hasPendingRetries() {
        return !retries.isEmpty();
    }

    private boolean validIndex(int index) {
        return plan != null && index >= 0 && index < plan.cells().size();
    }
}
