package com.coinrush.commun;

import static org.junit.jupiter.api.Assertions.*;

import com.coinrush.commun.CinematiquePayload.Evenement;
import com.coinrush.commun.CinematiquePayload.Media;
import com.coinrush.commun.CinematiquePayload.Pose;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class CinematiquePayloadTest {
	private static final Pose P = new Pose(1, 64, -2, 90, 10);
	private static final String SHA_PNG = "a".repeat(64), SHA_OGG = "b".repeat(64);
	private static final List<Media> MEDIAS = List.of(new Media(SHA_PNG, "png", 1000), new Media(SHA_OGG, "ogg", 2000));

	private static Evenement ev(String type, int t, int duree, List<Pose> poses, int in, int hold, int out, String texte, float taille, int couleur, boolean bas,
			String media, float x, float y, float largeur, boolean actif, float volume, float hauteur) {
		return new Evenement(type, t, duree, poses, true, in, hold, out, texte, taille, couleur, bas, media, x, y, largeur, actif, volume, hauteur);
	}

	private static Evenement camera(int t, int duree, Pose... poses) {
		return ev("camera", t, duree, List.of(poses), 0, 0, 0, null, 0, 0, false, null, 0, 0, 0, false, 0, 0);
	}

	private static Evenement fade(int in, int hold, int out) {
		return ev("fade", 0, 0, null, in, hold, out, null, 0, 0, false, null, 0, 0, 0, false, 0, 0);
	}

	private static Evenement texte(String s, float taille, int couleur) {
		return ev("text", 10, 0, null, 5, 40, 5, s, taille, couleur, true, null, 0, 0, 0, false, 0, 0);
	}

	private static Evenement image(String sha, float x, float y, float largeur, int hold) {
		return ev("image", 10, 0, null, 5, hold, 5, null, 0, 0, false, sha, x, y, largeur, false, 0, 0);
	}

	private static Evenement audio(String sha, float volume, float hauteur) {
		return ev("audio", 10, 0, null, 0, 0, 0, null, 0, 0, false, sha, 0, 0, 0, false, volume, hauteur);
	}

	private static Evenement bandes(boolean ouvrir, int duree) {
		return ev("bandes", 10, duree, null, 0, 0, 0, null, 0, 0, false, null, 0, 0, 0, ouvrir, 0, 0);
	}

	private static CinematiquePayload cine(Evenement... e) {
		return new CinematiquePayload("a", 200, MEDIAS, List.of(e));
	}

	private static void refuse(CinematiquePayload p) {
		assertThrows(IllegalArgumentException.class, p::validate);
	}

	@Test
	void roundTrip() {
		CinematiquePayload p = cine(camera(0, 100, P, new Pose(0, 70, 0, -45, -5), P), fade(5, 10, 5), texte("§6Titre\nligne 2", 2, 0xFFAA00),
				image(SHA_PNG, 0.5f, 0.25f, 0.3f, 40), audio(SHA_OGG, 1, 1), bandes(true, 20),
				ev("cameraoff", 150, 0, null, 0, 0, 0, null, 0, 0, false, null, 0, 0, 0, false, 0, 0));
		CinematiquePayload back = CinematiquePayload.fromJson(p.toJson());
		assertEquals(p, back);
		back.validate();
	}

	@Test
	void garbageIsRefused() {
		for (String bad : new String[] {"", "null", "{", "[]", "{}", "{\"id\":\"a\"}", "{\"id\":\"a\",\"totalTicks\":5}", "{\"id\":\"a\",\"totalTicks\":5,\"evenements\":[null]}",
				"{\"id\":\"a\",\"totalTicks\":5,\"evenements\":[]}", "n'importe quoi"}) {
			assertThrows(IllegalArgumentException.class, () -> CinematiquePayload.fromJson(bad).validate(), bad);
		}
		assertThrows(IllegalArgumentException.class, () -> CinematiquePayload.fromJson(null).validate());
	}

	@Test
	void refuseInvalidHeader() {
		List<Media> aucun = List.of();
		refuse(new CinematiquePayload(null, 10, aucun, List.of()));
		refuse(new CinematiquePayload(" ", 10, aucun, List.of()));
		refuse(new CinematiquePayload("x".repeat(65), 10, aucun, List.of()));
		refuse(new CinematiquePayload("a", 0, aucun, List.of()));
		refuse(new CinematiquePayload("a", 36_001, aucun, List.of()));
		refuse(new CinematiquePayload("a", 10, aucun, null));
		refuse(new CinematiquePayload("a", 10, null, List.of()));
		refuse(new CinematiquePayload("a", 10, aucun, Collections.nCopies(257, fade(1, 0, 0))));
		new CinematiquePayload("a", 36_000, aucun, List.of()).validate(); // sans événement : permis
	}

	@Test
	void refuseInvalidMedias() {
		java.util.function.Function<List<Media>, CinematiquePayload> avec = m -> new CinematiquePayload("a", 10, m, List.of());
		avec.apply(MEDIAS).validate();
		refuse(avec.apply(Collections.nCopies(13, new Media(SHA_PNG, "png", 10)))); // trop nombreux (et doublons)
		refuse(avec.apply(List.of(new Media(SHA_PNG, "png", 10), new Media(SHA_PNG, "ogg", 10)))); // même empreinte
		refuse(avec.apply(List.of(new Media("xyz", "png", 10))));
		refuse(avec.apply(List.of(new Media("A".repeat(64), "png", 10)))); // majuscules
		refuse(avec.apply(List.of(new Media(SHA_PNG, "exe", 10))));
		refuse(avec.apply(List.of(new Media(SHA_PNG, "png", 0))));
		refuse(avec.apply(List.of(new Media(SHA_PNG, "png", CinematiquePayload.MAX_PNG + 1))));
		refuse(avec.apply(List.of(new Media(SHA_OGG, "ogg", CinematiquePayload.MAX_OGG + 1))));
		refuse(avec.apply(java.util.Arrays.asList(new Media(SHA_PNG, "png", 10), null)));
		List<Media> lourds = new java.util.ArrayList<>();
		for (int i = 0; i < 5; i++) lourds.add(new Media(String.valueOf(i).repeat(64), "ogg", CinematiquePayload.MAX_OGG)); // 20 Mio > 16 Mio
		refuse(avec.apply(lourds));
	}

	@Test
	void refuseInvalidCamera() {
		refuse(cine(camera(0, 10))); // aucune pose
		refuse(cine(camera(0, 10, Collections.nCopies(65, P).toArray(new Pose[0]))));
		refuse(cine(camera(0, 0, P, P))); // trajet sans durée
		refuse(cine(camera(0, 36_001, P, P)));
		refuse(cine(camera(-1, 10, P, P)));
		refuse(cine(camera(201, 10, P, P))); // après la fin
		refuse(cine(camera(0, 10, new Pose(Double.NaN, 0, 0, 0, 0), P)));
		refuse(cine(camera(0, 10, new Pose(31_000_000, 0, 0, 0, 0), P)));
		refuse(cine(camera(0, 10, new Pose(0, 0, 0, Float.NaN, 0), P)));
		refuse(cine(camera(0, 10, new Pose(0, 0, 0, 0, 91), P)));
		refuse(cine(ev("camera", 0, 10, java.util.Arrays.asList(P, null), 0, 0, 0, null, 0, 0, false, null, 0, 0, 0, false, 0, 0)));
		cine(camera(0, 0, P)).validate(); // caméra fixe
	}

	@Test
	void refuseInvalidEffects() {
		refuse(cine(fade(0, 0, 0)));
		refuse(cine(fade(-1, 5, 5)));
		refuse(cine(texte(" ", 1, 0)));
		refuse(cine(texte(null, 1, 0)));
		refuse(cine(texte("a\tb", 1, 0)));
		refuse(cine(texte("x".repeat(257), 1, 0)));
		refuse(cine(texte("ok", 0.4f, 0)));
		refuse(cine(texte("ok", 8.1f, 0)));
		refuse(cine(texte("ok", Float.NaN, 0)));
		refuse(cine(texte("ok", 1, -1)));
		refuse(cine(texte("ok", 1, 0x1000000)));
		refuse(cine(ev("teleport", 0, 0, null, 0, 0, 0, null, 0, 0, false, null, 0, 0, 0, false, 0, 0))); // le reste se passe côté serveur
		cine(texte("ok\nok", 8, 0xFFFFFF)).validate();
	}

	@Test
	void imagesSonsEtBandes() {
		cine(image(SHA_PNG, 0, 1, 0.02f, 10), image(SHA_PNG, 1, 0, 1, 10), audio(SHA_OGG, 0, 0.5f), audio(SHA_OGG, 10, 2), bandes(false, 0), bandes(true, 200)).validate();
		refuse(cine(image(SHA_OGG, 0.5f, 0.5f, 0.3f, 10))); // un son n'est pas une image
		refuse(cine(image("c".repeat(64), 0.5f, 0.5f, 0.3f, 10))); // fichier absent de la liste
		refuse(cine(image(null, 0.5f, 0.5f, 0.3f, 10)));
		refuse(cine(image(SHA_PNG, -0.1f, 0.5f, 0.3f, 10)));
		refuse(cine(image(SHA_PNG, 0.5f, 1.1f, 0.3f, 10)));
		refuse(cine(image(SHA_PNG, Float.NaN, 0.5f, 0.3f, 10)));
		refuse(cine(image(SHA_PNG, 0.5f, 0.5f, 0.01f, 10)));
		refuse(cine(image(SHA_PNG, 0.5f, 0.5f, 1.01f, 10)));
		refuse(cine(ev("image", 10, 0, null, 0, 0, 0, null, 0, 0, false, SHA_PNG, 0.5f, 0.5f, 0.3f, false, 0, 0))); // sans durée
		refuse(cine(audio(SHA_PNG, 1, 1))); // une image n'est pas un son
		refuse(cine(audio(SHA_OGG, -1, 1)));
		refuse(cine(audio(SHA_OGG, 10.1f, 1)));
		refuse(cine(audio(SHA_OGG, 1, 0.4f)));
		refuse(cine(audio(SHA_OGG, 1, 2.1f)));
		refuse(cine(audio(SHA_OGG, Float.NaN, 1)));
		refuse(cine(bandes(true, -1)));
		refuse(cine(bandes(true, 36_001)));
	}
}
