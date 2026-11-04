package com.coinrush.commun;

import com.google.gson.Gson;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Cinématique envoyée par le serveur : chemin de caméra et textes. Transportée en JSON, puis validée ici :
 * un scénario invalide est refusé ({@link IllegalArgumentException}) et ne fait jamais planter le client.
 * Chaque point est atteint {@code ticks} après le précédent ; pour le premier, c'est le temps d'attente au départ.
 */
public record Scenario(String id, boolean skippable, List<Point> points, List<Text> texts) {
	/** Position de caméra. {@code ticks} : durée depuis le point précédent. */
	public record Point(double x, double y, double z, float yaw, float pitch, int ticks) { }

	/** Texte affiché de {@code from} (inclus) à {@code to} (exclu), en ticks depuis le début. */
	public record Text(int from, int to, String text) { }

	public static final int MAX_POINTS = 64, MAX_TEXTS = 64, MAX_TICKS = 6000, MAX_TEXT_LENGTH = 200;
	private static final double MAX_COORD = 30_000_000;
	private static final Pattern ID = Pattern.compile("[a-z0-9_./-]{1,64}");
	private static final Gson GSON = new Gson();

	public static Scenario parse(String json) {
		Scenario s;
		try {
			s = GSON.fromJson(json, Scenario.class);
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("JSON illisible");
		}
		if (s == null) throw new IllegalArgumentException("scénario vide");
		s.validate();
		return s;
	}

	public String toJson() {
		return GSON.toJson(this);
	}

	public void validate() {
		if (id == null || !ID.matcher(id).matches()) throw new IllegalArgumentException("id invalide");
		if (points == null || points.isEmpty() || points.size() > MAX_POINTS) throw new IllegalArgumentException("nombre de points invalide");
		long total = 0;
		for (Point p : points) {
			if (p == null || !finite(p)) throw new IllegalArgumentException("point invalide");
			if (Math.abs(p.x) > MAX_COORD || Math.abs(p.y) > MAX_COORD || Math.abs(p.z) > MAX_COORD) throw new IllegalArgumentException("coordonnée hors monde");
			if (Math.abs(p.pitch) > 90) throw new IllegalArgumentException("pitch hors limites");
			if (p.ticks < 0) throw new IllegalArgumentException("durée négative");
			total += p.ticks;
		}
		if (total < 1 || total > MAX_TICKS) throw new IllegalArgumentException("durée totale invalide");
		if (texts == null) return;
		if (texts.size() > MAX_TEXTS) throw new IllegalArgumentException("trop de textes");
		for (Text t : texts) {
			if (t == null || !Textes.valide(t.text, MAX_TEXT_LENGTH)) throw new IllegalArgumentException("texte invalide");
			if (t.from < 0 || t.to <= t.from || t.to > total) throw new IllegalArgumentException("fenêtre de texte invalide");
		}
	}

	private static boolean finite(Point p) {
		return Double.isFinite(p.x) && Double.isFinite(p.y) && Double.isFinite(p.z) && Float.isFinite(p.yaw) && Float.isFinite(p.pitch);
	}

	public int totalTicks() {
		return points.stream().mapToInt(Point::ticks).sum();
	}

	/** Caméra au tick donné : interpolation linéaire, le yaw prenant le plus court chemin. shortcut: pas de courbe lissée, à ajouter si les trajets paraissent saccadés. */
	public Point at(double tick) {
		Point prev = points.get(0);
		if (tick <= prev.ticks) return prev;
		double start = prev.ticks;
		for (int i = 1; i < points.size(); i++) {
			Point next = points.get(i);
			double end = start + next.ticks;
			if (tick < end) {
				double f = (tick - start) / next.ticks;
				double dyaw = ((next.yaw - prev.yaw + 180) % 360 + 360) % 360 - 180;
				return new Point(lerp(prev.x, next.x, f), lerp(prev.y, next.y, f), lerp(prev.z, next.z, f),
						(float) (prev.yaw + dyaw * f), (float) lerp(prev.pitch, next.pitch, f), 0);
			}
			start = end;
			prev = next;
		}
		return prev;
	}

	/** Textes visibles au tick donné. */
	public List<Text> textsAt(int tick) {
		return texts == null ? List.of() : texts.stream().filter(t -> t.from <= tick && tick < t.to).toList();
	}

	private static double lerp(double a, double b, double f) {
		return a + (b - a) * f;
	}
}
