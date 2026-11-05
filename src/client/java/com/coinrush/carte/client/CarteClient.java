package com.coinrush.carte.client;

import com.coinrush.commun.CartePayload;
import com.coinrush.commun.Commun;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Carte en jeu : garde l'adresse du panel joueurs reçue à la connexion et ajoute le bouton « Map » à l'écran d'inventaire
 * (placé par InventaireClient, sous les boutons Agenda et Quêtes).
 */
public final class CarteClient implements ClientModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger("coinrush");

	private static volatile String url;

	@Override
	public void onInitializeClient() {
		Commun.init();
		ClientPlayNetworking.registerGlobalReceiver(CartePayload.TYPE, (payload, ctx) -> {
			try {
				payload.validate();
				url = payload.url().isEmpty() ? null : payload.url();
				ProfilClient.configurer(url);
			} catch (IllegalArgumentException e) {
				LOGGER.warn("Adresse de la carte refusée : {}", e.getMessage());
				url = null;
				ProfilClient.configurer(null);
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			url = null;
			ProfilClient.configurer(null);
		});
	}

	/** La carte est-elle joignable sur ce serveur ? */
	public static boolean disponible() {
		return url != null;
	}

	/** Ouvre la carte depuis l'écran d'inventaire, qui redevient l'écran parent. Appelé par le bouton « Map » d'InventaireClient. */
	public static void ouvrir(net.minecraft.client.gui.screens.Screen parent) {
		String u = url;
		if (u != null) Minecraft.getInstance().setScreen(new CarteScreen(parent, u));
	}
}
