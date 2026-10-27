package com.yision.fluidlogistics.content.equipment.handPointer.network;

import java.util.Comparator;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.packagerLink.LogisticsNetwork;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.yision.fluidlogistics.config.Config;
import com.yision.fluidlogistics.content.equipment.handPointer.HandPointerItem;
import com.yision.fluidlogistics.content.equipment.handPointer.client.LogisticsSelectionHandler;
import com.yision.fluidlogistics.content.equipment.handPointer.logistics.LogisticsLinkResolver;
import com.yision.fluidlogistics.network.FluidLogisticsPackets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.PacketDistributor;

public class HandPointerNetworkStatusPacket extends SimplePacketBase {
	private final BlockPos sourcePos;
	private final FactoryPanelBlock.PanelSlot panelSlot;
	private final int requestId;

	public HandPointerNetworkStatusPacket(BlockPos sourcePos, FactoryPanelBlock.PanelSlot panelSlot, int requestId) {
		this.sourcePos = sourcePos;
		this.panelSlot = panelSlot;
		this.requestId = requestId;
	}

	public HandPointerNetworkStatusPacket(FriendlyByteBuf buffer) {
		this(buffer.readBlockPos(), HandPointerAuthorizeLogisticsNetworkPacket.readPanelSlot(buffer),
			buffer.readVarInt());
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeBlockPos(sourcePos);
		HandPointerAuthorizeLogisticsNetworkPacket.writePanelSlot(buffer, panelSlot);
		buffer.writeVarInt(requestId);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player != null)
				handle(player);
		});
		return true;
	}

	private static final Comparator<GlobalPos> POSITION_ORDER = Comparator
		.comparing((GlobalPos pos) -> pos.dimension().location().toString()).thenComparingInt(pos -> pos.pos().getX())
		.thenComparingInt(pos -> pos.pos().getY()).thenComparingInt(pos -> pos.pos().getZ());

	public void handle(ServerPlayer player) {
		if (!Config.isHandPointerEnabled() || !HandPointerInteractionGuard.canUseHandPointer(player, sourcePos)) {
			return;
		}

		LogisticsLinkResolver.resolve(player.level(), sourcePos, panelSlot).ifPresent(resolved -> {
			UUID networkId = resolved.networkId();
			if (!Create.LOGISTICS.mayInteract(networkId, player)
				&& !HandPointerItem.isAuthorizedFor(player.getMainHandItem(), networkId)) {
				return;
			}

			LogisticsNetwork network = Create.LOGISTICS.logisticsNetworks.get(networkId);
			int loadedLinks = 0;
			GlobalPos unloadedLink = null;
			if (network != null) {
				for (GlobalPos pos : network.totalLinks) {
					if (network.loadedLinks.contains(pos)) {
						loadedLinks++;
					} else if (unloadedLink == null || POSITION_ORDER.compare(pos, unloadedLink) < 0) {
						unloadedLink = pos;
					}
				}
			}
			FluidLogisticsPackets.getChannel().send(PacketDistributor.PLAYER.with(() -> player),
				new Response(requestId, networkId, loadedLinks, unloadedLink));
		});
	}

	public static class Response extends SimplePacketBase {
		private final int requestId;
		private final UUID networkId;
		private final int loadedLinks;
		private final GlobalPos unloadedLink;

		public Response(int requestId, UUID networkId, int loadedLinks, @Nullable GlobalPos unloadedLink) {
			this.requestId = requestId;
			this.networkId = networkId;
			this.loadedLinks = loadedLinks;
			this.unloadedLink = unloadedLink;
		}

		public Response(FriendlyByteBuf buffer) {
			this(buffer.readVarInt(), buffer.readUUID(), buffer.readVarInt(),
				buffer.readBoolean()
					? GlobalPos.of(ResourceKey.create(Registries.DIMENSION, buffer.readResourceLocation()),
						buffer.readBlockPos())
					: null);
		}

		public int requestId() {
			return requestId;
		}

		public UUID networkId() {
			return networkId;
		}

		public int loadedLinks() {
			return loadedLinks;
		}

		public GlobalPos unloadedLink() {
			return unloadedLink;
		}

		@Override
		public void write(FriendlyByteBuf buffer) {
			buffer.writeVarInt(requestId);
			buffer.writeUUID(networkId);
			buffer.writeVarInt(loadedLinks);
			buffer.writeBoolean(unloadedLink != null);
			if (unloadedLink != null) {
				buffer.writeResourceLocation(unloadedLink.dimension().location());
				buffer.writeBlockPos(unloadedLink.pos());
			}
		}

		@Override
		public boolean handle(Context context) {
			context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::handleClient));
			return true;
		}

		@OnlyIn(Dist.CLIENT)
		private void handleClient() {
			LogisticsSelectionHandler.receiveNetworkStatus(this);
		}
	}
}
