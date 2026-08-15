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
	 * Rows the play buttons occupy.
	 *
	 * <p>Three shapes, for three amounts of room:
	 *
	 * <ul>
	 * <li><b>Desktop</b> — a row each: Village, then Solo and Ranked together,
	 * then Debug. There is height to spare, and full-width targets suit a mouse.
	 * <li><b>Phone landscape</b> — one row. 234px holds all four side by side,
	 * and vertical space is the scarce thing.
	 * <li><b>Portrait</b> — two rows. 133px will not hold three or four buttons
	 * with room for an icon and a label, so Village takes the full width and the
	 * shortcuts (with Debug) share the row beneath it.
	 * </ul>
	 */
	public static int playRows(boolean landscape, boolean desktop, boolean debugVisible) {
		if (desktop) {
			return debugVisible ? 3 : 2;
		}
		return landscape ? 1 : 2;
	}

	/**
	 * Number of title button rows for gap sizing.
	 *
	 * @param playRows       rows the play buttons take, from {@link #playRows}
	 * @param landscape      whether the title scene is in landscape
	 * @param supportVisible whether the Support row is shown
	 * @param feedVisible    whether the News/Changes buttons are shown
	 * @param singleMetaRow  phone landscape: one meta row of four buttons
	 */
	public static int buttonRows(
			int playRows,
			boolean landscape,
			boolean supportVisible,
			boolean feedVisible,
			boolean singleMetaRow) {
		if (landscape && singleMetaRow) {
			// play + one meta row (Rankings|Journal|Settings|About); optional Support.
			int rows = playRows + 1;
			if (supportVisible) {
				rows++;
			}
			return rows;
		}
		// play + rankings + settings; Support is its own row;
		// News/Changes only add a row in portrait.
		int rows = playRows + 2;
		if (supportVisible) {
			rows++;
		}
		if (!landscape && feedVisible) {
			rows++;
		}
		return rows;
	}

	/**
	 * Play buttons sharing the widest play row.
	 *
	 * <p>The counterpart to {@link #playRows}: that says how the four buttons stack,
	 * this says how many of them end up side by side on the row that holds the most.
	 * Desktop pairs Solo and Ranked, phone landscape puts everything on one row, and
	 * portrait gives Village its own and leaves the shortcuts below it.
	 */
	public static int playSlots(boolean landscape, boolean desktop, boolean debugVisible) {
		if (desktop) {
			return 2;
		}
		if (landscape) {
			return debugVisible ? 4 : 3;
		}
		return debugVisible ? 3 : 2;
	}

	/**
	 * Width of one button when {@code slots} of them share the area with 2px gutters.
	 *
	 * <p>The gutters come out of the area rather than being added to it, so a row
	 * always ends exactly where the button area does however many buttons are on it.
	 */
	public static float slotWidth(float areaWidth, int slots) {
		if (slots <= 1) {
			return areaWidth;
		}
		return (areaWidth - 2 * (slots - 1)) / slots;
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
