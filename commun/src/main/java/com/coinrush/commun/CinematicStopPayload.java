package com.coinrush.commun;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Serveur vers client : arrête la cinématique en cours et rend la main au joueur. */
public record CinematicStopPayload() implements CustomPacketPayload {
	public static final Type<CinematicStopPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("coinrush", "cinematic_stop"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CinematicStopPayload> CODEC = StreamCodec.unit(new CinematicStopPayload());

	@Override
	public Type<CinematicStopPayload> type() {
		return TYPE;
	}
}
