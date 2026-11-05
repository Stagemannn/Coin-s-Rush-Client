package com.coinrush.commun;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LocationPayloadTest {
	private static void refuse(LocationPayload p) {
		assertThrows(IllegalArgumentException.class, () -> LocationPayload.fromJson(p.toJson()).validate());
	}

	@Test
	void roundTrip() {
		LocationPayload p = new LocationPayload("§aParcelle de Loïc");
		assertEquals(p, LocationPayload.fromJson(p.toJson()));
		p.validate();
		new LocationPayload("").validate(); // vide : cache la ligne
	}

	@Test
	void garbageIsRefused() {
		for (String bad : new String[] {"", "null", "{", "[]", "{}", "n'importe quoi"}) {
			assertThrows(IllegalArgumentException.class, () -> LocationPayload.fromJson(bad).validate(), bad);
		}
		assertThrows(IllegalArgumentException.class, () -> LocationPayload.fromJson(null).validate());
	}

	@Test
	void refuseInvalidFields() {
		refuse(new LocationPayload(null));
		refuse(new LocationPayload(" "));
		refuse(new LocationPayload("x".repeat(LocationPayload.MAX_LIEU + 1)));
		refuse(new LocationPayload("a\tb"));
	}
}
