package com.coinrush.camera.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;

/** Charge les options de caméra, anime la caméra libre et ajoute « Coin Rush… » en haut à gauche de l'écran des options du jeu. */
public final class CameraClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		CameraOptions.load();
		ClientTickEvents.START_CLIENT_TICK.register(FreeCamera::startTick);
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof OptionsScreen) {
				Screens.getButtons(screen).add(Button.builder(Component.literal("Coin Rush…"), b -> client.setScreen(new CoinRushOptionsScreen(screen)))
						.bounds(6, 6, 90, 20).build());
			}
		});
	}
}
