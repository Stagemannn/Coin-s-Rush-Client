package com.coinrush.commun;

import com.google.gson.Gson;
import java.net.URI;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Serveur vers client, à chaque connexion : adresse du panel joueurs avec le jeton personnel du joueur
 * (par exemple {@code http://hote:8766/?t=...}). Le client s'en sert pour lire la carte, en lecture seule.
 * Chaîne vide : pas de panel joueurs sur ce serveur. Transporté en JSON puis validé : le décodage ne lève jamais d'exception.
 */
public record CartePayload(String url) implements CustomPacketPayload {
	public static final int MAX_URL = 300;
	public static final Type<CartePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("coinrush", "carte"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CartePayload> CODEC =
			StreamCodec.composite(ByteBufCodecs.stringUtf8(32767), CartePayload::toJson, CartePayload::fromJson);
	private static final Gson GSON = new Gson();

	static CartePayload fromJson(String json) {
		try {
			CartePayload p = GSON.fromJson(json, CartePayload.class);
			if (p != null) return p;
		} catch (RuntimeException e) {
			// refusé plus bas
		}
		return new CartePayload(null);
	}

	String toJson() {
		return GSON.toJson(this);
	}

	/** @throws IllegalArgumentException si l'adresse n'est ni vide ni une adresse http(s) avec un jeton */
	public void validate() {
		if (url == null || url.length() > MAX_URL || url.chars().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("adresse invalide");
		if (url.isEmpty()) return;
		try {
			URI u = URI.create(url);
			if (!("http".equals(u.getScheme()) || "https".equals(u.getScheme())) || u.getHost() == null) throw new IllegalArgumentException("adresse invalide");
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("adresse invalide");
		}
	}

	@Override
	public Type<CartePayload> type() {
		return TYPE;
	}
}
