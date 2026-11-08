package com.coinrush.options.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;

/** Ajoute « Coin's Rush Options » en haut à gauche du menu Échap. */
public final class OptionsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof PauseScreen) {
				Screens.getButtons(screen).add(Button.builder(Component.literal("Coin's Rush Options"), b -> client.setScreen(new CoinRushOptionsScreen(screen)))
						.bounds(6, 6, 130, 20).build());
			}
		});
	}
}
