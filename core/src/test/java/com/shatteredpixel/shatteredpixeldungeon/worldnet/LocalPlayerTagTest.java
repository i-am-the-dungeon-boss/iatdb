package com.shatteredpixel.shatteredpixeldungeon.worldnet;

import static org.assertj.core.api.Assertions.assertThat;

import com.shatteredpixel.shatteredpixeldungeon.worldnet.ui.LocalPlayerTag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Local player name tag")
class LocalPlayerTagTest {

	@Test
	@DisplayName("labels the local hero with their username while in the village")
	void labelsInVillage() {
		assertThat(LocalPlayerTag.label(true, "Marwan")).isEqualTo("Marwan");
	}

	@Test
	@DisplayName("trims surrounding whitespace from the username")
	void trimsUsername() {
		assertThat(LocalPlayerTag.label(true, "  Marwan  ")).isEqualTo("Marwan");
	}

	@Test
	@DisplayName("shows no tag outside the village, so a run stays unlabelled")
	void hiddenInRun() {
		assertThat(LocalPlayerTag.label(false, "Marwan")).isNull();
	}

	@Test
	@DisplayName("shows no tag when the player has no username yet")
	void hiddenWithoutUsername() {
		assertThat(LocalPlayerTag.label(true, null)).isNull();
		assertThat(LocalPlayerTag.label(true, "")).isNull();
		assertThat(LocalPlayerTag.label(true, "   ")).isNull();
	}
}
