package com.coinrush.options.client;

import com.coinrush.panneau.client.PanneauClient;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Options de Coin Rush, ouvertes depuis le menu Échap : réglages du panneau. */
public final class CoinRushOptionsScreen extends Screen {
	private final Screen parent;
	private Button keyButton;
	private boolean listening;

	public CoinRushOptionsScreen(Screen parent) {
		super(Component.literal("Coin's Rush Options"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int x = width / 2 - 100, y = height / 2 - 30;
		addRenderableWidget(Button.builder(modeLabel(), b -> {
			PanneauClient.compact(!PanneauClient.compact());
			b.setMessage(modeLabel());
		}).bounds(x, y, 200, 20).build());
		keyButton = addRenderableWidget(Button.builder(keyLabel(), b -> {
			listening = true;
			b.setMessage(Component.literal("> appuyez sur une touche <"));
		}).bounds(x, y + 24, 200, 20).build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(x, y + 60, 200, 20).build());
	}

	private static Component modeLabel() {
		return Component.literal("Panneau : " + (PanneauClient.compact() ? "Réduit" : "Complet"));
	}

	private static Component keyLabel() {
		return Component.literal("Réduire / agrandir : ").append(PanneauClient.TOGGLE.getTranslatedKeyMessage());
	}

	/** Même touche que dans Commandes, enregistrée dans options.txt. Échap la désassigne. */
	private void bind(InputConstants.Key key) {
		PanneauClient.TOGGLE.setKey(key);
		KeyMapping.resetMapping();
		minecraft.options.save();
		listening = false;
		keyButton.setMessage(keyLabel());
	}

	@Override
	public boolean keyPressed(int key, int scanCode, int modifiers) {
		if (!listening) return super.keyPressed(key, scanCode, modifiers);
		bind(key == 256 ? InputConstants.UNKNOWN : InputConstants.getKey(key, scanCode));
		return true;
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (!listening) return super.mouseClicked(mx, my, button);
		bind(InputConstants.Type.MOUSE.getOrCreate(button));
		return true;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		g.drawCenteredString(font, title, width / 2, height / 2 - 56, 0xFFFFFFFF);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return !listening; // pendant la saisie, Échap désassigne la touche
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}
}
