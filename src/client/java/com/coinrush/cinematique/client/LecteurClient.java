package com.coinrush.cinematique.client;

import com.coinrush.commun.CinematiqueFiniePayload;
import com.coinrush.commun.CinematiquePayload;
import com.coinrush.commun.CinematiquePayload.Pose;
import com.coinrush.commun.CinematiqueStopPayload;
import com.coinrush.commun.Commun;
import com.coinrush.panneau.client.PanneauClient;
import net.fabricmc.api.ClientModInitializer;
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
	private static final Logger LOGGER = LoggerFactory.getLogger("coinrush");
	private static Lecture current;

	@Override
	public void onInitializeClient() {
		Commun.init();
		ClientPlayNetworking.registerGlobalReceiver(CinematiquePayload.TYPE, (payload, context) -> start(payload));
		ClientPlayNetworking.registerGlobalReceiver(CinematiqueStopPayload.TYPE, (payload, context) -> stop());
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

		Lecture(Minecraft mc, CinematiquePayload data) {
			this.mc = mc;
			this.data = data;
			this.chrono = new Chrono(data);
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
			if (notify && ClientPlayNetworking.canSend(CinematiqueFiniePayload.TYPE)) ClientPlayNetworking.send(new CinematiqueFiniePayload(data.id()));
		}

		void draw(GuiGraphics g, int width, int height, float partial) {
			double t = tick + partial;
			if (detachee && data.bandesNoires()) {
				int bar = height / 8;
				g.fill(0, 0, width, bar, 0xFF000000);
				g.fill(0, height - bar, width, height, 0xFF000000);
			}
			int noir = (int) (chrono.fondu(t) * 255);
			if (noir > 0) g.fill(0, 0, width, height, noir << 24);
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
