package com.yision.fluidlogistics.content.processing.snowGolemFreezer;

import java.util.List;
import java.util.Map;

import com.yision.fluidlogistics.config.Config;
import com.yision.fluidlogistics.registry.AllBlocks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.random.WeightedEntry.Wrapper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

public class SnowGolemFreezerBlockItem extends BlockItem {

	private final boolean capturedSnowGolem;

	public static SnowGolemFreezerBlockItem empty(Properties properties) {
		return new SnowGolemFreezerBlockItem(AllBlocks.SNOW_GOLEM_FREEZER.get(), properties, false);
	}

	public static SnowGolemFreezerBlockItem withSnowGolem(Block block, Properties properties) {
		return new SnowGolemFreezerBlockItem(block, properties, true);
	}

	private SnowGolemFreezerBlockItem(Block block, Properties properties, boolean capturedSnowGolem) {
		super(block, properties);
		this.capturedSnowGolem = capturedSnowGolem;
	}

	@Override
	public void registerBlocks(Map<Block, Item> blockToItemMap, Item item) {
		if (capturedSnowGolem)
			super.registerBlocks(blockToItemMap, item);
	}

	@Override
	public String getDescriptionId() {
		return capturedSnowGolem ? super.getDescriptionId() : "item.fluidlogistics.empty_snow_golem_freezer";
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (capturedSnowGolem || !Config.isSnowGolemFreezerEnabled()
			|| !(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof SpawnerBlockEntity blockEntity))
			return super.useOn(context);

		CompoundTag spawnerData = blockEntity.getSpawner().save(new CompoundTag());
		List<SpawnData> possibleSpawns = SpawnData.LIST_CODEC
			.parse(NbtOps.INSTANCE, spawnerData.getList("SpawnPotentials", Tag.TAG_COMPOUND)).result()
			.orElse(net.minecraft.util.random.SimpleWeightedRandomList.empty()).unwrap().stream().map(Wrapper::getData)
			.toList();
		if (possibleSpawns.isEmpty())
			possibleSpawns = SpawnData.CODEC.parse(NbtOps.INSTANCE, spawnerData.getCompound("SpawnData")).result()
				.stream().toList();
		for (SpawnData spawn : possibleSpawns) {
			if (EntityType.by(spawn.entityToSpawn()).orElse(null) != EntityType.SNOW_GOLEM)
				continue;
			Player player = context.getPlayer();
			if (!context.getLevel().isClientSide && player != null) {
				giveFreezerItemTo(player, context.getItemInHand(), context.getHand());
				context.getLevel().playSound(null, context.getClickedPos(), SoundEvents.SNOW_GOLEM_AMBIENT,
					SoundSource.NEUTRAL, .5f, 1);
			}
			return InteractionResult.SUCCESS;
		}
		return super.useOn(context);
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack heldItem, Player player, LivingEntity entity,
		InteractionHand hand) {
		if (capturedSnowGolem || !Config.isSnowGolemFreezerEnabled() || !(entity instanceof SnowGolem)
			|| !entity.isAlive())
			return InteractionResult.PASS;
		if (player.level().isClientSide)
			return InteractionResult.FAIL;

		giveFreezerItemTo(player, heldItem, hand);
		player.level().playSound(null, entity.blockPosition(), SoundEvents.SNOW_GOLEM_AMBIENT, SoundSource.NEUTRAL, .5f,
			1);
		entity.discard();
		return InteractionResult.FAIL;
	}

	private void giveFreezerItemTo(Player player, ItemStack heldItem, InteractionHand hand) {
		ItemStack filled = AllBlocks.SNOW_GOLEM_FREEZER.asStack();
		if (!player.isCreative())
			heldItem.shrink(1);
		if (heldItem.isEmpty())
			player.setItemInHand(hand, filled);
		else
			player.getInventory().placeItemBackInInventory(filled);
	}

	public boolean hasCapturedSnowGolem() {
		return capturedSnowGolem;
	}
}
