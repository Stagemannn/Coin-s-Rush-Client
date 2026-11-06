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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

/**
 * Inventaire, en survie comme en créatif, à gauche de celui-ci : une ligne de trois boutons « Agenda », « Quêtes » et « Map »,
 * exactement de la largeur du profil, juste au-dessus de lui. Le livre de recettes décale l'inventaire : on suit sa position à chaque
 * image, et s'il est ouvert tout passe dans le coin de l'écran pour ne pas le recouvrir.
 */
public final class InventaireClient implements ClientModInitializer {
	private static final int H = 16, GAP = 2, HAUT = 8;

	@Override
	public void onInitializeClient() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof InventoryScreen) && !(screen instanceof CreativeModeInventoryScreen)) return;
			ContainerScreenAccessor a = (ContainerScreenAccessor) screen;
			Button map = Button.builder(Component.literal("Map"), b -> CarteClient.ouvrir(screen)).bounds(0, 0, 40, H).build();
			ProfilBox profil = new ProfilBox();
			List<Button> boutons = List.of(commande("Agenda", "agenda"), commande("Quêtes", "quests"), map);
			boutons.forEach(Screens.getButtons(screen)::add);
			ScreenEvents.beforeRender(screen).register((s, g, mx, my, delta) -> {
				map.active = CarteClient.disponible(); // pas de panel joueurs sur ce serveur : bouton grisé
				int x = gauche(a, s, ProfilBox.largeur(client.font)), w = ProfilBox.largeur(client.font), y = ligneBoutons(a, s);
				int bw = (w - 2 * GAP) / 3;
				for (int i = 0; i < boutons.size(); i++) {
					Button b = boutons.get(i);
					b.setPosition(x + i * (bw + GAP), y);
					b.setWidth(i == 2 ? w - 2 * (bw + GAP) : bw); // le dernier prend le reste : la ligne fait pile la largeur du profil
				}
			});
			ScreenEvents.afterRender(screen).register((s, g, mx, my, delta) -> {
				if (CarteClient.disponible()) profil.render(g, client.font, gauche(a, s, ProfilBox.largeur(client.font)), ligneBoutons(a, s) + H + GAP, false);
			});
			ScreenMouseEvents.allowMouseClick(screen).register((s, mx, my, bouton) -> !(CarteClient.disponible() && profil.clic(mx, my, bouton)));
		});
	}

	private static boolean livreOuvert(ContainerScreenAccessor a, Screen s) {
		return a.coinrush$leftPos() > (s.width - a.coinrush$imageWidth()) / 2 + 2;
	}

	/** Bord gauche de la colonne : à gauche de l'inventaire, ou dans le coin de l'écran si le livre de recettes est ouvert. */
	private static int gauche(ContainerScreenAccessor a, Screen s, int largeur) {
		return livreOuvert(a, s) ? 6 : Math.max(2, a.coinrush$leftPos() - GAP * 2 - largeur);
	}

	private static int ligneBoutons(ContainerScreenAccessor a, Screen s) {
		return livreOuvert(a, s) ? 6 : a.coinrush$topPos() + HAUT;
	}

	private static Button commande(String titre, String commande) {
		return Button.builder(Component.literal(titre), b -> {
			var c = Minecraft.getInstance();
			if (c.player != null) c.player.connection.sendCommand(commande);
		}).bounds(0, 0, 40, H).build();
	}
}
