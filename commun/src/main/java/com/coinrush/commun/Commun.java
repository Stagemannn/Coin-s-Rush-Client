package com.coinrush.commun;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;

/** Ce que le serveur et le client enregistrent à l'identique. Les items et entités s'ajouteront à {@link #init()}. */
public final class Commun {
	/** Requête de connexion : le serveur demande sa version au client, qui répond par une chaîne. */
	public static final ResourceLocation VERSION_CHANNEL = ResourceLocation.fromNamespaceAndPath("coinrush", "version");

	private Commun() { }

	/** À appeler une fois, en tête de l'initialisation de chaque mod. */
	public static void init() {
		PayloadTypeRegistry.playS2C().register(BalanceSyncPayload.TYPE, BalanceSyncPayload.CODEC);
	}

	/** Version de ce mod : commun/version.txt, écrite dans le jar à la compilation. */
	public static String version() {
		return FabricLoader.getInstance().getModContainer("coinrush").orElseThrow().getMetadata().getVersion().getFriendlyString();
	}
}
