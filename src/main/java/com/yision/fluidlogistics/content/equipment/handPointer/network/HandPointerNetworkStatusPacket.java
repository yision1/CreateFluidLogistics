package com.yision.fluidlogistics.content.equipment.handPointer.network;

import java.util.Comparator;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.packagerLink.LogisticsNetwork;
import com.yision.fluidlogistics.content.equipment.handPointer.HandPointerItem;
import com.yision.fluidlogistics.content.equipment.handPointer.client.LogisticsSelectionHandler;
import com.yision.fluidlogistics.content.equipment.handPointer.logistics.LogisticsLinkResolver;
import com.yision.fluidlogistics.infrastructure.config.Config;
import com.yision.fluidlogistics.infrastructure.network.FluidLogisticsPackets;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record HandPointerNetworkStatusPacket(
	BlockPos sourcePos,
	FactoryPanelBlock.PanelSlot panelSlot,
	int requestId) implements ServerboundPacketPayload {

	private static final Comparator<GlobalPos> POSITION_ORDER = Comparator
		.comparing((GlobalPos pos) -> pos.dimension().location().toString())
		.thenComparingInt(pos -> pos.pos().getX())
		.thenComparingInt(pos -> pos.pos().getY())
		.thenComparingInt(pos -> pos.pos().getZ());

	public static final StreamCodec<RegistryFriendlyByteBuf, HandPointerNetworkStatusPacket> STREAM_CODEC =
		StreamCodec.composite(
			BlockPos.STREAM_CODEC, HandPointerNetworkStatusPacket::sourcePos,
			FactoryPanelBlock.PanelSlot.STREAM_CODEC, HandPointerNetworkStatusPacket::panelSlot,
			ByteBufCodecs.VAR_INT, HandPointerNetworkStatusPacket::requestId,
			HandPointerNetworkStatusPacket::new);

	@Override
	public void handle(ServerPlayer player) {
		if (!Config.isHandPointerEnabled()
			|| !HandPointerInteractionGuard.canUseHandPointer(player, sourcePos)) {
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
			CatnipServices.NETWORK.sendToClient(player,
				new Response(requestId, networkId, loadedLinks, unloadedLink));
		});
	}

	@Override
	public PacketTypeProvider getTypeProvider() {
		return FluidLogisticsPackets.HAND_POINTER_NETWORK_STATUS;
	}

	public record Response(int requestId, UUID networkId, int loadedLinks, @Nullable GlobalPos unloadedLink)
		implements ClientboundPacketPayload {

		public static final StreamCodec<RegistryFriendlyByteBuf, Response> STREAM_CODEC = StreamCodec.of(
			(buf, packet) -> {
				buf.writeVarInt(packet.requestId);
				UUIDUtil.STREAM_CODEC.encode(buf, packet.networkId);
				buf.writeVarInt(packet.loadedLinks);
				buf.writeBoolean(packet.unloadedLink != null);
				if (packet.unloadedLink != null) {
					GlobalPos.STREAM_CODEC.encode(buf, packet.unloadedLink);
				}
			},
			buf -> new Response(buf.readVarInt(), UUIDUtil.STREAM_CODEC.decode(buf), buf.readVarInt(),
				buf.readBoolean() ? GlobalPos.STREAM_CODEC.decode(buf) : null));

		@Override
		@OnlyIn(Dist.CLIENT)
		public void handle(LocalPlayer player) {
			LogisticsSelectionHandler.receiveNetworkStatus(this);
		}

		@Override
		public PacketTypeProvider getTypeProvider() {
			return FluidLogisticsPackets.HAND_POINTER_NETWORK_STATUS_RESPONSE;
		}
	}
}
