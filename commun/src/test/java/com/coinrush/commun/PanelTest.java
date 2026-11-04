package com.coinrush.commun;

import static org.junit.jupiter.api.Assertions.*;

import com.coinrush.commun.Panel.Entry;
import com.coinrush.commun.Panel.Kind;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class PanelTest {
	private static void refuse(Entry... entries) {
		assertThrows(IllegalArgumentException.class, () -> Panel.parse(new Panel(List.of(entries)).toJson()));
	}

	@Test
	void roundTrip() {
		Panel p = new Panel(List.of(new Entry(Kind.QUETE, "Livrer le colis", "au spawn", 40, 600), new Entry(Kind.GM, "Tempête", null, null, null)));
		assertEquals(p, Panel.parse(p.toJson()));
		assertEquals(Panel.EMPTY, Panel.parse(Panel.EMPTY.toJson()));
	}

	@Test
	void refuseGarbage() {
		for (String bad : new String[] {"", "null", "{", "[]", "{\"entries\":null}", "{\"entries\":[null]}",
				"{\"entries\":[{\"kind\":\"INCONNU\",\"title\":\"a\"}]}", "{\"entries\":[{\"title\":\"a\"}]}"}) {
			assertThrows(IllegalArgumentException.class, () -> Panel.parse(bad), bad);
		}
		assertThrows(IllegalArgumentException.class, () -> Panel.parse(null));
	}

	@Test
	void refuseInvalidFields() {
		refuse(new Entry(Kind.QUETE, null, null, null, null));
		refuse(new Entry(Kind.QUETE, "  ", null, null, null));
		refuse(new Entry(Kind.QUETE, "x".repeat(Panel.MAX_TITLE + 1), null, null, null));
		refuse(new Entry(Kind.QUETE, "a", "x".repeat(Panel.MAX_DETAIL + 1), null, null));
		refuse(new Entry(Kind.QUETE, "a\tb", null, null, null));
		refuse(new Entry(Kind.QUETE, "a", null, 101, null));
		refuse(new Entry(Kind.QUETE, "a", null, -1, null));
		refuse(new Entry(Kind.QUETE, "a", null, null, -1));
		refuse(new Entry(Kind.QUETE, "a", null, null, Panel.MAX_SECONDS + 1));
		assertThrows(IllegalArgumentException.class,
				() -> Panel.parse(new Panel(Collections.nCopies(Panel.MAX_ENTRIES + 1, new Entry(Kind.FARM, "a", null, null, null))).toJson()));
	}
}
