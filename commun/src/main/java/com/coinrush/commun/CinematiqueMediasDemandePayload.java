package com.coinrush.commun;

import com.google.gson.Gson;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client vers serveur : les fichiers de la cinématique {@code id} que le client n'a pas (liste d'empreintes ; vide = il a tout). */
public record CinematiqueMediasDemandePayload(String id, List<String> manquants) implements CustomPacketPayload {
	public static final Type<CinematiqueMediasDemandePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("coinrush", "cinematique_medias_demande"));
	private static final Gson GSON = new Gson();
	public static final StreamCodec<RegistryFriendlyByteBuf, CinematiqueMediasDemandePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(CinematiquePayload.MAX_ID), CinematiqueMediasDemandePayload::id,
			ByteBufCodecs.stringUtf8(2048), p -> GSON.toJson(p.manquants()),
			(id, json) -> new CinematiqueMediasDemandePayload(id, lire(json)));

	/** Jamais d'exception : une liste illisible est vide. */
	private static List<String> lire(String json) {
		try {
			String[] l = GSON.fromJson(json, String[].class);
			return l == null ? List.of() : java.util.Arrays.asList(l);
		} catch (RuntimeException e) {
			return List.of();
		}
	}

	@Override
	public Type<CinematiqueMediasDemandePayload> type() {
		return TYPE;
	}
}
