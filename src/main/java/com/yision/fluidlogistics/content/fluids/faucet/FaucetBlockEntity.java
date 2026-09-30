package com.yision.fluidlogistics.content.fluids.faucet;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.mojang.blaze3d.vertex.PoseStack;
import com.yision.fluidlogistics.content.fluids.infiniteWater.InfiniteWaterSource;
import com.yision.fluidlogistics.foundation.fluid.CachedFluidInterface;
import com.yision.fluidlogistics.foundation.fluid.CauldronFills;
import com.yision.fluidlogistics.foundation.fluid.DepotFills;
import com.yision.fluidlogistics.foundation.fluid.FluidContainerPolicies;
import com.yision.fluidlogistics.foundation.fluid.FluidSourceScans;
import com.yision.fluidlogistics.registry.AllBlockEntities;
import com.yision.fluidlogistics.util.MergedFluidDisplayHandler;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.Nullable;

public class FaucetBlockEntity extends SmartBlockEntity {

    static final int TRANSFER_INTERVAL = 10;
    static final int SUCCESS_COOLDOWN = 5;
    private static final int TRANSFER_RATE = 250;
    private static final int IDLE_RECHECK_INTERVAL = 20;
    private static final String TAG_RENDERING_FLUID = "RenderingFluid";
    private static final String TAG_TRANSFER_COOLDOWN = "TransferCooldown";
    protected BeltProcessingBehaviour beltProcessing;
    protected FilteringBehaviour filtering;
    protected FluidStack renderingFluid = FluidStack.EMPTY;
    private int transferCooldown;
    private boolean visualStateCleared;
    private final CachedFluidInterface sourceCache = new CachedFluidInterface();
    private final CachedFluidInterface targetTankCache = new CachedFluidInterface();
    private FaucetItemFilling itemFilling;
    private FaucetDrips drips;

    public FaucetBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        ensureComponents();
    }

    private void ensureComponents() {
        if (itemFilling == null) {
            itemFilling = new FaucetItemFilling(this);
            drips = new FaucetDrips(this);
        }
    }

    boolean isSmart() {
        return getType() == AllBlockEntities.SMART_FAUCET.get();
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().expandTowards(0, -2, 0);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        sourceCache.invalidate();
        targetTankCache.invalidate();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        ensureComponents();
        if (isSmart()) {
            behaviours.add(filtering = new FilteringBehaviour(this, new FilterSlotPositioning()).forFluids()
                .withCallback($ -> notifyUpdate()));
            behaviours.add(beltProcessing = new BeltProcessingBehaviour(this).whenItemEnters(itemFilling::onBeltItemReceived)
                .whileItemHeld(itemFilling::whenBeltItemHeld));
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide) {
            return;
        }

        if (!isOpen()) {
            resetVisualState();
            return;
        }
        visualStateCleared = false;

        tickCooldown();
        if (itemFilling.tickActiveFill()) {
            return;
        }

        drips.tickCacheRefresh();

        if (transferCooldown == 0) {
            tryTransferFluid();
        }

        drips.tickEffect();
    }

    public FluidStack getRenderingFluid() {
        return renderingFluid;
    }

    public int getProcessingTicks() {
        return itemFilling.getProcessingTicks();
    }

    public boolean isProcessing() {
        return itemFilling.isProcessing();
    }

    public boolean isProcessingOnBelt() {
        return itemFilling.isProcessingOnBelt();
    }

    public boolean hasFluidToRender() {
        return !renderingFluid.isEmpty();
    }

    public boolean shouldRenderSourceInterface() {
        if (level == null) {
            return false;
        }

        Direction sourceSide = getBlockState().getValue(FaucetBlock.FACING).getOpposite();
        BlockPos sourcePos = worldPosition.relative(sourceSide);
        BlockState sourceState = level.getBlockState(sourcePos);
        if (InfiniteWaterSource.isWaterSourceBlock(sourceState)) {
            return false;
        }

        return sourceCache.get(level, sourcePos, sourceSide.getOpposite()) != null;
    }

    @Nullable
    public IFluidHandler getFluidDisplayCapability() {
        if (level == null) {
            return null;
        }

        Direction facing = getBlockState().getValue(FaucetBlock.FACING);
        BlockPos sourcePos = worldPosition.relative(facing.getOpposite());
        if (InfiniteWaterSource.isWaterSourceBlock(level.getBlockState(sourcePos))) {
            return null;
        }

        IFluidHandler source = sourceHandler(sourcePos, facing);
        if (source == null) {
            return null;
        }

        MergedFluidDisplayHandler display = new MergedFluidDisplayHandler(source, this::testFluidFilter);
        return display.getTanks() == 0 ? null : display;
    }

    public void onTargetChanged() {
        if (itemFilling.isFilling()) {
            itemFilling.cancel();
        }
        transferCooldown = 0;
        itemFilling.resetRetryCooldown();
        targetTankCache.invalidate();
        drips.clear();
        notifyUpdate();
    }

    boolean testFluidFilter(FluidStack fluid) {
        return filtering == null || filtering.test(fluid);
    }

    @Nullable
    IFluidHandler sourceHandler(BlockPos sourcePos, Direction side) {
        IFluidHandler infinite = InfiniteWaterSource.getSourceHandler(
            InfiniteWaterSource.Consumer.FAUCET, level.getBlockState(sourcePos));
        if (infinite != null) {
            return infinite;
        }
        return sourceCache.get(level, sourcePos, side);
    }

    @Nullable
    IFluidHandler rawSourceCapability(BlockPos sourcePos, Direction side) {
        return sourceCache.get(level, sourcePos, side);
    }

    Direction sourceFacing() {
        return getBlockState().getValue(FaucetBlock.FACING);
    }

    BlockPos sourcePos() {
        return worldPosition.relative(sourceFacing().getOpposite());
    }

    void setRenderingFluid(FluidStack fluid) {
        this.renderingFluid = fluid;
    }

    void setTransferCooldown(int cooldown) {
        this.transferCooldown = cooldown;
    }

    boolean hasProcessableTargetBelow() {
        return resolveTarget(worldPosition.below()).kind() != TargetKind.NONE;
    }

    private void tryTransferFluid() {
        BlockPos targetPos = worldPosition.below();
        Direction facing = getBlockState().getValue(FaucetBlock.FACING);
        BlockPos sourcePos = worldPosition.relative(facing.getOpposite());

        ResolvedTarget target = resolveTarget(targetPos);
        IFluidHandler source = sourceHandler(sourcePos, facing);

        if (target.kind() == TargetKind.NONE) {
            transferCooldown = IDLE_RECHECK_INTERVAL;
            if (source == null) {
                clearFlowVisuals();
                return;
            }

            drips.prime(source);
            return;
        }

        if (source == null) {
            transferCooldown = IDLE_RECHECK_INTERVAL;
            clearFlowVisuals();
            return;
        }

        boolean success = tryProcessTarget(source, target, facing, sourcePos);
        if (success) {
            transferCooldown = SUCCESS_COOLDOWN;
            if (drips.isDripping()) {
                drips.clear();
                notifyUpdate();
            }
            return;
        }

        transferCooldown = TRANSFER_INTERVAL;
        drips.update(source);
    }

    private ResolvedTarget resolveTarget(BlockPos targetPos) {
        BlockState directState = level.getBlockState(targetPos);
        if (isCauldronTarget(directState)) {
            return new ResolvedTarget(TargetKind.CAULDRON, targetPos, directState, null, false);
        }

        BlockEntity directEntity = level.getBlockEntity(targetPos);
        if (directEntity != null) {
            return resolveBlockEntityTarget(targetPos, directState, directEntity, true);
        }

        if (FluidContainerPolicies.allowsFaucet(directState)
            && targetTankCache.get(level, targetPos, Direction.UP) != null) {
            return new ResolvedTarget(TargetKind.TANK, targetPos, directState, null, true);
        }

        return ResolvedTarget.NONE;
    }

    private ResolvedTarget resolveBlockEntityTarget(BlockPos targetPos, BlockState targetState,
        BlockEntity targetEntity, boolean cacheTankHandler) {
        if (DepotFills.isDepot(targetEntity)) {
            ItemStack itemOnDepot = DepotFills.getItemOnDepot(targetEntity);
            return !itemOnDepot.isEmpty() && FaucetFilling.canItemBeFilled(level, itemOnDepot)
                ? new ResolvedTarget(TargetKind.DEPOT, targetPos, targetState, targetEntity, false)
                : ResolvedTarget.NONE;
        }

        if (!FluidContainerPolicies.allowsFaucet(targetState)) {
            return ResolvedTarget.NONE;
        }
        ResolvedTarget target = new ResolvedTarget(
            TargetKind.TANK, targetPos, targetState, targetEntity, cacheTankHandler);
        return getTargetHandler(target) != null ? target : ResolvedTarget.NONE;
    }

    private boolean tryProcessTarget(IFluidHandler source, ResolvedTarget target,
        Direction sourceDir, BlockPos sourcePos) {
        if (target.kind() == TargetKind.DEPOT && target.entity() != null) {
            ItemStack itemOnDepot = DepotFills.getItemOnDepot(target.entity());
            FluidStack fillableFluid = FluidSourceScans.findForItem(level, source,
                this::testFluidFilter, itemOnDepot, true);
            if (!fillableFluid.isEmpty()) {
                return itemFilling.startDepotFilling(source, itemOnDepot, sourceDir, sourcePos, fillableFluid);
            }
            return false;
        }

        if (target.kind() == TargetKind.CAULDRON) {
            FluidStack fillableFluid = FluidSourceScans.findForCauldron(source,
                this::testFluidFilter, target.state(), Integer.MAX_VALUE, true);
            if (fillableFluid.isEmpty()) {
                return false;
            }
            int waterLevel = getWaterCauldronTargetLevel(target.state(), fillableFluid);
            FluidStack transferred = CauldronFills.fill(level, source, target.pos(), target.state(), fillableFluid);
            if (transferred.isEmpty()) {
                return false;
            }
            level.playSound(null, target.pos(), net.minecraft.sounds.SoundEvents.BUCKET_EMPTY,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, waterLevel > 0 ? 0.8f + waterLevel * 0.1f : 1.0f);
            renderingFluid = transferred.copy();
            notifyUpdate();
            return true;
        }

        if (target.kind() != TargetKind.TANK || !FluidContainerPolicies.allowsFaucet(target.state())) {
            return false;
        }

        IFluidHandler targetHandler = getTargetHandler(target);
        if (targetHandler == null) {
            return false;
        }

        FluidStack transferred = fillContainer(source, targetHandler);
        if (transferred.isEmpty()) {
            return false;
        }
        renderingFluid = transferred.copy();
        notifyUpdate();
        return true;
    }

    private @Nullable IFluidHandler getTargetHandler(ResolvedTarget target) {
        if (target.cacheTankHandler()) {
            return targetTankCache.get(level, target.pos(), Direction.UP);
        }
        BlockEntity targetEntity = target.entity();
        if (targetEntity == null) {
            return null;
        }
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, targetEntity.getBlockPos(),
            Direction.UP);
        if (handler == null) {
            handler = level.getCapability(Capabilities.FluidHandler.BLOCK, targetEntity.getBlockPos(), null);
        }
        return handler;
    }

    private FluidStack fillContainer(IFluidHandler source, IFluidHandler targetHandler) {
        FluidStack availableFluid = FluidSourceScans.findForContainer(source, this::testFluidFilter,
            targetHandler, TRANSFER_RATE, $ -> true, true);
        if (availableFluid.isEmpty()) {
            return FluidStack.EMPTY;
        }

        FluidStack toTransfer = availableFluid.copyWithAmount(Math.min(availableFluid.getAmount(), TRANSFER_RATE));
        int filled = targetHandler.fill(toTransfer, FluidAction.SIMULATE);
        if (filled <= 0) {
            return FluidStack.EMPTY;
        }

        FluidStack actualDrain = source.drain(toTransfer.copyWithAmount(filled), FluidAction.EXECUTE);
        if (actualDrain.isEmpty()) {
            return FluidStack.EMPTY;
        }

        targetHandler.fill(actualDrain, FluidAction.EXECUTE);
        if (level.random.nextFloat() < 0.1f) {
            AllSoundEvents.SPOUTING.playOnServer(level, worldPosition, 0.3f, 0.9f + 0.2f * level.random.nextFloat());
        }
        return actualDrain;
    }

    private static boolean isCauldronTarget(BlockState targetState) {
        return targetState.is(Blocks.CAULDRON) || targetState.is(Blocks.WATER_CAULDRON);
    }

    private static int getWaterCauldronTargetLevel(BlockState targetState, FluidStack availableFluid) {
        if (availableFluid.getFluid() != Fluids.WATER) {
            return 0;
        }
        if (targetState.is(Blocks.CAULDRON)) {
            return 1;
        }
        if (targetState.is(Blocks.WATER_CAULDRON) && targetState.hasProperty(LayeredCauldronBlock.LEVEL)) {
            return targetState.getValue(LayeredCauldronBlock.LEVEL) + 1;
        }
        return 0;
    }

    private void resetVisualState() {
        boolean needsUpdate = !renderingFluid.isEmpty() || itemFilling.hasPending() || drips.isDripping()
            || itemFilling.isFilling();
        if (visualStateCleared && !needsUpdate) {
            return;
        }
        renderingFluid = FluidStack.EMPTY;
        itemFilling.clearState();
        transferCooldown = 0;
        drips.clear();
        drips.clearCache();
        visualStateCleared = true;
        if (needsUpdate) {
            notifyUpdate();
        }
    }

    void clearFlowVisuals() {
        if (!renderingFluid.isEmpty() || drips.isDripping()) {
            renderingFluid = FluidStack.EMPTY;
            drips.clear();
            notifyUpdate();
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        FaucetFilling.writeFluid(tag, registries, TAG_RENDERING_FLUID, renderingFluid);
        itemFilling.write(tag, registries);
        drips.write(tag, registries);
        tag.putInt(TAG_TRANSFER_COOLDOWN, transferCooldown);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        ensureComponents();
        renderingFluid = FaucetFilling.readFluid(tag, registries, TAG_RENDERING_FLUID);
        itemFilling.read(tag, registries);
        drips.read(tag, registries);
        transferCooldown = tag.getInt(TAG_TRANSFER_COOLDOWN);
    }

    private boolean isOpen() {
        return getBlockState().getValue(FaucetBlock.OPEN);
    }

    private void tickCooldown() {
        if (transferCooldown > 0) {
            transferCooldown--;
        }
    }

    private record ResolvedTarget(TargetKind kind, BlockPos pos, BlockState state,
        @Nullable BlockEntity entity, boolean cacheTankHandler) {

        private static final ResolvedTarget NONE = new ResolvedTarget(TargetKind.NONE, BlockPos.ZERO,
            Blocks.AIR.defaultBlockState(), null, false);
    }

    private enum TargetKind {
        NONE,
        DEPOT,
        CAULDRON,
        TANK
    }

    private static class FilterSlotPositioning extends ValueBoxTransform {

        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            Direction facing = state.getValue(FaucetBlock.FACING).getOpposite();
            return VecHelper.rotateCentered(VecHelper.voxelSpace(8, 12.5f, 10), AngleHelper.horizontalAngle(facing),
                Direction.Axis.Y);
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
            Direction facing = state.getValue(FaucetBlock.FACING).getOpposite();
            TransformStack.of(ms)
                .rotateYDegrees(AngleHelper.horizontalAngle(facing))
                .rotateXDegrees(90);
        }
    }
}
