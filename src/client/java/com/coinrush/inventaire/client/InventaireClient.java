package com.coinrush.inventaire.client;

import com.coinrush.carte.client.CarteClient;
import com.coinrush.carte.client.mixin.ContainerScreenAccessor;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

/** Colonne de boutons à droite de l'inventaire, en survie comme en créatif : « Agenda », « Quêtes » et « Map ». */
public final class InventaireClient implements ClientModInitializer {
	private static final int W = 50, H = 16, GAP = 4;

	@Override
	public void onInitializeClient() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof InventoryScreen) && !(screen instanceof CreativeModeInventoryScreen)) return;
			ContainerScreenAccessor a = (ContainerScreenAccessor) screen;
			Button map = Button.builder(Component.literal("Map"), b -> CarteClient.ouvrir(screen)).bounds(0, 0, W, H).build();
			map.active = CarteClient.disponible(); // pas de panel joueurs sur ce serveur : bouton grisé
			List<Button> boutons = List.of(commande("Agenda", "agenda"), commande("Quêtes", "quests"), map);
			boutons.forEach(Screens.getButtons(screen)::add);
			// Le livre de recettes décale l'inventaire : on suit sa position à chaque image.
			ScreenEvents.beforeRender(screen).register((s, g, mx, my, delta) -> {
				for (int i = 0; i < boutons.size(); i++)
					boutons.get(i).setPosition(a.coinrush$leftPos() + a.coinrush$imageWidth() + GAP, a.coinrush$topPos() + 8 + i * (H + GAP));
			});
		});
	}

	private static Button commande(String titre, String commande) {
		return Button.builder(Component.literal(titre), b -> {
			var c = Minecraft.getInstance();
			if (c.player != null) c.player.connection.sendCommand(commande);
		}).bounds(0, 0, W, H).build();
	}
}
