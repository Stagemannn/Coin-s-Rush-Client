package com.coinrush.commun.temp;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

/** TEMPORAIRE : entité de test, immobile. Voir {@link ContenuTemporaire}. */
public class EntiteTemporaire extends PathfinderMob {
	public EntiteTemporaire(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
	}
}
