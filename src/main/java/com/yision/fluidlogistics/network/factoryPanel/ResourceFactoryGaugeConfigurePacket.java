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
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class ResourceFactoryGaugeConfigurePacket
    extends com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket<FactoryPanelBlockEntity> {

    private FactoryPanelPosition position;
    private String address;
    private Map<FactoryPanelPosition, Integer> inputAmounts;
    private int outputAmount;
    private int targetAmount;
    private int promiseClearingInterval;
    private int restockThreshold;
    private int promiseLimit;
    private int additionalStock;
    private boolean enhancementsVisible;
    private FactoryPanelPosition removeConnection;
    private boolean clearPromises;
    private boolean reset;
    private boolean redstoneReset;

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

    public ResourceFactoryGaugeConfigurePacket(FriendlyByteBuf buffer) {
        super(buffer);
    }

    @Override
    protected void writeSettings(FriendlyByteBuf buffer) {
        position.send(buffer);
        buffer.writeUtf(address);
        buffer.writeVarInt(inputAmounts.size());
        for (Map.Entry<FactoryPanelPosition, Integer> entry : inputAmounts.entrySet()) {
            entry.getKey().send(buffer);
            buffer.writeVarInt(entry.getValue());
        }
        buffer.writeVarInt(outputAmount);
        buffer.writeVarInt(targetAmount);
        buffer.writeVarInt(promiseClearingInterval);
        buffer.writeVarInt(restockThreshold);
        buffer.writeVarInt(promiseLimit);
        buffer.writeVarInt(additionalStock);
        buffer.writeBoolean(enhancementsVisible);
        buffer.writeBoolean(removeConnection != null);
        if (removeConnection != null)
            removeConnection.send(buffer);
        buffer.writeBoolean(clearPromises);
        buffer.writeBoolean(reset);
        buffer.writeBoolean(redstoneReset);
    }

    @Override
    protected void readSettings(FriendlyByteBuf buffer) {
        position = FactoryPanelPosition.receive(buffer);
        address = buffer.readUtf();
        inputAmounts = new HashMap<>();
        int inputCount = buffer.readVarInt();
        for (int i = 0; i < inputCount; i++)
            inputAmounts.put(FactoryPanelPosition.receive(buffer), buffer.readVarInt());
        outputAmount = buffer.readVarInt();
        targetAmount = buffer.readVarInt();
        promiseClearingInterval = buffer.readVarInt();
        restockThreshold = buffer.readVarInt();
        promiseLimit = buffer.readVarInt();
        additionalStock = buffer.readVarInt();
        enhancementsVisible = buffer.readBoolean();
        removeConnection = buffer.readBoolean() ? FactoryPanelPosition.receive(buffer) : null;
        clearPromises = buffer.readBoolean();
        reset = buffer.readBoolean();
        redstoneReset = buffer.readBoolean();
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
        resource.recipeOutput = Mth.clamp(outputAmount, 1, batchCap);
        resource.promiseClearingInterval = Mth.clamp(promiseClearingInterval, -1, 30);

        for (Map.Entry<FactoryPanelPosition, Integer> entry : inputAmounts.entrySet()) {
            FactoryPanelConnection connection = resource.targetedBy.get(entry.getKey());
            if (connection != null)
                connection.amount = Mth.clamp(entry.getValue(), 1, batchCap);
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

    @Override
    protected void applySettings(FactoryPanelBlockEntity be) {
    }
}
