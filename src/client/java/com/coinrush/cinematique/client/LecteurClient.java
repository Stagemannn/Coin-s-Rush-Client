package com.coinrush.cinematique.client;

import com.coinrush.commun.CinematiqueFiniePayload;
import com.coinrush.commun.CinematiqueMediaMorceauPayload;
import com.coinrush.commun.CinematiqueMediasDemandePayload;
import com.coinrush.commun.CinematiqueMediasPayload;
import com.coinrush.commun.CinematiquePayload;
import com.coinrush.commun.CinematiquePayload.Pose;
import com.coinrush.commun.CinematiqueStopPayload;
import com.coinrush.commun.Commun;
import com.coinrush.panneau.client.PanneauClient;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lecteur de cinématiques en événements datés (voir {@link CinematiquePayload}) : caméra détachée du joueur qui suit un chemin lissé, bandes
 * noires, fondus au noir et textes, HUD masqué et joueur bloqué par un écran tant que la caméra est détachée. Après un « cameraoff » le joueur
 * reprend la main (le serveur peut encore l'immobiliser) et fondus et textes continuent par-dessus le jeu. Échap ouvre le menu (pas de « passer »).
 * À la fin, retour à la vue normale puis signal {@link CinematiqueFiniePayload} ; le serveur peut aussi arrêter la lecture.
 * Le client ne décide de rien : la validation borne la durée.
 */
public final class LecteurClient implements ClientModInitializer {
	/** Côté maximal d'une image importée, en pixels (le serveur borne déjà le poids du fichier). */
	private static final int MAX_COTE_IMAGE = 2048;
	private static final Logger LOGGER = LoggerFactory.getLogger("coinrush");
	private static Lecture current;
	/** Images et sons importés par les cinématiques, gardés d'une partie à l'autre dans {@code config/coinrush/medias}. */
	private static final Medias MEDIAS = new Medias(FabricLoader.getInstance().getConfigDir().resolve("coinrush").resolve("medias"));

	@Override
	public void onInitializeClient() {
		Commun.init();
		ClientPlayNetworking.registerGlobalReceiver(CinematiquePayload.TYPE, (payload, context) -> start(payload));
		ClientPlayNetworking.registerGlobalReceiver(CinematiqueStopPayload.TYPE, (payload, context) -> stop());
		ClientPlayNetworking.registerGlobalReceiver(CinematiqueMediasPayload.TYPE, (payload, context) -> manifeste(payload));
		ClientPlayNetworking.registerGlobalReceiver(CinematiqueMediaMorceauPayload.TYPE, (payload, context) -> MEDIAS.morceau(payload));
		ClientTickEvents.END_CLIENT_TICK.register(c -> {
			if (current != null) current.tick();
		});
		// Caméra rendue au joueur : fondus et textes passent par le HUD ; caméra détachée : par l'écran de lecture (et le menu Échap).
		HudElementRegistry.addLast(ResourceLocation.fromNamespaceAndPath("coinrush", "cinematique"), (g, delta) -> {
			if (current != null && !current.detachee) current.draw(g, g.guiWidth(), g.guiHeight(), delta.getGameTimeDeltaPartialTick(false));
		});
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof PauseScreen) ScreenEvents.afterRender(screen).register((s, g, mx, my, delta) -> {
				if (current != null && current.detachee) current.draw(g, s.width, s.height, delta);
			});
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(LecteurClient::stop)); // thread réseau
	}

	/** Liste des fichiers d'une cinématique : demande au serveur ceux qui manquent (liste vide : tout est déjà là). */
	private static void manifeste(CinematiqueMediasPayload p) {
		try {
			List<String> manquants = MEDIAS.manifeste(p.medias());
			if (ClientPlayNetworking.canSend(CinematiqueMediasDemandePayload.TYPE)) ClientPlayNetworking.send(new CinematiqueMediasDemandePayload(p.id(), manquants));
		} catch (IllegalArgumentException e) {
			LOGGER.warn("Liste de fichiers de cinématique refusée : {}", e.getMessage());
		}
	}

	static void start(CinematiquePayload payload) {
		try {
			payload.validate();
		} catch (IllegalArgumentException e) {
			LOGGER.warn("Cinématique refusée : {}", e.getMessage());
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;
		CinematiqueClient.stop(); // l'ancien lecteur (scénario JSON) cède la place
		stop();
		current = new Lecture(mc, payload);
	}

	/** Arrêt sans signal : ordre du serveur, déconnexion ou remplacement. */
	public static void stop() {
		Lecture l = current;
		current = null;
		if (l != null) l.end(false);
	}

	private static final class Lecture {
		private final Minecraft mc;
		private final CinematiquePayload data;
		private final Chrono chrono;
		private final Entity camera;
		private final Entity previousCamera;
		private final boolean hideGui;
		private final CameraType cameraType;
		private final Screen screen;
		private int tick = -1;
		/** Vrai tant que la caméra est détachée du joueur. */
		boolean detachee;
		private final List<CinematiquePayload.Evenement> audios;
		private int prochainAudio;
		private final List<SonImporte> sons = new ArrayList<>();
		/** Textures des images, par empreinte ; null = fichier illisible (signalé une fois). */
		private final Map<String, Image> images = new HashMap<>();

		private record Image(net.minecraft.resources.ResourceLocation id, int largeur, int hauteur) { }

		Lecture(Minecraft mc, CinematiquePayload data) {
			this.mc = mc;
			this.data = data;
			this.chrono = new Chrono(data);
			this.audios = chrono.audios();
			this.previousCamera = mc.getCameraEntity();
			this.hideGui = mc.options.hideGui;
			this.cameraType = mc.options.getCameraType();
			this.camera = new Marker(EntityType.MARKER, mc.level);
			screen = new LecteurScreen(this);
			tick();
		}

		void tick() {
			if (mc.player == null || mc.level != camera.level()) {
				stop();
				return;
			}
			if (++tick >= chrono.total()) {
				finish();
				return;
			}
			jouerSons();
			Pose p = chrono.camera(tick);
			if (p == null) {
				if (detachee) detacher();
				return;
			}
			if (!detachee) attacher(p);
			else if (mc.screen == null) mc.setScreen(screen); // retour du menu Échap
			else if (mc.screen != screen && !(mc.screen instanceof PauseScreen)) { // un autre écran a pris la place : on rend la main
				finish();
				return;
			}
			camera.setOldPosAndRot();
			camera.setPos(p.x(), p.y(), p.z());
			camera.setYRot(p.yaw());
			camera.setXRot(p.pitch());
		}

		/** Lance les sons importés arrivés à leur date, et libère ceux qui ont fini. */
		private void jouerSons() {
			sons.removeIf(s -> {
				boolean fini = s.fini();
				if (fini) s.arreter();
				return fini;
			});
			while (prochainAudio < audios.size() && audios.get(prochainAudio).t() <= tick) {
				CinematiquePayload.Evenement e = audios.get(prochainAudio++);
				try {
					float gain = e.volume() * mc.options.getSoundSourceVolume(SoundSource.MASTER);
					sons.add(SonImporte.jouer(MEDIAS.chemin(e.media(), CinematiquePayload.OGG), gain, e.hauteur()));
				} catch (IOException | RuntimeException x) {
					LOGGER.warn("Son de cinématique {} ignoré : {}", data.id(), x.toString());
				}
			}
		}

		/** Texture d'une image du cache, créée à la première demande ; null si le fichier manque ou est illisible. */
		private Image image(String sha) {
			if (images.containsKey(sha)) return images.get(sha);
			Image r = null;
			try (InputStream in = Files.newInputStream(MEDIAS.chemin(sha, CinematiquePayload.PNG))) {
				NativeImage px = NativeImage.read(in);
				if (px.getWidth() > MAX_COTE_IMAGE || px.getHeight() > MAX_COTE_IMAGE) {
					px.close();
					throw new IOException("image trop grande");
				}
				net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("coinrush", "cinematique/" + sha);
				mc.getTextureManager().register(id, new DynamicTexture(() -> "coinrush cinematique " + sha, px));
				r = new Image(id, px.getWidth(), px.getHeight());
			} catch (IOException | RuntimeException x) {
				LOGGER.warn("Image de cinématique {} ignorée : {}", data.id(), x.toString());
			}
			images.put(sha, r);
			return r;
		}

		private void attacher(Pose p) {
			detachee = true;
			camera.snapTo(p.x(), p.y(), p.z(), p.yaw(), p.pitch());
			mc.options.hideGui = true;
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			mc.setCameraEntity(camera);
			mc.setScreen(screen);
		}

		private void detacher() {
			detachee = false;
			mc.options.hideGui = hideGui;
			mc.options.setCameraType(cameraType);
			if (mc.getCameraEntity() == camera) mc.setCameraEntity(mc.player != null ? mc.player : previousCamera);
			if (mc.screen == screen || mc.screen instanceof PauseScreen) mc.setScreen(null);
		}

		/** Fin normale : vue rendue, puis signal au serveur. */
		private void finish() {
			current = null;
			end(true);
		}

		void end(boolean notify) {
			if (detachee) detacher();
			sons.forEach(SonImporte::arreter);
			sons.clear();
			images.values().stream().filter(i -> i != null).forEach(i -> mc.getTextureManager().release(i.id()));
			images.clear();
			if (notify && ClientPlayNetworking.canSend(CinematiqueFiniePayload.TYPE)) ClientPlayNetworking.send(new CinematiqueFiniePayload(data.id()));
		}

		void draw(GuiGraphics g, int width, int height, float partial) {
			double t = tick + partial;
			int bar = Math.round(height / 8f * chrono.bandes(t));
			if (bar > 0) {
				g.fill(0, 0, width, bar, 0xFF000000);
				g.fill(0, height - bar, width, height, 0xFF000000);
			}
			int noir = (int) (chrono.fondu(t) * 255);
			if (noir > 0) g.fill(0, 0, width, height, noir << 24);
			for (Chrono.Image im : chrono.images(t)) {
				Image tex = image(im.media());
				int a = (int) (im.alpha() * 255);
				if (tex == null || a < 4) continue;
				int w = Math.max(1, Math.round(im.largeur() * width)), hh = Math.max(1, Math.round(w * (float) tex.hauteur() / tex.largeur()));
				g.blit(RenderPipelines.GUI_TEXTURED, tex.id(), Math.round(im.x() * width - w / 2f), Math.round(im.y() * height - hh / 2f), 0f, 0f, w, hh,
						tex.largeur(), tex.hauteur(), tex.largeur(), tex.hauteur(), a << 24 | 0xFFFFFF);
			}
			Chrono.Texte texte = chrono.texte(t);
			if (texte == null) return;
			int alpha = (int) (texte.alpha() * 255);
			if (alpha < 4) return; // sous 4, Minecraft dessine le texte opaque
			var font = mc.font;
			String[] lignes = texte.texte().split("\n");
			float s = texte.taille(), hauteur = 9 * s;
			float y = texte.bas() ? height * 0.85f - hauteur * lignes.length : (height - hauteur * lignes.length) / 2f;
			String reprise = "";
			for (String brute : lignes) {
				String ligne = reprise + brute; // les codes § ne passent pas d'une ligne à l'autre : on répète l'état en début de ligne
				reprise = codesActifs(ligne);
				Component c = PanneauClient.fmt(ligne);
				g.pose().pushMatrix();
				g.pose().translate((width - font.width(c) * s) / 2f, y);
				g.pose().scale(s, s);
				g.drawString(font, c, 0, 0, alpha << 24 | (texte.couleur() & 0xFFFFFF), false);
				g.pose().popMatrix();
				y += hauteur;
			}
		}

		/** Codes § encore actifs en fin de ligne : une couleur remet les styles à zéro, §r efface tout. */
		static String codesActifs(String ligne) {
			String couleur = "", styles = "";
			for (int i = 0; i + 1 < ligne.length(); i++) {
				if (ligne.charAt(i) != '§') continue;
				char c = Character.toLowerCase(ligne.charAt(++i));
				if (c == 'r') couleur = styles = "";
				else if ("0123456789abcdef".indexOf(c) >= 0) {
					couleur = "§" + c;
					styles = "";
				} else if ("klmno".indexOf(c) >= 0 && styles.indexOf(c) < 0) styles += "§" + c;
			}
			return couleur + styles;
		}
	}

	/** Écran qui capte clavier et souris pendant que la caméra est détachée ; Échap ouvre le menu du jeu. */
	private static final class LecteurScreen extends Screen {
		private final Lecture lecture;

		LecteurScreen(Lecture lecture) {
			super(Component.empty());
			this.lecture = lecture;
		}

		@Override
		public boolean isPauseScreen() {
			return false;
		}

		@Override
		public void onClose() {
			minecraft.setScreen(new PauseScreen(true));
		}

		@Override
		public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) { }

		@Override
		public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			lecture.draw(g, width, height, partialTick);
		}
	}
}
