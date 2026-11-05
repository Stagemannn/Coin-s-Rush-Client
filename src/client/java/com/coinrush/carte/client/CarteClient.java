package com.coinrush.carte.client;

import com.coinrush.carte.client.mixin.ContainerScreenAccessor;
import com.coinrush.commun.CartePayload;
import com.coinrush.commun.Commun;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Carte en jeu : garde l'adresse du panel joueurs reçue à la connexion et ajoute le bouton « Map » à l'écran d'inventaire
 * (appelé par {@code InventaireMixin}), sous les boutons Agenda et Quêtes.
 */
public final class CarteClient implements ClientModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger("coinrush");
	/** Même forme que les boutons Agenda et Quêtes, à droite de l'inventaire ; « Map » est le troisième de la colonne. */
	static final int LARGEUR = 50, HAUTEUR = 16, ECART = 4, RANG = 2; // aligné sur les boutons Agenda et Quêtes (InventaireClient)

	private static volatile String url;

	@Override
	public void onInitializeClient() {
		Commun.init();
		ClientPlayNetworking.registerGlobalReceiver(CartePayload.TYPE, (payload, ctx) -> {
			try {
				payload.validate();
				url = payload.url().isEmpty() ? null : payload.url();
			} catch (IllegalArgumentException e) {
				LOGGER.warn("Adresse de la carte refusée : {}", e.getMessage());
				url = null;
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> url = null);
	}

	/** Appelé à chaque construction de l'écran d'inventaire. */
	public static void ajouterBouton(InventoryScreen ecran) {
		ContainerScreenAccessor a = (ContainerScreenAccessor) ecran;
		int x = a.coinrush$leftPos() + a.coinrush$imageWidth() + 4, y = a.coinrush$topPos() + 8 + RANG * (HAUTEUR + ECART);
		Button b = Button.builder(Component.literal("Map"), bouton -> {
			String u = url;
			if (u != null) Minecraft.getInstance().setScreen(new CarteScreen(ecran, u));
		}).bounds(x, y, LARGEUR, HAUTEUR).build();
		b.active = url != null; // pas de panel joueurs sur ce serveur : bouton grisé
		Screens.getButtons(ecran).add(b);
		// Le livre de recettes décale l'inventaire : on suit sa position, comme Agenda et Quêtes.
		ScreenEvents.beforeRender(ecran).register((s, g, mx, my, delta) ->
				b.setPosition(a.coinrush$leftPos() + a.coinrush$imageWidth() + 4, a.coinrush$topPos() + 8 + RANG * (HAUTEUR + ECART)));
	}
}
