package com.yision.fluidlogistics.content.schematics.cannon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.schematics.cannon.ConfigureSchematicannonPacket;
import com.simibubi.create.content.schematics.cannon.LaunchedItem;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.simibubi.create.content.schematics.cannon.SchematicannonMenu;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.yision.fluidlogistics.api.packager.PackageResources;
import com.yision.fluidlogistics.content.logistics.fluidPackage.FluidPackageResourceType;
import com.yision.fluidlogistics.content.schematics.FluidSchematicPlacement;
import com.yision.fluidlogistics.content.schematics.FluidSchematicPlan;
import com.yision.fluidlogistics.content.schematics.FluidSchematicPlan.Cell;
import com.yision.fluidlogistics.content.schematics.FluidSchematicPlan.Kind;
import com.yision.fluidlogistics.mixin.accessor.SchematicannonBlockEntityAccessor;
import com.yision.fluidlogistics.registry.AllItems;
import com.yision.fluidlogistics.registry.AllMenuTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class CopperSchematicannonBlockEntity extends SchematicannonBlockEntity {

    private final List<IFluidHandler> attachedFluidHandlers = new ArrayList<>();
    private UUID fluidJobId = UUID.randomUUID();

    public CopperSchematicannonBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        inventory = new CopperSchematicannonInventory(this);
        printer = new FluidSchematicPrinter();
        checklist = new FluidMaterialChecklist();
    }

    @Override
    public void tick() {
        if (level != null && level.isClientSide) {
            spawnLaunchParticles();
        }
        super.tick();
    }

    private void spawnLaunchParticles() {
        for (LaunchedItem launched : flyingBlocks) {
            if (launched.ticksRemaining != launched.totalTicks) {
                continue;
            }

            Vec3 start = Vec3.atCenterOf(getBlockPos().above());
            Vec3 target = Vec3.atCenterOf(launched.target);
            Vec3 distance = target.subtract(start);
            double yDifference = target.y - start.y;
            double throwHeight = Math.sqrt(distance.lengthSqr()) * .6f + yDifference;
            Vec3 cannonOffset = distance.add(0, throwHeight, 0).normalize().scale(2);
            start = start.add(cannonOffset).subtract(.5, .5, .5);

            for (int i = 0; i < 10; i++) {
                RandomSource random = level.getRandom();
                double speedX = cannonOffset.x * .01f;
                double speedY = (cannonOffset.y + 1) * .01f;
                double speedZ = cannonOffset.z * .01f;
                double randomX = random.nextFloat() - speedX * 40;
                double randomY = random.nextFloat() - speedY * 40;
                double randomZ = random.nextFloat() - speedZ * 40;
                level.addParticle(
                    ParticleTypes.CLOUD,
                    start.x + randomX, start.y + randomY, start.z + randomZ,
                    speedX, speedY, speedZ);
            }
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new SchematicannonMenu(AllMenuTypes.COPPER_SCHEMATICANNON.get(), id, inventory, this);
    }

    @Override
    public void findInventories() {
        super.findInventories();
        attachedFluidHandlers.clear();
        for (Direction direction : Direction.values()) {
            BlockPos target = worldPosition.relative(direction);
            if (!level.isLoaded(target)) {
                continue;
            }
            IFluidHandler handler = level.getCapability(
                Capabilities.FluidHandler.BLOCK, target, direction.getOpposite());
            if (handler != null && attachedFluidHandlers.stream().noneMatch(existing -> existing == handler)) {
                attachedFluidHandlers.add(handler);
            }
        }
    }

    @Override
    protected void initializePrinter(ItemStack blueprint) {
        if (!blueprint.has(AllDataComponents.SCHEMATIC_ANCHOR)) {
            state = State.STOPPED;
            statusMsg = "schematicInvalid";
            sendUpdate = true;
            return;
        }
        if (!blueprint.getOrDefault(AllDataComponents.SCHEMATIC_DEPLOYED, false)) {
            state = State.STOPPED;
            statusMsg = "schematicNotPlaced";
            sendUpdate = true;
            return;
        }

        fluidPrinter().loadSchematic(blueprint, level, false);
        if (printer.isErrored() || printer.isWorldEmpty()) {
            state = State.STOPPED;
            statusMsg = printer.isErrored() ? "schematicErrored" : "schematicExpired";
            inventory.setStackInSlot(0, ItemStack.EMPTY);
            inventory.setStackInSlot(1, AllItems.EMPTY_FLUID_SCHEMATIC.asStack());
            printer.resetSchematic();
            sendUpdate = true;
            return;
        }
        if (!printer.getAnchor().closerThan(getBlockPos(), MAX_ANCHOR_DISTANCE)) {
            state = State.STOPPED;
            statusMsg = "targetOutsideRange";
            printer.resetSchematic();
            sendUpdate = true;
            return;
        }

        state = State.PAUSED;
        statusMsg = "ready";
        updateChecklist();
        sendUpdate = true;
    }

    @Override
    protected void tickPrinter() {
        SchematicannonBlockEntityAccessor accessor = (SchematicannonBlockEntityAccessor) this;
        accessor.fluidlogistics$setBlockSkipped(false);
        ItemStack blueprint = inventory.getStackInSlot(0);

        if (blueprint.isEmpty() && !statusMsg.equals("idle") && inventory.getStackInSlot(1).isEmpty()) {
            state = State.STOPPED;
            statusMsg = "idle";
            sendUpdate = true;
            return;
        }
        if (state == State.STOPPED) {
            if (printer.isLoaded()) {
                super.resetPrinter();
            }
            return;
        }
        if (state == State.PAUSED && !positionNotLoaded && missingItem == null && remainingFuel > 0
            && !statusMsg.equals("waitingForHost") && !statusMsg.equals("missingFluid")) {
            return;
        }
        if (!printer.isLoaded()) {
            initializePrinter(blueprint);
            return;
        }
        if (accessor.fluidlogistics$getPrinterCooldown() > 0) {
            accessor.fluidlogistics$setPrinterCooldown(accessor.fluidlogistics$getPrinterCooldown() - 1);
            return;
        }
        if (remainingFuel <= 0 && !hasCreativeCrate) {
            refillFuelIfPossible();
            if (remainingFuel <= 0) {
                state = State.PAUSED;
                statusMsg = "noGunpowder";
                sendUpdate = true;
                return;
            }
        }
        if (hasCreativeCrate) {
            remainingFuel = 0;
        }

        FluidSchematicPrinter fluidPrinter = fluidPrinter();
        if (fluidPrinter.currentIndex() < 0 && !fluidPrinter.advanceCurrentPos()) {
            finishedPrinting();
            return;
        }

        BlockPos target = fluidPrinter.getCurrentTarget();
        if (!level.isLoaded(target)) {
            positionNotLoaded = true;
            state = State.PAUSED;
            statusMsg = "targetNotLoaded";
            sendUpdate = true;
            return;
        }
        if (positionNotLoaded) {
            positionNotLoaded = false;
            state = State.RUNNING;
        }

        Cell cell = fluidPrinter.currentCell();
        FluidSchematicPlacement.Result placement = FluidSchematicPlacement.evaluate(level, target, cell);
        if (placement == FluidSchematicPlacement.Result.SATISFIED) {
            fluidPrinter.completeCurrent();
            missingItem = null;
            statusMsg = "searching";
            accessor.fluidlogistics$setBlockSkipped(true);
            return;
        }
        if (cell.kind() == Kind.WATERLOGGED && placement != FluidSchematicPlacement.Result.READY) {
            handleUnavailableCurrent("waitingForHost", accessor);
            return;
        }
        if (!fluidPrinter.shouldPlaceCurrent(level, this::shouldPlace, placement)) {
            fluidPrinter.completeCurrent();
            missingItem = null;
            sendUpdate = !statusMsg.equals("searching");
            statusMsg = "searching";
            accessor.fluidlogistics$setBlockSkipped(true);
            return;
        }
        if (cell.kind() == Kind.AIR) {
            state = State.RUNNING;
            statusMsg = "clearing";
            missingItem = null;
            launchBlock(target, ItemStack.EMPTY, Blocks.AIR.defaultBlockState(), null);
            fluidPrinter.completeCurrent();
            accessor.fluidlogistics$setPrinterCooldown(config().schematicannonDelay.get());
            if (!hasCreativeCrate) {
                remainingFuel--;
            }
            sendUpdate = true;
            return;
        }

        boolean fluidConsumed = false;
        if (!hasCreativeCrate) {
            findInventories();
            CombinedTankWrapper tanks = combinedFluidHandler();
            FluidStack request = cell.fluid().copyWithAmount(FluidType.BUCKET_VOLUME);
            FluidStack simulated = tanks.drain(request, IFluidHandler.FluidAction.SIMULATE);
            if (simulated.getAmount() != FluidType.BUCKET_VOLUME) {
                missingItem = FluidPackageResourceType.createFluidKey(cell.fluid());
                handleUnavailableCurrent("missingFluid", accessor);
                return;
            }
            FluidStack drained = tanks.drain(request, IFluidHandler.FluidAction.EXECUTE);
            if (drained.getAmount() != FluidType.BUCKET_VOLUME) {
                refundFluid(tanks, drained);
                missingItem = FluidPackageResourceType.createFluidKey(cell.fluid());
                handleUnavailableCurrent("missingFluid", accessor);
                return;
            }
            fluidConsumed = true;
        }

        state = State.RUNNING;
        statusMsg = "placingFluid";
        missingItem = null;
        CompoundTag data = FluidShotPayload.create(
            fluidJobId, fluidPrinter.plan().hash(), cell, fluidConsumed);
        ItemStack icon = FluidPackageResourceType.createFluidKey(cell.fluid());
        flyingBlocks.add(new LaunchedItem.ForBlockState(
            worldPosition, target, icon, cell.previewState(), data));
        playFiringSound();
        fluidPrinter.completeCurrent();
        accessor.fluidlogistics$setPrinterCooldown(config().schematicannonDelay.get());
        if (!hasCreativeCrate) {
            remainingFuel--;
        }
        sendUpdate = true;
    }

    private void handleUnavailableCurrent(String status, SchematicannonBlockEntityAccessor accessor) {
        if (skipMissing) {
            fluidPrinter().completeCurrent();
            missingItem = null;
            state = State.RUNNING;
            statusMsg = "skipping";
            accessor.fluidlogistics$setBlockSkipped(true);
            return;
        }
        state = State.PAUSED;
        statusMsg = status;
        sendUpdate = true;
    }

    @Override
    public void updateChecklist() {
        FluidMaterialChecklist fluidChecklist = fluidChecklist();
        fluidChecklist.clearFluids();
        if (printer.isLoaded() && !printer.isErrored()) {
            blocksToPlace = blocksPlaced;
            blocksToPlace += printer.markAllBlockRequirements(fluidChecklist, level, this::shouldPlace);
        }

        findInventories();
        CombinedTankWrapper tanks = combinedFluidHandler();
        for (Fluid fluid : fluidChecklist.requiredFluids()) {
            int amount = fluidChecklist.requiredAmount(fluid);
            FluidStack available = hasCreativeCrate
                ? new FluidStack(fluid, amount)
                : tanks.drain(new FluidStack(fluid, amount), IFluidHandler.FluidAction.SIMULATE);
            fluidChecklist.collect(available);
        }
        sendUpdate = true;
    }

    @Override
    public void finishedPrinting() {
        if (hasCurrentFluidShots() || fluidPrinter().hasPendingRetries()) {
            state = State.RUNNING;
            statusMsg = "placingFluid";
            return;
        }
        if (replaceMode == ConfigureSchematicannonPacket.Option.REPLACE_EMPTY.ordinal()) {
            printer.sendBlockUpdates(level);
        }
        inventory.setStackInSlot(0, ItemStack.EMPTY);
        ItemStack empty = AllItems.EMPTY_FLUID_SCHEMATIC.asStack();
        empty.setCount(inventory.getStackInSlot(1).getCount() + 1);
        inventory.setStackInSlot(1, empty);
        state = State.STOPPED;
        statusMsg = "finished";
        resetPrinter();
        AllSoundEvents.SCHEMATICANNON_FINISH.playOnServer(level, worldPosition);
        sendUpdate = true;
    }

    @Override
    protected void resetPrinter() {
        super.resetPrinter();
        fluidJobId = UUID.randomUUID();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.fluidlogistics.copper_schematicannon");
    }

    public void resolveFluidShot(BlockPos target, FluidShotPayload.Data shot) {
        FluidSchematicPrinter printer = fluidPrinter();
        if (!printer.isLoaded() && !inventory.getStackInSlot(0).isEmpty()) {
            printer.loadSchematic(inventory.getStackInSlot(0), level, false);
        }
        if (!shot.jobId().equals(fluidJobId) || printer.plan() == null
            || shot.planHash() != printer.plan().hash()
            || shot.cellIndex() < 0 || shot.cellIndex() >= printer.plan().cells().size()) {
            refundShotFluid(shot);
            return;
        }

        Cell cell = printer.plan().cell(shot.cellIndex());
        if (cell.kind() != shot.kind() || cell.expectedHost() != shot.expectedHost()
            || FluidHelper.convertToStill(cell.fluid().getFluid()) != FluidHelper.convertToStill(shot.fluid())) {
            refundShotFluid(shot);
            return;
        }

        FluidSchematicPlacement.Result placement = FluidSchematicPlacement.evaluate(level, target, cell);
        if (placement == FluidSchematicPlacement.Result.SATISFIED) {
            blocksPlaced++;
            refundShotFuel();
            refundShotFluid(shot);
            sendUpdate = true;
            return;
        }
        if (cell.kind() == Kind.WATERLOGGED && placement != FluidSchematicPlacement.Result.READY) {
            failShot(shot, "waitingForHost");
            return;
        }
        if (!FluidSchematicPlacement.place(level, target, cell, cell.kind() == Kind.FREE_SOURCE)) {
            failShot(shot, "waitingForHost");
            return;
        }

        blocksPlaced++;
        if (state != State.STOPPED) {
            state = State.RUNNING;
            statusMsg = "placingFluid";
            missingItem = null;
        }
        sendUpdate = true;
    }

    private void failShot(FluidShotPayload.Data shot, String status) {
        refundShotFuel();
        refundShotFluid(shot);
        if (state == State.STOPPED) {
            sendUpdate = true;
            return;
        }
        if (skipMissing) {
            state = State.RUNNING;
            statusMsg = "skipping";
        } else {
            fluidPrinter().retry(shot.cellIndex());
            state = State.PAUSED;
            statusMsg = status;
        }
        sendUpdate = true;
    }

    private void refundShotFuel() {
        if (!hasCreativeCrate) {
            remainingFuel++;
        }
    }

    private void refundShotFluid(FluidShotPayload.Data shot) {
        if (!shot.fluidConsumed()) {
            return;
        }
        findInventories();
        refundFluid(combinedFluidHandler(), shot.requiredFluid());
    }

    private void refundFluid(CombinedTankWrapper tanks, FluidStack drained) {
        if (drained.isEmpty()) {
            return;
        }
        int returned = tanks.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        if (returned >= drained.getAmount()) {
            return;
        }
        int remainder = drained.getAmount() - returned;
        ItemStack key = FluidPackageResourceType.createFluidKey(drained);
        ItemStack packageStack = PackageResources.createPackage(key, remainder);
        if (!packageStack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), packageStack);
        }
    }

    @Override
    protected void tickFlyingBlocks() {
        Iterator<LaunchedItem> iterator = flyingBlocks.iterator();
        while (iterator.hasNext()) {
            LaunchedItem launched = iterator.next();
            if (updateLaunchedItem(launched)) {
                iterator.remove();
            }
        }
    }

    private boolean updateLaunchedItem(LaunchedItem launched) {
        if (!(launched instanceof LaunchedItem.ForBlockState blockState)
            || !FluidShotPayload.isFluidShot(blockState.data)) {
            return launched.update(level);
        }
        if (launched.ticksRemaining > 0) {
            launched.ticksRemaining--;
            return false;
        }
        if (level.isClientSide) {
            return false;
        }
        FluidShotPayload.Data shot = FluidShotPayload.read(blockState.data);
        if (shot != null) {
            resolveFluidShot(blockState.target, shot);
        }
        return true;
    }

    private boolean hasCurrentFluidShots() {
        for (LaunchedItem launched : flyingBlocks) {
            if (launched instanceof LaunchedItem.ForBlockState blockState) {
                FluidShotPayload.Data shot = FluidShotPayload.read(blockState.data);
                if (shot != null && shot.jobId().equals(fluidJobId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private CombinedTankWrapper combinedFluidHandler() {
        return new CombinedTankWrapper(attachedFluidHandlers.toArray(IFluidHandler[]::new));
    }

    private FluidSchematicPrinter fluidPrinter() {
        return (FluidSchematicPrinter) printer;
    }

    private FluidMaterialChecklist fluidChecklist() {
        return (FluidMaterialChecklist) checklist;
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (compound.hasUUID("FluidJob")) {
            fluidJobId = compound.getUUID("FluidJob");
        }
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putUUID("FluidJob", fluidJobId);
    }
}
