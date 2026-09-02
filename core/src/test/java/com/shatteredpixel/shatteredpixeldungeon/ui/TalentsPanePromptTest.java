package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The locked-tier prompt is an instruction to the player: go reach level 6.
 * Whose talents are on screen decides whether that instruction means anything.
 */
@ExtendWith(GdxTestExtension.class)
@DisplayName("The talent pane's locked-tier prompt")
class TalentsPanePromptTest {

	@Test
	@DisplayName("The player's own pane tells them what to do about the locked tiers")
	void ownPaneIsInstructed() {
		Assertions.assertThat(TalentsPane.lockedTierPrompt(1, true)).contains("level 6");
		Assertions.assertThat(TalentsPane.lockedTierPrompt(2, true)).contains("level 12");
		Assertions.assertThat(TalentsPane.lockedTierPrompt(3, true)).contains("level 20");
	}

	@Test
	@DisplayName("Nothing is locked once every tier is showing")
	void nothingToSayWhenAllTiersShow() {
		Assertions.assertThat(TalentsPane.lockedTierPrompt(4, true)).isNull();
	}

	@Test
	@DisplayName("Somebody else's talents carry no instruction for the person reading them")
	void previewIsNotInstructed() {
		// Levelling up does not unlock another player's echo's fourth tier, so
		// the prompt would be telling the reader to do something that cannot
		// affect what they are looking at.
		Assertions.assertThat(TalentsPane.lockedTierPrompt(1, false)).isNull();
		Assertions.assertThat(TalentsPane.lockedTierPrompt(2, false)).isNull();
		Assertions.assertThat(TalentsPane.lockedTierPrompt(3, false)).isNull();
	}
}
