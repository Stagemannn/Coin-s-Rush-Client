package com.coinrush.commun;

import static org.junit.jupiter.api.Assertions.*;

import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class CoinsTest {
	@Test
	void parseAndFormat() {
		assertEquals(OptionalLong.of(12000), Coins.parse("12"));
		assertEquals(OptionalLong.of(1500), Coins.parse("1.5"));
		assertEquals(OptionalLong.of(196), Coins.parse("0.196"));
		for (String bad : new String[] {"-1", "1.2345", "abc", "", "1e5", "99999999999999999999", "1."}) {
			assertTrue(Coins.parse(bad).isEmpty(), bad);
		}
		assertEquals("12", Coins.format(12000));
		assertEquals("1200", Coins.format(1200000));
		assertEquals("0", Coins.format(0));
		assertEquals("1.5", Coins.format(1500));
		assertEquals("0.196", Coins.format(196));
		assertEquals("0.001", Coins.format(1));
	}

	@Test
	void largestTypedAmountFitsAndParsesWithoutException() {
		String largest = "999999999999999.999";
		assertEquals(OptionalLong.of(999_999_999_999_999_999L), Coins.parse(largest));
		assertEquals(largest, Coins.format(Coins.parse(largest).getAsLong()));
		assertTrue(Coins.parse("1000000000000000").isEmpty());
	}
}
