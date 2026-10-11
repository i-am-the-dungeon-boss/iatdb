package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TitleBrandTitleTest {

	@AfterEach
	void clearDemoFlag() {
		Dungeon.demoPackage = false;
	}

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

	@Test
	@DisplayName("demo package appends DEMO to the title name")
	void demoPackageAppendsDemoToTitle() {
		Dungeon.demoPackage = true;
		Assertions.assertThat(TitleBrandTitle.bossLine()).isEqualTo("DUNGEON BOSS DEMO");
	}

	@Test
	@DisplayName("full game title name is DUNGEON BOSS")
	void fullGameTitleNameIsDungeonBoss() {
		Dungeon.demoPackage = false;
		Assertions.assertThat(TitleBrandTitle.bossLine()).isEqualTo("DUNGEON BOSS");
	}
}
