package com.coinrush.commun;

import com.google.gson.Gson;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Serveur vers client : nom du lieu affiché sous le solde (« Lieu : … »), déjà nommé et converti en codes § couleur.
 * Chaîne vide : la ligne disparaît. Transporté en JSON puis validé ({@link #validate()}) : le décodage ne lève jamais d'exception.
 */
public record LocationPayload(String lieu) implements CustomPacketPayload {
	public static final int MAX_LIEU = 64;
	public static final Type<LocationPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("coinrush", "location"));
	public static final StreamCodec<RegistryFriendlyByteBuf, LocationPayload> CODEC =
			StreamCodec.composite(ByteBufCodecs.stringUtf8(32767), LocationPayload::toJson, LocationPayload::fromJson);
	private static final Gson GSON = new Gson();

	/** Jamais d'exception : un JSON illisible donne un lieu nul, que {@link #validate()} refuse. */
	static LocationPayload fromJson(String json) {
		try {
			LocationPayload p = GSON.fromJson(json, LocationPayload.class);
			if (p != null) return p;
		} catch (RuntimeException e) {
			// refusé plus bas
		}
		return new LocationPayload(null);
	}

	String toJson() {
		return GSON.toJson(this);
	}

	public void validate() {
		if (lieu == null) throw new IllegalArgumentException("lieu absent");
		if (!lieu.isEmpty() && !Textes.valide(lieu, MAX_LIEU)) throw new IllegalArgumentException("lieu invalide");
	}

	@Override
	public Type<LocationPayload> type() {
		return TYPE;
	}
}
