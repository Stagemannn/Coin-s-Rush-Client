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
		PayloadTypeRegistry.playS2C().register(NpcDialoguePayload.TYPE, NpcDialoguePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(WaypointPayload.TYPE, WaypointPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(HatsPayload.TYPE, HatsPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(LocationPayload.TYPE, LocationPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(JourPayload.TYPE, JourPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(CartePayload.TYPE, CartePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(CinematiquePayload.TYPE, CinematiquePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(CinematiqueStopPayload.TYPE, CinematiqueStopPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(CinematiqueFiniePayload.TYPE, CinematiqueFiniePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(CinematiqueMediasPayload.TYPE, CinematiqueMediasPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(CinematiqueMediasDemandePayload.TYPE, CinematiqueMediasDemandePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(CinematiqueMediaMorceauPayload.TYPE, CinematiqueMediaMorceauPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ClassementPayload.TYPE, ClassementPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(CamoPayload.TYPE, CamoPayload.CODEC);
		Contenu.init();
		ContenuTemporaire.init();
	}

	/** Version de ce mod : commun/version.txt, écrite dans le jar à la compilation. */
	public static String version() {
		return FabricLoader.getInstance().getModContainer("coinrush").orElseThrow().getMetadata().getVersion().getFriendlyString();
	}
}
