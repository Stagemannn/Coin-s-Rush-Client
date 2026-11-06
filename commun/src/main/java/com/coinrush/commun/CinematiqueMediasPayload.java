package com.coinrush.commun;

import com.google.gson.Gson;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Serveur vers client, avant une cinématique qui utilise des images ou des sons importés : la liste de ses fichiers. Le client répond par
 * {@link CinematiqueMediasDemandePayload} avec ceux qu'il n'a pas en cache, reçoit des {@link CinematiqueMediaMorceauPayload}, puis la cinématique.
 */
public record CinematiqueMediasPayload(String id, List<CinematiquePayload.Media> medias) implements CustomPacketPayload {
	public static final Type<CinematiqueMediasPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("coinrush", "cinematique_medias"));
	private static final Gson GSON = new Gson();
	public static final StreamCodec<RegistryFriendlyByteBuf, CinematiqueMediasPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(CinematiquePayload.MAX_ID), CinematiqueMediasPayload::id,
			ByteBufCodecs.stringUtf8(4096), p -> GSON.toJson(p.medias()),
			(id, json) -> new CinematiqueMediasPayload(id, lire(json)));

	/** Jamais d'exception : une liste illisible est refusée par {@link CinematiquePayload#validerMedias}. */
	private static List<CinematiquePayload.Media> lire(String json) {
		try {
			CinematiquePayload.Media[] l = GSON.fromJson(json, CinematiquePayload.Media[].class);
			return l == null ? null : java.util.Arrays.asList(l);
		} catch (RuntimeException e) {
			return null;
		}
	}

	@Override
	public Type<CinematiqueMediasPayload> type() {
		return TYPE;
	}
}
