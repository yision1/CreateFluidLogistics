package com.yision.fluidlogistics.content.logistics.potatoServer;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import com.yision.fluidlogistics.FluidLogistics;
import java.util.List;
import java.util.Locale;
import net.createmod.catnip.lang.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class PotatoServerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private double mspt;

    public PotatoServerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(20);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public void lazyTick() {
        if (!(level instanceof ServerLevel serverLevel))
            return;
        double current = serverLevel.getServer().getAverageTickTimeNanos() / 1_000_000d;
        if (Math.round(current * 10) == Math.round(mspt * 10))
            return;
        mspt = current;
        sendData();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (clientPacket)
            tag.putDouble("MSPT", mspt);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (clientPacket)
            mspt = tag.getDouble("MSPT");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        Lang.builder(FluidLogistics.MODID).translate("gui.potato_server.info").forGoggles(tooltip);
        CreateLang.text("TPS:").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        double tps = mspt <= 0 ? 20 : Math.min(20, 1000 / mspt);
        CreateLang.text((tps == 20 ? "20" : String.format(Locale.ROOT, "%.1f", tps)) + " ticks/s")
            .style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        CreateLang.text("MSPT:").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        CreateLang.text(String.format(Locale.ROOT, "%.1f ms/tick", mspt))
            .style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        return true;
    }
}
