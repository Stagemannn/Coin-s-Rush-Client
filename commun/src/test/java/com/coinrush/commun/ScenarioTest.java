package com.coinrush.commun;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ScenarioTest {
	private static Scenario.Point p(double x, int ticks) {
		return new Scenario.Point(x, 64, 0, 0, 0, ticks);
	}

	private static Scenario ok() {
		return new Scenario("intro/test-1", true, List.of(p(0, 10), p(10, 100)), List.of(new Scenario.Text(5, 50, "Bonjour")));
	}

	private static void refuse(Scenario s) {
		assertThrows(IllegalArgumentException.class, () -> Scenario.parse(s.toJson()));
	}

	@Test
	void roundTrip() {
		Scenario s = Scenario.parse(ok().toJson());
		assertEquals(ok(), s);
		assertEquals(110, s.totalTicks());
	}

	@Test
	void refuseGarbage() {
		for (String bad : new String[] {"", "null", "[]", "{", "{\"id\":1}", "{\"id\":\"a\"}", "\"x\"", "{\"id\":\"a\",\"points\":[{\"x\":\"NaN\"}]}"}) {
			assertThrows(IllegalArgumentException.class, () -> Scenario.parse(bad), bad);
		}
		assertThrows(IllegalArgumentException.class, () -> Scenario.parse(null));
	}

	@Test
	void refuseInvalidFields() {
		refuse(new Scenario("MAJUSCULE", true, ok().points(), null));
		refuse(new Scenario(null, true, ok().points(), null));
		refuse(new Scenario("a", true, List.of(), null));
		refuse(new Scenario("a", true, null, null));
		refuse(new Scenario("a", true, List.of(p(0, 0)), null)); // durée totale nulle
		refuse(new Scenario("a", true, List.of(p(0, 5), p(1, -5)), null));
		refuse(new Scenario("a", true, List.of(p(0, Scenario.MAX_TICKS), p(1, 1)), null));
		refuse(new Scenario("a", true, List.of(p(Double.POSITIVE_INFINITY, 5)), null));
		refuse(new Scenario("a", true, List.of(p(31_000_000, 5)), null));
		refuse(new Scenario("a", true, List.of(new Scenario.Point(0, 0, 0, 0, 91, 5)), null));
		refuse(new Scenario("a", true, ok().points(), List.of(new Scenario.Text(0, 111, "trop long"))));
		refuse(new Scenario("a", true, ok().points(), List.of(new Scenario.Text(10, 10, "vide"))));
		refuse(new Scenario("a", true, ok().points(), List.of(new Scenario.Text(0, 5, " "))));
		refuse(new Scenario("a", true, ok().points(), List.of(new Scenario.Text(0, 5, "x".repeat(201)))));
		refuse(new Scenario("a", true, ok().points(), List.of(new Scenario.Text(0, 5, "a\nb"))));
	}

	@Test
	void refuseTooMany() {
		List<Scenario.Point> points = java.util.Collections.nCopies(Scenario.MAX_POINTS + 1, p(0, 1));
		refuse(new Scenario("a", true, points, null));
		List<Scenario.Text> texts = java.util.Collections.nCopies(Scenario.MAX_TEXTS + 1, new Scenario.Text(0, 1, "t"));
		refuse(new Scenario("a", true, ok().points(), texts));
	}

	@Test
	void interpolation() {
		Scenario s = ok();
		assertEquals(0, s.at(0).x());
		assertEquals(0, s.at(10).x());
		assertEquals(5, s.at(60).x(), 1e-9);
		assertEquals(10, s.at(110).x());
		assertEquals(10, s.at(9999).x());
		assertTrue(s.textsAt(4).isEmpty());
		assertEquals(1, s.textsAt(5).size());
		assertTrue(s.textsAt(50).isEmpty());
	}

	@Test
	void yawTakesShortestWay() {
		Scenario s = new Scenario("a", true, List.of(new Scenario.Point(0, 0, 0, 350, 0, 0), new Scenario.Point(0, 0, 0, 10, 0, 10)), null);
		assertEquals(360, s.at(5).yaw(), 1e-3);
	}
}
