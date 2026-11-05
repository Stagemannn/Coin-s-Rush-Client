package com.coinrush.commun;

import static org.junit.jupiter.api.Assertions.*;

import com.coinrush.commun.NpcDialoguePayload.Bouton;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class NpcDialoguePayloadTest {
	private static void refuse(NpcDialoguePayload p) {
		assertThrows(IllegalArgumentException.class, () -> NpcDialoguePayload.fromJson(p.toJson()).validate());
	}

	private static NpcDialoguePayload with(Bouton... b) {
		return new NpcDialoguePayload("§6Marchand", "Bonjour voyageur.", List.of(b));
	}

	@Test
	void roundTrip() {
		NpcDialoguePayload p = with(new Bouton("Accepter", "quest accept 1"), new Bouton("Fermer", null));
		NpcDialoguePayload back = NpcDialoguePayload.fromJson(p.toJson());
		assertEquals(p, back);
		back.validate();
		with().validate(); // aucun bouton : Échap ferme
	}

	@Test
	void garbageNeverThrowsOnDecodeButIsRefused() {
		for (String bad : new String[] {"", "null", "{", "[]", "{\"titre\":1}", "{\"titre\":\"a\"}", "n'importe quoi"}) {
			assertThrows(IllegalArgumentException.class, () -> NpcDialoguePayload.fromJson(bad).validate(), bad);
		}
		assertThrows(IllegalArgumentException.class, () -> NpcDialoguePayload.fromJson(null).validate());
	}

	@Test
	void refuseInvalidFields() {
		refuse(new NpcDialoguePayload(null, "t", List.of()));
		refuse(new NpcDialoguePayload(" ", "t", List.of()));
		refuse(new NpcDialoguePayload("x".repeat(NpcDialoguePayload.MAX_TITRE + 1), "t", List.of()));
		refuse(new NpcDialoguePayload("a", "x".repeat(NpcDialoguePayload.MAX_TEXTE + 1), List.of()));
		refuse(new NpcDialoguePayload("a", "ligne\nbrisée", List.of()));
		refuse(new NpcDialoguePayload("a", "t", null));
		refuse(with(new Bouton(null, null)));
		refuse(with(new Bouton("x".repeat(NpcDialoguePayload.MAX_LABEL + 1), null)));
		refuse(with(new Bouton("ok", "/give @s diamond")));
		refuse(with(new Bouton("ok", "a\nb")));
		refuse(with(new Bouton("ok", "x".repeat(NpcDialoguePayload.MAX_COMMANDE + 1))));
		refuse(new NpcDialoguePayload("a", "t", Collections.nCopies(NpcDialoguePayload.MAX_BOUTONS + 1, new Bouton("ok", null))));
	}
}
