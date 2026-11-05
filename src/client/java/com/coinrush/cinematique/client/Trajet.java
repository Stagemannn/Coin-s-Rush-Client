package com.coinrush.cinematique.client;

import com.coinrush.commun.CinematiquePayload.Plan;
import java.util.List;

/**
 * Trajet de la caméra : le plan i dure {@code dureeTicks} et mène la caméra du plan i au plan i+1 (adouci aux deux bouts) ;
 * le dernier plan est tenu sur sa durée. Les angles prennent le chemin le plus court et restent continus d'un plan à l'autre.
 */
final class Trajet {
	record Pose(double x, double y, double z, float yaw, float pitch) { }

	private final List<Plan> plans;
	private final float[] yaws;
	private final int total;

	Trajet(List<Plan> plans) {
		this.plans = plans;
		yaws = new float[plans.size()];
		int sum = 0;
		for (int i = 0; i < yaws.length; i++) {
			float wanted = plans.get(i).yaw();
			yaws[i] = i == 0 ? wanted : yaws[i - 1] + wrap(wanted - yaws[i - 1]);
			sum += plans.get(i).dureeTicks();
		}
		total = sum;
	}

	int total() {
		return total;
	}

	static float wrap(float degrees) {
		float d = degrees % 360;
		return d >= 180 ? d - 360 : d < -180 ? d + 360 : d;
	}

	/** Adouci : départ et arrivée sans à-coup. */
	static double ease(double t) {
		return t * t * (3 - 2 * t);
	}

	/** Numéro du plan en cours au tick donné (borné au dernier plan). */
	int index(int tick) {
		for (int i = 0; i < plans.size(); i++) {
			tick -= plans.get(i).dureeTicks();
			if (tick < 0) return i;
		}
		return plans.size() - 1;
	}

	/** Ticks écoulés dans le plan en cours. */
	int local(int tick) {
		for (Plan p : plans) {
			if (tick < p.dureeTicks()) return tick;
			tick -= p.dureeTicks();
		}
		return plans.getLast().dureeTicks();
	}

	Pose pose(int tick) {
		int i = index(tick);
		Plan a = plans.get(i);
		if (i == plans.size() - 1 || tick >= total) return new Pose(a.x(), a.y(), a.z(), yaws[i], a.pitch());
		Plan b = plans.get(i + 1);
		double e = ease((double) local(tick) / a.dureeTicks());
		return new Pose(a.x() + (b.x() - a.x()) * e, a.y() + (b.y() - a.y()) * e, a.z() + (b.z() - a.z()) * e,
				(float) (yaws[i] + (yaws[i + 1] - yaws[i]) * e), (float) (a.pitch() + (b.pitch() - a.pitch()) * e));
	}

	/** Opacité d'un texte de plan : fondu d'entrée et de sortie (au plus 10 ticks, au plus un tiers de la durée). */
	static float fade(int local, int duree) {
		float f = Math.max(1, Math.min(10, duree / 3f));
		return Math.max(0f, Math.min(1f, Math.min(local / f, (duree - local) / f)));
	}
}
