package com.coinrush.carte.client;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Calculs purs de la carte (sans Minecraft) : niveau de tuile, saisie de coordonnées. */
public final class CarteMath {
	private static final Pattern COORDS = Pattern.compile("^\\s*(-?\\d{1,8})\\s*[ ,;]\\s*(-?\\d{1,8})\\s*$");
	public static final int LIMITE = 30_000_000;

	private CarteMath() { }

	/** Niveau de tuile à demander pour une échelle de {@code s} pixels par bloc : 0 net, 2 très éloigné. */
	public static int niveau(double s) {
		return s >= 1 ? 0 : s >= 0.5 ? 1 : 2;
	}

	/** Côté d'une tuile en blocs au niveau {@code k}. */
	public static int taille(int k) {
		return 256 << k;
	}

	/** @return {x, z} pour « 120 -45 », « 120,-45 » ou « 120;-45 » ; null si invalide ou hors du monde. */
	public static int[] coordonnees(String texte) {
		if (texte == null) return null;
		Matcher m = COORDS.matcher(texte);
		if (!m.matches()) return null;
		int x = Integer.parseInt(m.group(1)), z = Integer.parseInt(m.group(2));
		return Math.abs(x) <= LIMITE && Math.abs(z) <= LIMITE ? new int[] {x, z} : null;
	}
}
