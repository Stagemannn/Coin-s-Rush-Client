package com.coinrush.commun;

import com.google.gson.Gson;
import java.util.List;

/**
 * Contenu du panneau d'affichage envoyé par le serveur, sans les Coins (déjà synchronisés à part).
 * Une section vide n'est pas envoyée. Transporté en JSON puis validé ici : une donnée invalide est refusée.
 */
public record Panel(List<Entry> entries) {
	public enum Kind { QUETE, FARM, EVENEMENT, GM }

	/** {@code progress} : 0 à 100, absent si sans objet. {@code seconds} : temps restant à la réception, absent si sans objet.
	 * {@code label} : nom de la catégorie, sinon celui du {@code kind}. {@code done} : coche à côté du titre. {@code description} : vue détaillée.
	 * Les textes peuvent porter des codes couleur en §. */
	public record Entry(Kind kind, String title, String detail, Integer progress, Integer seconds, String label, boolean done, String description) {
		public Entry(Kind kind, String title, String detail, Integer progress, Integer seconds) {
			this(kind, title, detail, progress, seconds, null, false, null);
		}
	}

	public static final Panel EMPTY = new Panel(List.of());
	public static final int MAX_ENTRIES = 8, MAX_TITLE = 96, MAX_DETAIL = 160, MAX_LABEL = 48, MAX_DESCRIPTION = 400, MAX_SECONDS = 31 * 86400;
	private static final Gson GSON = new Gson();

	public static Panel parse(String json) {
		Panel p;
		try {
			p = GSON.fromJson(json, Panel.class);
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("JSON illisible");
		}
		if (p == null) throw new IllegalArgumentException("panneau vide");
		p.validate();
		return p;
	}

	public String toJson() {
		return GSON.toJson(this);
	}

	public void validate() {
		if (entries == null || entries.size() > MAX_ENTRIES) throw new IllegalArgumentException("nombre de sections invalide");
		for (Entry e : entries) {
			if (e == null || e.kind == null) throw new IllegalArgumentException("section invalide");
			if (!Textes.valide(e.title, MAX_TITLE)) throw new IllegalArgumentException("titre invalide");
			if (e.detail != null && !e.detail.isEmpty() && !Textes.valide(e.detail, MAX_DETAIL)) throw new IllegalArgumentException("détail invalide");
			if (e.label != null && !e.label.isEmpty() && !Textes.valide(e.label, MAX_LABEL)) throw new IllegalArgumentException("nom de catégorie invalide");
			if (e.description != null && !e.description.isEmpty() && !Textes.valide(e.description, MAX_DESCRIPTION)) throw new IllegalArgumentException("description invalide");
			if (e.progress != null && (e.progress < 0 || e.progress > 100)) throw new IllegalArgumentException("progression invalide");
			if (e.seconds != null && (e.seconds < 0 || e.seconds > MAX_SECONDS)) throw new IllegalArgumentException("temps invalide");
		}
	}
}
