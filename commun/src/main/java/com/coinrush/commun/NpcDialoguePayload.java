package com.coinrush.commun;

import com.google.gson.Gson;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Serveur vers client : fenêtre de dialogue de PNJ. Textes avec codes couleur en §, déjà convertis par le serveur.
 * {@code commande} est sans « / » ; null = le bouton ferme simplement la fenêtre. Transporté en JSON puis validé
 * ({@link #validate()}) : le décodage ne lève jamais d'exception, une donnée invalide est refusée par le client.
 */
public record NpcDialoguePayload(String titre, String texte, List<Bouton> boutons) implements CustomPacketPayload {
	public record Bouton(String label, String commande) { }

	public static final int MAX_TITRE = 64, MAX_TEXTE = 800, MAX_BOUTONS = 6, MAX_LABEL = 32, MAX_COMMANDE = 256;
	public static final Type<NpcDialoguePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("coinrush", "npc_dialogue"));
	public static final StreamCodec<RegistryFriendlyByteBuf, NpcDialoguePayload> CODEC =
			StreamCodec.composite(ByteBufCodecs.stringUtf8(32767), NpcDialoguePayload::toJson, NpcDialoguePayload::fromJson);
	private static final Gson GSON = new Gson();

	/** Jamais d'exception : un JSON illisible donne un message vide, que {@link #validate()} refuse. */
	static NpcDialoguePayload fromJson(String json) {
		try {
			NpcDialoguePayload p = GSON.fromJson(json, NpcDialoguePayload.class);
			if (p != null) return p;
		} catch (RuntimeException e) {
			// refusé plus bas
		}
		return new NpcDialoguePayload(null, null, null);
	}

	String toJson() {
		return GSON.toJson(this);
	}

	public void validate() {
		if (!Textes.valide(titre, MAX_TITRE)) throw new IllegalArgumentException("titre invalide");
		if (!Textes.valide(texte, MAX_TEXTE)) throw new IllegalArgumentException("texte invalide");
		if (boutons == null || boutons.size() > MAX_BOUTONS) throw new IllegalArgumentException("nombre de boutons invalide");
		for (Bouton b : boutons) {
			if (b == null || !Textes.valide(b.label, MAX_LABEL)) throw new IllegalArgumentException("libellé de bouton invalide");
			if (b.commande != null && (!Textes.valide(b.commande, MAX_COMMANDE) || b.commande.startsWith("/"))) throw new IllegalArgumentException("commande invalide");
		}
	}

	@Override
	public Type<NpcDialoguePayload> type() {
		return TYPE;
	}
}
