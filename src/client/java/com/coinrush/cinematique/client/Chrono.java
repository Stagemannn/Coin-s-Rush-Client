package com.coinrush.cinematique.client;

import com.coinrush.commun.CinematiquePayload;
import com.coinrush.commun.CinematiquePayload.Evenement;
import com.coinrush.commun.CinematiquePayload.Pose;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Ce que montre une cinématique à un instant donné (en ticks). Sans état : on peut interroger n'importe quel instant, dans n'importe quel ordre. */
final class Chrono {
	record Texte(String texte, float taille, int couleur, boolean bas, float alpha) { }

	private final List<Evenement> evenements; // triés par date, à date égale dans l'ordre reçu
	private final Chemin[] chemins;           // un par événement caméra
	private final int total;

	Chrono(CinematiquePayload p) {
		evenements = new ArrayList<>(p.evenements());
		evenements.sort(Comparator.comparingInt(Evenement::t));
		chemins = new Chemin[evenements.size()];
		for (int i = 0; i < chemins.length; i++) {
			Evenement e = evenements.get(i);
			if (e.type().equals(CinematiquePayload.CAMERA)) chemins[i] = new Chemin(e.poses(), e.lisse());
		}
		total = p.totalTicks();
	}

	int total() {
		return total;
	}

	/** Pose de la caméra détachée, ou null quand la vue est celle du joueur (avant la première caméra, ou après un « cameraoff »). */
	Pose camera(double tick) {
		for (int i = evenements.size() - 1; i >= 0; i--) {
			Evenement e = evenements.get(i);
			if (e.t() > tick) continue;
			if (e.type().equals(CinematiquePayload.CAMERA_OFF)) return null;
			if (e.type().equals(CinematiquePayload.CAMERA)) return chemins[i].en(e.duree() <= 0 ? 1 : (tick - e.t()) / e.duree());
		}
		return null;
	}

	/** Opacité du noir : le plus opaque des fondus en cours. */
	float fondu(double tick) {
		float max = 0;
		for (Evenement e : evenements) {
			if (e.t() > tick) break;
			if (e.type().equals(CinematiquePayload.FADE)) max = Math.max(max, alpha(e, tick));
		}
		return max;
	}

	/** Texte à l'écran : le plus récent encore visible. */
	Texte texte(double tick) {
		for (int i = evenements.size() - 1; i >= 0; i--) {
			Evenement e = evenements.get(i);
			if (e.t() > tick || !e.type().equals(CinematiquePayload.TEXT)) continue;
			float a = alpha(e, tick);
			return a > 0 ? new Texte(e.texte(), e.taille(), e.couleur(), e.bas(), a) : null;
		}
		return null;
	}

	/** Ouverture des bandes noires, de 0 (fermées) à 1 (ouvertes) : chaque événement « bandes » mène à son état en {@code duree} ticks, depuis l'état atteint à sa date. */
	float bandes(double tick) {
		float depart = 0, cible = 0;
		double t0 = 0, duree = 0;
		for (Evenement e : evenements) {
			if (!e.type().equals(CinematiquePayload.BANDES)) continue;
			if (e.t() > tick) break;
			depart = valeur(depart, cible, t0, duree, e.t());
			cible = e.actif() ? 1 : 0;
			t0 = e.t();
			duree = e.duree();
		}
		return valeur(depart, cible, t0, duree, tick);
	}

	private static float valeur(float depart, float cible, double t0, double duree, double tick) {
		if (duree <= 0 || tick >= t0 + duree) return cible;
		return depart + (cible - depart) * (float) ((tick - t0) / duree);
	}

	/** Une image à l'écran : empreinte du fichier, centre (parts de l'écran), largeur (part de l'écran), opacité. */
	record Image(String media, float x, float y, float largeur, float alpha) { }

	/** Images visibles, dans l'ordre : la dernière est dessinée par-dessus. */
	List<Image> images(double tick) {
		List<Image> l = new ArrayList<>();
		for (Evenement e : evenements) {
			if (e.t() > tick) break;
			if (!e.type().equals(CinematiquePayload.IMAGE)) continue;
			float a = alpha(e, tick);
			if (a > 0) l.add(new Image(e.media(), e.x(), e.y(), e.largeur(), a));
		}
		return l;
	}

	/** Sons importés, par date. */
	List<Evenement> audios() {
		return evenements.stream().filter(e -> e.type().equals(CinematiquePayload.AUDIO)).toList();
	}

	/** Courbe d'opacité d'un effet : entrée, maintien, sortie ; 0 avant et après. */
	static float alpha(Evenement e, double tick) {
		double ms = tick - e.t();
		if (ms < 0) return 0;
		if (ms < e.entree()) return (float) (ms / e.entree());
		if (ms < e.entree() + e.maintien()) return 1;
		if (ms < e.entree() + e.maintien() + e.sortie()) return (float) (1 - (ms - e.entree() - e.maintien()) / e.sortie());
		return 0;
	}
}
