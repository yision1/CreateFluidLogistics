package com.yision.fluidlogistics.network.factoryPanel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.yision.fluidlogistics.api.packager.PackageResourceDisplay;
import com.yision.fluidlogistics.api.packager.PackageResources;
import com.yision.fluidlogistics.content.logistics.factoryGauge.ResourceFactoryPanelBehaviour;
import com.yision.fluidlogistics.network.FluidLogisticsPackets;
import com.yision.fluidlogistics.util.ResourceGaugeHelper;

import net.createmod.catnip.codecs.stream.CatnipLargerStreamCodecs;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class ResourceFactoryGaugeConfigurePacket
    extends com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket<FactoryPanelBlockEntity> {

    public static final StreamCodec<RegistryFriendlyByteBuf, ResourceFactoryGaugeConfigurePacket> STREAM_CODEC =
        CatnipLargerStreamCodecs.composite(
            FactoryPanelPosition.STREAM_CODEC, packet -> packet.position,
            ByteBufCodecs.STRING_UTF8, packet -> packet.address,
            ByteBufCodecs.map(HashMap::new, FactoryPanelPosition.STREAM_CODEC, ByteBufCodecs.INT),
            packet -> packet.inputAmounts,
            ByteBufCodecs.VAR_INT, packet -> packet.outputAmount,
            ByteBufCodecs.VAR_INT, packet -> packet.targetAmount,
            ByteBufCodecs.VAR_INT, packet -> packet.promiseClearingInterval,
            ByteBufCodecs.VAR_INT, packet -> packet.restockThreshold,
            ByteBufCodecs.VAR_INT, packet -> packet.promiseLimit,
            ByteBufCodecs.VAR_INT, packet -> packet.additionalStock,
            ByteBufCodecs.BOOL, packet -> packet.enhancementsVisible,
            CatnipStreamCodecBuilders.nullable(FactoryPanelPosition.STREAM_CODEC), packet -> packet.removeConnection,
            ByteBufCodecs.BOOL, packet -> packet.clearPromises,
            ByteBufCodecs.BOOL, packet -> packet.reset,
            ByteBufCodecs.BOOL, packet -> packet.redstoneReset,
            ResourceFactoryGaugeConfigurePacket::new
        );

    private final FactoryPanelPosition position;
    private final String address;
    private final Map<FactoryPanelPosition, Integer> inputAmounts;
    private final int outputAmount;
    private final int targetAmount;
    private final int promiseClearingInterval;
    private final int restockThreshold;
    private final int promiseLimit;
    private final int additionalStock;
    private final boolean enhancementsVisible;
    private final FactoryPanelPosition removeConnection;
    private final boolean clearPromises;
    private final boolean reset;
    private final boolean redstoneReset;

    public ResourceFactoryGaugeConfigurePacket(FactoryPanelPosition position, String address,
        Map<FactoryPanelPosition, Integer> inputAmounts, int outputAmount, int targetAmount,
        int promiseClearingInterval,
        int restockThreshold, int promiseLimit, int additionalStock, boolean enhancementsVisible,
        @Nullable FactoryPanelPosition removeConnection, boolean clearPromises, boolean reset,
        boolean redstoneReset) {
        super(position.pos());
        this.position = position;
        this.address = address;
        this.inputAmounts = inputAmounts;
        this.outputAmount = outputAmount;
        this.targetAmount = targetAmount;
        this.promiseClearingInterval = promiseClearingInterval;
        this.restockThreshold = restockThreshold;
        this.promiseLimit = promiseLimit;
        this.additionalStock = additionalStock;
        this.enhancementsVisible = enhancementsVisible;
        this.removeConnection = removeConnection;
        this.clearPromises = clearPromises;
        this.reset = reset;
        this.redstoneReset = redstoneReset;
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return FluidLogisticsPackets.RESOURCE_FACTORY_GAUGE_CONFIGURE;
    }

    @Override
    protected void applySettings(ServerPlayer player, FactoryPanelBlockEntity be) {
        FactoryPanelBehaviour behaviour = be.panels.get(position.slot());
        if (!(behaviour instanceof ResourceFactoryPanelBehaviour resource) || !resource.isResourceGauge())
            return;
        if (resource.registeredType()
            .isEmpty())
            return;
        if (!Create.LOGISTICS.mayInteract(resource.network, player))
            return;

        if (reset) {
            resource.recipeAddress = "";
            resource.recipeOutput = 1;
            resource.promiseClearingInterval = -1;
            resource.activeCraftingArrangement = List.of();
            resource.forceClearPromises = true;
            resource.disconnectAll();
            resource.setFilter(ItemStack.EMPTY);
            resource.count = 0;
            resource.resetResourceSettings();
            be.redraw = true;
            be.notifyUpdate();
            return;
        }

        ItemStack filter = resource.getFilter();
        PackageResourceDisplay display = PackageResources.displayOf(filter)
            .orElse(null);
        PackageResourceDisplay.FactoryPanelRestockPolicy policy =
            display == null ? PackageResourceDisplay.FactoryPanelRestockPolicy.standard()
                : display.factoryPanelRestockPolicy(filter);
        int batchCap = policy.maxRequestPerBatch();

        resource.fluidlogistics$setTargetAmount(targetAmount);

        resource.recipeAddress = address == null ? "" : address;
        resource.recipeOutput = Math.clamp(outputAmount, 1, batchCap);
        resource.promiseClearingInterval = Math.clamp(promiseClearingInterval, -1, 30);

        for (Map.Entry<FactoryPanelPosition, Integer> entry : inputAmounts.entrySet()) {
            FactoryPanelConnection connection = resource.targetedBy.get(entry.getKey());
            if (connection != null)
                connection.amount = Math.clamp(entry.getValue(), 1, batchCap);
        }

        if (removeConnection != null) {
            resource.targetedBy.remove(removeConnection);
            FactoryPanelBehaviour source = FactoryPanelBehaviour.at(be.getLevel(), removeConnection);
            if (source != null) {
                source.targeting.remove(resource.getPanelPosition());
                source.blockEntity.sendData();
            }
        }

        if (clearPromises)
            resource.forceClearPromises = true;

        if (redstoneReset)
            resource.disconnectAllLinks();

        if (policy.configurableThreshold())
            resource.fluidlogistics$setRestockThreshold(restockThreshold);
        if (policy.configurablePromiseLimit())
            resource.fluidlogistics$setPromiseLimit(promiseLimit);
        if (policy.configurableAdditionalStock())
            resource.fluidlogistics$setAdditionalStock(additionalStock);
        resource.fluidlogistics$setEnhancementsVisible(enhancementsVisible);

        resource.resetTimerSlightly();
        be.notifyUpdate();
    }
}
