package com.coinrush.carte.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Carte du monde en jeu, ouverte depuis l'inventaire. Mêmes données que le site du panel joueurs (lecture seule) : tuiles mises à
 * jour par le serveur, soi-même et son équipe avec leur tête, Ta Chambre, Ton Terrain, terrains à vendre, PNJ, points nommés.
 * Glisser pour déplacer, molette pour zoomer, ◎ pour suivre son personnage, case en bas à droite pour aller à des coordonnées.
 */
final class CarteScreen extends Screen {
	private static final int ORANGE = 0xFFFFAA00, ROUGE = 0xFFFF7777, BLANC = 0xFFFFFFFF, GRIS = 0xFFB49A8E, FOND = 0xFF120B09;
	private static final String[][] STATS = {{"poses", "Blocs posés"}, {"casses", "Blocs cassés"}, {"mobs", "Mobs tués"}, {"morts", "Morts"},
			{"joueurs", "Joueurs tués"}, {"boss", "Boss tués"}, {"quetes", "Quêtes terminées"}};
	private static int compteur;

	private final Screen parent;
	private final CarteDonnees d;
	private double vx, vz, s = 1;
	private boolean suivre = true, statsOuvertes, premiere = true;
	private double[] lisse;
	private final Map<UUID, double[]> lissesEquipe = new HashMap<>();
	private long dernierImage = System.nanoTime(), dernierePurge, derniereRevalidation;
	private EditBox aller;
	private int rx, ry, rw, rh; // cadre du profil tel que dessiné, pour le clic

	CarteScreen(Screen parent, String url) {
		super(Component.literal("Carte"));
		this.parent = parent;
		this.d = new CarteDonnees(url);
	}

	@Override
	protected void init() {
		int w = width, h = height;
		addRenderableWidget(Button.builder(Component.literal("+"), b -> zoom(1.5, w / 2.0, h / 2.0)).bounds(w - 26, 6, 20, 20).build());
		addRenderableWidget(Button.builder(Component.literal("-"), b -> zoom(1 / 1.5, w / 2.0, h / 2.0)).bounds(w - 26, 30, 20, 20).build());
		addRenderableWidget(Button.builder(Component.literal("◎"), b -> suivre = true).bounds(w - 26, 54, 20, 20).build());
		String ancien = aller == null ? "" : aller.getValue();
		aller = new EditBox(font, w - 128, h - 24, 88, 18, Component.literal("Aller aux coordonnées"));
		aller.setHint(Component.literal("X Z"));
		aller.setMaxLength(24);
		aller.setValue(ancien);
		addRenderableWidget(aller);
		addRenderableWidget(Button.builder(Component.literal("Aller"), b -> allerA()).bounds(w - 36, h - 24, 30, 18).build());
		addRenderableWidget(Button.builder(Component.literal("Fermer"), b -> onClose()).bounds(6, h - 24, 50, 18).build());
	}

	private void allerA() {
		int[] c = CarteMath.coordonnees(aller.getValue());
		aller.setTextColor(c == null ? 0xFFFF5555 : 0xFFE0E0E0); // entrée invalide : refusée, texte en rouge
		if (c == null) return;
		suivre = false;
		vx = c[0] + 0.5;
		vz = c[1] + 0.5;
	}

	private void zoom(double f, double sx, double sz) {
		double wx = vx + (sx - width / 2.0) / s, wz = vz + (sz - height / 2.0) / s;
		s = Math.min(8, Math.max(0.25, s * f));
		if (!suivre) { // le point sous le curseur reste en place
			vx = wx - (sx - width / 2.0) / s;
			vz = wz - (sz - height / 2.0) / s;
		}
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	@Override
	public void removed() {
		d.close();
		for (CarteDonnees.Tuile t : d.tuiles.values()) if (t.texture != null) minecraft.getTextureManager().release((ResourceLocation) t.texture);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// ---- Entrées ----

	@Override
	public boolean mouseClicked(double x, double y, int bouton) {
		if (super.mouseClicked(x, y, bouton)) return true;
		if (bouton == 0 && rw > 0 && x >= rx && x < rx + rw && y >= ry && y < ry + rh) {
			statsOuvertes = !statsOuvertes;
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double x, double y, int bouton, double dx, double dy) {
		if (super.mouseDragged(x, y, bouton, dx, dy)) return true;
		if (bouton != 0) return false;
		suivre = false;
		vx -= dx / s;
		vz -= dy / s;
		return true;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double sx, double sy) {
		if (super.mouseScrolled(x, y, sx, sy)) return true;
		zoom(sy > 0 ? 1.2 : 1 / 1.2, x, y);
		return true;
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		if ((key == 257 || key == 335) && aller.isFocused()) { // Entrée
			allerA();
			return true;
		}
		return super.keyPressed(key, scan, mods);
	}

	// ---- Dessin ----

	private double sx(double wx) { return width / 2.0 + (wx - vx) * s; }

	private double sz(double wz) { return height / 2.0 + (wz - vz) * s; }

	private static void lisser(double[] a, double[] cible, double f) {
		a[0] += (cible[0] - a[0]) * f;
		a[1] += (cible[1] - a[1]) * f;
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		long maintenant = System.nanoTime();
		double f = 1 - Math.exp(-(maintenant - dernierImage) / 1e9 * 8); // glisse vers la dernière position reçue
		dernierImage = maintenant;
		double[] moi = d.moi;
		if (moi == null) lisse = null;
		else if (lisse == null) lisse = moi.clone();
		else lisser(lisse, moi, f);
		Set<UUID> presents = new HashSet<>();
		for (CarteDonnees.Equipier e : d.equipe) {
			presents.add(e.id());
			double[] c = {e.x(), e.z()};
			double[] l = lissesEquipe.get(e.id());
			if (l == null) lissesEquipe.put(e.id(), c);
			else lisser(l, c, f);
		}
		lissesEquipe.keySet().retainAll(presents);
		if (lisse != null && premiere) { vx = lisse[0]; vz = lisse[1]; premiere = false; }
		if (suivre && lisse != null) { vx = lisse[0]; vz = lisse[1]; }

		g.fill(0, 0, width, height, FOND);
		tuiles(g);
		boites(g);
		points(g);
		tetes(g);
		profil(g);
		infobulle(g, mx, my);
		int[] c = {(int) Math.floor(vx + (mx - width / 2.0) / s), (int) Math.floor(vz + (my - height / 2.0) / s)};
		g.drawString(font, "X " + c[0] + "  Z " + c[1], width - 128 - font.width("X " + c[0] + "  Z " + c[1]) - 6, height - 19, ORANGE);
		if (d.erreur != null) g.drawString(font, Component.literal("Panel injoignable : " + d.erreur), 62, height - 19, ROUGE);
		super.render(g, mx, my, partial);
	}

	private void tuiles(GuiGraphics g) {
		int k = CarteMath.niveau(s), taille = CarteMath.taille(k);
		int x0 = (int) Math.floor((vx - width / 2.0 / s) / taille), x1 = (int) Math.floor((vx + width / 2.0 / s) / taille);
		int z0 = (int) Math.floor((vz - height / 2.0 / s) / taille), z1 = (int) Math.floor((vz + height / 2.0 / s) / taille);
		boolean revalider = System.currentTimeMillis() - derniereRevalidation > 5000;
		if (revalider) derniereRevalidation = System.currentTimeMillis();
		Set<String> visibles = new HashSet<>();
		for (int tx = x0; tx <= x1; tx++) for (int tz = z0; tz <= z1; tz++) {
			String cle = k + "/" + tx + "/" + tz;
			visibles.add(cle);
			d.tuile(k, tx, tz, revalider);
			CarteDonnees.Tuile t = d.tuiles.get(cle);
			ResourceLocation id = t == null ? null : texture(t);
			if (id == null) continue;
			int ax = (int) Math.round(sx(tx * (double) taille)), az = (int) Math.round(sz(tz * (double) taille));
			int bx = (int) Math.round(sx((tx + 1) * (double) taille)), bz = (int) Math.round(sz((tz + 1) * (double) taille));
			g.blit(RenderPipelines.GUI_TEXTURED, id, ax, az, 0, 0, bx - ax, bz - az, 256, 256, 256, 256);
		}
		if (System.currentTimeMillis() - dernierePurge > 3000) {
			dernierePurge = System.currentTimeMillis();
			purger(visibles);
		}
	}

	/** Transforme les octets reçus en texture (thread de rendu seulement). */
	private ResourceLocation texture(CarteDonnees.Tuile t) {
		byte[] png = t.png;
		if (png != null) {
			t.png = null;
			try {
				NativeImage img = NativeImage.read(png);
				ResourceLocation id = (ResourceLocation) t.texture;
				if (id == null) id = ResourceLocation.fromNamespaceAndPath("coinrush", "carte/" + (compteur++));
				else minecraft.getTextureManager().release(id);
				minecraft.getTextureManager().register(id, new DynamicTexture(() -> "coinrush carte", img));
				t.texture = id;
			} catch (IOException | RuntimeException e) {
				t.etag = null; // illisible : sera redemandée
			}
		}
		return (ResourceLocation) t.texture;
	}

	/** Libère les textures des tuiles loin de la vue, pour garder la mémoire bornée. */
	private void purger(Set<String> visibles) {
		if (d.tuiles.size() <= 60) return;
		d.tuiles.entrySet().removeIf(e -> {
			if (visibles.contains(e.getKey())) return false;
			if (e.getValue().texture != null) minecraft.getTextureManager().release((ResourceLocation) e.getValue().texture);
			return true;
		});
	}

	private static int argb(int rgb, int alpha) { return alpha << 24 | rgb; }

	private static int couleurBoite(String type) {
		return switch (type) {
			case "chambre" -> 0xFFAA00;
			case "terrain" -> 0xFFCC55;
			default -> 0x66DD88; // terrain libre
		};
	}

	private void boites(GuiGraphics g) {
		for (CarteDonnees.Boite b : d.boites) {
			int x1 = (int) Math.round(sx(b.x1())), z1 = (int) Math.round(sz(b.z1())), x2 = (int) Math.round(sx(b.x2())), z2 = (int) Math.round(sz(b.z2()));
			if (x2 < 0 || z2 < 0 || x1 > width || z1 > height) continue;
			int c = couleurBoite(b.type());
			g.fill(x1, z1, x2, z2, argb(c, 0x40));
			g.renderOutline(x1, z1, x2 - x1, z2 - z1, argb(c, 0xFF));
			if (x2 - x1 > font.width(b.nom()) + 8) g.drawString(font, b.nom(), x1 + 3, z1 + 3, argb(c, 0xFF));
		}
	}

	private void points(GuiGraphics g) {
		for (CarteDonnees.Point p : d.points) marque(g, p, false);
		for (CarteDonnees.Point p : d.pnj) marque(g, p, true);
	}

	private void marque(GuiGraphics g, CarteDonnees.Point p, boolean losange) {
		int x = (int) Math.round(sx(p.x())), z = (int) Math.round(sz(p.z()));
		if (x < -80 || z < -10 || x > width + 10 || z > height + 10) return;
		int c = argb(losange ? 0xFFEE55 : p.couleur(), 0xFF);
		if (losange) {
			g.fill(x - 1, z - 5, x + 2, z + 6, c);
			g.fill(x - 3, z - 3, x + 4, z + 4, c);
			g.fill(x - 5, z - 1, x + 6, z + 2, c);
		} else {
			g.fill(x - 3, z - 3, x + 4, z + 4, 0xFF000000);
			g.fill(x - 2, z - 2, x + 3, z + 3, c);
		}
		g.drawString(font, p.nom(), x + 8, z - 4, c);
	}

	private PlayerSkin skin(UUID id) {
		if (minecraft.player != null && minecraft.player.getUUID().equals(id)) return minecraft.player.getSkin();
		PlayerInfo info = minecraft.getConnection() == null ? null : minecraft.getConnection().getPlayerInfo(id);
		return info == null ? null : info.getSkin();
	}

	/** Têtes avec la couche supérieure du skin : la nôtre et celles de nos coéquipiers, jamais d'autres joueurs. */
	private void tetes(GuiGraphics g) {
		for (CarteDonnees.Equipier e : d.equipe) {
			double[] l = lissesEquipe.get(e.id());
			if (l != null) tete(g, skin(e.id()), l[0], l[1], e.nom());
		}
		if (lisse != null && minecraft.player != null) tete(g, minecraft.player.getSkin(), lisse[0], lisse[1], null);
	}

	private void tete(GuiGraphics g, PlayerSkin skin, double wx, double wz, String nom) {
		int x = (int) Math.round(sx(wx)), z = (int) Math.round(sz(wz));
		g.fill(x - 9, z - 9, x + 9, z + 9, 0xFF000000);
		if (skin != null) PlayerFaceRenderer.draw(g, skin, x - 8, z - 8, 16);
		if (nom != null) g.drawString(font, nom, x - font.width(nom) / 2, z + 11, BLANC);
	}

	private void profil(GuiGraphics g) {
		CarteDonnees.Profil p = d.profil;
		String pseudo = p != null ? p.pseudo() : minecraft.player != null ? minecraft.player.getGameProfile().getName() : "";
		String coins = p != null ? p.coins() + " Coins" : "…";
		rx = 6;
		ry = 6;
		rw = Math.max(110, 30 + Math.max(font.width(pseudo), font.width(coins)) + 8);
		rh = 26 + (statsOuvertes ? 6 + 10 * STATS.length : 0);
		g.fill(rx, ry, rx + rw, ry + rh, 0xE62A1C18);
		g.renderOutline(rx, ry, rw, rh, 0xFF4A2F27);
		g.fill(rx, ry, rx + rw, ry + 2, ORANGE);
		g.fill(rx + 3, ry + 4, rx + 23, ry + 24, ORANGE);
		if (minecraft.player != null) PlayerFaceRenderer.draw(g, minecraft.player.getSkin(), rx + 4, ry + 5, 18);
		g.drawString(font, pseudo, rx + 28, ry + 6, BLANC);
		g.drawString(font, coins, rx + 28, ry + 16, ORANGE);
		if (statsOuvertes) {
			for (int i = 0; i < STATS.length; i++) {
				long v = p == null ? 0 : p.stats().getOrDefault(STATS[i][0], 0L);
				int y = ry + 30 + 10 * i;
				g.drawString(font, STATS[i][1], rx + 6, y, GRIS);
				String val = String.valueOf(v);
				g.drawString(font, val, rx + rw - 6 - font.width(val), y, ORANGE);
			}
		}
	}

	private void infobulle(GuiGraphics g, int mx, int my) {
		if (rw > 0 && mx >= rx && mx < rx + rw && my >= ry && my < ry + rh) return;
		double wx = vx + (mx - width / 2.0) / s, wz = vz + (my - height / 2.0) / s, r = 7 / s;
		Component texte = null;
		for (CarteDonnees.Point p : d.points) if (Math.abs(p.x() - wx) < r && Math.abs(p.z() - wz) < r) texte = p.nom();
		for (CarteDonnees.Point p : d.pnj) if (Math.abs(p.x() - wx) < r && Math.abs(p.z() - wz) < r) texte = p.nom();
		if (texte == null) for (CarteDonnees.Boite b : d.boites) {
			if (wx >= b.x1() && wx < b.x2() && wz >= b.z1() && wz < b.z2()) texte = Component.literal(b.prix() != null ? b.nom() + " : " + b.prix() : b.nom());
		}
		if (texte != null) g.setTooltipForNextFrame(font, texte, mx, my);
	}
}
