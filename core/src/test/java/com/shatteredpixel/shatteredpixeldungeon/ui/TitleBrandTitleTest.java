package com.shatteredpixel.shatteredpixeldungeon.ui;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TitleBrandTitleTest {

	@Test
	@DisplayName("brand title uses compact display sizes")
	void brandTitleUsesCompactDisplaySizes() {
		Assertions.assertThat(TitleBrandTitle.TITLE_SIZE).isEqualTo(12);
		Assertions.assertThat(TitleBrandTitle.BOSS_TITLE_SIZE).isEqualTo(14);
	}

	@Test
	@DisplayName("dungeon boss line uses faux bold offset")
	void dungeonBossLineUsesFauxBoldOffset() {
		Assertions.assertThat(TitleBrandTitle.BOSS_BOLD_OFFSET_X).isEqualTo(1f);
	}

	@Test
	@DisplayName("dungeon boss line is larger than I am the")
	void dungeonBossLineIsLargerThanIAmThe() {
		Assertions.assertThat(TitleBrandTitle.BOSS_TITLE_SIZE)
				.isGreaterThan(TitleBrandTitle.TITLE_SIZE);
	}
}
