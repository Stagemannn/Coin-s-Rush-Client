package com.coinrush.commun;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client vers serveur : la cinématique {@code id} est terminée côté client. Un simple signal : le serveur ne s'y fie pas. */
public record CinematiqueFiniePayload(String id) implements CustomPacketPayload {
	public static final Type<CinematiqueFiniePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("coinrush", "cinematique_finie"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CinematiqueFiniePayload> CODEC =
			StreamCodec.composite(ByteBufCodecs.stringUtf8(CinematiquePayload.MAX_ID), CinematiqueFiniePayload::id, CinematiqueFiniePayload::new);

	@Override
	public Type<CinematiqueFiniePayload> type() {
		return TYPE;
	}
}
