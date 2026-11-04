package com.coinrush.panneau.client;

import com.coinrush.commun.Commun;
import com.coinrush.commun.Panel;
import com.coinrush.commun.PanelPayload;
import com.coinrush.commun.Coins;
import com.coinrush.socle.client.SocleClient;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Panneau d'affichage en haut à droite : Coins, puis les sections reçues du serveur (une section vide n'apparaît pas).
 * Style inspiré du panneau du mod FK : fond sombre, liseré d'accent, texte ombré, valeurs alignées à droite.
 */
public final class PanneauClient implements ClientModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger("coinrush");
	private static final int PAD = 4, GAP = 10, MAX_WIDTH = 190;
	private static final KeyMapping TOGGLE = new KeyMapping("key.coinrush.panneau", InputConstants.Type.KEYSYM, InputConstants.KEY_K, "key.categories.coinrush");

	private static volatile Panel panel = Panel.EMPTY;
	private static volatile long receivedAt;
	private static boolean compact;

	/** Une ligne dessinée : texte à gauche, valeur à droite, ou barre de progression si {@code bar} >= 0. */
	private record Line(String text, int color, String value, int valueColor, int bar, int barColor, int gapBefore) { }

	@Override
	public void onInitializeClient() {
		Commun.init();
		KeyBindingHelper.registerKeyBinding(TOGGLE);
		ClientTickEvents.END_CLIENT_TICK.register(c -> {
			while (TOGGLE.consumeClick()) compact = !compact;
		});
		ClientPlayNetworking.registerGlobalReceiver(PanelPayload.TYPE, (payload, context) -> {
			try {
				panel = Panel.parse(payload.json());
				receivedAt = System.currentTimeMillis();
			} catch (IllegalArgumentException e) {
				LOGGER.warn("Panneau refusé : {}", e.getMessage());
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> panel = Panel.EMPTY);
		HudElementRegistry.addLast(ResourceLocation.fromNamespaceAndPath("coinrush", "panneau"), (graphics, delta) -> render(graphics));
	}

	private static int accent(Panel.Kind kind) {
		return switch (kind) {
			case QUETE -> 0x55D0FF;
			case FARM -> 0x6EE06E;
			case EVENEMENT -> 0xFFAA33;
			case GM -> 0xFF5577;
		};
	}

	private static String label(Panel.Kind kind) {
		return switch (kind) {
			case QUETE -> "Quête";
			case FARM -> "Farm";
			case EVENEMENT -> "Événement";
			case GM -> "Événement GM";
		};
	}

	/** m:ss ou h:mm:ss. */
	static String clock(long seconds) {
		seconds = Math.max(0, seconds);
		return seconds >= 3600 ? "%d:%02d:%02d".formatted(seconds / 3600, seconds / 60 % 60, seconds % 60) : "%d:%02d".formatted(seconds / 60, seconds % 60);
	}

	private static List<Line> lines() {
		List<Line> lines = new ArrayList<>();
		lines.add(new Line("Coins", 0xFFD700, Coins.format(SocleClient.balance()), 0xFFFFFF, -1, 0, 0));
		if (compact) return lines;
		long elapsed = (System.currentTimeMillis() - receivedAt) / 1000;
		for (Panel.Kind kind : Panel.Kind.values()) {
			for (Panel.Entry e : panel.entries()) {
				if (e.kind() != kind) continue;
				int color = accent(kind);
				String timer = e.seconds() == null ? "" : clock(e.seconds() - elapsed);
				lines.add(new Line(label(kind), color, timer, color, -1, 0, 4));
				lines.add(new Line(e.title(), 0xFFFFFF, "", 0, -1, 0, 0));
				if (e.detail() != null && !e.detail().isEmpty()) lines.add(new Line(e.detail(), 0xAAAAAA, "", 0, -1, 0, 0));
				if (e.progress() != null) lines.add(new Line("", 0, "", 0, e.progress(), color, 0));
			}
		}
		return lines;
	}

	/** Les icônes d'effets de potion occupent le haut à droite : le panneau se place dessous. */
	private static int effectsHeight(Minecraft mc) {
		boolean good = false, bad = false;
		for (MobEffectInstance effect : mc.player.getActiveEffects()) {
			if (!effect.showIcon()) continue;
			if (effect.getEffect().value().isBeneficial()) good = true;
			else bad = true;
		}
		return (good ? 26 : 0) + (bad ? 26 : 0);
	}

	private static void render(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || (mc.screen != null && !(mc.screen instanceof ChatScreen)) || mc.getDebugOverlay().showDebugScreen()) return;
		Font font = mc.font;
		List<Line> lines = lines();
		int maxWidth = Math.min(MAX_WIDTH, g.guiWidth() / 3);
		int lineH = font.lineHeight + 2;
		int x0 = 0, inner = 0;
		for (Line l : lines) {
			if (l.bar >= 0) continue;
			inner = Math.max(inner, font.width(l.text) + (l.value.isEmpty() ? 0 : GAP + font.width(l.value)));
		}
		inner = Math.min(inner, maxWidth);
		int w = inner + PAD * 2 + 3;
		int y = 6 + effectsHeight(mc);
		int limit = g.guiHeight() * 45 / 100; // le panneau ne descend jamais sur le milieu de l'écran (tableau des scores)
		int h = PAD * 2 - 2, shown = 0;
		for (Line l : lines) {
			int next = h + l.gapBefore + (l.bar >= 0 ? 5 : lineH);
			if (y + next > limit && shown > 0) break;
			h = next;
			shown++;
		}
		int x = g.guiWidth() - w - 6;
		g.fill(x, y, x + w, y + h, 0xB00E0E14);
		g.fill(x, y, x + 3, y + h, 0xFFFFD700);
		g.fill(x + 3, y, x + w, y + 1, 0x33FFFFFF);
		g.fill(x + 3, y + h - 1, x + w, y + h, 0x33000000);
		int ty = y + PAD;
		int left = x + 3 + PAD, right = x + w - PAD;
		for (Line l : lines.subList(0, shown)) {
			ty += l.gapBefore;
			if (l.bar >= 0) {
				g.fill(left, ty, right, ty + 3, 0x66000000);
				g.fill(left, ty, left + (right - left) * l.bar / 100, ty + 3, 0xFF000000 | l.barColor);
				ty += 5;
				continue;
			}
			int room = right - left - (l.value.isEmpty() ? 0 : GAP + font.width(l.value));
			String text = font.width(l.text) > room ? font.plainSubstrByWidth(l.text, room - font.width("…")) + "…" : l.text;
			g.drawString(font, text, left, ty, 0xFF000000 | l.color, true);
			if (!l.value.isEmpty()) g.drawString(font, l.value, right - font.width(l.value), ty, 0xFF000000 | l.valueColor, true);
			ty += lineH;
		}
	}
}
