package com.coinrush.commun;

import java.util.function.Function;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Item;

/**
 * Enregistre items et entités sous {@code coinrush:<nom>}, identiquement des deux côtés : à appeler depuis {@link Commun#init()},
 * en gardant le résultat dans un champ statique d'une classe de contenu. Le rendu est côté client : modèle JSON d'item sous
 * {@code assets/coinrush/items} et {@code models/item}, entité via {@code EntityRendererRegistry}.
 */
public final class Registre {
	private Registre() { }

	private static ResourceLocation id(String name) {
		return ResourceLocation.fromNamespaceAndPath("coinrush", name);
	}

	public static <T extends Item> T item(String name, Function<Item.Properties, T> factory) {
		ResourceLocation id = id(name);
		return Registry.register(BuiltInRegistries.ITEM, id, factory.apply(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))));
	}

	public static <T extends Entity> EntityType<T> entite(String name, EntityType.EntityFactory<T> factory, MobCategory category, float width, float height) {
		ResourceLocation id = id(name);
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, id,
				EntityType.Builder.of(factory, category).sized(width, height).build(ResourceKey.create(Registries.ENTITY_TYPE, id)));
	}

	/** Entité vivante : ses attributs sont enregistrés avec elle. */
	public static <T extends LivingEntity> EntityType<T> creature(String name, EntityType.EntityFactory<T> factory, MobCategory category,
			float width, float height, AttributeSupplier.Builder attributes) {
		EntityType<T> type = entite(name, factory, category, width, height);
		FabricDefaultAttributeRegistry.register(type, attributes);
		return type;
	}
}
