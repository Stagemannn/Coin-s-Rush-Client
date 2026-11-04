package com.coinrush.commun;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Serveur vers client : lance une cinématique (JSON de {@link Scenario}). Un scénario invalide est ignoré par le client. */
public record CinematicPayload(String json) implements CustomPacketPayload {
	public static final Type<CinematicPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("coinrush", "cinematic"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CinematicPayload> CODEC =
			StreamCodec.composite(ByteBufCodecs.stringUtf8(32767), CinematicPayload::json, CinematicPayload::new);

	public static CinematicPayload of(Scenario scenario) {
		return new CinematicPayload(scenario.toJson());
	}

	@Override
	public Type<CinematicPayload> type() {
		return TYPE;
	}
}
