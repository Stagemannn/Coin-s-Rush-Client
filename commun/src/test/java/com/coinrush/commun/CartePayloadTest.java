package com.coinrush.commun;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CartePayloadTest {
	private static void refuse(CartePayload p) {
		assertThrows(IllegalArgumentException.class, () -> CartePayload.fromJson(p.toJson()).validate());
	}

	@Test
	void roundTrip() {
		CartePayload p = new CartePayload("http://127.0.0.1:8766/?t=abc_DEF-123");
		assertEquals(p, CartePayload.fromJson(p.toJson()));
		p.validate();
		new CartePayload("").validate(); // pas de panel joueurs
		new CartePayload("https://panel.example.org/?t=x").validate();
	}

	@Test
	void garbageIsRefused() {
		for (String bad : new String[] {"", "null", "{", "[]", "{}", "n'importe quoi"}) {
			assertThrows(IllegalArgumentException.class, () -> CartePayload.fromJson(bad).validate(), bad);
		}
	}

	@Test
	void refuseInvalidFields() {
		refuse(new CartePayload(null));
		refuse(new CartePayload("file:///etc/passwd"));
		refuse(new CartePayload("ftp://hote/x"));
		refuse(new CartePayload("http://"));
		refuse(new CartePayload("pas une adresse"));
		refuse(new CartePayload("http://h/" + "x".repeat(CartePayload.MAX_URL)));
		refuse(new CartePayload("http://h/\n"));
	}
}
