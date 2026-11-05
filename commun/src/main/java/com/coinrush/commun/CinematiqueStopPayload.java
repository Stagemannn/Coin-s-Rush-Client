package com.coinrush.commun;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Serveur vers client : arrêt forcé de la cinématique en plans en cours (le client ne répond pas). */
public record CinematiqueStopPayload() implements CustomPacketPayload {
	public static final Type<CinematiqueStopPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("coinrush", "cinematique_stop"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CinematiqueStopPayload> CODEC = StreamCodec.unit(new CinematiqueStopPayload());

	@Override
	public Type<CinematiqueStopPayload> type() {
		return TYPE;
	}
}
