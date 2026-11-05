package com.coinrush.commun;

import static org.junit.jupiter.api.Assertions.*;

import com.coinrush.commun.CinematiquePayload.Evenement;
import com.coinrush.commun.CinematiquePayload.Pose;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class CinematiquePayloadTest {
	private static final Pose P = new Pose(1, 64, -2, 90, 10);

	private static Evenement camera(int t, int duree, Pose... poses) {
		return new Evenement("camera", t, duree, List.of(poses), true, 0, 0, 0, null, 0, 0, false);
	}

	private static Evenement fade(int in, int hold, int out) {
		return new Evenement("fade", 0, 0, null, false, in, hold, out, null, 0, 0, false);
	}

	private static Evenement texte(String s, float taille, int couleur) {
		return new Evenement("text", 10, 0, null, false, 5, 40, 5, s, taille, couleur, true);
	}

	private static CinematiquePayload cine(Evenement... e) {
		return new CinematiquePayload("a", true, 200, List.of(e));
	}

	private static void refuse(CinematiquePayload p) {
		assertThrows(IllegalArgumentException.class, p::validate);
	}

	@Test
	void roundTrip() {
		CinematiquePayload p = cine(camera(0, 100, P, new Pose(0, 70, 0, -45, -5), P), fade(5, 10, 5), texte("§6Titre\nligne 2", 2, 0xFFAA00),
				new Evenement("cameraoff", 150, 0, null, false, 0, 0, 0, null, 0, 0, false));
		CinematiquePayload back = CinematiquePayload.fromJson(p.toJson());
		assertEquals(p, back);
		back.validate();
	}

	@Test
	void garbageIsRefused() {
		for (String bad : new String[] {"", "null", "{", "[]", "{}", "{\"id\":\"a\"}", "{\"id\":\"a\",\"totalTicks\":5}", "{\"id\":\"a\",\"totalTicks\":5,\"evenements\":[null]}", "n'importe quoi"}) {
			assertThrows(IllegalArgumentException.class, () -> CinematiquePayload.fromJson(bad).validate(), bad);
		}
		assertThrows(IllegalArgumentException.class, () -> CinematiquePayload.fromJson(null).validate());
	}

	@Test
	void refuseInvalidHeader() {
		refuse(new CinematiquePayload(null, false, 10, List.of()));
		refuse(new CinematiquePayload(" ", false, 10, List.of()));
		refuse(new CinematiquePayload("x".repeat(65), false, 10, List.of()));
		refuse(new CinematiquePayload("a", false, 0, List.of()));
		refuse(new CinematiquePayload("a", false, 36_001, List.of()));
		refuse(new CinematiquePayload("a", false, 10, null));
		refuse(new CinematiquePayload("a", false, 10, Collections.nCopies(257, fade(1, 0, 0))));
		new CinematiquePayload("a", false, 36_000, List.of()).validate(); // sans événement : permis
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
		refuse(new CinematiquePayload("a", true, 200, List.of(new Evenement("camera", 0, 10, java.util.Arrays.asList(P, null), true, 0, 0, 0, null, 0, 0, false))));
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
		refuse(cine(new Evenement("teleport", 0, 0, null, false, 0, 0, 0, null, 0, 0, false))); // le reste se passe côté serveur
		cine(texte("ok\nok", 8, 0xFFFFFF)).validate();
	}
}
