package com.coinrush.carte.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;

/**
 * Encadré du profil : tête (avec la couche supérieure du skin), pseudo et Coins ; un clic déplie les stats (blocs posés et cassés,
 * mobs, morts, joueurs et boss tués, quêtes terminées). Utilisé par la carte (en haut à gauche) et par l'inventaire.
 */
public final class ProfilBox {
	private static final int ORANGE = 0xFFFFAA00, BLANC = 0xFFFFFFFF, GRIS = 0xFFB49A8E;
	private static final String[][] STATS = {{"poses", "Blocs posés"}, {"casses", "Blocs cassés"}, {"mobs", "Mobs tués"}, {"morts", "Morts"},
			{"joueurs", "Joueurs tués"}, {"boss", "Boss tués"}, {"quetes", "Quêtes terminées"}};

	private boolean ouvert;
	private int x, y, w, h; // cadre tel que dessiné à la dernière image, pour le clic

	/** Largeur minimale : assez pour les trois boutons de l'inventaire placés au-dessus. */
	public static final int LARGEUR_MIN = 152;

	private static String pseudo(ProfilClient.Profil p) {
		Minecraft mc = Minecraft.getInstance();
		return p != null ? p.pseudo() : mc.player != null ? mc.player.getGameProfile().getName() : "";
	}

	private static String coins(ProfilClient.Profil p) {
		return p != null ? p.coins() + " Coins" : "…";
	}

	/** Largeur de l'encadré, connue avant de le dessiner (pour aligner les boutons de l'inventaire dessus). */
	public static int largeur(Font font) {
		ProfilClient.Profil p = ProfilClient.actuel();
		return Math.max(LARGEUR_MIN, 30 + Math.max(font.width(pseudo(p)), font.width(coins(p))) + 8);
	}

	/** @param ancre bord gauche, ou bord droit si {@code aDroite} */
	public void render(GuiGraphics g, Font font, int ancre, int top, boolean aDroite) {
		Minecraft mc = Minecraft.getInstance();
		ProfilClient.Profil p = ProfilClient.actuel();
		String pseudo = pseudo(p), coins = coins(p);
		w = largeur(font);
		h = 26 + (ouvert ? 6 + 10 * STATS.length : 0);
		x = aDroite ? ancre - w : ancre;
		y = top;
		g.fill(x, y, x + w, y + h, 0xE62A1C18);
		g.renderOutline(x, y, w, h, 0xFF4A2F27);
		g.fill(x, y, x + w, y + 2, ORANGE);
		g.fill(x + 3, y + 4, x + 23, y + 24, ORANGE);
		if (mc.player != null) PlayerFaceRenderer.draw(g, mc.player.getSkin(), x + 4, y + 5, 18);
		g.drawString(font, pseudo, x + 28, y + 6, BLANC);
		g.drawString(font, coins, x + 28, y + 16, ORANGE);
		if (ouvert) {
			for (int i = 0; i < STATS.length; i++) {
				long v = p == null ? 0 : p.stats().getOrDefault(STATS[i][0], 0L);
				int ly = y + 30 + 10 * i;
				g.drawString(font, STATS[i][1], x + 6, ly, GRIS);
				String val = String.valueOf(v);
				g.drawString(font, val, x + w - 6 - font.width(val), ly, ORANGE);
			}
		}
	}

	public boolean contient(double mx, double my) {
		return w > 0 && mx >= x && mx < x + w && my >= y && my < y + h;
	}

	/** Un clic gauche dans l'encadré le déplie ou le replie. @return vrai si le clic lui était destiné */
	public boolean clic(double mx, double my, int bouton) {
		if (bouton != 0 || !contient(mx, my)) return false;
		ouvert = !ouvert;
		return true;
	}
}
