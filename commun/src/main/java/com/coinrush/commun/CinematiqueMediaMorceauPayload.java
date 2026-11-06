package com.coinrush.commun;

import com.google.gson.Gson;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Serveur vers client : un morceau d'un fichier demandé. Les morceaux d'un fichier arrivent dans l'ordre, puis la cinématique. */
public record CinematiqueMediaMorceauPayload(String sha, int index, int total, byte[] donnees) implements CustomPacketPayload {
	/** Octets par morceau (un paquet du serveur au client plafonne à 1 Mio). */
	public static final int MORCEAU = 256 * 1024;
	public static final Type<CinematiqueMediaMorceauPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("coinrush", "cinematique_media_morceau"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CinematiqueMediaMorceauPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(64), CinematiqueMediaMorceauPayload::sha,
			ByteBufCodecs.VAR_INT, CinematiqueMediaMorceauPayload::index,
			ByteBufCodecs.VAR_INT, CinematiqueMediaMorceauPayload::total,
			ByteBufCodecs.byteArray(MORCEAU), CinematiqueMediaMorceauPayload::donnees,
			CinematiqueMediaMorceauPayload::new);

	@Override
	public Type<CinematiqueMediaMorceauPayload> type() {
		return TYPE;
	}
}
