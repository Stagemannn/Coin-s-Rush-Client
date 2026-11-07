package com.coinrush.commun;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemLore;
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
	/** Items de zone (4 par zone : facile, moyen, hard, trophée de boss), par id : sans craft ni usage, gagnés en quête puis revendus. */
	public static final Map<String, Item> ITEMS_ZONE = new LinkedHashMap<>();
	public static CreativeModeTab ONGLET;

	private Contenu() { }

	/** Item simple (pile de 64) : nom doré (rouge pour un trophée de boss) et infobulle d'une ligne, tous deux lus dans le fichier de langue. */
	private static Item objet(String nom, boolean boss) {
		Component description = Component.translatable("item.coinrush." + nom + ".desc").withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(false));
		// Couleur sur un enfant : la rareté recolore la racine du nom, pas ses enfants.
		Component titre = Component.empty().append(Component.translatable("item.coinrush." + nom).withStyle(boss ? ChatFormatting.RED : ChatFormatting.GOLD));
		return Registre.item(nom, p -> new Item(p.rarity(Rarity.COMMON)
				.component(DataComponents.ITEM_NAME, titre)
				.component(DataComponents.LORE, new ItemLore(List.of(description)))));
	}

	static void init() {
		ROULETTE = Registre.bloc("roulette", RouletteBlock::new, p -> p.strength(2.0f, 6.0f).sound(SoundType.WOOD).noOcclusion());
		ROULETTE_ITEM = ROULETTE.asItem();
		JETON_RESORT = objet("jeton_resort", false);
		for (String zone : List.of("casino", "resort", "mall", "theatre")) {
			for (String niveau : List.of("facile", "moyen", "hard", "boss")) ITEMS_ZONE.put(zone + "_" + niveau, objet(zone + "_" + niveau, niveau.equals("boss")));
		}
		ONGLET = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath("coinrush", "coinrush"),
				FabricItemGroup.builder().title(Component.translatable("itemGroup.coinrush")).icon(() -> new ItemStack(ROULETTE_ITEM))
						.displayItems((contexte, sortie) -> Registre.ITEMS.forEach(sortie::accept)).build());
	}
}
