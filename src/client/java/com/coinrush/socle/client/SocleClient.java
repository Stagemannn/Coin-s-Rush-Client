package com.coinrush.socle.client;

import com.coinrush.commun.BalanceSyncPayload;
import com.coinrush.commun.Coins;
import com.coinrush.commun.Commun;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Reçoit le solde et l'affiche en haut à gauche, en attendant le panneau final. */
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
		HudElementRegistry.addLast(ResourceLocation.fromNamespaceAndPath("coinrush", "solde"), (graphics, delta) ->
				graphics.drawString(Minecraft.getInstance().font, "Coins : " + Coins.format(balance), 4, 4, 0xFFFFD700));
	}
}
