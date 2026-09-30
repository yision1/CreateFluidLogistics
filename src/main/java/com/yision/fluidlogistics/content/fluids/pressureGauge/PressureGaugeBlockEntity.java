package com.yision.fluidlogistics.content.fluids.pressureGauge;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity.StraightPipeFluidTransportBehaviour;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.gauge.SpeedGaugeBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.CreateLang;
import com.yision.fluidlogistics.FluidLogistics;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.lang.LangNumberFormat;
import net.createmod.catnip.theme.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class PressureGaugeBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    FlowMeasurementUpdates.Watch flowWatch;
    private double flowRate;
    private float dialTarget;
    private int color;
    public float dialState;
    public float prevDialState;

    public PressureGaugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new StraightPipeFluidTransportBehaviour(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide || isVirtual()) {
            prevDialState = dialState;
            dialState += (dialTarget - dialState) * 0.125f;
            if (dialState > 1 && level.random.nextFloat() < 0.5f)
                dialState -= (dialState - 1) * level.random.nextFloat();
            return;
        }
        PipeFlowMeasurement.update(this);
    }

    public void setTheoreticalFlowRate(double rate) {
        if (Double.compare(flowRate, rate) == 0)
            return;
        int previousSignal = getComparatorOutput();
        flowRate = rate;
        dialTarget = Math.min(SpeedGaugeBlockEntity.getDialTarget((float) rate), 1.125f);
        color = Color.mixColors(SpeedLevel.of((float) rate).getColor(), 0xffffff, .25f);
        sendData();
        if (previousSignal != getComparatorOutput())
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    int getComparatorOutput() {
        return Mth.ceil(Mth.clamp(dialTarget * 14, 0, 15));
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        if (clientPacket) {
            tag.putDouble("FlowRate", flowRate);
            tag.putFloat("Value", dialTarget);
            tag.putInt("Color", color);
        }
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if (clientPacket) {
            flowRate = tag.getDouble("FlowRate");
            dialTarget = tag.getFloat("Value");
            color = tag.getInt("Color");
        }
    }

    float getDialTarget() {
        return dialTarget;
    }

    int getColor() {
        return color;
    }

    double getFlowRate() {
        return flowRate;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CreateLang.translate("gui.gauge.info_header").forGoggles(tooltip);
        Lang.builder(FluidLogistics.MODID).translate("gui.flow_meter.title")
            .style(ChatFormatting.GRAY).forGoggles(tooltip);
        SpeedLevel level = SpeedLevel.of((float) flowRate);
        CreateLang.text(TooltipHelper.makeProgressBar(3, level.ordinal()))
            .translate("tooltip.speedRequirement." + Lang.asId(level.name()))
            .space().text("(").text(LangNumberFormat.format(flowRate)).text(" mB/t)")
            .style(level.getTextColor()).forGoggles(tooltip);
        return true;
    }

}
