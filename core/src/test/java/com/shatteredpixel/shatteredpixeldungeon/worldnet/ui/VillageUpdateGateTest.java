package com.shatteredpixel.shatteredpixeldungeon.worldnet.ui;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The village half of the forced-update gate.
 *
 * <p>A deploy is announced over the world socket, which reaches a player
 * wherever they happen to be — including forty floors down. Interrupting that
 * player with an undismissable window would cost them the run, so the gate is
 * held until they are standing in the village.
 */
@DisplayName("Village update gate")
class VillageUpdateGateTest {

	@Test
	@DisplayName("gates the village when the server expects a newer build")
	void gatesTheVillage() {
		assertThat(VillageUpdateGate.shouldGate(true, true, false)).isTrue();
	}

	@Test
	@DisplayName("does not interrupt a player who is in a run")
	void aRunIsNeverInterrupted() {
		assertThat(VillageUpdateGate.shouldGate(false, true, false)).isFalse();
	}

	@Test
	@DisplayName("does not gate a village the server never complained about")
	void anUncomplainedVillageStaysOpen() {
		assertThat(VillageUpdateGate.shouldGate(true, false, false)).isFalse();
	}

	@Test
	@DisplayName("shows the gate once, not on every frame")
	void gatesOnlyOnce() {
		assertThat(VillageUpdateGate.shouldGate(true, true, true)).isFalse();
	}
}
