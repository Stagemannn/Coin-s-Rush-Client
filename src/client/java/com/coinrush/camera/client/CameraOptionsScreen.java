package com.coinrush.camera.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Écran d'options de CoinRush, ouvert depuis les options du jeu : le décalage latéral de la caméra en 3e personne. */
public final class CameraOptionsScreen extends Screen {
	private final Screen parent;

	public CameraOptionsScreen(Screen parent) {
		super(Component.literal("CoinRush"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		addRenderableWidget(new AbstractSliderButton(width / 2 - 140, height / 2 - 10, 280, 20, Component.empty(), (CameraOptions.cameraOffset + 2) / 4) {
			{
				updateMessage();
			}

			@Override
			protected void updateMessage() {
				double v = Math.round((value * 4 - 2) * 10) / 10.0;
				setMessage(Component.literal("Caméra 3e personne : " + (v == 0 ? "centrée" : v < 0 ? "à gauche " + (-v) : "à droite " + v)));
			}

			@Override
			protected void applyValue() {
				CameraOptions.cameraOffset = Math.round((value * 4 - 2) * 10) / 10.0;
			}
		});
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(width / 2 - 100, height / 2 + 30, 200, 20).build());
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		g.drawCenteredString(font, title, width / 2, height / 2 - 50, 0xFFFFFFFF);
	}

	@Override
	public void removed() {
		CameraOptions.save();
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}
}
