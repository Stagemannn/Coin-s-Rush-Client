package com.coinrush.commun.temp;

import com.coinrush.commun.Registre;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;

/**
 * TEMPORAIRE, À SUPPRIMER avec tout le dossier temp/, les fichiers temp_* sous assets/coinrush et l'entrée client correspondante :
 * sert seulement à prouver l'enregistrement et le rendu d'un item et d'une entité. Actif uniquement avec
 * -Dcoinrush.contenuTest=true, sur le client ET le serveur (sinon la synchronisation des registres échoue). Rien ne change en production.
 */
public final class ContenuTemporaire {
	public static Item ITEM;
	public static EntityType<EntiteTemporaire> ENTITE;

	private ContenuTemporaire() { }

	public static boolean actif() {
		return Boolean.getBoolean("coinrush.contenuTest");
	}

	public static void init() {
		if (!actif()) return;
		ITEM = Registre.item("temp_item", Item::new);
		ENTITE = Registre.creature("temp_entity", EntiteTemporaire::new, MobCategory.CREATURE, 0.8f, 1.0f, Mob.createMobAttributes());
	}
}
