package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.watabou.noosa.ui.Component;

/**
 * Title-screen brand: animated hero logo + text title.
 */
public class TitleBrandBlock extends Component {

	private TitleHeroLogo heroLogo;
	private TitleBrandTitle brandTitle;

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
		float titleHeight = heroLogo.preferredHeight() + 6f + brandTitle.preferredHeight();
		width = titleWidth;
		height = titleHeight;

		heroLogo.setPos(x + (titleWidth - heroLogo.preferredWidth()) / 2f, y);
		brandTitle.setPos(x + (titleWidth - brandTitle.preferredWidth()) / 2f, y + heroLogo.preferredHeight() + 6f);
		brandTitle.layout();
	}

	public void alpha(float a) {
		brandTitle.alpha(a);
	}

	public float logoAnchorY() {
		return y + heroLogo.preferredHeight() * 0.55f;
	}
}
