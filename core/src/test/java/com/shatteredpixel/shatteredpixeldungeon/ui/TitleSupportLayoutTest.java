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
	@DisplayName("phone landscape with Support budgets three button rows")
	void phoneLandscapeWithSupportBudgetsThreeRows() {
		int playRows = TitleSupportLayout.playRows(true, false, false);
		Assertions.assertThat(TitleSupportLayout.buttonRows(playRows, true, true, false, true))
				.isEqualTo(3);
	}

	@Test
	@DisplayName("desktop landscape with Support budgets five button rows")
	void desktopLandscapeWithSupportBudgetsFiveRows() {
		int playRows = TitleSupportLayout.playRows(true, true, false);
		Assertions.assertThat(TitleSupportLayout.buttonRows(playRows, true, true, false, false))
				.isEqualTo(5);
	}

	@Test
	@DisplayName("phone landscape without Support budgets two button rows")
	void phoneLandscapeWithoutSupportBudgetsTwoRows() {
		int playRows = TitleSupportLayout.playRows(true, false, false);
		Assertions.assertThat(TitleSupportLayout.buttonRows(playRows, true, false, false, true))
				.isEqualTo(2);
	}

	@Test
	@DisplayName("single meta row is only for non-desktop landscape")
	void singleMetaRowIsOnlyForNonDesktopLandscape() {
		Assertions.assertThat(TitleSupportLayout.singleLandscapeMetaRow(true, false)).isTrue();
		Assertions.assertThat(TitleSupportLayout.singleLandscapeMetaRow(true, true)).isFalse();
		Assertions.assertThat(TitleSupportLayout.singleLandscapeMetaRow(false, false)).isFalse();
	}

	@Test
	@DisplayName("landscape meta row splits into four equal button widths")
	void landscapeMetaRowSplitsIntoFourEqualWidths() {
		Assertions.assertThat(TitleSupportLayout.landscapeMetaButtonWidth(234f))
				.isEqualTo(57f);
	}

	@Test
	@DisplayName("phone landscape keeps the play buttons on one row")
	void phoneLandscapeKeepsOnePlayRow() {
		// 234px of landscape width holds Village, Solo, Ranked and Debug side by side
		Assertions.assertThat(TitleSupportLayout.playRows(true, false, false)).isEqualTo(1);
		Assertions.assertThat(TitleSupportLayout.playRows(true, false, true)).isEqualTo(1);
	}

	@Test
	@DisplayName("desktop gives Village, the shortcuts and Debug a row each")
	void desktopStacksThePlayRows() {
		Assertions.assertThat(TitleSupportLayout.playRows(true, true, false)).isEqualTo(2);
		Assertions.assertThat(TitleSupportLayout.playRows(true, true, true)).isEqualTo(3);
	}

	@Test
	@DisplayName("portrait splits the play buttons over two rows, Debug sharing the second")
	void portraitSplitsThePlayRow() {
		// 133px of portrait width will not hold Village, Solo and Ranked side by side
		Assertions.assertThat(TitleSupportLayout.playRows(false, false, false)).isEqualTo(2);
		Assertions.assertThat(TitleSupportLayout.playRows(false, false, true)).isEqualTo(2);
	}

	@Test
	@DisplayName("portrait with Support and no feed budgets five button rows")
	void portraitWithSupportNoFeedBudgetsFiveRows() {
		int playRows = TitleSupportLayout.playRows(false, false, false);
		Assertions.assertThat(TitleSupportLayout.buttonRows(playRows, false, true, false, false))
				.isEqualTo(5);
	}

	@Test
	@DisplayName("portrait without Support or feed budgets four button rows")
	void portraitWithoutSupportOrFeedBudgetsFourRows() {
		int playRows = TitleSupportLayout.playRows(false, false, false);
		Assertions.assertThat(TitleSupportLayout.buttonRows(playRows, false, false, false, false))
				.isEqualTo(4);
	}

	@Test
	@DisplayName("portrait with Support and feed budgets six button rows")
	void portraitWithSupportAndFeedBudgetsSixRows() {
		int playRows = TitleSupportLayout.playRows(false, false, false);
		Assertions.assertThat(TitleSupportLayout.buttonRows(playRows, false, true, true, false))
				.isEqualTo(6);
	}

	@Test
	@DisplayName("menu stack height includes leading gap and every button row")
	void menuStackHeightIncludesLeadingGapAndRows() {
		Assertions.assertThat(TitleSupportLayout.menuStackHeight(4, 20, 2))
				.isEqualTo(4 * 20 + 4 * 2);
	}

	@Test
	@DisplayName("menu top shrinks so four landscape rows fit under a tall brand")
	void menuTopShrinksSoFourLandscapeRowsFit() {
		float contentHeight = 160f;
		float desiredTop = 120f;
		int buttonRows = 4;
		int btnHeight = 20;

		float menuTop = TitleSupportLayout.menuTop(desiredTop, contentHeight, buttonRows, btnHeight);

		Assertions.assertThat(menuTop).isEqualTo(contentHeight - buttonRows * btnHeight);
		Assertions.assertThat(menuTop + TitleSupportLayout.menuStackHeight(buttonRows, btnHeight, 0))
				.isLessThanOrEqualTo(contentHeight);
	}

	@Test
	@DisplayName("button gap collapses to zero when available height equals the button stack")
	void buttonGapCollapsesWhenHeightEqualsStack() {
		Assertions.assertThat(TitleSupportLayout.buttonGap(80, 4, 20, true))
				.isEqualTo(0);
	}

	@Test
	@DisplayName("button gap never exceeds what keeps the full stack on screen")
	void buttonGapNeverExceedsFit() {
		int available = 100;
		int buttonRows = 4;
		int btnHeight = 20;
		int gap = TitleSupportLayout.buttonGap(available, buttonRows, btnHeight, true);

		Assertions.assertThat(TitleSupportLayout.menuStackHeight(buttonRows, btnHeight, gap))
				.isLessThanOrEqualTo(available);
	}

	@Test
	@DisplayName("menu top clears brand title under the hero character")
	void menuTopClearsBrandTitleUnderCharacter() {
		float desiredTop = 50f;
		float contentHeight = 160f;
		float characterBottom = 40f;
		float brandTitleHeight = 30f;
		int buttonRows = 4;
		int btnHeight = 20;

		float menuTop = TitleSupportLayout.menuTopClearingBrand(
				desiredTop, contentHeight, characterBottom, brandTitleHeight, buttonRows, btnHeight);

		Assertions.assertThat(menuTop).isGreaterThanOrEqualTo(characterBottom + brandTitleHeight);
		Assertions.assertThat(menuTop + TitleSupportLayout.menuStackHeight(buttonRows, btnHeight, 0))
				.isLessThanOrEqualTo(contentHeight);
	}

	@Test
	@DisplayName("logo top leaves room for brand title and menu on short landscape")
	void logoTopLeavesRoomForBrandTitleAndMenu() {
		float contentHeight = 160f;
		float characterOffset = 35f;
		float brandTitleHeight = 30f;
		int buttonRows = 4;
		int btnHeight = 20;

		float logoTop = TitleSupportLayout.logoTopForClearBrand(
				contentHeight, characterOffset, brandTitleHeight, buttonRows, btnHeight);

		float characterBottom = logoTop + characterOffset;
		float menuTop = characterBottom + brandTitleHeight;
		Assertions.assertThat(menuTop + TitleSupportLayout.menuStackHeight(buttonRows, btnHeight, 0))
				.isLessThanOrEqualTo(contentHeight);
	}

	@Test
	@DisplayName("a row of buttons ends exactly where the button area does")
	void gutteredSlotsFillTheAreaExactly() {
		float area = 234f;
		int slots = 4;
		float width = TitleSupportLayout.slotWidth(area, slots);

		Assertions.assertThat(width * slots + 2 * (slots - 1)).isEqualTo(area);
	}

	@Test
	@DisplayName("a lone button on a row takes the whole width, gutters and all")
	void oneSlotTakesEverything() {
		Assertions.assertThat(TitleSupportLayout.slotWidth(234f, 1)).isEqualTo(234f);
	}

	@Test
	@DisplayName("desktop pairs Solo and Ranked, whether or not Debug is shown")
	void desktopAlwaysPairsTheShortcuts() {
		// Village and Debug get rows of their own there, so neither joins the pair.
		Assertions.assertThat(TitleSupportLayout.playSlots(true, true, false)).isEqualTo(2);
		Assertions.assertThat(TitleSupportLayout.playSlots(true, true, true)).isEqualTo(2);
	}

	@Test
	@DisplayName("phone landscape puts every play button on the one row it has")
	void phoneLandscapeSharesTheRowBetweenAllOfThem() {
		Assertions.assertThat(TitleSupportLayout.playSlots(true, false, false)).isEqualTo(3);
		Assertions.assertThat(TitleSupportLayout.playSlots(true, false, true)).isEqualTo(4);
	}

	@Test
	@DisplayName("portrait shares the shortcut row, Village having taken its own")
	void portraitSplitsOnlyTheShortcutRow() {
		Assertions.assertThat(TitleSupportLayout.playSlots(false, false, false)).isEqualTo(2);
		Assertions.assertThat(TitleSupportLayout.playSlots(false, false, true)).isEqualTo(3);
	}

	@Test
	@DisplayName("portrait shortcut buttons stay wide enough to label on the narrowest phone")
	void portraitShortcutsStayLegible() {
		// 133px is the narrowest button area the title screen lays out into.
		float withDebug = TitleSupportLayout.slotWidth(133f, TitleSupportLayout.playSlots(false, false, true));

		Assertions.assertThat(withDebug).isGreaterThan(40f);
	}
}
