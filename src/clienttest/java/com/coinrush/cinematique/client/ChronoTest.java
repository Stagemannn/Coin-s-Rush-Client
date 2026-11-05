package com.coinrush.cinematique.client;

import static org.junit.jupiter.api.Assertions.*;

import com.coinrush.commun.CinematiquePayload;
import com.coinrush.commun.CinematiquePayload.Evenement;
import com.coinrush.commun.CinematiquePayload.Pose;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChronoTest {
	private static Pose pose(double x, double y, double z, float yaw, float pitch) {
		return new Pose(x, y, z, yaw, pitch);
	}

	private static Evenement camera(int t, int duree, boolean lisse, Pose... poses) {
		return new Evenement("camera", t, duree, List.of(poses), lisse, 0, 0, 0, null, 0, 0, false);
	}

	private static Evenement fade(int t, int in, int hold, int out) {
		return new Evenement("fade", t, 0, null, false, in, hold, out, null, 0, 0, false);
	}

	private static Evenement texte(int t, String s) {
		return new Evenement("text", t, 0, null, false, 10, 20, 10, s, 2, 0xFFFFFF, false);
	}

	private static Chrono chrono(int total, Evenement... e) {
		CinematiquePayload p = new CinematiquePayload("a", true, total, List.of(e));
		p.validate();
		return new Chrono(p);
	}

	@Test
	void straightSegmentIsLinear() {
		Chemin c = new Chemin(List.of(pose(0, 0, 0, 0, 0), pose(10, 0, 0, 90, 30)), false);
		assertEquals(0, c.en(0).x(), 1e-9);
		assertEquals(5, c.en(0.5).x(), 1e-6);
		assertEquals(45, c.en(0.5).yaw(), 1e-3);
		assertEquals(15, c.en(0.5).pitch(), 1e-3);
		assertEquals(10, c.en(1).x(), 1e-9);
		assertEquals(10, c.en(5).x(), 1e-9); // borné
		assertEquals(0, c.en(-1).x(), 1e-9);
	}

	@Test
	void smoothPathPassesThroughEveryPose() {
		Chemin c = new Chemin(List.of(pose(0, 0, 0, 0, 0), pose(10, 0, 0, 0, 0), pose(10, 0, 10, 0, 0), pose(0, 0, 10, 0, 0)), true);
		assertEquals(0, c.en(0).x(), 1e-9);
		assertEquals(0, c.en(1).z() - 10, 1e-9);
		Pose mid = c.en(0.5); // les points sont symétriques : la mi-course est à mi-chemin du coin
		assertTrue(mid.x() > 5 && mid.x() < 12 && mid.z() > 0 && mid.z() < 10, mid.toString());
		// constant speed: equal fractions cover roughly equal distances
		double d1 = dist(c.en(0), c.en(0.1)), d2 = dist(c.en(0.5), c.en(0.6));
		assertEquals(d1, d2, d1 * 0.15);
	}

	private static double dist(Pose a, Pose b) {
		return Math.sqrt(Math.pow(a.x() - b.x(), 2) + Math.pow(a.y() - b.y(), 2) + Math.pow(a.z() - b.z(), 2));
	}

	@Test
	void anglesTakeShortestPathAndStayContinuous() {
		Chemin c = new Chemin(List.of(pose(0, 0, 0, 350, 0), pose(0, 0, 0, 10, 0), pose(0, 0, 0, 350, 0)), false);
		assertEquals(360, c.en(0.25).yaw(), 1e-3); // 350 -> 370, pas par 180
		assertEquals(370, c.en(0.5).yaw(), 1e-3);
		assertEquals(360, c.en(0.75).yaw(), 1e-3);
	}

	@Test
	void rotationOnSpotStillMoves() {
		Chemin c = new Chemin(List.of(pose(1, 2, 3, 0, 0), pose(1, 2, 3, 90, 0)), true);
		assertEquals(45, c.en(0.5).yaw(), 1e-3);
		assertEquals(1, new Chemin(List.of(pose(1, 2, 3, 0, 0)), true).en(0.7).x(), 1e-9);
	}

	@Test
	void pitchNeverLeavesTheRange() {
		Chemin c = new Chemin(List.of(pose(0, 0, 0, 0, 89), pose(1, 0, 0, 0, 90), pose(2, 0, 0, 0, 89), pose(3, 0, 0, 0, -90), pose(4, 0, 0, 0, 90)), true);
		for (int i = 0; i <= 100; i++) assertTrue(Math.abs(c.en(i / 100.0).pitch()) <= 90);
	}

	@Test
	void cameraFollowsEventsInOrderAndCameraOffReleases() {
		Chrono ch = chrono(300,
				new Evenement("cameraoff", 200, 0, null, false, 0, 0, 0, null, 0, 0, false),
				camera(100, 50, false, pose(0, 0, 0, 0, 0), pose(10, 0, 0, 0, 0)), // volontairement désordonnés
				camera(0, 0, false, pose(5, 5, 5, 0, 0)));
		assertEquals(5, ch.camera(50).x(), 1e-9); // fixe
		assertEquals(0, ch.camera(100).x(), 1e-9); // coupe : nouveau trajet
		assertEquals(5, ch.camera(125).x(), 1e-6);
		assertEquals(10, ch.camera(180).x(), 1e-9); // reste à l'arrivée
		assertNull(ch.camera(200));
		assertNull(ch.camera(299));
	}

	@Test
	void noCameraBeforeTheFirstOne() {
		Chrono ch = chrono(100, camera(40, 10, false, pose(0, 0, 0, 0, 0), pose(1, 0, 0, 0, 0)));
		assertNull(ch.camera(10));
		assertNotNull(ch.camera(40));
	}

	@Test
	void fadeCurveAndOverlap() {
		Chrono ch = chrono(200, fade(10, 10, 20, 10), fade(15, 0, 40, 0));
		assertEquals(0, ch.fondu(5), 1e-6);
		assertEquals(0.5f, ch.fondu(15 - 0.0001), 0.01f);
		assertEquals(1, ch.fondu(15), 1e-6); // le plus opaque des deux
		assertEquals(1, ch.fondu(54), 1e-6);
		assertEquals(0, ch.fondu(60), 1e-6);
	}

	@Test
	void latestTextWinsAndFadesOut() {
		Chrono ch = chrono(200, texte(0, "un"), texte(100, "deux"));
		assertNull(ch.texte(0)); // alpha 0 à l'instant exact
		assertEquals("un", ch.texte(15).texte());
		assertEquals(1, ch.texte(15).alpha(), 1e-6);
		assertEquals(0.5f, ch.texte(35).alpha(), 1e-6);
		assertNull(ch.texte(40));
		assertEquals("deux", ch.texte(120).texte());
	}
}
