package com.coinrush.commun;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/**
 * Items et blocs du mod, enregistrés à l'identique des deux côtés, et l'onglet créatif « Coin's Rush » qui les regroupe
 * (tout ce qui passe par {@link Registre#item} et {@link Registre#bloc} y entre). Modèles et textures : temporaires, en
 * attendant les fichiers du propriétaire (voir assets/coinrush).
 */
public final class Contenu {
	public static Block ROULETTE;
	public static Item ROULETTE_ITEM;
	/** Jeton gagné au Resort ; le Changeur le rachète quelques centimes après la fermeture de la zone. */
	public static Item JETON_RESORT;
	public static CreativeModeTab ONGLET;

	private Contenu() { }

	static void init() {
		ROULETTE = Registre.bloc("roulette", RouletteBlock::new, p -> p.strength(2.0f, 6.0f).sound(SoundType.WOOD).noOcclusion());
		ROULETTE_ITEM = ROULETTE.asItem();
		JETON_RESORT = Registre.item("jeton_resort", Item::new);
		ONGLET = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath("coinrush", "coinrush"),
				FabricItemGroup.builder().title(Component.translatable("itemGroup.coinrush")).icon(() -> new ItemStack(ROULETTE_ITEM))
						.displayItems((contexte, sortie) -> Registre.ITEMS.forEach(sortie::accept)).build());
	}
}
