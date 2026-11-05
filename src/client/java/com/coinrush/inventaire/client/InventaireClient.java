package com.coinrush.inventaire.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

/** Deux petits boutons à droite de l'inventaire : « Agenda » (/agenda) et « Quêtes » (/quests). Le serveur fait le reste. */
public final class InventaireClient implements ClientModInitializer {
	private static final int W = 50, H = 16, GAP = 4;

	@Override
	public void onInitializeClient() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof InventoryScreen inv)) return;
			Button agenda = bouton("Agenda", "agenda");
			Button quetes = bouton("Quêtes", "quests");
			Screens.getButtons(screen).add(agenda);
			Screens.getButtons(screen).add(quetes);
			// Le livre de recettes décale l'inventaire : on suit sa position à chaque image.
			ScreenEvents.beforeRender(screen).register((s, g, mx, my, delta) -> {
				var r = ((AbstractContainerScreen<?>) inv).getRectangle();
				int x = r.left() + r.width() + GAP;
				agenda.setPosition(x, r.top() + 8);
				quetes.setPosition(x, r.top() + 8 + H + GAP);
			});
		});
	}

	private static Button bouton(String titre, String commande) {
		return Button.builder(Component.literal(titre), b -> {
			var c = net.minecraft.client.Minecraft.getInstance();
			if (c.player != null) c.player.connection.sendCommand(commande);
		}).bounds(0, 0, W, H).build();
	}
}
