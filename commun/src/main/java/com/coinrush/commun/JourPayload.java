package com.coinrush.commun;

import com.google.gson.Gson;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Serveur vers client : première ligne du panneau, « En Pause » ou « Jour <n> / <hh>:<mm> » (heure française), déjà converti en codes § couleur.
 * Chaîne vide : la ligne disparaît (aventure pas commencée). Transporté en JSON puis validé ({@link #validate()}) : le décodage ne lève jamais d exception.
 */
public record JourPayload(String texte) implements CustomPacketPayload {
	public static final int MAX_TEXTE = 48;
	public static final Type<JourPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("coinrush", "jour"));
	public static final StreamCodec<RegistryFriendlyByteBuf, JourPayload> CODEC =
			StreamCodec.composite(ByteBufCodecs.stringUtf8(32767), JourPayload::toJson, JourPayload::fromJson);
	private static final Gson GSON = new Gson();

	/** Jamais d'exception : un JSON illisible donne un texte nul, que {@link #validate()} refuse. */
	static JourPayload fromJson(String json) {
		try {
			JourPayload p = GSON.fromJson(json, JourPayload.class);
			if (p != null) return p;
		} catch (RuntimeException e) {
			// refusé plus bas
		}
		return new JourPayload(null);
	}

	String toJson() {
		return GSON.toJson(this);
	}

	public void validate() {
		if (texte == null) throw new IllegalArgumentException("texte absent");
		if (!texte.isEmpty() && !Textes.valide(texte, MAX_TEXTE)) throw new IllegalArgumentException("texte invalide");
	}

	@Override
	public Type<JourPayload> type() {
		return TYPE;
	}
}
