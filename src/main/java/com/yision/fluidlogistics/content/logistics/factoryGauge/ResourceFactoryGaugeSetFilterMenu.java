package com.yision.fluidlogistics.content.logistics.factoryGauge;

import java.util.Optional;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSetItemMenu;
import com.yision.fluidlogistics.api.factorygauge.FactoryGaugeType;
import com.yision.fluidlogistics.registry.AllMenuTypes;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class ResourceFactoryGaugeSetFilterMenu extends FactoryPanelSetItemMenu {

    private static final int FILTER_SLOT_ID = 36;

    public ResourceFactoryGaugeSetFilterMenu(MenuType<?> type, int id, Inventory inv,
        FactoryPanelBehaviour contentHolder) {
        super(type, id, inv, contentHolder);
    }

    public ResourceFactoryGaugeSetFilterMenu(MenuType<?> type, int id, Inventory inv,
        FriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public static ResourceFactoryGaugeSetFilterMenu create(int id, Inventory inv, FactoryPanelBehaviour behaviour) {
        return new ResourceFactoryGaugeSetFilterMenu(AllMenuTypes.RESOURCE_GAUGE_SET_FILTER.get(), id, inv, behaviour);
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (slotId != FILTER_SLOT_ID) {
            super.clicked(slotId, dragType, clickType, player);
            return;
        }

        if (clickType == ClickType.THROW) {
            return;
        }

        ItemStack held = getCarried();

        if (clickType == ClickType.CLONE) {
            if (player.isCreative() && held.isEmpty()) {
                ItemStack stackInSlot = ghostInventory.getStackInSlot(0)
                    .copy();
                if (!stackInSlot.isEmpty()) {
                    stackInSlot.setCount(stackInSlot.getMaxStackSize());
                    setCarried(stackInSlot);
                }
            }
            return;
        }

        if (held.isEmpty()) {
            ghostInventory.setStackInSlot(0, ItemStack.EMPTY);
            getSlot(slotId).setChanged();
            return;
        }

        ItemStack resolved = resolveResourceKey(player, held);
        if (resolved == null)
            return;
        ghostInventory.setStackInSlot(0, resolved);
        getSlot(slotId).setChanged();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= 36) {
            ghostInventory.extractItem(index - 36, 1, false);
            getSlot(index).setChanged();
            return ItemStack.EMPTY;
        }

        if (ghostInventory.getStackInSlot(0)
            .isEmpty()) {
            ItemStack stack = getSlot(index)
                .getItem();
            if (!stack.isEmpty()) {
                ItemStack resolved = resolveResourceKey(player, stack);
                if (resolved != null) {
                    ghostInventory.setStackInSlot(0, resolved);
                    getSlot(FILTER_SLOT_ID).setChanged();
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void saveData(FactoryPanelBehaviour contentHolder) {
        if (contentHolder instanceof ResourceFactoryPanelBehaviour resource
            && !resource.getFilter()
                .isEmpty())
            return;
        super.saveData(contentHolder);
    }

    private ItemStack resolveResourceKey(Player player, ItemStack candidate) {
        if (!(contentHolder instanceof ResourceFactoryPanelBehaviour resource))
            return null;
        FactoryGaugeType type = resource.registeredType()
            .orElse(null);
        if (type == null)
            return null;
        try {
            Optional<ItemStack> resolved = type.filterResolver()
                .resolve(player.level(), candidate.copyWithCount(1));
            if (resolved == null || resolved.isEmpty())
                return null;
            ItemStack key = resolved.get();
            return key.isEmpty() ? null : key.copyWithCount(1);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
