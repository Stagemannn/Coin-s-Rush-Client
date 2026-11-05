package com.coinrush.camera.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Options de caméra du joueur, enregistrées localement dans config/coinrush.json. */
public final class CameraOptions {
	/** Décalage latéral de la caméra en 3e personne (vue de dos), en blocs : de -2 (à gauche) à +2 (à droite), 0 = vanilla. */
	public static volatile double cameraOffset;

	private CameraOptions() { }

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("coinrush.json");
	}

	public static void load() {
		try {
			if (!Files.exists(file())) return;
			JsonObject o = new Gson().fromJson(Files.readString(file(), StandardCharsets.UTF_8), JsonObject.class);
			if (o != null && o.has("cameraOffset")) cameraOffset = Math.max(-2, Math.min(2, o.get("cameraOffset").getAsDouble()));
		} catch (IOException | RuntimeException e) {
			// fichier illisible : valeur par défaut
		}
	}

	public static void save() {
		JsonObject o = new JsonObject();
		o.addProperty("cameraOffset", cameraOffset);
		try {
			Files.writeString(file(), new Gson().toJson(o), StandardCharsets.UTF_8);
		} catch (IOException e) {
			// non enregistré : valable jusqu'à la fermeture du jeu
		}
	}
}
