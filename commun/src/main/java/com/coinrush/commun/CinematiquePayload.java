package com.coinrush.commun;

import com.google.gson.Gson;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Serveur vers client : cinématique en événements datés, sur le modèle de l'intro de FK. Le serveur a déjà tout résolu (plans nommés
 * en poses, millisecondes en ticks, codes {@code &} en {@code §}) ; il garde pour lui téléportation, immobilisation, sons et commandes.
 * <ul>
 * <li>{@link #CAMERA} : à {@code t}, la caméra se détache et parcourt {@code poses} en {@code duree} ticks, à vitesse constante
 * (courbe lisse passant par chaque pose si {@code lisse}, segments droits sinon). Une seule pose : caméra fixe. Une nouvelle caméra est une coupe.</li>
 * <li>{@link #CAMERA_OFF} : la caméra revient au joueur, qui reprend la main.</li>
 * <li>{@link #FADE} : fondu au noir (entrée, noir plein, sortie).</li>
 * <li>{@link #TEXT} : texte centré (ou en bas), lignes séparées par {@code \n}, taille et couleur RVB, avec entrée/maintien/sortie en ticks.</li>
 * </ul>
 * Les bandes noires restent affichées tant que la caméra est détachée. La lecture s'arrête à {@code totalTicks}.
 * Transporté en JSON puis validé ({@link #validate()}) : le décodage ne lève jamais d'exception.
 */
public record CinematiquePayload(String id, boolean bandesNoires, int totalTicks, List<Evenement> evenements) implements CustomPacketPayload {
	public record Pose(double x, double y, double z, float yaw, float pitch) { }

	/** Champs inutilisés par un type : valeurs neutres (0, null, false). */
	public record Evenement(String type, int t, int duree, List<Pose> poses, boolean lisse, int entree, int maintien, int sortie,
			String texte, float taille, int couleur, boolean bas) { }

	public static final String CAMERA = "camera", CAMERA_OFF = "cameraoff", FADE = "fade", TEXT = "text";
	public static final int MAX_ID = 64, MAX_EVENEMENTS = 256, MAX_POSES = 64, MAX_TOTAL_TICKS = 36_000, MAX_TEXTE = 256;
	public static final float MIN_TAILLE = 0.5f, MAX_TAILLE = 8f;
	private static final double MAX_COORD = 30_000_000;
	public static final Type<CinematiquePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("coinrush", "cinematique"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CinematiquePayload> CODEC =
			StreamCodec.composite(ByteBufCodecs.stringUtf8(524_288), CinematiquePayload::toJson, CinematiquePayload::fromJson);
	private static final Gson GSON = new Gson();

	/** Jamais d'exception : un JSON illisible donne une cinématique sans identifiant, que {@link #validate()} refuse. */
	static CinematiquePayload fromJson(String json) {
		try {
			CinematiquePayload p = GSON.fromJson(json, CinematiquePayload.class);
			if (p != null) return p;
		} catch (RuntimeException e) {
			// refusé plus bas
		}
		return new CinematiquePayload(null, false, 0, null);
	}

	String toJson() {
		return GSON.toJson(this);
	}

	public void validate() {
		if (!Textes.valide(id, MAX_ID)) throw new IllegalArgumentException("id invalide");
		if (totalTicks < 1 || totalTicks > MAX_TOTAL_TICKS) throw new IllegalArgumentException("durée invalide");
		if (evenements == null || evenements.size() > MAX_EVENEMENTS) throw new IllegalArgumentException("nombre d'événements invalide");
		for (Evenement e : evenements) {
			if (e == null || e.type() == null) throw new IllegalArgumentException("événement absent");
			if (e.t() < 0 || e.t() > totalTicks) throw new IllegalArgumentException("date invalide");
			switch (e.type()) {
				case CAMERA -> camera(e);
				case CAMERA_OFF -> { }
				case FADE -> duree(e);
				case TEXT -> texte(e);
				default -> throw new IllegalArgumentException("type inconnu");
			}
		}
	}

	private static void camera(Evenement e) {
		List<Pose> poses = e.poses();
		if (poses == null || poses.isEmpty() || poses.size() > MAX_POSES) throw new IllegalArgumentException("nombre de poses invalide");
		for (Pose p : poses) {
			if (p == null) throw new IllegalArgumentException("pose absente");
			for (double c : new double[] {p.x(), p.y(), p.z()}) {
				if (!Double.isFinite(c) || Math.abs(c) > MAX_COORD) throw new IllegalArgumentException("coordonnée invalide");
			}
			if (!Float.isFinite(p.yaw()) || !Float.isFinite(p.pitch()) || Math.abs(p.pitch()) > 90) throw new IllegalArgumentException("orientation invalide");
		}
		if (e.duree() < (poses.size() > 1 ? 1 : 0) || e.duree() > MAX_TOTAL_TICKS) throw new IllegalArgumentException("durée de caméra invalide");
	}

	/** Entrée, maintien et sortie : chacun dans [0, max], au moins un tick au total. */
	private static void duree(Evenement e) {
		for (int d : new int[] {e.entree(), e.maintien(), e.sortie()}) {
			if (d < 0 || d > MAX_TOTAL_TICKS) throw new IllegalArgumentException("durée d'effet invalide");
		}
		if ((long) e.entree() + e.maintien() + e.sortie() < 1) throw new IllegalArgumentException("effet sans durée");
	}

	private static void texte(Evenement e) {
		duree(e);
		String s = e.texte();
		if (s == null || s.isBlank() || s.length() > MAX_TEXTE || s.chars().anyMatch(c -> c != '\n' && Character.isISOControl(c))) throw new IllegalArgumentException("texte invalide");
		if (!Float.isFinite(e.taille()) || e.taille() < MIN_TAILLE || e.taille() > MAX_TAILLE) throw new IllegalArgumentException("taille invalide");
		if (e.couleur() < 0 || e.couleur() > 0xFFFFFF) throw new IllegalArgumentException("couleur invalide");
	}

	@Override
	public Type<CinematiquePayload> type() {
		return TYPE;
	}
}
