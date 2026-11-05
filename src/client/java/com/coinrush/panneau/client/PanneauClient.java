package com.coinrush.panneau.client;

import com.coinrush.commun.Commun;
import com.coinrush.commun.JourPayload;
import com.coinrush.commun.LocationPayload;
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
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffectInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Panneau d'affichage en haut à droite : Coins, puis les sections reçues du serveur (une section vide n'apparaît pas).
 * Vue normale : textes longs défilant dans leur cadre. Vue détaillée (clic sur le panneau, chat ouvert) : descriptions sur plusieurs lignes.
 * Style inspiré du panneau du mod FK : fond sombre, liseré d'accent, texte ombré, valeurs alignées à droite.
 */
public final class PanneauClient implements ClientModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger("coinrush");
	private static final int PAD = 4, GAP = 10, MAX_WIDTH = 190, MAX_WIDTH_DETAIL = 250, CHECK_W = 11;
	private static final int ORANGE = 0xFFAA00, RED = 0xFF5555, GREEN = 0x55FF55;
	private static final KeyMapping TOGGLE = new KeyMapping("key.coinrush.panneau", InputConstants.Type.KEYSYM, InputConstants.KEY_K, "key.categories.coinrush");

	private static volatile Panel panel = Panel.EMPTY;
	private static volatile long receivedAt;
	private static volatile String lieu = "";
	private static volatile String jour = "";
	private static boolean compact, detailed;
	/** Rectangle du panneau tel que dessiné à la dernière image (largeur 0 : rien d'affiché), pour le clic. */
	private static int rx, ry, rw, rh;

	/** Une ligne dessinée : texte à gauche (avec coche éventuelle), valeur à droite, ou barre de progression si {@code bar} >= 0. */
	private record Line(FormattedCharSequence text, int color, String value, int valueColor, boolean check, int bar, int barColor, int gapBefore) { }

	@Override
	public void onInitializeClient() {
		Commun.init();
		KeyBindingHelper.registerKeyBinding(TOGGLE);
		ClientTickEvents.END_CLIENT_TICK.register(c -> {
			while (TOGGLE.consumeClick()) compact = !compact;
			if (!(c.screen instanceof ChatScreen)) detailed = false;
		});
		// Un clic sur le panneau bascule la vue détaillée et n'atteint pas le chat.
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof ChatScreen) ScreenMouseEvents.allowMouseClick(screen).register((s, mx, my, button) -> {
				if (rw == 0 || mx < rx || mx >= rx + rw || my < ry || my >= ry + rh) return true;
				if (button == 0) detailed = !detailed;
				return false;
			});
		});
		ClientPlayNetworking.registerGlobalReceiver(PanelPayload.TYPE, (payload, context) -> {
			try {
				panel = Panel.parse(payload.json());
				receivedAt = System.currentTimeMillis();
			} catch (IllegalArgumentException e) {
				LOGGER.warn("Panneau refusé : {}", e.getMessage());
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(LocationPayload.TYPE, (payload, context) -> {
			try {
				payload.validate();
				lieu = payload.lieu();
			} catch (IllegalArgumentException e) {
				LOGGER.warn("Lieu refusé : {}", e.getMessage());
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(JourPayload.TYPE, (payload, context) -> {
			try {
				payload.validate();
				jour = payload.texte();
			} catch (IllegalArgumentException e) {
				LOGGER.warn("Jour refusé : {}", e.getMessage());
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			panel = Panel.EMPTY;
			lieu = "";
			jour = "";
		});
		HudElementRegistry.addLast(ResourceLocation.fromNamespaceAndPath("coinrush", "panneau"), (graphics, delta) -> render(graphics));
	}

	private static int accent(Panel.Kind kind) {
		return switch (kind) {
			case QUETE, EVENEMENT -> ORANGE;
			case FARM, GM -> RED;
		};
	}

	private static String label(Panel.Entry e) {
		if (e.label() != null && !e.label().isBlank()) return e.label();
		return switch (e.kind()) {
			case QUETE -> "Quête";
			case FARM -> "Demande du Casino";
			case EVENEMENT -> "Événement";
			case GM -> "Événement GM";
		};
	}

	/** Texte avec codes § : chaque code est consommé et appliqué en style, jamais affiché ni coupé. */
	public static Component fmt(String s) {
		MutableComponent out = Component.empty();
		Style style = Style.EMPTY;
		StringBuilder run = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c != '§' || i + 1 >= s.length()) {
				run.append(c);
				continue;
			}
			ChatFormatting f = ChatFormatting.getByCode(s.charAt(++i));
			if (run.length() > 0) out.append(Component.literal(run.toString()).withStyle(style));
			run.setLength(0);
			if (f == ChatFormatting.RESET) style = Style.EMPTY;
			else if (f != null) style = f.isFormat() ? style.applyFormat(f) : Style.EMPTY.applyFormat(f);
		}
		if (run.length() > 0) out.append(Component.literal(run.toString()).withStyle(style));
		return out;
	}

	/** m:ss ou h:mm:ss. */
	static String clock(long seconds) {
		seconds = Math.max(0, seconds);
		return seconds >= 3600 ? "%d:%02d:%02d".formatted(seconds / 3600, seconds / 60 % 60, seconds % 60) : "%d:%02d".formatted(seconds / 60, seconds % 60);
	}

	/** Un texte : une ligne qui défile si trop longue (vue normale), ou plusieurs lignes (vue détaillée). */
	private static void add(List<Line> lines, Font font, String text, int color, int room, boolean wrap, boolean check, int gap) {
		Component c = fmt(text);
		List<FormattedCharSequence> parts = wrap ? font.split(c, room - (check ? CHECK_W : 0)) : List.of(c.getVisualOrderText());
		for (int i = 0; i < parts.size(); i++) lines.add(new Line(parts.get(i), color, "", 0, check && i == 0, -1, 0, i == 0 ? gap : 0));
	}

	private static List<Line> lines(Font font, int room, boolean full) {
		List<Line> lines = new ArrayList<>();
		if (!jour.isEmpty()) lines.add(new Line(fmt(jour).getVisualOrderText(), ORANGE, "", 0, false, -1, 0, 0)); // tout en haut : pause ou jour et heure
		Component balance = Component.literal("Coin's : ").withStyle(Style.EMPTY.withColor(ORANGE)).append(Component.literal(Coins.format(SocleClient.balance())).withStyle(Style.EMPTY.withColor(0xFFFFFF)));
		lines.add(new Line(balance.getVisualOrderText(), 0xFFFFFF, "", 0, false, -1, 0, 0));
		String place = lieu;
		if (!place.isEmpty()) {
			Component c = Component.literal("Lieu : ").withStyle(Style.EMPTY.withColor(ORANGE)).append(fmt(place));
			lines.add(new Line(c.getVisualOrderText(), 0xFFFFFF, "", 0, false, -1, 0, 0));
		}
		if (compact) return lines;
		long elapsed = (System.currentTimeMillis() - receivedAt) / 1000;
		for (Panel.Kind kind : Panel.Kind.values()) {
			for (Panel.Entry e : panel.entries()) {
				if (e.kind() != kind) continue;
				int color = accent(kind);
				String timer = e.seconds() == null ? "" : clock(e.seconds() - elapsed);
				lines.add(new Line(fmt(label(e)).getVisualOrderText(), color, timer, color, false, -1, 0, 5));
				add(lines, font, e.title(), 0xFFFFFF, room, full, e.done(), 0);
				if (e.detail() != null && !e.detail().isEmpty()) add(lines, font, e.detail(), 0xAAAAAA, room, full, false, 0);
				if (full && e.description() != null && !e.description().isEmpty()) add(lines, font, e.description(), 0xCCCCCC, room, true, false, 2);
				if (e.progress() != null) lines.add(new Line(FormattedCharSequence.EMPTY, 0, "", 0, false, e.progress(), color, 0));
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

	/** Texte qui défile doucement dans son cadre quand il est trop long, avec une pause à chaque bout ; jamais coupé. */
	public static void text(GuiGraphics g, Font font, FormattedCharSequence s, int x, int y, int room, int color) {
		int tw = font.width(s);
		if (tw <= room) {
			g.drawString(font, s, x, y, 0xFF000000 | color, true);
			return;
		}
		int over = tw - room;
		long cycle = over * 40L + 2000; // 25 px/s, 1 s de pause de chaque côté
		long t = System.currentTimeMillis() % (cycle * 2);
		long pos = t < cycle ? t : cycle * 2 - t;
		int off = (int) Math.max(0, Math.min(over, (pos - 1000) / 40));
		g.enableScissor(x, y - 1, x + room, y + font.lineHeight + 1);
		g.drawString(font, s, x - off, y, 0xFF000000 | color, true);
		g.disableScissor();
	}

	/** Bas du panneau tel que dessiné (0 si non affiché), pour placer ce qui vient dessous. */
	public static int bottom() {
		return rw == 0 ? 0 : ry + rh;
	}

	private static void check(GuiGraphics g, int x, int y) {
		int[][] px = {{0, 4}, {1, 5}, {2, 6}, {3, 5}, {4, 4}, {5, 3}, {6, 2}, {7, 1}, {8, 0}};
		for (int[] p : px) g.fill(x + p[0], y + p[1], x + p[0] + 1, y + p[1] + 2, 0xFF000000 | GREEN);
	}

	private static void render(GuiGraphics g) {
		rw = 0;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || (mc.screen != null && !(mc.screen instanceof ChatScreen)) || mc.getDebugOverlay().showDebugScreen()) return;
		Font font = mc.font;
		boolean full = detailed && !compact;
		int maxWidth = full ? Math.min(MAX_WIDTH_DETAIL, g.guiWidth() * 45 / 100) : Math.min(MAX_WIDTH, g.guiWidth() / 3);
		int room = maxWidth - PAD * 2 - 3;
		List<Line> lines = lines(font, room, full);
		int lineH = font.lineHeight + 2;
		int inner = 0;
		for (Line l : lines) {
			if (l.bar >= 0) continue;
			inner = Math.max(inner, (l.check ? CHECK_W : 0) + font.width(l.text) + (l.value.isEmpty() ? 0 : GAP + font.width(l.value)));
		}
		inner = full ? room : Math.min(inner, room);
		int w = inner + PAD * 2 + 3;
		int y = 6 + effectsHeight(mc);
		int limit = g.guiHeight() * 45 / 100; // le panneau ne descend jamais sur le milieu de l'écran (tableau des scores)
		int h = PAD * 2 - 2, shown = 0;
		for (Line l : lines) {
			int next = h + l.gapBefore + (l.bar >= 0 ? 5 : lineH);
			if (y + next > limit && shown > 0) {
				int k = shown; // coupe sur le début d'une section, jamais au milieu
				while (k > 1 && lines.get(k - 1).gapBefore < 5) k--;
				if (k > 1 && lines.get(shown).gapBefore < 5) shown = k - 1;
				break;
			}
			h = next;
			shown++;
		}
		h = PAD * 2 - 2;
		for (Line l : lines.subList(0, shown)) h += l.gapBefore + (l.bar >= 0 ? 5 : lineH);
		int x = g.guiWidth() - w - 6;
		rx = x;
		ry = y;
		rw = w;
		rh = h;
		g.fill(x, y, x + w, y + h, 0xB00E0E14);
		g.fillGradient(x, y, x + 3, y + h, 0xFF000000 | ORANGE, 0xFF000000 | RED);
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
			int tx = left;
			if (l.check) {
				check(g, left, ty);
				tx += CHECK_W;
			}
			int space = right - tx - (l.value.isEmpty() ? 0 : GAP + font.width(l.value));
			text(g, font, l.text, tx, ty, space, l.color);
			if (!l.value.isEmpty()) g.drawString(font, l.value, right - font.width(l.value), ty, 0xFF000000 | l.valueColor, true);
			ty += lineH;
		}
	}
}
