package com.coinrush.commun;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Les items de zone et le jeton du Resort ont leur modèle, leur texture 16x16 et leurs textes (nom + infobulle) dans les deux langues. */
class ItemsZoneTest {
	private static final Path ASSETS = Path.of("src/main/resources/assets/coinrush");

	private static List<String> ids() {
		List<String> ids = new ArrayList<>(List.of("jeton_resort"));
		for (String zone : List.of("casino", "resort", "mall", "theatre")) {
			for (String niveau : List.of("facile", "moyen", "hard", "boss")) ids.add(zone + "_" + niveau);
		}
		return ids;
	}

	@Test
	void everyItemHasAssetsAndTexts() throws Exception {
		for (String lang : List.of("fr_fr", "en_us")) {
			JsonObject texts = JsonParser.parseString(Files.readString(ASSETS.resolve("lang/" + lang + ".json"))).getAsJsonObject();
			for (String id : ids()) {
				assertTrue(texts.has("item.coinrush." + id), lang + " nom " + id);
				assertTrue(texts.has("item.coinrush." + id + ".desc"), lang + " description " + id);
				assertFalse(texts.get("item.coinrush." + id).getAsString().contains("&"), "code & resté dans le nom " + id);
			}
		}
		for (String id : ids()) {
			assertTrue(Files.exists(ASSETS.resolve("items/" + id + ".json")), "items/" + id);
			assertTrue(Files.exists(ASSETS.resolve("models/item/" + id + ".json")), "models/item/" + id);
			var png = javax.imageio.ImageIO.read(ASSETS.resolve("textures/item/" + id + ".png").toFile());
			assertEquals(16, png.getWidth(), id);
			assertEquals(16, png.getHeight(), id);
		}
	}
}
