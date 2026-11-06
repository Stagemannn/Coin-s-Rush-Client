package com.coinrush.chapeaux.client;

import com.coinrush.commun.Commun;
import com.coinrush.commun.HatsPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chapeaux cosmétiques : le serveur envoie des couples joueur / identifiant ({@link HatsPayload}). Une couche sur le rendu des joueurs
 * dessine le modèle {@code coinrush:hat/<id>} (item player_head de custom_model_data « cr_hat_<id> ») sur leur tête, comme un casque :
 * avec la transformation « head » du modèle, par-dessus l'armure sans la remplacer. Un identifiant inconnu du client est ignoré.
 */
public final class ChapeauxClient implements ClientModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger("coinrush");
	/** Chapeaux dont le modèle est dans le mod : sans cela, l'item afficherait une tête vide. */
	private static final Set<String> KNOWN = Set.of("aviator", "axolotl_hood", "bloom_crown", "bumblebee", "bunny_beanie", "chef", "corsair", "cozy_pom",
			"dusty_cowboy", "fiesta", "fox_ears", "frog_bucket", "frost_diadem", "jellyfish", "little_dragon", "midnight_wizard", "miner", "panda_cap",
			"parcel", "retro_tv", "royal_crown", "rubber_duck", "sea_captain", "steam_topper", "straw_garden", "street_cap", "sushi", "toadstool",
			"trailblazer", "ufo", "directeur_topper", "chicken_hat", "diving_helmet", "flight_hat", "military_helmet", "motorcycle_helmet", "pot_hat");
	private static final Map<UUID, String> HATS = new HashMap<>();
	private static final Map<String, ItemStack> STACKS = new HashMap<>();
	private static final ItemStackRenderState STATE = new ItemStackRenderState();
	/** Hauteur (blocs) dont le chapeau monte quand le joueur porte un casque, pour limiter les chevauchements. */
	private static final float HELMET_LIFT = 0.05F;

	@Override
	public void onInitializeClient() {
		Commun.init();
		ClientPlayNetworking.registerGlobalReceiver(HatsPayload.TYPE, (payload, context) -> {
			try {
				payload.validate();
			} catch (IllegalArgumentException e) {
				LOGGER.warn("Chapeaux refusés : {}", e.getMessage());
				return;
			}
			payload.hats().forEach((uuid, id) -> {
				if (id.isEmpty()) HATS.remove(uuid);
				else if (KNOWN.contains(id)) HATS.put(uuid, id);
				else LOGGER.warn("Chapeau inconnu ignoré : {}", id);
			});
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(HATS::clear));
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, ctx) -> {
			if (type == EntityType.PLAYER) {
				@SuppressWarnings("unchecked")
				var parent = (RenderLayerParent<PlayerRenderState, PlayerModel>) renderer;
				helper.register(new HatLayer(parent));
			}
		});
	}

	private static ItemStack stack(String id) {
		return STACKS.computeIfAbsent(id, k -> {
			ItemStack st = new ItemStack(Items.PLAYER_HEAD);
			st.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of("cr_hat_" + k), List.of()));
			return st;
		});
	}

	private static final class HatLayer extends RenderLayer<PlayerRenderState, PlayerModel> {
		HatLayer(RenderLayerParent<PlayerRenderState, PlayerModel> parent) {
			super(parent);
		}

		@Override
		public void render(PoseStack poseStack, MultiBufferSource buffers, int light, PlayerRenderState state, float yRot, float xRot) {
			if (state.isInvisible || state.isSpectator || HATS.isEmpty()) return;
			var mc = Minecraft.getInstance();
			if (mc.level == null || !(mc.level.getEntity(state.id) instanceof Player player)) return;
			String id = HATS.get(player.getUUID());
			if (id == null) return;
			// Pas en vraie première personne ; mais oui dans l'aperçu d'un écran (inventaire) et quand la caméra n'est pas le joueur (cinématique).
			if (player == mc.player && mc.screen == null && mc.getCameraEntity() == player && mc.options.getCameraType().isFirstPerson()) return;
			mc.getItemModelResolver().updateForTopItem(STATE, stack(id), ItemDisplayContext.HEAD, mc.level, player, state.id);
			if (STATE.isEmpty()) return;
			poseStack.pushPose();
			getParentModel().root().translateAndRotate(poseStack);
			getParentModel().head.translateAndRotate(poseStack); // suit la tête (regard, sneak)
			if (!state.headEquipment.isEmpty()) poseStack.translate(0, -HELMET_LIFT, 0); // repère de la tête : y vers le bas
			CustomHeadLayer.translateToHead(poseStack, CustomHeadLayer.Transforms.DEFAULT);
			STATE.render(poseStack, buffers, light, OverlayTexture.NO_OVERLAY);
			poseStack.popPose();
		}
	}
}
