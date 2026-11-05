package com.coinrush.commun;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Serveur vers client : chapeaux cosmétiques des joueurs (UUID vers identifiant de chapeau). Un identifiant vide retire le chapeau.
 * Les entrées remplacent celles déjà connues pour ces joueurs ; le client vide sa liste à la déconnexion.
 * Transporté en JSON puis validé ({@link #validate()}) : le décodage ne lève jamais d'exception.
 */
public record HatsPayload(Map<UUID, String> hats) implements CustomPacketPayload {
	public static final int MAX_ENTRIES = 512;
	private static final Pattern ID = Pattern.compile("[a-z0-9_]{1,32}");
	public static final Type<HatsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("coinrush", "hats"));
	public static final StreamCodec<RegistryFriendlyByteBuf, HatsPayload> CODEC =
			StreamCodec.composite(ByteBufCodecs.stringUtf8(32767), HatsPayload::toJson, HatsPayload::fromJson);
	private static final Gson GSON = new Gson();

	/** Jamais d'exception : un JSON illisible donne un message sans table, que {@link #validate()} refuse. */
	static HatsPayload fromJson(String json) {
		try {
			Map<UUID, String> m = GSON.fromJson(json, new TypeToken<HashMap<UUID, String>>() { }.getType());
			if (m != null) return new HatsPayload(m);
		} catch (RuntimeException e) {
			// refusé plus bas
		}
		return new HatsPayload(null);
	}

	String toJson() {
		return GSON.toJson(hats, new TypeToken<Map<UUID, String>>() { }.getType());
	}

	public void validate() {
		if (hats == null || hats.size() > MAX_ENTRIES) throw new IllegalArgumentException("table de chapeaux invalide");
		hats.forEach((uuid, id) -> {
			if (uuid == null || id == null || (!id.isEmpty() && !ID.matcher(id).matches())) throw new IllegalArgumentException("chapeau invalide");
		});
	}

	@Override
	public Type<HatsPayload> type() {
		return TYPE;
	}
}
