package com.yision.fluidlogistics.content.fluids.pressureGauge;

import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Arrays;

final class FlowMeasurementObserver extends BlockEntityBehaviour {
    static final BehaviourType<FlowMeasurementObserver> TYPE = new BehaviourType<>();
    private final float[] pressure = new float[12];
    private final int[] flows = new int[6];
    private final FluidStack[] fluids = new FluidStack[6];
    private FluidTransportBehaviour.UpdatePhase phase;
    private BlockState state;

    FlowMeasurementObserver(SmartBlockEntity blockEntity) {
        super(blockEntity);
        Arrays.fill(fluids, FluidStack.EMPTY);
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    @Override
    public void initialize() {
        sample();
    }

    @Override
    public void tick() {
        if (getWorld().isClientSide || !FlowMeasurementUpdates.observed(getWorld(), getPos()))
            return;
        if (sample())
            FlowMeasurementUpdates.changed(getWorld(), getPos());
    }

    private boolean sample() {
        FluidTransportBehaviour transport = blockEntity.getBehaviour(FluidTransportBehaviour.TYPE);
        if (transport == null)
            return false;
        boolean changed = state != blockEntity.getBlockState() || phase != transport.phase;
        state = blockEntity.getBlockState();
        phase = transport.phase;
        for (var side : Iterate.directions) {
            int i = side.ordinal();
            var connection = transport.getConnection(side);
            float inbound = connection == null ? 0 : connection.getPressure().getFirst();
            float outbound = connection == null ? 0 : connection.getPressure().getSecond();
            var flow = transport.getFlow(side);
            int flags = connection == null ? 0 : flow == null ? 1 : 2 + (flow.inbound ? 1 : 0) + (flow.complete ? 2 : 0);
            FluidStack fluid = flow == null ? FluidStack.EMPTY : flow.fluid;
            changed |= pressure[i * 2] != inbound || pressure[i * 2 + 1] != outbound || flows[i] != flags;
            pressure[i * 2] = inbound;
            pressure[i * 2 + 1] = outbound;
            flows[i] = flags;
            if (!FluidStack.isSameFluidSameComponents(fluids[i], fluid)) {
                fluids[i] = fluid.copy();
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public void unload() {
        FlowMeasurementUpdates.changed(getWorld(), getPos());
        if (blockEntity instanceof PressureGaugeBlockEntity meter)
            FlowMeasurementUpdates.release(meter);
    }
}
