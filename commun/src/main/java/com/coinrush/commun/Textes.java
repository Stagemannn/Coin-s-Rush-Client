package com.coinrush.commun;

/** Contrôles de texte partagés par les données reçues du serveur. */
final class Textes {
	private Textes() { }

	static boolean valide(String s, int max) {
		return s != null && !s.isBlank() && s.length() <= max && s.chars().noneMatch(Character::isISOControl);
	}
}
