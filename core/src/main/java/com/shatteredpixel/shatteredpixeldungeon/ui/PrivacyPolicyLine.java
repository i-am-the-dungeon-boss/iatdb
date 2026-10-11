package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.ProjectLinks;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.ui.Component;

/**
 * One sentence whose last words open the privacy policy.
 */
public class PrivacyPolicyLine extends Component {

	private static final int COLOR = 0xFFFFFF;

	private final RenderedTextBlock prefix;
	private final RenderedTextBlock link;
	private final RenderedTextBlock period;
	private final ColorBlock underline;
	private final PointerArea hit;

	public PrivacyPolicyLine(String beforeLink) {
		prefix = PixelScene.renderTextBlock(beforeLink, 6);
		prefix.hardlight(COLOR);
		add(prefix);

		link = PixelScene.renderTextBlock(Messages.get(TitleScene.class, "privacy_link"), 6);
		link.hardlight(COLOR);
		add(link);

		period = PixelScene.renderTextBlock(".", 6);
		period.hardlight(COLOR);
		add(period);

		underline = new ColorBlock(1, 1, COLOR);
		add(underline);

		hit = new PointerArea(0, 0, 0, 0) {
			@Override
			protected void onClick(PointerEvent event) {
				ShatteredPixelDungeon.platform.openURI(ProjectLinks.HOMEPAGE_URL + "/privacy");
			}
		};
		add(hit);
	}

	public void alpha(float value) {
		prefix.alpha(value);
		link.alpha(value);
		period.alpha(value);
		underline.alpha(value);
	}

	@Override
	protected void layout() {
		prefix.maxWidth((int) width);
		link.maxWidth((int) width);
		float gap = 2;
		float wordsW = link.width() + period.width();
		boolean oneLine = prefix.nLines <= 1 && prefix.width() + gap + wordsW <= width;
		if (oneLine) {
			float blockW = prefix.width() + gap + wordsW;
			float left = x + Math.max(0, (width - blockW) / 2f);
			prefix.setPos(left, y);
			link.setPos(prefix.right() + gap, y);
		} else {
			prefix.setPos(x + Math.max(0, (width - prefix.width()) / 2f), y);
			float wordsLeft = x + Math.max(0, (width - wordsW) / 2f);
			link.setPos(wordsLeft, prefix.bottom() + 1);
		}
		period.setPos(link.right(), link.top());
		PixelScene.align(prefix);
		PixelScene.align(link);
		PixelScene.align(period);

		underline.size(link.width(), 1);
		underline.x = link.left();
		underline.y = link.bottom() - 1;

		hit.x = link.left() - 1;
		hit.y = link.top() - 1;
		hit.width = link.width() + 2;
		hit.height = link.height() + 2;

		height = link.bottom() - y + 1;
	}
}
