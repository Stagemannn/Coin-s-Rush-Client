package com.coinrush.cinematique.client;

import com.coinrush.commun.CinematiqueFiniePayload;
import com.coinrush.commun.CinematiquePayload;
import com.coinrush.commun.CinematiqueStopPayload;
import com.coinrush.commun.Commun;
import com.coinrush.panneau.client.PanneauClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lecteur de cinématiques en plans (0.5.0) : caméra détachée du joueur qui suit le {@link Trajet}, bandes noires, titres en fondu,
 * HUD masqué, joueur bloqué par un écran. Échap ouvre le menu (pas de « passer »). À la fin, retour à la vue normale puis signal
 * {@link CinematiqueFiniePayload} ; le serveur peut aussi arrêter la lecture. Le client ne décide de rien : la validation borne la durée.
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
		// Le menu Échap s'ouvre par-dessus la cinématique : on y redessine bandes et titres.
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof PauseScreen) ScreenEvents.afterRender(screen).register((s, g, mx, my, delta) -> {
				if (current != null) current.draw(g, s.width, s.height);
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
		private final Trajet trajet;
		private final Entity camera;
		private final Entity previousCamera;
		private final boolean hideGui;
		private final CameraType cameraType;
		private final Screen screen;
		private int tick;

		Lecture(Minecraft mc, CinematiquePayload data) {
			this.mc = mc;
			this.data = data;
			this.trajet = new Trajet(data.plans());
			this.previousCamera = mc.getCameraEntity();
			this.hideGui = mc.options.hideGui;
			this.cameraType = mc.options.getCameraType();
			this.camera = new Marker(EntityType.MARKER, mc.level);
			Trajet.Pose p = trajet.pose(0);
			camera.snapTo(p.x(), p.y(), p.z(), p.yaw(), p.pitch());
			mc.options.hideGui = true;
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			mc.setCameraEntity(camera);
			screen = new LecteurScreen(this);
			mc.setScreen(screen);
		}

		void tick() {
			if (mc.player == null || mc.level != camera.level()) {
				stop();
				return;
			}
			if (mc.screen == null) mc.setScreen(screen); // retour du menu Échap
			else if (mc.screen != screen && !(mc.screen instanceof PauseScreen)) { // un autre écran a pris la place : on rend la main
				finish();
				return;
			}
			if (++tick >= trajet.total()) {
				finish();
				return;
			}
			Trajet.Pose p = trajet.pose(tick);
			camera.setOldPosAndRot();
			camera.setPos(p.x(), p.y(), p.z());
			camera.setYRot(p.yaw());
			camera.setXRot(p.pitch());
		}

		/** Fin normale : vue rendue, puis signal au serveur. */
		private void finish() {
			current = null;
			end(true);
		}

		void end(boolean notify) {
			mc.options.hideGui = hideGui;
			mc.options.setCameraType(cameraType);
			if (mc.getCameraEntity() == camera) mc.setCameraEntity(mc.player != null ? mc.player : previousCamera);
			if (mc.screen == screen || mc.screen instanceof PauseScreen) mc.setScreen(null);
			if (notify && ClientPlayNetworking.canSend(CinematiqueFiniePayload.TYPE)) ClientPlayNetworking.send(new CinematiqueFiniePayload(data.id()));
		}

		void draw(GuiGraphics g, int width, int height) {
			if (data.bandesNoires()) {
				int bar = height / 8;
				g.fill(0, 0, width, bar, 0xFF000000);
				g.fill(0, height - bar, width, height, 0xFF000000);
			}
			CinematiquePayload.Plan plan = data.plans().get(trajet.index(tick));
			float a = Trajet.fade(trajet.local(tick), plan.dureeTicks());
			int alpha = (int) (a * 255);
			if (alpha < 4) return; // sous 4, Minecraft dessine le texte opaque
			var font = mc.font;
			int y = height * 2 / 3;
			if (plan.titre() != null && !plan.titre().isEmpty()) {
				g.pose().pushMatrix();
				g.pose().translate(width / 2f, y);
				g.pose().scale(2f, 2f);
				g.drawCenteredString(font, PanneauClient.fmt(plan.titre()), 0, 0, alpha << 24 | 0xFFFFFF);
				g.pose().popMatrix();
				y += font.lineHeight * 2 + 4;
			}
			if (plan.sousTitre() != null && !plan.sousTitre().isEmpty()) g.drawCenteredString(font, PanneauClient.fmt(plan.sousTitre()), width / 2, y, alpha << 24 | 0xFFFFFF);
		}
	}

	/** Écran qui capte clavier et souris ; Échap ouvre le menu du jeu. */
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
			lecture.draw(g, width, height);
		}
	}
}
