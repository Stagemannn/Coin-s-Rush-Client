package com.coinrush.commun;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HatsPayloadTest {
	private static void refuse(Map<UUID, String> m) {
		assertThrows(IllegalArgumentException.class, () -> HatsPayload.fromJson(new HatsPayload(m).toJson()).validate());
	}

	@Test
	void roundTrip() {
		Map<UUID, String> m = new HashMap<>();
		m.put(UUID.randomUUID(), "royal_crown");
		m.put(UUID.randomUUID(), ""); // retire le chapeau
		HatsPayload back = HatsPayload.fromJson(new HatsPayload(m).toJson());
		assertEquals(m, back.hats());
		back.validate();
		new HatsPayload(Map.of()).validate();
	}

	@Test
	void garbageIsRefused() {
		for (String bad : new String[] {"", "null", "{", "{\"pas-un-uuid\":\"aviator\"}", "n'importe quoi"}) {
			assertThrows(IllegalArgumentException.class, () -> HatsPayload.fromJson(bad).validate(), bad);
		}
		assertThrows(IllegalArgumentException.class, () -> HatsPayload.fromJson(null).validate());
	}

	@Test
	void refuseInvalidIds() {
		refuse(Map.of(UUID.randomUUID(), "Majuscule"));
		refuse(Map.of(UUID.randomUUID(), "avec espace"));
		refuse(Map.of(UUID.randomUUID(), "x".repeat(33)));
		Map<UUID, String> big = new HashMap<>();
		for (int i = 0; i <= HatsPayload.MAX_ENTRIES; i++) big.put(UUID.randomUUID(), "aviator");
		refuse(big);
	}
}
