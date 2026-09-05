package com.yision.fluidlogistics.mixin.logistics;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.yision.fluidlogistics.api.factorygauge.FactoryGaugeType;
import com.yision.fluidlogistics.api.factorygauge.FactoryGauges;
import com.yision.fluidlogistics.content.logistics.factoryGauge.FactoryGaugeHostHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.ForgeMod;

@Mixin(FactoryPanelBlock.class)
public abstract class FactoryGaugeHostBlockMixin {

    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level,
        BlockPos pos, Player player) {
        if (!AllBlocks.FACTORY_GAUGE.has(state))
            return ((FactoryPanelBlock) (Object) this).getCloneItemStack(level, pos, state);

        ItemStack picked = FactoryGaugeHostHooks.pickStackFor(state, target, level, pos);
        if (!picked.isEmpty())
            return picked;
        return ((FactoryPanelBlock) (Object) this).getCloneItemStack(level, pos, state);
    }

    @Inject(
        method = "use",
        at = @At("HEAD"),
        cancellable = true,
        remap = true
    )
    private void fluidlogistics$useRegisteredGaugeOnHost(BlockState state, Level level, BlockPos pos,
        Player player, InteractionHand hand, BlockHitResult hitResult,
        CallbackInfoReturnable<InteractionResult> cir) {

        ItemStack stack = player.getItemInHand(hand);
        FactoryGaugeType type = FactoryGauges.findByItem(stack.getItem())
            .orElse(null);
        if (type == null)
            return;

        cir.setReturnValue(FactoryGaugeHostHooks.useRegisteredGaugeOnExistingHost(
            type, stack, state, level, pos, player, hand, hitResult));
    }

    @Inject(
        method = "onSneakWrenched",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void fluidlogistics$wrenchRemoveResourcePanel(BlockState state, UseOnContext context,
        CallbackInfoReturnable<InteractionResult> cir) {

        if (!(context.getLevel() instanceof ServerLevel serverLevel))
            return;
        BlockPos pos = context.getClickedPos();
        PanelSlot slot = FactoryPanelBlock.getTargetedSlot(pos, state, context.getClickLocation());
        if (!FactoryGaugeHostHooks.isRemovableResourceSlot(serverLevel.getBlockEntity(pos), slot))
            return;

        cir.setReturnValue(FactoryGaugeHostHooks.removePanel(serverLevel, pos, slot, context.getPlayer()));
    }

    @Inject(
        method = "tryDestroySubPanelFirst",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void fluidlogistics$punchRemoveResourcePanel(BlockState state, Level level, BlockPos pos,
        Player player, CallbackInfoReturnable<Boolean> cir) {

        if (!(level instanceof ServerLevel serverLevel) || player == null)
            return;

        if (!(serverLevel.getBlockEntity(pos) instanceof FactoryPanelBlockEntity be))
            return;
        if (be.activePanels() < 2)
            return;

        double range = player.getAttribute(ForgeMod.BLOCK_REACH.get()).getValue() + 1;
        HitResult hitResult = player.pick(range, 1, false);
        PanelSlot slot = FactoryPanelBlock.getTargetedSlot(pos, state, hitResult.getLocation());
        if (!FactoryGaugeHostHooks.isRemovableResourceSlot(be, slot))
            return;

        InteractionResult result = FactoryGaugeHostHooks.removePanel(serverLevel, pos, slot, player);
        cir.setReturnValue(result == InteractionResult.SUCCESS);
    }
}
