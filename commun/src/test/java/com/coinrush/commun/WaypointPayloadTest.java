package com.coinrush.commun;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class WaypointPayloadTest {
	private static void refuse(WaypointPayload p) {
		assertThrows(IllegalArgumentException.class, () -> WaypointPayload.fromJson(p.toJson()).validate());
	}

	@Test
	void roundTrip() {
		WaypointPayload p = new WaypointPayload(true, "minecraft:overworld", 10.5, 64, -20, "§cLe marchand");
		assertEquals(p, WaypointPayload.fromJson(p.toJson()));
		p.validate();
		new WaypointPayload(false, null, 0, 0, 0, null).validate(); // effacement : le reste est ignoré
	}

	@Test
	void garbageIsRefused() {
		for (String bad : new String[] {"", "null", "{", "[]", "{\"actif\":true}", "n'importe quoi"}) {
			assertThrows(IllegalArgumentException.class, () -> WaypointPayload.fromJson(bad).validate(), bad);
		}
		assertThrows(IllegalArgumentException.class, () -> WaypointPayload.fromJson(null).validate());
	}

	@Test
	void refuseInvalidFields() {
		refuse(new WaypointPayload(true, null, 0, 0, 0, "a"));
		refuse(new WaypointPayload(true, "Overworld", 0, 0, 0, "a"));
		refuse(new WaypointPayload(true, "minecraft:overworld", Double.NaN, 0, 0, "a"));
		refuse(new WaypointPayload(true, "minecraft:overworld", 0, Double.POSITIVE_INFINITY, 0, "a"));
		refuse(new WaypointPayload(true, "minecraft:overworld", 0, 0, 31_000_000, "a"));
		refuse(new WaypointPayload(true, "minecraft:overworld", 0, 0, 0, null));
		refuse(new WaypointPayload(true, "minecraft:overworld", 0, 0, 0, " "));
		refuse(new WaypointPayload(true, "minecraft:overworld", 0, 0, 0, "x".repeat(WaypointPayload.MAX_LABEL + 1)));
		refuse(new WaypointPayload(true, "minecraft:overworld", 0, 0, 0, "a\tb"));
	}
}
