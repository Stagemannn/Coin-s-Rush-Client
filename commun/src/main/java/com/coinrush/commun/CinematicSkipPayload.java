package com.coinrush.commun;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client vers serveur : le joueur demande à passer la cinématique. Une simple demande : le serveur décide, et ne lui doit rien. */
public record CinematicSkipPayload() implements CustomPacketPayload {
	public static final Type<CinematicSkipPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("coinrush", "cinematic_skip"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CinematicSkipPayload> CODEC = StreamCodec.unit(new CinematicSkipPayload());

	@Override
	public Type<CinematicSkipPayload> type() {
		return TYPE;
	}
}
