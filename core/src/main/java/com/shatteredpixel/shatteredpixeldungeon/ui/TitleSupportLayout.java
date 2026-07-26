package com.shatteredpixel.shatteredpixeldungeon.ui;

/**
 * Title-screen vertical layout helpers around optional Support and News/Changes
 * rows.
 */
public final class TitleSupportLayout {

	private TitleSupportLayout() {
	}

	/**
	 * Top Y for the Rankings row.
	 *
	 * @param soloBottom    bottom of the Solo/Ranked row
	 * @param gap           spacing between rows
	 * @param supportBottom bottom of the Support row, or {@code null} when Support
	 *                      is hidden
	 */
	public static float rankingsY(float soloBottom, float gap, Float supportBottom) {
		float rowAbove = supportBottom != null ? supportBottom : soloBottom;
		return rowAbove + gap;
	}

	/**
	 * Top Y for the Settings row.
	 *
	 * @param rankingsBottom bottom of the Rankings row
	 * @param gap            spacing between rows
	 * @param newsBottom     bottom of the News/Changes row, or {@code null} when
	 *                       that row is hidden
	 */
	public static float settingsY(float rankingsBottom, float gap, Float newsBottom) {
		float rowAbove = newsBottom != null ? newsBottom : rankingsBottom;
		return rowAbove + gap;
	}

	/**
	 * Whether Rankings/Journal/Settings/About share one landscape row (phones /
	 * mobile only — not desktop).
	 */
	public static boolean singleLandscapeMetaRow(boolean landscape, boolean desktop) {
		return landscape && !desktop;
	}

	/**
	 * Number of title button rows for gap sizing.
	 *
	 * @param landscape      whether the title scene is in landscape
	 * @param supportVisible whether the Support row is shown
	 * @param feedVisible    whether the News/Changes buttons are shown
	 * @param singleMetaRow  phone landscape: one meta row of four buttons
	 */
	public static int buttonRows(
			boolean landscape,
			boolean supportVisible,
			boolean feedVisible,
			boolean singleMetaRow) {
		if (landscape && singleMetaRow) {
			// play + one meta row (Rankings|Journal|Settings|About); optional Support.
			int rows = 2;
			if (supportVisible) {
				rows++;
			}
			return rows;
		}
		// play + rankings + settings; Support is its own row;
		// News/Changes only add a row in portrait.
		int rows = 3;
		if (supportVisible) {
			rows++;
		}
		if (!landscape && feedVisible) {
			rows++;
		}
		return rows;
	}

	/**
	 * Width for each of the four phone-landscape meta buttons (2px gutters).
	 */
	public static float landscapeMetaButtonWidth(float buttonAreaWidth) {
		return (float) Math.floor(buttonAreaWidth / 4f) - 1f;
	}

	/**
	 * Vertical space for the button stack: each row plus a leading gap before the
	 * first row and a gap between rows (same spacing TitleScene applies).
	 */
	public static int menuStackHeight(int buttonRows, int btnHeight, int gap) {
		return buttonRows * btnHeight + buttonRows * gap;
	}

	/**
	 * Top of the menu stack in content coordinates. Shrinks below
	 * {@code desiredTop} when needed so {@link #menuStackHeight} at gap 0 still
	 * fits in {@code contentHeight} (short Android landscape + tall brand).
	 */
	public static float menuTop(float desiredTop, float contentHeight, int buttonRows, int btnHeight) {
		float maxTop = contentHeight - menuStackHeight(buttonRows, btnHeight, 0);
		if (maxTop < 0) {
			maxTop = 0;
		}
		return Math.min(desiredTop, maxTop);
	}

	/**
	 * Inter-row gap that keeps the full stack on screen. Uses the same free-space
	 * divisor as TitleScene, then clamps so Settings/About are not pushed below
	 * the viewport.
	 */
	public static int buttonGap(int availableHeight, int buttonRows, int btnHeight, boolean landscape) {
		if (buttonRows <= 0) {
			return 0;
		}
		int remaining = availableHeight - buttonRows * btnHeight;
		if (remaining <= 0) {
			return 0;
		}
		int gap = remaining / 3;
		gap /= landscape ? 3 : 5;
		int maxGap = remaining / buttonRows;
		if (gap > maxGap) {
			gap = maxGap;
		}
		return Math.max(gap, 0);
	}

	/**
	 * Menu top that clears the brand title under the hero character, while still
	 * keeping the button stack on screen.
	 */
	public static float menuTopClearingBrand(
			float desiredTop,
			float contentHeight,
			float characterBottom,
			float brandTitleHeight,
			int buttonRows,
			int btnHeight) {
		float maxTop = contentHeight - menuStackHeight(buttonRows, btnHeight, 0);
		if (maxTop < 0) {
			maxTop = 0;
		}
		float minTop = characterBottom + brandTitleHeight;
		return Math.min(Math.max(desiredTop, minTop), maxTop);
	}

	/**
	 * Logo top (content Y) so character + brand title + menu stack still fit on
	 * short landscape heights.
	 */
	public static float logoTopForClearBrand(
			float contentHeight,
			float characterOffsetFromLogoTop,
			float brandTitleHeight,
			int buttonRows,
			int btnHeight) {
		float maxMenuTop = contentHeight - menuStackHeight(buttonRows, btnHeight, 0);
		float maxCharacterBottom = maxMenuTop - brandTitleHeight;
		return maxCharacterBottom - characterOffsetFromLogoTop;
	}
}
