package com.coinrush.inventaire.client;

import com.coinrush.carte.client.CarteClient;
import com.coinrush.carte.client.ProfilBox;
import com.coinrush.carte.client.mixin.ContainerScreenAccessor;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

/** Colonne de boutons à droite de l'inventaire, en survie comme en créatif : « Agenda », « Quêtes » et « Map ». Encadré du profil à gauche. */
public final class InventaireClient implements ClientModInitializer {
	private static final int W = 50, H = 16, GAP = 4;

	@Override
	public void onInitializeClient() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof InventoryScreen) && !(screen instanceof CreativeModeInventoryScreen)) return;
			ContainerScreenAccessor a = (ContainerScreenAccessor) screen;
			Button map = Button.builder(Component.literal("Map"), b -> CarteClient.ouvrir(screen)).bounds(0, 0, W, H).build();
			map.active = CarteClient.disponible(); // pas de panel joueurs sur ce serveur : bouton grisé
			ProfilBox profil = new ProfilBox();
			List<Button> boutons = List.of(commande("Agenda", "agenda"), commande("Quêtes", "quests"), map);
			boutons.forEach(Screens.getButtons(screen)::add);
			// Le livre de recettes décale l'inventaire : on suit sa position à chaque image.
			ScreenEvents.beforeRender(screen).register((s, g, mx, my, delta) -> {
				for (int i = 0; i < boutons.size(); i++)
					boutons.get(i).setPosition(a.coinrush$leftPos() + a.coinrush$imageWidth() + GAP, a.coinrush$topPos() + 8 + i * (H + GAP));
			});
			// Profil à gauche de l'inventaire ; si le livre de recettes est ouvert (inventaire décalé), dans le coin de l'écran pour ne pas le recouvrir.
			ScreenEvents.afterRender(screen).register((s, g, mx, my, delta) -> {
				if (!CarteClient.disponible()) return;
				boolean livre = a.coinrush$leftPos() > (s.width - a.coinrush$imageWidth()) / 2 + 2;
				if (livre) profil.render(g, client.font, 6, 6, false);
				else profil.render(g, client.font, a.coinrush$leftPos() - GAP, a.coinrush$topPos() + 8, true);
			});
			ScreenMouseEvents.allowMouseClick(screen).register((s, mx, my, bouton) -> !(CarteClient.disponible() && profil.clic(mx, my, bouton)));
		});
	}

	private static Button commande(String titre, String commande) {
		return Button.builder(Component.literal(titre), b -> {
			var c = Minecraft.getInstance();
			if (c.player != null) c.player.connection.sendCommand(commande);
		}).bounds(0, 0, W, H).build();
	}
}
