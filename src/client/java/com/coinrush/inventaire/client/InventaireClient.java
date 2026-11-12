package com.coinrush.inventaire.client;

import com.coinrush.commun.CartePayload;
import com.coinrush.commun.Commun;
import com.coinrush.inventaire.client.mixin.ContainerScreenAccessor;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inventaire, en survie comme en créatif, à gauche de celui-ci : une ligne de deux boutons « Agenda » et « Quêtes », exactement
 * de la largeur du profil, juste au-dessus de lui. Le livre de recettes décale l'inventaire : on suit sa position à chaque
 * image, et s'il est ouvert tout passe dans le coin de l'écran pour ne pas le recouvrir. Le profil se lit sur le panel joueurs,
 * dont l'adresse arrive du serveur à la connexion ({@link CartePayload}) ; sans elle, le profil n'est pas affiché.
 */
public final class InventaireClient implements ClientModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger("coinrush");
	private static final int H = 16, GAP = 2, HAUT = 8;

	private static volatile String url;

	@Override
	public void onInitializeClient() {
		Commun.init();
		ClientPlayNetworking.registerGlobalReceiver(CartePayload.TYPE, (payload, ctx) -> {
			try {
				payload.validate();
				url = payload.url().isEmpty() ? null : payload.url();
			} catch (IllegalArgumentException e) {
				LOGGER.warn("Adresse du panel refusée : {}", e.getMessage());
				url = null;
			}
			ProfilClient.configurer(url);
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			url = null;
			ProfilClient.configurer(null);
		});
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof InventoryScreen) && !(screen instanceof CreativeModeInventoryScreen)) return;
			ContainerScreenAccessor a = (ContainerScreenAccessor) screen;
			ProfilBox profil = new ProfilBox();
			List<Button> boutons = List.of(commande("Agenda", "agenda"), commande("Quêtes", "quests"));
			boutons.forEach(Screens.getButtons(screen)::add);
			ScreenEvents.beforeRender(screen).register((s, g, mx, my, delta) -> {
				int x = gauche(a, s, ProfilBox.largeur(client.font)), w = ProfilBox.largeur(client.font), y = ligneBoutons(a, s);
				int bw = (w - GAP) / 2;
				for (int i = 0; i < boutons.size(); i++) {
					Button b = boutons.get(i);
					b.setPosition(x + i * (bw + GAP), y);
					b.setWidth(i == 1 ? w - (bw + GAP) : bw); // le dernier prend le reste : la ligne fait pile la largeur du profil
				}
			});
			ScreenEvents.afterRender(screen).register((s, g, mx, my, delta) -> {
				if (url != null) profil.render(g, client.font, gauche(a, s, ProfilBox.largeur(client.font)), ligneBoutons(a, s) + H + GAP);
			});
			ScreenMouseEvents.allowMouseClick(screen).register((s, mx, my, bouton) -> !(url != null && profil.clic(mx, my, bouton)));
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
