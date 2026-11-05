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
	/** Faux : caméra 3e personne vanilla (le décalage latéral reste réglable à part). */
	public static volatile boolean enabled = true;
	/** Décalage latéral de la caméra en 3e personne (vue de dos), en blocs : de -2 (à gauche) à +2 (à droite), 0 = vanilla. */
	public static volatile double cameraOffset;
	/** Le personnage s'aligne sur la caméra quand on clique (attaque, utilisation), pendant {@link #alignTicks} ticks après le clic. */
	public static volatile boolean alignOnClick = true;
	public static volatile int alignTicks = 40;
	/** Angle (degrés) dans lequel un personnage immobile suit légèrement la caméra. */
	public static volatile int idleAngle = 45;
	/** Caméra libre aussi en vol en élytres. */
	public static volatile boolean elytra;
	/** La vue de face est retirée du cycle de F5. */
	public static volatile boolean noFront = true;
	/** Vitesses de 0 à 100 : part de l'écart rattrapée à chaque tick (voir {@link CameraMath#fraction}). */
	public static volatile int rotationSpeed = 50;
	public static volatile int pitchSpeed = 65;

	private CameraOptions() { }

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("coinrush.json");
	}

	private static int clamp(JsonObject o, String key, int min, int max, int fallback) {
		return o.has(key) ? Math.max(min, Math.min(max, o.get(key).getAsInt())) : fallback;
	}

	private static boolean bool(JsonObject o, String key, boolean fallback) {
		return o.has(key) ? o.get(key).getAsBoolean() : fallback;
	}

	public static void load() {
		try {
			if (!Files.exists(file())) return;
			JsonObject o = new Gson().fromJson(Files.readString(file(), StandardCharsets.UTF_8), JsonObject.class);
			if (o == null) return;
			if (o.has("cameraOffset")) cameraOffset = Math.max(-2, Math.min(2, o.get("cameraOffset").getAsDouble()));
			enabled = bool(o, "freeCamera", enabled);
			alignOnClick = bool(o, "alignOnClick", alignOnClick);
			alignTicks = clamp(o, "alignTicks", 0, 100, alignTicks);
			idleAngle = clamp(o, "idleAngle", 0, 90, idleAngle);
			elytra = bool(o, "freeCameraElytra", elytra);
			noFront = bool(o, "noFrontView", noFront);
			rotationSpeed = clamp(o, "rotationSpeed", 0, 100, rotationSpeed);
			pitchSpeed = clamp(o, "pitchSpeed", 0, 100, pitchSpeed);
		} catch (IOException | RuntimeException e) {
			// fichier illisible : valeurs par défaut
		}
	}

	public static void save() {
		JsonObject o = new JsonObject();
		o.addProperty("cameraOffset", cameraOffset);
		o.addProperty("freeCamera", enabled);
		o.addProperty("alignOnClick", alignOnClick);
		o.addProperty("alignTicks", alignTicks);
		o.addProperty("idleAngle", idleAngle);
		o.addProperty("freeCameraElytra", elytra);
		o.addProperty("noFrontView", noFront);
		o.addProperty("rotationSpeed", rotationSpeed);
		o.addProperty("pitchSpeed", pitchSpeed);
		try {
			Files.writeString(file(), new Gson().toJson(o), StandardCharsets.UTF_8);
		} catch (IOException e) {
			// non enregistré : valable jusqu'à la fermeture du jeu
		}
	}
}
