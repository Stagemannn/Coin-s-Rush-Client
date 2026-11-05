package com.coinrush.carte.client;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CarteMathTest {
	@Test
	void coordonneesValides() {
		assertArrayEquals(new int[] {120, -45}, CarteMath.coordonnees("120 -45"));
		assertArrayEquals(new int[] {120, -45}, CarteMath.coordonnees(" 120 , -45 "));
		assertArrayEquals(new int[] {-1, 0}, CarteMath.coordonnees("-1;0"));
		assertArrayEquals(new int[] {30_000_000, -30_000_000}, CarteMath.coordonnees("30000000 -30000000"));
	}

	@Test
	void coordonneesInvalides() {
		for (String s : new String[] {null, "", "12", "a b", "1 2 3", "1.5 2", "30000001 0", "99999999 0", "123456789 0", "--1 0", "1,,2"}) {
			assertNull(CarteMath.coordonnees(s), String.valueOf(s));
		}
	}

	@Test
	void niveauxEtTailles() {
		assertEquals(0, CarteMath.niveau(2));
		assertEquals(0, CarteMath.niveau(1));
		assertEquals(1, CarteMath.niveau(0.7));
		assertEquals(2, CarteMath.niveau(0.25));
		assertEquals(1024, CarteMath.taille(2));
	}
}
