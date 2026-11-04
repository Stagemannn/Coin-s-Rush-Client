package com.coinrush.commun;

import java.math.BigDecimal;
import java.util.OptionalLong;

/** Montants en unités internes : {@value #UNITS_PER_COIN} unités = 1 Coin. */
public final class Coins {
	public static final long UNITS_PER_COIN = 1000;

	private Coins() { }

	/** Lit un montant saisi en Coins ("12" ou "0.196"), au plus trois décimales, sans signe. Vide si invalide. */
	public static OptionalLong parse(String coins) {
		// 15 chiffres entiers au plus : le résultat tient toujours dans un long, donc longValueExact ne peut pas lever d'exception
		if (!coins.matches("\\d{1,15}(\\.\\d{1,3})?")) return OptionalLong.empty();
		return OptionalLong.of(new BigDecimal(coins).multiply(BigDecimal.valueOf(UNITS_PER_COIN)).longValueExact());
	}

	/** Affiche des unités en Coins, sans zéros inutiles : "12", "1.5" ou "0.196". */
	public static String format(long units) {
		return BigDecimal.valueOf(units).divide(BigDecimal.valueOf(UNITS_PER_COIN)).stripTrailingZeros().toPlainString();
	}
}
