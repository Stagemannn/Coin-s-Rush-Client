package com.coinrush.commun;

import com.google.gson.Gson;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Serveur vers client : cinématique en plans. La caméra part du plan 0 ; le plan i dure {@code dureeTicks} et mène la caméra
 * du plan i au plan i+1 (le dernier plan est tenu sur sa durée). Titre et sous-titre du plan s'affichent pendant sa durée
 * (vides = rien), codes § déjà convertis. Transporté en JSON puis validé ({@link #validate()}) : le décodage ne lève jamais d'exception.
 */
public record CinematiquePayload(String id, boolean bandesNoires, List<Plan> plans) implements CustomPacketPayload {
	public record Plan(double x, double y, double z, float yaw, float pitch, int dureeTicks, String titre, String sousTitre) { }

	public static final int MAX_ID = 64, MAX_PLANS = 64, MAX_PLAN_TICKS = 6000, MAX_TOTAL_TICKS = 36_000, MAX_TITRE = 96, MAX_SOUS_TITRE = 128;
	private static final double MAX_COORD = 30_000_000;
	public static final Type<CinematiquePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("coinrush", "cinematique"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CinematiquePayload> CODEC =
			StreamCodec.composite(ByteBufCodecs.stringUtf8(262_144), CinematiquePayload::toJson, CinematiquePayload::fromJson);
	private static final Gson GSON = new Gson();

	/** Jamais d'exception : un JSON illisible donne une cinématique sans identifiant, que {@link #validate()} refuse. */
	static CinematiquePayload fromJson(String json) {
		try {
			CinematiquePayload p = GSON.fromJson(json, CinematiquePayload.class);
			if (p != null) return p;
		} catch (RuntimeException e) {
			// refusé plus bas
		}
		return new CinematiquePayload(null, false, null);
	}

	String toJson() {
		return GSON.toJson(this);
	}

	/** Durée totale, en ticks (à appeler après {@link #validate()}). */
	public int totalTicks() {
		int total = 0;
		for (Plan p : plans) total += p.dureeTicks();
		return total;
	}

	public void validate() {
		if (!Textes.valide(id, MAX_ID)) throw new IllegalArgumentException("id invalide");
		if (plans == null || plans.isEmpty() || plans.size() > MAX_PLANS) throw new IllegalArgumentException("nombre de plans invalide");
		long total = 0;
		for (Plan p : plans) {
			if (p == null) throw new IllegalArgumentException("plan absent");
			for (double c : new double[] {p.x(), p.y(), p.z()}) {
				if (!Double.isFinite(c) || Math.abs(c) > MAX_COORD) throw new IllegalArgumentException("coordonnée invalide");
			}
			if (!Float.isFinite(p.yaw()) || !Float.isFinite(p.pitch()) || Math.abs(p.pitch()) > 90) throw new IllegalArgumentException("orientation invalide");
			if (p.dureeTicks() < 1 || p.dureeTicks() > MAX_PLAN_TICKS) throw new IllegalArgumentException("durée invalide");
			if (!texteOk(p.titre(), MAX_TITRE) || !texteOk(p.sousTitre(), MAX_SOUS_TITRE)) throw new IllegalArgumentException("texte invalide");
			total += p.dureeTicks();
		}
		if (total > MAX_TOTAL_TICKS) throw new IllegalArgumentException("cinématique trop longue");
	}

	/** Absent ou vide : rien à afficher. */
	private static boolean texteOk(String s, int max) {
		return s == null || s.isEmpty() || Textes.valide(s, max);
	}

	@Override
	public Type<CinematiquePayload> type() {
		return TYPE;
	}
}
