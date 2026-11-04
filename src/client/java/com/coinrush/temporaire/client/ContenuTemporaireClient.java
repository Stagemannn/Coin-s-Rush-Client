package com.coinrush.temporaire.client;

import com.coinrush.commun.Commun;
import com.coinrush.commun.temp.ContenuTemporaire;
import com.coinrush.commun.temp.EntiteTemporaire;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;

/**
 * TEMPORAIRE, À SUPPRIMER avec ContenuTemporaire : rendu de l'entité de test avec un modèle en code et une texture du mod.
 * Montre le chemin à suivre pour une vraie entité : couche de modèle, rendu, texture sous assets/coinrush/textures/entity.
 */
public final class ContenuTemporaireClient implements ClientModInitializer {
	private static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("coinrush", "temp_entity"), "main");
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("coinrush", "textures/entity/temp_entity.png");

	@Override
	public void onInitializeClient() {
		Commun.init();
		if (!ContenuTemporaire.actif()) return;
		EntityModelLayerRegistry.registerModelLayer(LAYER, ContenuTemporaireClient::layer);
		EntityRendererRegistry.register(ContenuTemporaire.ENTITE, Renderer::new);
	}

	private static LayerDefinition layer() {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8), PartPose.offset(0, 24, 0));
		mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 16).addBox(-2, -6, -2, 4, 4, 4), PartPose.offset(0, 8, 0));
		return LayerDefinition.create(mesh, 32, 32);
	}

	private static final class Model extends EntityModel<LivingEntityRenderState> {
		Model(ModelPart root) {
			super(root);
		}
	}

	private static final class Renderer extends MobRenderer<EntiteTemporaire, LivingEntityRenderState, Model> {
		Renderer(EntityRendererProvider.Context context) {
			super(context, new Model(context.bakeLayer(LAYER)), 0.4f);
		}

		@Override
		public LivingEntityRenderState createRenderState() {
			return new LivingEntityRenderState();
		}

		@Override
		public ResourceLocation getTextureLocation(LivingEntityRenderState state) {
			return TEXTURE;
		}
	}
}
