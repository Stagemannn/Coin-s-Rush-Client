package com.coinrush.dialogue.client;

import com.coinrush.commun.Commun;
import com.coinrush.commun.NpcDialoguePayload;
import com.coinrush.panneau.client.PanneauClient;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fenêtre de dialogue de PNJ en bas de l'écran : le monde reste visible et n'est pas assombri.
 * Un bouton envoie sa commande au serveur (comme si le joueur la tapait : le serveur la contrôle) puis ferme ; Échap ferme.
 */
public final class DialogueClient implements ClientModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger("coinrush");
	private static final int ORANGE = 0xFFAA00, RED = 0xFF5555, WIDTH = 360, BUTTON_H = 20, PAD = 8;

	private static double savedX, savedY;
	private static long savedAt;

	private static void remember(Minecraft mc) {
		savedX = mc.mouseHandler.xpos();
		savedY = mc.mouseHandler.ypos();
		savedAt = System.currentTimeMillis();
	}

	@Override
	public void onInitializeClient() {
		Commun.init();
		ClientPlayNetworking.registerGlobalReceiver(NpcDialoguePayload.TYPE, (payload, context) -> {
			try {
				payload.validate();
			} catch (IllegalArgumentException e) {
				LOGGER.warn("Dialogue refusé : {}", e.getMessage());
				return;
			}
			Minecraft mc = Minecraft.getInstance();
			// Ouvrir un écran recentre la souris : on la remet où elle était (fenêtre qui change, ou qui se rouvre juste après un clic).
			if (mc.screen instanceof DialogueScreen) remember(mc);
			mc.setScreen(new DialogueScreen(payload));
			if (System.currentTimeMillis() - savedAt < 3000) GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), savedX, savedY);
			savedAt = 0;
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
			if (client.screen instanceof DialogueScreen) client.setScreen(null);
		}));
	}

	/** Bouton dans la charte : fond sombre, contour orange au survol. */
	private static final class ChoiceButton extends Button {
		ChoiceButton(Component label, OnPress onPress) {
			super(0, 0, 0, BUTTON_H, label, onPress, DEFAULT_NARRATION);
		}

		@Override
		protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			int x = getX(), y = getY(), r = x + getWidth(), b = y + getHeight();
			boolean on = isHoveredOrFocused();
			g.fill(x, y, r, b, on ? 0xFF2A2230 : 0xFF1A1A24);
			int edge = on ? 0xFF000000 | ORANGE : 0x44FFFFFF;
			g.fill(x, y, r, y + 1, edge);
			g.fill(x, b - 1, r, b, edge);
			g.fill(x, y, x + 1, b, edge);
			g.fill(r - 1, y, r, b, edge);
			Minecraft mc = Minecraft.getInstance();
			g.drawCenteredString(mc.font, getMessage(), x + getWidth() / 2, y + (getHeight() - mc.font.lineHeight) / 2 + 1, 0xFFFFFFFF);
		}
	}

	private static final class DialogueScreen extends Screen {
		private final NpcDialoguePayload data;
		private List<FormattedCharSequence> lines = List.of();
		private int boxX, boxY, boxW, boxH;

		DialogueScreen(NpcDialoguePayload data) {
			super(Component.empty());
			this.data = data;
		}

		@Override
		public boolean isPauseScreen() {
			return false;
		}

		@Override
		protected void init() {
			boxW = Math.min(WIDTH, width - 20);
			lines = font.split(PanneauClient.fmt(data.texte()), boxW - PAD * 2);
			int buttons = data.boutons().size();
			boxH = PAD + font.lineHeight + 6 + lines.size() * (font.lineHeight + 2) + (buttons == 0 ? 0 : 6 + buttons * (BUTTON_H + 2)) + PAD;
			boxX = (width - boxW) / 2;
			boxY = Math.max(4, height - boxH - 10);
			int by = boxY + boxH - PAD - buttons * (BUTTON_H + 2) + 2;
			for (NpcDialoguePayload.Bouton b : data.boutons()) {
				Button button = new ChoiceButton(PanneauClient.fmt(b.label()), x -> choose(b.commande()));
				button.setX(boxX + PAD);
				button.setY(by);
				button.setWidth(boxW - PAD * 2);
				addRenderableWidget(button);
				by += BUTTON_H + 2;
			}
		}

		private void choose(String command) {
			if (command != null && minecraft.player != null) minecraft.player.connection.sendCommand(command);
			remember(minecraft);
			onClose();
		}

		@Override
		public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) { }

		@Override
		public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			g.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xE00E0E14);
			g.fillGradient(boxX, boxY, boxX + boxW, boxY + 2, 0xFF000000 | ORANGE, 0xFF000000 | RED);
			g.fill(boxX, boxY + boxH - 1, boxX + boxW, boxY + boxH, 0x33000000);
			int y = boxY + PAD;
			g.drawString(font, PanneauClient.fmt(data.titre()).getVisualOrderText(), boxX + PAD, y, 0xFF000000 | ORANGE, true);
			y += font.lineHeight + 6;
			for (FormattedCharSequence line : lines) {
				g.drawString(font, line, boxX + PAD, y, 0xFFFFFFFF, true);
				y += font.lineHeight + 2;
			}
			super.render(g, mouseX, mouseY, partialTick);
		}
	}
}
