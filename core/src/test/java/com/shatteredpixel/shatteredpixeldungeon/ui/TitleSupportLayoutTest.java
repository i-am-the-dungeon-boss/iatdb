package com.shatteredpixel.shatteredpixeldungeon.ui;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TitleSupportLayoutTest {

	@Test
	@DisplayName("title rankings sit under play button when Support is hidden")
	void rankingsSitUnderPlayWhenSupportHidden() {
		float playBottom = 100f;
		float gap = 4f;

		Assertions.assertThat(TitleSupportLayout.rankingsY(playBottom, gap, null))
				.isEqualTo(playBottom + gap);
	}

	@Test
	@DisplayName("title settings sit under rankings when News/Changes are hidden")
	void settingsSitUnderRankingsWhenFeedHidden() {
		float rankingsBottom = 140f;
		float gap = 4f;

		Assertions.assertThat(TitleSupportLayout.settingsY(rankingsBottom, gap, null))
				.isEqualTo(rankingsBottom + gap);
	}

	@Test
	@DisplayName("portrait drops a button row when News/Changes are hidden")
	void portraitDropsRowWhenFeedHidden() {
		Assertions.assertThat(TitleSupportLayout.buttonRows(false, false, false))
				.isEqualTo(2);
	}
}
