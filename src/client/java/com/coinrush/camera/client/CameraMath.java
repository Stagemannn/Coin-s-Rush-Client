package com.coinrush.camera.client;

/**
 * Calculs d'angle de la caméra libre, sans dépendance au jeu (testés à part).
 * Les angles sont en degrés, comme le jeu : le yaw augmente quand on tourne vers la droite, 0 regarde vers le sud.
 */
public final class CameraMath {
	/** Vitesse minimale appliquée : à 0 le personnage ne se tournerait jamais et la marche resterait bloquée dans sa direction. */
	public static final float MIN_SPEED = 5;

	private CameraMath() { }

	/** Ramène un angle dans [-180, 180). */
	public static float wrap(float degrees) {
		float d = degrees % 360;
		if (d >= 180) d -= 360;
		if (d < -180) d += 360;
		return d;
	}

	/** Échelle de 0 à 100 : part de l'écart rattrapée à chaque tick (100 = instantané), avec un minimum de {@link #MIN_SPEED}. */
	public static float fraction(float speed) {
		return Math.min(1f, Math.max(speed, MIN_SPEED) / 100f);
	}

	/** Un tick de rotation vers {@code target} par le plus court chemin ; le résultat est un angle équivalent, pas forcément wrappé. */
	public static float stepToward(float current, float target, float speed) {
		float diff = wrap(target - current);
		float step = diff * fraction(speed);
		return Math.abs(diff - step) < 0.05f ? current + diff : current + step;
	}

	/** Un tick d'inclinaison vers {@code target}, sans enroulement. */
	public static float stepPitch(float current, float target, float speed) {
		float diff = target - current;
		float step = diff * fraction(speed);
		return Math.abs(diff - step) < 0.05f ? target : current + step;
	}

	/** Direction du monde visée par les touches, par rapport à la caméra : avant 0°, droite +90°, arrière 180°, gauche -90°. */
	public static float moveYaw(float cameraYaw, float left, float forward) {
		return cameraYaw + (float) Math.toDegrees(Math.atan2(-left, forward));
	}

	public static float clampPitch(float pitch) {
		return Math.max(-90f, Math.min(90f, pitch));
	}
}
