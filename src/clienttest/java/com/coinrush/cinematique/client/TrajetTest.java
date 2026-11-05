package com.coinrush.cinematique.client;

import static org.junit.jupiter.api.Assertions.*;

import com.coinrush.commun.CinematiquePayload.Plan;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrajetTest {
	private static Plan plan(double x, float yaw, float pitch, int ticks) {
		return new Plan(x, 64, 0, yaw, pitch, ticks, "", "");
	}

	@Test
	void walksFromPlanToPlan() {
		Trajet t = new Trajet(List.of(plan(0, 0, 0, 20), plan(10, 90, 30, 20), plan(10, 90, 30, 10)));
		assertEquals(50, t.total());
		assertEquals(0, t.pose(0).x(), 1e-9);
		assertEquals(5, t.pose(10).x(), 1e-9); // mi-chemin : adouci, donc exactement la moitié
		assertEquals(10, t.pose(20).x(), 1e-9); // arrivée au plan 1
		assertEquals(45, t.pose(10).yaw(), 1e-4);
		assertEquals(15, t.pose(10).pitch(), 1e-4);
		assertEquals(10, t.pose(49).x(), 1e-9); // dernier plan : tenu
		assertEquals(10, t.pose(500).x(), 1e-9); // au-delà : reste au dernier plan
	}

	@Test
	void easingIsGentleAtTheEnds() {
		assertEquals(0, Trajet.ease(0), 1e-12);
		assertEquals(1, Trajet.ease(1), 1e-12);
		assertTrue(Trajet.ease(0.1) < 0.1 && Trajet.ease(0.9) > 0.9);
	}

	@Test
	void anglesTakeShortestPathAndStayContinuous() {
		Trajet t = new Trajet(List.of(plan(0, 350, 0, 10), plan(0, 10, 0, 10), plan(0, 350, 0, 10)));
		assertEquals(360, t.pose(5).yaw(), 1e-3); // 350 -> 370 par le plus court, pas par 180
		assertEquals(370, t.pose(10).yaw(), 1e-3); // continu : pas de saut de 360 au changement de plan
		assertEquals(360, t.pose(15).yaw(), 1e-3);
	}

	@Test
	void locatesPlanAndLocalTick() {
		Trajet t = new Trajet(List.of(plan(0, 0, 0, 20), plan(1, 0, 0, 30)));
		assertEquals(0, t.index(19));
		assertEquals(1, t.index(20));
		assertEquals(0, t.local(20));
		assertEquals(9, t.local(29));
		assertEquals(1, t.index(999));
	}

	@Test
	void fadeInAndOut() {
		assertEquals(0, Trajet.fade(0, 100), 1e-6);
		assertEquals(1, Trajet.fade(50, 100), 1e-6);
		assertEquals(0.5f, Trajet.fade(5, 100), 1e-6);
		assertEquals(0, Trajet.fade(100, 100), 1e-6);
		assertTrue(Trajet.fade(1, 2) >= 0 && Trajet.fade(1, 2) <= 1);
	}
}
