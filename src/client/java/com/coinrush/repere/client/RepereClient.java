package com.coinrush.repere.client;

import com.coinrush.commun.Commun;
import com.coinrush.commun.WaypointPayload;
import com.coinrush.panneau.client.PanneauClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repère : flèche orientée vers le point, label et distance, sous le panneau. Dans une autre dimension : label seul.
 * Effacé par le serveur ({@code actif=false}) ou à la déconnexion.
 */
public final class RepereClient implements ClientModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger("coinrush");
	private static final int ORANGE = 0xFFAA00, RED = 0xFF5555, PAD = 4, ARROW = 16, MAX_WIDTH = 190;
	private static volatile WaypointPayload waypoint;

	@Override
	public void onInitializeClient() {
		Commun.init();
		ClientPlayNetworking.registerGlobalReceiver(WaypointPayload.TYPE, (payload, context) -> {
			try {
				payload.validate();
				waypoint = payload.actif() ? payload : null;
			} catch (IllegalArgumentException e) {
				LOGGER.warn("Repère refusé : {}", e.getMessage());
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> waypoint = null);
		// Enregistré après le panneau (ordre des entrées) : sa position est à jour quand on dessine.
		HudElementRegistry.addLast(ResourceLocation.fromNamespaceAndPath("coinrush", "repere"), (graphics, delta) -> render(graphics));
	}

	private static void render(GuiGraphics g) {
		WaypointPayload w = waypoint;
		Minecraft mc = Minecraft.getInstance();
		if (w == null || mc.player == null || (mc.screen != null && !(mc.screen instanceof ChatScreen)) || mc.getDebugOverlay().showDebugScreen()) return;
		Font font = mc.font;
		boolean same = mc.level.dimension().location().toString().equals(w.dimension());
		double dx = w.x() - mc.player.getX(), dy = w.y() - mc.player.getY(), dz = w.z() - mc.player.getZ();
		int shownW = Math.min(MAX_WIDTH, g.guiWidth() / 3);
		int room = shownW - PAD * 2 - 3 - (same ? ARROW + 4 : 0);
		int h = PAD * 2 + (same ? ARROW : font.lineHeight * 2 + 2);
		int x = g.guiWidth() - shownW - 6;
		int y = Math.max(PanneauClient.bottom(), 2) + 4;
		g.fill(x, y, x + shownW, y + h, 0xB00E0E14);
		g.fillGradient(x, y, x + 3, y + h, 0xFF000000 | ORANGE, 0xFF000000 | RED);
		int tx = x + 3 + PAD;
		if (same) {
			double rel = Math.atan2(-dx, dz) - Math.toRadians(mc.player.getYRot()); // 0 : droit devant, positif : à droite
			drawArrow(g, tx + ARROW / 2f, y + PAD + ARROW / 2f, (float) rel);
			tx += ARROW + 4;
		}
		int ty = y + PAD + (same ? (ARROW - font.lineHeight * 2 - 2) / 2 : 0);
		PanneauClient.text(g, font, PanneauClient.fmt(w.label()).getVisualOrderText(), tx, ty, room, ORANGE);
		if (same) {
			String distance = (int) Math.sqrt(dx * dx + dy * dy + dz * dz) + " blocs";
			g.drawString(font, Component.literal(distance), tx, ty + font.lineHeight + 2, 0xFFAAAAAA, true);
		} else {
			g.drawString(font, Component.literal("Autre dimension"), tx, ty + font.lineHeight + 2, 0xFFAAAAAA, true);
		}
	}

	/** Flèche pleine pointant vers le haut avant rotation (angle en radians, sens horaire à l'écran), centrée en (cx, cy). */
	private static void drawArrow(GuiGraphics g, float cx, float cy, float angle) {
		g.pose().pushMatrix();
		g.pose().translate(cx, cy);
		g.pose().rotate(angle);
		int color = 0xFF000000 | ORANGE;
		for (int row = 0; row < 7; row++) g.fill(-row, -8 + row, row + 1, -7 + row, color); // pointe
		g.fill(-2, -1, 3, 8, color); // hampe
		g.pose().popMatrix();
	}
}
