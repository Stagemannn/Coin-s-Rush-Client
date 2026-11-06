package com.coinrush.panneau.client;

import com.coinrush.commun.CamoPayload;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

/** Joueurs sous /camo, reçus du serveur ({@link CamoPayload}) : leur skin devient celui de Mr le Directeur (voir PlayerSkinMixin). */
public final class CamoClient {
	private static final ResourceLocation CAPE = ResourceLocation.fromNamespaceAndPath("coinrush", "textures/entity/cape_directeur.png");
	public static final PlayerSkin SKIN = new PlayerSkin(ResourceLocation.fromNamespaceAndPath("coinrush", "textures/entity/directeur.png"),
			null, CAPE, CAPE, PlayerSkin.Model.WIDE, true);
	private static volatile Set<UUID> camo = Set.of();

	private CamoClient() { }

	public static boolean camo(UUID joueur) {
		return camo.contains(joueur);
	}

	static void init() {
		ClientPlayNetworking.registerGlobalReceiver(CamoPayload.TYPE, (payload, context) -> camo = Set.copyOf(payload.joueurs()));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> camo = Set.of());
	}
}
