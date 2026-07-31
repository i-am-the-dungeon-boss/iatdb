package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.utils.DungeonSeed;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class CustomSeedPlayModeTest {

	@AfterEach
	void cleanup() {
		SPDSettings.customSeed("");
		Dungeon.customSeedText = "";
		Dungeon.daily = false;
		Dungeon.dailyReplay = false;
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
	}

	@Test
	@DisplayName("custom seed is allowed in solo mode only")
	void customSeedAllowedInSoloOnly() {
		Assertions.assertThat(SPDSettings.customSeedAllowedForPlayMode(EchoPlayMode.SOLO)).isTrue();
		Assertions.assertThat(SPDSettings.customSeedAllowedForPlayMode(EchoPlayMode.RANKED)).isFalse();
		Assertions.assertThat(SPDSettings.customSeedAllowedForPlayMode(EchoPlayMode.DEBUG)).isFalse();
	}

	@Test
	@DisplayName("ranked mode clears custom seed settings")
	void rankedModeClearsCustomSeedSettings() {
		SPDSettings.customSeed("ABCDEFGH");
		Dungeon.customSeedText = "ABCDEFGH";

		SPDSettings.clearCustomSeedIfDisallowed(EchoPlayMode.RANKED);

		Assertions.assertThat(SPDSettings.customSeed()).isEmpty();
		Assertions.assertThat(Dungeon.customSeedText).isEmpty();
	}

	@Test
	@DisplayName("solo mode keeps custom seed settings")
	void soloModeKeepsCustomSeedSettings() {
		SPDSettings.customSeed("ABCDEFGH");
		Dungeon.customSeedText = "ABCDEFGH";

		SPDSettings.clearCustomSeedIfDisallowed(EchoPlayMode.SOLO);

		Assertions.assertThat(SPDSettings.customSeed()).isEqualTo("ABCDEFGH");
		Assertions.assertThat(Dungeon.customSeedText).isEqualTo("ABCDEFGH");
	}

	@Test
	@DisplayName("selecting ranked mode clears custom seed settings")
	void selectingRankedModeClearsCustomSeedSettings() {
		SPDSettings.customSeed("ABCDEFGH");
		Dungeon.customSeedText = "ABCDEFGH";

		GamesInProgress.selectEchoPlayMode(EchoPlayMode.RANKED);

		Assertions.assertThat(SPDSettings.customSeed()).isEmpty();
		Assertions.assertThat(Dungeon.customSeedText).isEmpty();
	}

	@Test
	@DisplayName("Dungeon.initSeed ignores custom seed outside solo")
	void initSeedIgnoresCustomSeedOutsideSolo() {
		SPDSettings.customSeed("ABCDEFGH");
		Dungeon.daily = false;
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;

		Dungeon.initSeed();

		Assertions.assertThat(Dungeon.customSeedText).isEmpty();
		Assertions.assertThat(Dungeon.seed).isNotEqualTo(DungeonSeed.convertFromText("ABCDEFGH"));
	}

	@Test
	@DisplayName("Dungeon.initSeed uses custom seed in solo")
	void initSeedUsesCustomSeedInSolo() {
		SPDSettings.customSeed("ABCDEFGH");
		Dungeon.daily = false;
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;

		Dungeon.initSeed();

		Assertions.assertThat(Dungeon.customSeedText).isEqualTo("ABCDEFGH");
		Assertions.assertThat(Dungeon.seed).isEqualTo(DungeonSeed.convertFromText("ABCDEFGH"));
	}
}
