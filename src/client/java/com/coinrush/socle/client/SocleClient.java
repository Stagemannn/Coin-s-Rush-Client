package com.coinrush.socle.client;

import com.coinrush.commun.BalanceSyncPayload;
import com.coinrush.commun.Commun;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;

/** Répond à la vérification de version et garde le solde reçu ; l'affichage est dans le panneau. */
public final class SocleClient implements ClientModInitializer {
	private static long balance;

	/** Dernier solde reçu du serveur pour le joueur local, en unités. */
	public static long balance() {
		return balance;
	}

	@Override
	public void onInitializeClient() {
		Commun.init();
		ClientLoginNetworking.registerGlobalReceiver(Commun.VERSION_CHANNEL, (client, handler, buf, callbacks) -> {
			FriendlyByteBuf response = PacketByteBufs.create();
			response.writeUtf(Commun.version());
			return CompletableFuture.completedFuture(response);
		});
		ClientPlayNetworking.registerGlobalReceiver(BalanceSyncPayload.TYPE, (payload, context) -> balance = payload.units());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> balance = 0);
	}
}
