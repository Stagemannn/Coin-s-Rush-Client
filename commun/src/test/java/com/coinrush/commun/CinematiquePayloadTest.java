package com.coinrush.commun;

import static org.junit.jupiter.api.Assertions.*;

import com.coinrush.commun.CinematiquePayload.Plan;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class CinematiquePayloadTest {
	private static Plan plan(int ticks) {
		return new Plan(1, 64, -2, 90, 10, ticks, "§6Titre", "");
	}

	private static void refuse(CinematiquePayload p) {
		assertThrows(IllegalArgumentException.class, p::validate);
	}

	@Test
	void roundTrip() {
		CinematiquePayload p = new CinematiquePayload("intro", true, List.of(plan(40), new Plan(0, 70, 0, -45, -5, 60, null, "sous")));
		CinematiquePayload back = CinematiquePayload.fromJson(p.toJson());
		assertEquals(p, back);
		back.validate();
		assertEquals(100, back.totalTicks());
	}

	@Test
	void garbageIsRefused() {
		for (String bad : new String[] {"", "null", "{", "[]", "{}", "{\"id\":\"a\"}", "{\"id\":\"a\",\"plans\":[]}", "{\"id\":\"a\",\"plans\":[null]}", "n'importe quoi"}) {
			assertThrows(IllegalArgumentException.class, () -> CinematiquePayload.fromJson(bad).validate(), bad);
		}
		assertThrows(IllegalArgumentException.class, () -> CinematiquePayload.fromJson(null).validate());
	}

	@Test
	void refuseInvalidFields() {
		refuse(new CinematiquePayload(null, false, List.of(plan(1))));
		refuse(new CinematiquePayload(" ", false, List.of(plan(1))));
		refuse(new CinematiquePayload("x".repeat(65), false, List.of(plan(1))));
		refuse(new CinematiquePayload("a", false, List.of()));
		refuse(new CinematiquePayload("a", false, Collections.nCopies(65, plan(1))));
		refuse(new CinematiquePayload("a", false, List.of(plan(0))));
		refuse(new CinematiquePayload("a", false, List.of(plan(6001))));
		refuse(new CinematiquePayload("a", false, Collections.nCopies(7, plan(6000)))); // 42 000 ticks
		refuse(new CinematiquePayload("a", false, List.of(new Plan(Double.NaN, 0, 0, 0, 0, 1, "", ""))));
		refuse(new CinematiquePayload("a", false, List.of(new Plan(31_000_000, 0, 0, 0, 0, 1, "", ""))));
		refuse(new CinematiquePayload("a", false, List.of(new Plan(0, 0, 0, Float.NaN, 0, 1, "", ""))));
		refuse(new CinematiquePayload("a", false, List.of(new Plan(0, 0, 0, 0, 91, 1, "", ""))));
		refuse(new CinematiquePayload("a", false, List.of(new Plan(0, 0, 0, 0, 0, 1, "a\tb", ""))));
		refuse(new CinematiquePayload("a", false, List.of(new Plan(0, 0, 0, 0, 0, 1, "", "x".repeat(129)))));
		refuse(new CinematiquePayload("a", false, List.of(new Plan(0, 0, 0, 0, 0, 1, " ", ""))));
	}
}
