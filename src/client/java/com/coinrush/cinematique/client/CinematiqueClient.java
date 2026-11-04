package com.coinrush.cinematique.client;

import com.coinrush.commun.CinematicPayload;
import com.coinrush.commun.CinematicSkipPayload;
import com.coinrush.commun.CinematicStopPayload;
import com.coinrush.commun.Commun;
import com.coinrush.commun.Scenario;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Joue la cinématique envoyée par le serveur : la caméra suit le chemin du scénario, le joueur ne peut rien faire (un écran
 * capte les touches) et retrouve son état normal à la fin, sur ordre du serveur, sur demande (Échap si permis) ou à la déconnexion.
 * Le client ne décide de rien : sa demande de passer n'est qu'une demande, et le temps du scénario est borné par la validation.
 */
public final class CinematiqueClient implements ClientModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger("coinrush");
	private static Playback current;

	@Override
	public void onInitializeClient() {
		Commun.init();
		ClientPlayNetworking.registerGlobalReceiver(CinematicPayload.TYPE, (payload, context) -> start(payload.json()));
		ClientPlayNetworking.registerGlobalReceiver(CinematicStopPayload.TYPE, (payload, context) -> stop());
		ClientTickEvents.END_CLIENT_TICK.register(c -> {
			if (current != null) current.tick();
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> stop());
	}

	static void start(String json) {
		Scenario scenario;
		try {
			scenario = Scenario.parse(json);
		} catch (IllegalArgumentException e) {
			LOGGER.warn("Cinématique refusée : {}", e.getMessage());
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;
		stop();
		current = new Playback(mc, scenario);
	}

	static void stop() {
		Playback p = current;
		current = null;
		if (p != null) p.end();
	}

	/** Une lecture en cours, avec l'état du joueur à rétablir. */
	private static final class Playback {
		private final Minecraft mc;
		private final Scenario scenario;
		private final Entity camera;
		private final Entity previousCamera;
		private final boolean hideGui;
		private final CameraType cameraType;
		private final Screen screen;
		private int tick;

		Playback(Minecraft mc, Scenario scenario) {
			this.mc = mc;
			this.scenario = scenario;
			this.previousCamera = mc.getCameraEntity();
			this.hideGui = mc.options.hideGui;
			this.cameraType = mc.options.getCameraType();
			this.camera = new Marker(EntityType.MARKER, mc.level);
			Scenario.Point p = scenario.at(0);
			camera.snapTo(p.x(), p.y(), p.z(), p.yaw(), p.pitch());
			mc.options.hideGui = true;
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			mc.setCameraEntity(camera);
			screen = new CinematicScreen(scenario, this);
			mc.setScreen(screen);
		}

		int elapsed() {
			return tick;
		}

		void tick() {
			if (mc.player == null || mc.level != camera.level() || mc.screen != screen || ++tick >= scenario.totalTicks()) {
				stop();
				return;
			}
			Scenario.Point p = scenario.at(tick);
			camera.setOldPosAndRot();
			camera.setPos(p.x(), p.y(), p.z());
			camera.setYRot(p.yaw());
			camera.setXRot(p.pitch());
		}

		void end() {
			mc.options.hideGui = hideGui;
			mc.options.setCameraType(cameraType);
			if (mc.getCameraEntity() == camera) mc.setCameraEntity(mc.player != null ? mc.player : previousCamera);
			if (mc.screen == screen) mc.setScreen(null);
		}
	}

	/** Barres de cinéma, textes du scénario. Échap demande au serveur de passer, s'il le permet. */
	private static final class CinematicScreen extends Screen {
		private final Scenario scenario;
		private final Playback playback;

		CinematicScreen(Scenario scenario, Playback playback) {
			super(Component.empty());
			this.scenario = scenario;
			this.playback = playback;
		}

		@Override
		public boolean isPauseScreen() {
			return false;
		}

		@Override
		public void onClose() {
			if (scenario.skippable() && ClientPlayNetworking.canSend(CinematicSkipPayload.TYPE)) ClientPlayNetworking.send(new CinematicSkipPayload());
		}

		@Override
		public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) { }

		@Override
		public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			int bar = height / 8;
			g.fill(0, 0, width, bar, 0xFF000000);
			g.fill(0, height - bar, width, height, 0xFF000000);
			int y = height - bar - 12;
			var texts = scenario.textsAt(playback.elapsed());
			for (int i = texts.size() - 1; i >= 0; i--) {
				g.drawCenteredString(font, texts.get(i).text(), width / 2, y, 0xFFFFFFFF);
				y -= font.lineHeight + 3;
			}
			if (scenario.skippable()) g.drawString(font, "Échap : passer", width - font.width("Échap : passer") - 6, height - bar + 6, 0xFF888888, true);
		}
	}
}
