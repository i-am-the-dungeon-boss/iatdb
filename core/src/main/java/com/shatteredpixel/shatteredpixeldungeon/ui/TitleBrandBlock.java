package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.watabou.noosa.ui.Component;

/**
 * Title-screen brand: animated hero logo + text title.
 */
public class TitleBrandBlock extends Component {

	private TitleHeroLogo heroLogo;
	private TitleBrandTitle brandTitle;
	private float brandTitleYOverride = Float.NaN;

	/**
	 * Top Y for the brand title, centered in the band between the hero character
	 * (not flare/rays) and the menu. When the band is shorter than the title,
	 * hugs the character.
	 */
	public static float brandTitleYBetween(float characterBottom, float menuTop, float titleHeight) {
		float space = menuTop - characterBottom;
		if (space <= titleHeight) {
			return characterBottom;
		}
		return characterBottom + (space - titleHeight) / 2f;
	}

	@Override
	protected void createChildren() {
		heroLogo = new TitleHeroLogo();
		add(heroLogo);
		brandTitle = new TitleBrandTitle();
		add(brandTitle);
	}

	@Override
	protected void layout() {
		float titleWidth = Math.max(heroLogo.preferredWidth(), brandTitle.preferredWidth());
		width = titleWidth;

		heroLogo.setPos(x + (titleWidth - heroLogo.preferredWidth()) / 2f, y);
		float titleY = Float.isNaN(brandTitleYOverride)
				? y + logoHeight()
				: brandTitleYOverride;
		brandTitle.setPos(x + (titleWidth - brandTitle.preferredWidth()) / 2f, titleY);
		brandTitle.layout();
		height = Math.max(logoHeight(), brandTitle.bottom() - y);
	}

	public float logoHeight() {
		return heroLogo.preferredHeight();
	}

	public float logoBottom() {
		return y + logoHeight();
	}

	/** Bottom of the hero sprite only — excludes flare/rays and logo padding. */
	public float characterBottom() {
		return heroLogo.characterBottom();
	}

	public float brandTitleHeight() {
		return brandTitle.preferredHeight();
	}

	/** Places the brand title at an absolute Y (e.g. centered above the menu). */
	public void setBrandTitleY(float absoluteY) {
		brandTitleYOverride = absoluteY;
		layout();
	}

	public void alpha(float a) {
		brandTitle.alpha(a);
	}

	public float logoAnchorY() {
		return y + logoHeight() * 0.55f;
	}
}
