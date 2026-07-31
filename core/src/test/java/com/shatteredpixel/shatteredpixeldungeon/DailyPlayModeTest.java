package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class DailyPlayModeTest {

	@AfterEach
	void cleanup() {
		Dungeon.daily = false;
		Dungeon.dailyReplay = false;
		Dungeon.customSeedText = "";
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
		SPDSettings.customSeed("");
	}

	@Test
	@DisplayName("daily run is allowed in solo mode only")
	void dailyAllowedInSoloOnly() {
		Assertions.assertThat(SPDSettings.dailyAllowedForPlayMode(EchoPlayMode.SOLO)).isTrue();
		Assertions.assertThat(SPDSettings.dailyAllowedForPlayMode(EchoPlayMode.RANKED)).isFalse();
		Assertions.assertThat(SPDSettings.dailyAllowedForPlayMode(EchoPlayMode.DEBUG)).isFalse();
	}

	@Test
	@DisplayName("ranked mode clears daily run flags")
	void rankedModeClearsDailyRunFlags() {
		Dungeon.daily = true;
		Dungeon.dailyReplay = true;

		SPDSettings.clearDailyIfDisallowed(EchoPlayMode.RANKED);

		Assertions.assertThat(Dungeon.daily).isFalse();
		Assertions.assertThat(Dungeon.dailyReplay).isFalse();
	}

	@Test
	@DisplayName("solo mode keeps daily run flags")
	void soloModeKeepsDailyRunFlags() {
		Dungeon.daily = true;
		Dungeon.dailyReplay = true;

		SPDSettings.clearDailyIfDisallowed(EchoPlayMode.SOLO);

		Assertions.assertThat(Dungeon.daily).isTrue();
		Assertions.assertThat(Dungeon.dailyReplay).isTrue();
	}

	@Test
	@DisplayName("selecting ranked mode clears daily run flags")
	void selectingRankedModeClearsDailyRunFlags() {
		Dungeon.daily = true;
		Dungeon.dailyReplay = true;

		GamesInProgress.selectEchoPlayMode(EchoPlayMode.RANKED);

		Assertions.assertThat(Dungeon.daily).isFalse();
		Assertions.assertThat(Dungeon.dailyReplay).isFalse();
	}

	@Test
	@DisplayName("Dungeon.initSeed ignores daily outside solo")
	void initSeedIgnoresDailyOutsideSolo() {
		SPDSettings.lastDaily(1_700_000_000_000L);
		Dungeon.daily = true;
		Dungeon.dailyReplay = true;
		Dungeon.echoPlayMode = EchoPlayMode.RANKED;

		Dungeon.initSeed();

		Assertions.assertThat(Dungeon.daily).isFalse();
		Assertions.assertThat(Dungeon.dailyReplay).isFalse();
		Assertions.assertThat(Dungeon.customSeedText).isEmpty();
	}

	@Test
	@DisplayName("Dungeon.initSeed uses daily in solo")
	void initSeedUsesDailyInSolo() {
		long lastDaily = 1_700_000_000_000L;
		SPDSettings.lastDaily(lastDaily);
		Dungeon.daily = true;
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;

		Dungeon.initSeed();

		Assertions.assertThat(Dungeon.daily).isTrue();
		Assertions.assertThat(Dungeon.seed)
				.isEqualTo(lastDaily + com.shatteredpixel.shatteredpixeldungeon.utils.DungeonSeed.TOTAL_SEEDS);
		Assertions.assertThat(Dungeon.customSeedText).isNotEmpty();
	}
}
