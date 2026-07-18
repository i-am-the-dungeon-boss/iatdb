package com.shatteredpixel.shatteredpixeldungeon.ui;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TitleRankedIconTest {

	@Test
	@DisplayName("title Ranked button uses CHALLENGE_COLOR")
	void rankedUsesChallengeColor() {
		Assertions.assertThat(TitleRankedIcon.type()).isEqualTo(Icons.CHALLENGE_COLOR);
	}
}
