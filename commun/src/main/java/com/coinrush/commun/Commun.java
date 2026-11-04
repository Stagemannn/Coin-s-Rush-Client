package com.coinrush.commun;

import com.coinrush.commun.temp.ContenuTemporaire;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;

/** Ce que le serveur et le client enregistrent à l'identique : réseau, items, entités (voir {@link Registre}). */
public final class Commun {
	/** Requête de connexion : le serveur demande sa version au client, qui répond par une chaîne. */
	public static final ResourceLocation VERSION_CHANNEL = ResourceLocation.fromNamespaceAndPath("coinrush", "version");

	private static boolean done;

	private Commun() { }

	/** À appeler en tête de l'initialisation de chaque mod ; sans effet au deuxième appel. */
	public static synchronized void init() {
		if (done) return;
		done = true;
		PayloadTypeRegistry.playS2C().register(BalanceSyncPayload.TYPE, BalanceSyncPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(PanelPayload.TYPE, PanelPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(CinematicPayload.TYPE, CinematicPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(CinematicStopPayload.TYPE, CinematicStopPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(CinematicSkipPayload.TYPE, CinematicSkipPayload.CODEC);
		ContenuTemporaire.init();
	}

	/** Version de ce mod : commun/version.txt, écrite dans le jar à la compilation. */
	public static String version() {
		return FabricLoader.getInstance().getModContainer("coinrush").orElseThrow().getMetadata().getVersion().getFriendlyString();
	}
}
