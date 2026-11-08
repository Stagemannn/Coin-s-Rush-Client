package com.coinrush.options.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Options de Coin Rush, ouvertes depuis le menu Échap. Vide pour l'instant : les réglages viendront ici. */
public final class CoinRushOptionsScreen extends Screen {
	private final Screen parent;

	public CoinRushOptionsScreen(Screen parent) {
		super(Component.literal("Coin's Rush Options"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(width / 2 - 100, height / 2 + 12, 200, 20).build());
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		g.drawCenteredString(font, title, width / 2, height / 2 - 50, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}
}
