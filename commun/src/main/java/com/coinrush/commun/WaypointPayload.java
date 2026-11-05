package com.coinrush.commun;

import com.google.gson.Gson;
import java.util.regex.Pattern;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Serveur vers client : repère à rejoindre (flèche, label, distance). {@code actif=false} efface le repère.
 * Transporté en JSON puis validé ({@link #validate()}) : le décodage ne lève jamais d'exception.
 */
public record WaypointPayload(boolean actif, String dimension, double x, double y, double z, String label) implements CustomPacketPayload {
	public static final int MAX_LABEL = 64;
	private static final double MAX_COORD = 30_000_000;
	private static final Pattern DIMENSION = Pattern.compile("[a-z0-9_.-]{1,32}:[a-z0-9_./-]{1,32}");
	public static final Type<WaypointPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("coinrush", "waypoint"));
	public static final StreamCodec<RegistryFriendlyByteBuf, WaypointPayload> CODEC =
			StreamCodec.composite(ByteBufCodecs.stringUtf8(32767), WaypointPayload::toJson, WaypointPayload::fromJson);
	private static final Gson GSON = new Gson();

	/** Jamais d'exception : un JSON illisible donne un repère actif sans dimension, que {@link #validate()} refuse. */
	static WaypointPayload fromJson(String json) {
		try {
			WaypointPayload p = GSON.fromJson(json, WaypointPayload.class);
			if (p != null) return p;
		} catch (RuntimeException e) {
			// refusé plus bas
		}
		return new WaypointPayload(true, null, 0, 0, 0, null);
	}

	String toJson() {
		return GSON.toJson(this);
	}

	public void validate() {
		if (!actif) return;
		if (dimension == null || !DIMENSION.matcher(dimension).matches()) throw new IllegalArgumentException("dimension invalide");
		for (double c : new double[] {x, y, z}) {
			if (!Double.isFinite(c) || Math.abs(c) > MAX_COORD) throw new IllegalArgumentException("coordonnée invalide");
		}
		if (!Textes.valide(label, MAX_LABEL)) throw new IllegalArgumentException("label invalide");
	}

	@Override
	public Type<WaypointPayload> type() {
		return TYPE;
	}
}
