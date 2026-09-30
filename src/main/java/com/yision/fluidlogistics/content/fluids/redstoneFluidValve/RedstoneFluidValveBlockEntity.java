package com.yision.fluidlogistics.content.fluids.redstoneFluidValve;

import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity.StraightPipeFluidTransportBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

public class RedstoneFluidValveBlockEntity extends SmartBlockEntity {

    final LerpedFloat pointer;

    public RedstoneFluidValveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        pointer = LerpedFloat.linear()
                .startWithValue(state.getValue(RedstoneFluidValveBlock.ENABLED) ? 1 : 0)
                .chase(state.getValue(RedstoneFluidValveBlock.ENABLED) ? 1 : 0, 0, Chaser.LINEAR);
    }

    public void setPowered(boolean powered) {
        float target = powered ? 0 : 1;
        if (pointer.getChaseTarget() == target)
            return;
        pointer.chase(target, 0.4f, Chaser.LINEAR);
        sendData();
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!level.isClientSide)
            setPowered(level.hasNeighborSignal(worldPosition));
    }

    @Override
    public void tick() {
        super.tick();
        pointer.tickChaser();
        if (level.isClientSide)
            return;
        boolean shouldOpen = pointer.getChaseTarget() == 1;
        BlockState state = getBlockState();
        if (pointer.settled() && state.getValue(RedstoneFluidValveBlock.ENABLED) != shouldOpen) {
            BlockState newState = state.setValue(RedstoneFluidValveBlock.ENABLED, shouldOpen);
            level.setBlockAndUpdate(worldPosition, newState);
        }
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.put("Pointer", pointer.writeNBT());
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        pointer.readNBT(tag.getCompound("Pointer"), clientPacket);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new RedstoneValvePipeBehaviour(this));
        registerAwardables(behaviours, FluidPropagator.getSharedTriggers());
    }

    private static class RedstoneValvePipeBehaviour extends StraightPipeFluidTransportBehaviour {

        public RedstoneValvePipeBehaviour(SmartBlockEntity blockEntity) {
            super(blockEntity);
        }

        @Override
        public boolean canHaveFlowToward(BlockState state, Direction direction) {
            return RedstoneFluidValveBlock.getPipeAxis(state) == direction.getAxis();
        }

        @Override
        public AttachmentTypes getRenderedRimAttachment(BlockAndTintGetter world, BlockPos pos, BlockState state,
                                                        Direction direction) {
            AttachmentTypes attachment = super.getRenderedRimAttachment(world, pos, state, direction);
            return attachment == AttachmentTypes.RIM || attachment == AttachmentTypes.PARTIAL_RIM
                    ? AttachmentTypes.NONE : attachment;
        }

        @Override
        public boolean canPullFluidFrom(FluidStack fluid, BlockState state, Direction direction) {
            return state.getValue(RedstoneFluidValveBlock.ENABLED)
                    && super.canPullFluidFrom(fluid, state, direction);
        }
    }
}
