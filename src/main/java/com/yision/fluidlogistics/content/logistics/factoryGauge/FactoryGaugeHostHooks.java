package com.yision.fluidlogistics.content.logistics.factoryGauge;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockItem;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem;
import com.simibubi.create.foundation.utility.CreateLang;
import com.yision.fluidlogistics.api.factorygauge.FactoryGaugeType;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;

public final class FactoryGaugeHostHooks {

    private FactoryGaugeHostHooks() {
        throw new AssertionError("This class should not be instantiated");
    }

    public static ResourceFactoryPanelBehaviour ensureResourceBehaviour(
        FactoryPanelBlockEntity be, PanelSlot slot) {
        FactoryPanelBehaviour existing = be.panels.get(slot);
        if (existing instanceof ResourceFactoryPanelBehaviour resource)
            return resource;

        ResourceFactoryPanelBehaviour replacement =
            ResourceFactoryPanelBehaviour.migrateRuntimeState(be, slot, existing);
        be.panels.put(slot, replacement);
        be.attachBehaviourLate(replacement);
        be.setChanged();
        return replacement;
    }

    public static boolean addPanelOfType(FactoryPanelBlockEntity be, PanelSlot slot,
        UUID frequency, ResourceLocation typeId) {
        ResourceFactoryPanelBehaviour behaviour = ensureResourceBehaviour(be, slot);
        boolean added = be.addPanel(slot, frequency);
        if (added)
            behaviour.setGaugeTypeId(typeId);
        return added;
    }

    public static InteractionResult useRegisteredGaugeOnExistingHost(
        FactoryGaugeType type, ItemStack stack, BlockState state, Level level, BlockPos pos,
        Player player, InteractionHand hand, BlockHitResult hitResult) {
        return useGaugeOnExistingHost(stack, state, level, pos, player, hand, hitResult,
            (be, slot, network) -> addPanelOfType(be, slot, network, type.id()));
    }

    public static InteractionResult useRegisteredGaugeOnExistingHost(
        FactoryGaugeType type, UseOnContext context, BlockPos pos) {
        return useRegisteredGaugeOnExistingHost(type, context.getItemInHand(),
            context.getLevel().getBlockState(pos), context.getLevel(), pos, context.getPlayer(),
            context.getHand(), hitResultForHost(context, pos));
    }

    @Nullable
    public static BlockPos existingHostAfterFailedPlacement(
        InteractionResult placementResult, UseOnContext context) {
        if (placementResult != InteractionResult.FAIL
            || !FactoryPanelBlockItem.isTuned(context.getItemInHand()))
            return null;

        BlockPos clickedPos = context.getClickedPos();
        Level level = context.getLevel();
        if (AllBlocks.FACTORY_GAUGE.has(level.getBlockState(clickedPos)))
            return context.isSecondaryUseActive() ? null : clickedPos;

        BlockPos adjacentPos = clickedPos.relative(context.getClickedFace());
        return AllBlocks.FACTORY_GAUGE.has(level.getBlockState(adjacentPos)) ? adjacentPos : null;
    }

    private static BlockHitResult hitResultForHost(UseOnContext context, BlockPos pos) {
        return new BlockHitResult(context.getClickLocation(), context.getClickedFace(), pos, context.isInside());
    }

    private interface PanelAdder {
        boolean add(FactoryPanelBlockEntity be, PanelSlot slot, UUID network);
    }

    private static InteractionResult useGaugeOnExistingHost(
        ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
        InteractionHand hand, BlockHitResult hitResult, PanelAdder adder) {
        if (level.isClientSide())
            return InteractionResult.SUCCESS;
        if (player == null)
            return InteractionResult.PASS;

        if (!FactoryPanelBlockItem.isTuned(stack)) {
            AllSoundEvents.DENY.playOnServer(level, pos);
            player.displayClientMessage(CreateLang.translate("factory_panel.tune_before_placing")
                .component(), true);
            return InteractionResult.FAIL;
        }

        Vec3 location = hitResult.getLocation();
        if (location == null)
            return InteractionResult.SUCCESS;

        if (!(level.getBlockEntity(pos) instanceof FactoryPanelBlockEntity be))
            return InteractionResult.SUCCESS;

        PanelSlot slot = FactoryPanelBlock.getTargetedSlot(pos, state, location);
        FactoryPanelBehaviour existing = be.panels.get(slot);
        if (existing != null && existing.isActive())
            return InteractionResult.SUCCESS;

        UUID network = LogisticallyLinkedBlockItem
            .networkFromStack(FactoryPanelBlockItem.fixCtrlCopiedStack(stack));
        if (!adder.add(be, slot, network))
            return InteractionResult.SUCCESS;

        player.displayClientMessage(CreateLang.translateDirect("logistically_linked.connected"), true);
        level.playSound(null, pos, state.getSoundType()
            .getPlaceSound(), SoundSource.BLOCKS, 1.0f, 1.0f);
        if (!player.isCreative()) {
            stack.shrink(1);
            if (stack.isEmpty())
                player.setItemInHand(hand, ItemStack.EMPTY);
        }

        be.setChanged();
        be.redraw = true;
        be.lastShape = null;
        be.notifyUpdate();
        return InteractionResult.SUCCESS;
    }

    public static InteractionResult removePanel(ServerLevel level, BlockPos pos, PanelSlot slot, Player player) {
        if (!(level.getBlockEntity(pos) instanceof FactoryPanelBlockEntity be))
            return InteractionResult.PASS;
        FactoryPanelBehaviour behaviour = be.panels.get(slot);
        if (!(behaviour instanceof ResourceFactoryPanelBehaviour resource) || !resource.isResourceGauge())
            return InteractionResult.PASS;

        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, level.getBlockState(pos), player);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled())
            return InteractionResult.SUCCESS;

        ItemStack drop = FactoryGaugeDropPolicy.stackFor(resource);

        if (!be.removePanel(slot))
            return InteractionResult.SUCCESS;

        boolean destroyHost = be.activePanels() == 0;
        if (!destroyHost)
            replaceInactiveBehaviour(be, slot);

        if (!player.isCreative())
            player.getInventory()
                .placeItemBackInInventory(drop);

        IWrenchable.playRemoveSound(level, pos);
        if (destroyHost)
            level.destroyBlock(pos, false);

        return InteractionResult.SUCCESS;
    }

    private static void replaceInactiveBehaviour(FactoryPanelBlockEntity be, PanelSlot slot) {
        be.removeBehaviour(FactoryPanelBehaviour.getTypeForSlot(slot));
        ResourceFactoryPanelBehaviour replacement = new ResourceFactoryPanelBehaviour(be, slot);
        be.panels.put(slot, replacement);
        be.attachBehaviourLate(replacement);
    }

    public static boolean isRemovableResourceSlot(BlockEntity blockEntity, PanelSlot slot) {
        if (!(blockEntity instanceof FactoryPanelBlockEntity be))
            return false;
        FactoryPanelBehaviour behaviour = be.panels.get(slot);
        return behaviour instanceof ResourceFactoryPanelBehaviour resource && resource.isResourceGauge()
            && behaviour.isActive();
    }

    public static ItemStack pickStackFor(BlockState state, HitResult target,
        net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        if (target == null || target.getLocation() == null || level == null)
            return ItemStack.EMPTY;
        if (!(level.getBlockEntity(pos) instanceof FactoryPanelBlockEntity be))
            return ItemStack.EMPTY;

        PanelSlot slot = FactoryPanelBlock.getTargetedSlot(pos, state, target.getLocation());
        FactoryPanelBehaviour behaviour = be.panels.get(slot);
        if (behaviour == null || !behaviour.isActive())
            return ItemStack.EMPTY;

        return FactoryGaugeDropPolicy.stackFor(behaviour);
    }
}
