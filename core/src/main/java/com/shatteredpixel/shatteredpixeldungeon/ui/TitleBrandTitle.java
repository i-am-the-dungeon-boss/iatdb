package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.ui.Component;

/**
 * Text title replacing the static banner art: “I AM THE” + gold-pulsing
 * “DUNGEON BOSS”.
 */
public class TitleBrandTitle extends Component {

	public static final int TITLE_SIZE = 12;
	public static final int BOSS_TITLE_SIZE = 14;
	/** Horizontal offset for the faux-bold duplicate of “DUNGEON BOSS”. */
	public static final float BOSS_BOLD_OFFSET_X = 1f;

	private RenderedTextBlock line1;
	private RenderedTextBlock line2Bold;
	private RenderedTextBlock line2;
	private float pulseTime;
	private float baseAlpha = 1f;

	@Override
	protected void createChildren() {
		line1 = PixelScene.renderTextBlock("I AM THE", TITLE_SIZE);
		line1.hardlight(0xFFFFFF);
		add(line1);

		// Pixel font has no bold face — duplicate with a 1px offset for weight.
		line2Bold = PixelScene.renderTextBlock("DUNGEON BOSS", BOSS_TITLE_SIZE);
		line2Bold.hardlight(0xFFD84D);
		add(line2Bold);

		line2 = PixelScene.renderTextBlock("DUNGEON BOSS", BOSS_TITLE_SIZE);
		line2.hardlight(0xFFD84D);
		add(line2);
	}

	public float preferredWidth() {
		return Math.max(line1.width(), line2.width() + BOSS_BOLD_OFFSET_X);
	}

	public float preferredHeight() {
		return line1.height() + 4f + line2.height();
	}

	@Override
	protected void layout() {
		width = preferredWidth();
		height = preferredHeight();
		line1.setPos(x + (width - line1.width()) / 2f, y);
		float line2Y = y + line1.height() + 4f;
		float line2X = x + (width - line2.width()) / 2f;
		line2Bold.setPos(line2X + BOSS_BOLD_OFFSET_X, line2Y);
		line2.setPos(line2X, line2Y);
	}

	@Override
	public void update() {
		super.update();
		pulseTime += Game.elapsed;
		applyAlpha();
	}

	public void alpha(float a) {
		baseAlpha = a;
		applyAlpha();
	}

	private void applyAlpha() {
		line1.alpha(baseAlpha);
		float gold = baseAlpha * TitleHeroLogoTiming.pulseGoldAlpha(pulseTime);
		line2Bold.alpha(gold);
		line2.alpha(gold);
	}
}
