package com.coinrush.cinematique.client;

import com.coinrush.commun.CinematiquePayload.Pose;
import java.util.ArrayList;
import java.util.List;

/**
 * Trajet de la caméra par des poses, à vitesse constante. Une pose : caméra fixe ; deux poses, ou {@code lisse} faux : segments droits ;
 * sinon courbe de Catmull-Rom qui passe par chaque pose. Les angles prennent le plus court chemin et restent continus d'une pose à l'autre.
 */
final class Chemin {
	private static final int PAS = 24; // échantillons par segment lissé
	private final double[][] points; // x, y, z, yaw, pitch
	private final double[] cumul;    // « distance » cumulée : blocs + un peu des degrés, pour qu'une rotation sur place avance aussi

	Chemin(List<Pose> poses, boolean lisse) {
		int n = poses.size();
		double[][] c = new double[n][5];
		for (int i = 0; i < n; i++) {
			Pose p = poses.get(i);
			double yaw = p.yaw();
			if (i > 0) yaw = c[i - 1][3] + wrap(yaw - c[i - 1][3]);
			c[i] = new double[] {p.x(), p.y(), p.z(), yaw, p.pitch()};
		}
		List<double[]> s = new ArrayList<>();
		if (!lisse || n < 3) {
			s.addAll(List.of(c));
		} else {
			for (int i = 0; i < n - 1; i++) {
				double[] p0 = c[Math.max(0, i - 1)], p1 = c[i], p2 = c[i + 1], p3 = c[Math.min(n - 1, i + 2)];
				for (int k = 0; k < PAS; k++) s.add(catmull(p0, p1, p2, p3, (double) k / PAS));
			}
			s.add(c[n - 1]);
		}
		points = s.toArray(new double[0][]);
		cumul = new double[points.length];
		for (int i = 1; i < points.length; i++) cumul[i] = cumul[i - 1] + cout(points[i - 1], points[i]);
	}

	static float wrap(double degrees) {
		double d = degrees % 360;
		return (float) (d >= 180 ? d - 360 : d < -180 ? d + 360 : d);
	}

	private static double cout(double[] a, double[] b) {
		return Math.sqrt((b[0] - a[0]) * (b[0] - a[0]) + (b[1] - a[1]) * (b[1] - a[1]) + (b[2] - a[2]) * (b[2] - a[2])) + 0.01 * (Math.abs(b[3] - a[3]) + Math.abs(b[4] - a[4])) + 1e-9;
	}

	private static double[] catmull(double[] p0, double[] p1, double[] p2, double[] p3, double t) {
		double[] r = new double[5];
		double t2 = t * t, t3 = t2 * t;
		for (int j = 0; j < 5; j++) r[j] = 0.5 * (2 * p1[j] + (p2[j] - p0[j]) * t + (2 * p0[j] - 5 * p1[j] + 4 * p2[j] - p3[j]) * t2 + (3 * p1[j] - p0[j] - 3 * p2[j] + p3[j]) * t3);
		r[4] = Math.max(-90, Math.min(90, r[4]));
		return r;
	}

	/** Pose à la fraction {@code f} du trajet (bornée à [0, 1]). */
	Pose en(double f) {
		if (points.length == 1) return pose(points[0]);
		double cible = Math.max(0, Math.min(1, f)) * cumul[cumul.length - 1];
		int i = 1;
		while (i < points.length - 1 && cumul[i] < cible) i++;
		double[] a = points[i - 1], b = points[i];
		double u = (cible - cumul[i - 1]) / (cumul[i] - cumul[i - 1]);
		double[] r = new double[5];
		for (int j = 0; j < 5; j++) r[j] = a[j] + (b[j] - a[j]) * u;
		return pose(r);
	}

	private static Pose pose(double[] v) {
		return new Pose(v[0], v[1], v[2], (float) v[3], (float) v[4]);
	}
}
