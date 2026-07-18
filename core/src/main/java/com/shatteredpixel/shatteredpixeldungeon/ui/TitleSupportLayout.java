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
	 * Number of title button rows for gap sizing.
	 *
	 * @param landscape      whether the title scene is in landscape
	 * @param supportVisible whether the Support row is shown
	 * @param feedVisible    whether the News/Changes buttons are shown
	 */
	public static int buttonRows(boolean landscape, boolean supportVisible, boolean feedVisible) {
		int rows = landscape ? 3 : 4;
		if (!supportVisible) {
			rows--;
		}
		if (!landscape && !feedVisible) {
			rows--;
		}
		return rows;
	}
}
