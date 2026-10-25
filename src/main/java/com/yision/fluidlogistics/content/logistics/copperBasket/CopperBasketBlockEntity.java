package com.yision.fluidlogistics.content.logistics.copperBasket;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.processing.basin.BasinBlock;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.ItemHandlerContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.yision.fluidlogistics.content.fluids.multiFluidTank.SmartMultiFluidTank;
import com.yision.fluidlogistics.registry.AllBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

public class CopperBasketBlockEntity extends SmartBlockEntity implements MenuProvider {
	public static final int SLOTS = 27;
	public static final int FLUID_CAPACITY = 8000;
	public static final int FLUID_TYPES = 4;
	private static final int MAX_DISPLAYED_STACKS = 9;
	private boolean loading;
	private boolean contentsDirty;
	private boolean inventoryDirty = true;
	private boolean displayDirty = true;
	private boolean fullnessDirty = true;
	private boolean inventoryFull;
	private boolean syncDirty;
	private int comparatorSignal = -1;
	private int collectionCooldown;
	private Direction collectionFacing;
	private AABB collectionArea;
	private List<ItemStack> displayedItems = List.of();
	private float previousFluidLevel;
	private float fluidLevel;
	private final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return stack.getItem().canFitInsideContainerItems();
		}

		@Override
		protected void onContentsChanged(int slot) {
			itemsChanged();
		}
	};
	private final SmartMultiFluidTank fluids = new SmartMultiFluidTank(
		FLUID_CAPACITY, FLUID_TYPES, $ -> {
			if (!loading) {
				syncDirty = true;
				contentsChanged();
			}
		});
	private final ItemHandlerContainer container = new ItemHandlerContainer(inventory) {
		@Override
		public int getMaxStackSize() {
			return 64;
		}

		@Override
		public ItemStack removeItem(int slot, int count) {
			return inventory.extractItem(slot, count, false);
		}

		@Override
		public void setChanged() {
			itemsChanged();
		}

		@Override
		public boolean stillValid(Player player) {
			return level != null && level.getBlockEntity(worldPosition) == CopperBasketBlockEntity.this
				&& player.distanceToSqr(worldPosition.getX() + .5, worldPosition.getY() + .5,
					worldPosition.getZ() + .5) <= 64;
		}
	};

	public CopperBasketBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public ItemStackHandler getInventory() {
		return inventory;
	}

	public SmartMultiFluidTank getFluids() {
		return fluids;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(new DirectBeltInputBehaviour(this)
			.allowingBeltFunnelsWhen(() -> getBlockState().getValue(CopperBasketBlock.FACING) == Direction.UP)
			.onlyInsertWhen(direction -> CopperBasketBlock.acceptsOutput(getBlockState(), direction)));
	}

	private void itemsChanged() {
		if (loading)
			return;
		inventoryDirty = true;
		displayDirty = true;
		fullnessDirty = true;
		contentsChanged();
	}

	private void contentsChanged() {
		if (!contentsDirty && level != null && !level.isClientSide)
			level.blockEntityChanged(worldPosition);
		contentsDirty = true;
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null)
			return;
		if (level.isClientSide) {
			previousFluidLevel = fluidLevel;
			fluidLevel += (fluids.getFluidAmount() / (float) FLUID_CAPACITY - fluidLevel) * .25f;
			return;
		}
		if (collectionCooldown > 0)
			collectionCooldown--;
		if (collectionCooldown == 0 && !getBlockState().getValue(CopperBasketBlock.POWERED)
			&& !isInventoryFull() && collectItems())
			collectionCooldown = 8;
		if (inventoryDirty) {
			inventoryDirty = false;
			if (displayDirty)
				refreshDisplayedItems();
			int signal = ItemHelper.calcRedstoneFromInventory(inventory);
			if (signal != comparatorSignal) {
				comparatorSignal = signal;
				level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
			}
		}
		if (contentsDirty) {
			contentsDirty = false;
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				if (!CopperBasketBlock.acceptsOutput(getBlockState(), direction.getOpposite()))
					continue;
				BlockPos basinPos = worldPosition.above().relative(direction);
				BlockState basinState = level.getBlockState(basinPos);
				if (basinState.getBlock() instanceof BasinBlock
					&& basinState.getValue(BasinBlock.FACING) == direction.getOpposite()
					&& level.getBlockEntity(basinPos) instanceof BasinBlockEntity basin)
					basin.notifyChangeOfContents();
			}
		}
		if (syncDirty) {
			syncDirty = false;
			sendData();
		}
	}

	private boolean isInventoryFull() {
		if (fullnessDirty) {
			fullnessDirty = false;
			inventoryFull = true;
			for (int slot = 0; slot < SLOTS; slot++) {
				ItemStack stack = inventory.getStackInSlot(slot);
				if (stack.isEmpty()
					|| stack.getCount() < Math.min(stack.getMaxStackSize(), inventory.getSlotLimit(slot))) {
					inventoryFull = false;
					break;
				}
			}
		}
		return inventoryFull;
	}

	private boolean collectItems() {
		Direction facing = getBlockState().getValue(CopperBasketBlock.FACING);
		if (collectionFacing != facing) {
			collectionFacing = facing;
			collectionArea = new AABB(worldPosition)
				.expandTowards(facing.getStepX(), facing.getStepY(), facing.getStepZ());
		}
		for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, collectionArea, ItemEntity::isAlive)) {
			ItemStack stack = entity.getItem();
			ItemStack remainder = ItemHandlerHelper.insertItemStacked(inventory, stack.copy(), false);
			if (remainder.getCount() == stack.getCount())
				continue;
			if (remainder.isEmpty()) {
				entity.discard();
				return true;
			}
			entity.setItem(remainder);
		}
		return false;
	}

	public List<ItemStack> getDisplayedItems() {
		if (displayDirty)
			refreshDisplayedItems();
		return displayedItems;
	}

	private void refreshDisplayedItems() {
		displayDirty = false;
		List<ItemStack> updated = new ArrayList<>(MAX_DISPLAYED_STACKS);
		for (int slot = 0; slot < SLOTS; slot++) {
			ItemStack stack = inventory.getStackInSlot(slot);
			if (stack.isEmpty())
				continue;
			ItemStack matching = ItemStack.EMPTY;
			for (ItemStack displayed : updated) {
				if (ItemStack.isSameItemSameComponents(displayed, stack)) {
					matching = displayed;
					break;
				}
			}
			int limit = Math.min(64, stack.getMaxStackSize());
			if (!matching.isEmpty())
				matching.grow(Math.min(stack.getCount(), Math.max(0, limit - matching.getCount())));
			else if (updated.size() < MAX_DISPLAYED_STACKS)
				updated.add(stack.copyWithCount(Math.min(stack.getCount(), limit)));
		}
		boolean changed = updated.size() != displayedItems.size();
		for (int i = 0; !changed && i < updated.size(); i++)
			changed = !ItemStack.matches(updated.get(i), displayedItems.get(i));
		if (changed) {
			displayedItems = List.copyOf(updated);
			syncDirty = true;
		}
	}

	public float getFluidLevel(float partialTicks) {
		return previousFluidLevel + (fluidLevel - previousFluidLevel) * partialTicks;
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		loading = true;
		try {
			if (clientPacket) {
				List<ItemStack> displayed = new ArrayList<>(MAX_DISPLAYED_STACKS);
				ListTag items = tag.getList("DisplayedItems", Tag.TAG_COMPOUND);
				for (int i = 0; i < Math.min(items.size(), MAX_DISPLAYED_STACKS); i++) {
					ItemStack stack = ItemStack.parseOptional(registries, items.getCompound(i));
					if (!stack.isEmpty())
						displayed.add(stack);
				}
				displayedItems = List.copyOf(displayed);
				displayDirty = false;
			} else {
				CompoundTag items = tag.getCompound("Items").copy();
				items.putInt("Size", SLOTS);
				inventory.deserializeNBT(registries, items);
				inventoryDirty = true;
				displayDirty = true;
				fullnessDirty = true;
				collectionCooldown = Math.clamp(tag.getInt("CollectionCooldown"), 0, 8);
			}
			fluids.resetTanks();
			CompoundTag contents = tag.getCompound("Fluids");
			for (int slot = 0; slot < FLUID_TYPES; slot++)
				fluids.fill(FluidStack.parseOptional(registries, contents.getCompound(Integer.toString(slot))),
					FluidAction.EXECUTE);
		} finally {
			loading = false;
		}
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		if (clientPacket) {
			if (displayDirty)
				refreshDisplayedItems();
			ListTag items = new ListTag();
			for (ItemStack stack : displayedItems)
				items.add(stack.save(registries));
			tag.put("DisplayedItems", items);
		} else {
			tag.put("Items", inventory.serializeNBT(registries));
			tag.putInt("CollectionCooldown", collectionCooldown);
		}
		tag.put("Fluids", fluids.save(registries, new CompoundTag()));
	}

	@Override
	public void destroy() {
		super.destroy();
		if (level == null || level.isClientSide)
			return;
		ItemHelper.dropContents(level, worldPosition, inventory);
		fluids.resetTanks();
	}

	@Override
	public Component getDisplayName() {
		return getBlockState().getBlock().getName();
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
		return new ChestMenu(MenuType.GENERIC_9x3, id, playerInventory, container, 3) {
			@Override
			protected Slot addSlot(Slot slot) {
				if (slot.container == CopperBasketBlockEntity.this.container) {
					return super.addSlot(new Slot(slot.container, slot.getContainerSlot(), slot.x, slot.y) {
						@Override
						public boolean mayPlace(ItemStack stack) {
							return inventory.isItemValid(getContainerSlot(), stack);
						}
					});
				}
				return super.addSlot(slot);
			}
		};
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, AllBlockEntities.COPPER_BASKET.get(),
			(be, side) -> be.inventory);
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, AllBlockEntities.COPPER_BASKET.get(),
			(be, side) -> be.fluids);
	}
}
