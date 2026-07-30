package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import org.assertj.core.api.Assertions;

/**
 * Asserts an armor ability (or refuse) left the EchoBoss turn gate open.
 * Checks {@link EchoBoss#isBusy()} only — calling {@link EchoBoss#act()} would
 * run policy AI and couple the assertion to unrelated fight mutations.
 */
public final class EchoBossTurnAssert {

	private EchoBossTurnAssert() {
	}

	public static void assertCanTakeNextTurn(EchoBoss boss) {
		Assertions.assertThat(boss.isBusy())
				.as("armor ability must not leave EchoBoss busy — act() would stall")
				.isFalse();
	}
}
