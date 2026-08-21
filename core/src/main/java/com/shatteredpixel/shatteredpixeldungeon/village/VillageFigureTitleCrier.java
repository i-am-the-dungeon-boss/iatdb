package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.ui.NameTag;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;

/**
 * Keeps a standing figure's title showing, in the game's own good-news
 * animation: the same rising, fading text as "Level up!", shouted again every
 * few seconds rather than once.
 *
 * <p>A {@code Gizmo} rather than a tag because the text itself is transient —
 * {@code FloatingText} lives for a second and dies — so what persists is the
 * thing that keeps asking for it. It rides the render clock, not the actor
 * clock: a title is a piece of scenery and must keep showing while the player
 * stands still and the turn counter does not move.
 *
 * <p>It holds its own text rather than going through
 * {@code CharSprite.showStatus}, which draws from a shared pool fixed at combat
 * size. A title sits directly above a {@link NameTag} and is read as part of the
 * same label, so it is drawn a point under the name — close enough to belong to
 * it, small enough that the name stays the thing you read first.
 */
public class VillageFigureTitleCrier extends Gizmo {

	/** Seconds between shouts. The text itself lives for one of them. */
	public static final float PERIOD = 4f;

	/** Sized off the username rather than off the combat numbers. */
	private static final int SIZE = NameTag.SIZE - 1;

	private static class TitleText extends FloatingText {
		TitleText() {
			super(SIZE * PixelScene.defaultZoom);
		}
	}

	private final CharSprite owner;
	private final String title;
	/** Starts due, so a body that has just landed says what it is at once. */
	private float waited = PERIOD;
	private TitleText text;

	public VillageFigureTitleCrier(CharSprite owner, String title) {
		this.owner = owner;
		this.title = title;
	}

	@Override
	public void update() {
		super.update();
		if (owner == null || owner.parent == null) {
			killAndErase();
			return;
		}
		if (!advance(Game.elapsed) || !owner.visible) {
			return;
		}
		if (text == null) {
			text = new TitleText();
			GameScene.addToMobLayer(text);
		}
		// Same anchor CharSprite.showStatus uses: the top of the head, wherever
		// the sprite is standing now rather than where it was placed.
		float x = owner.destinationCenter().x;
		float y = owner.destinationCenter().y - owner.height() / 2f;
		text.reset(x, y, title, CharSprite.POSITIVE, FloatingText.NO_ICON, true);
	}

	/**
	 * Whether this frame is a shout. Split from {@link #update()} so the cadence
	 * is a question about elapsed time rather than about a live scene.
	 *
	 * <p>The wait resets rather than carrying the remainder over: a frame that
	 * ran long — a window closing, a level loading — means one late shout, not a
	 * burst of them catching up.
	 */
	boolean advance(float elapsed) {
		waited += elapsed;
		if (waited < PERIOD) {
			return false;
		}
		waited = 0f;
		return true;
	}

	/** The text is a second visual in the scene, and goes with this one. */
	@Override
	public void killAndErase() {
		super.killAndErase();
		if (text != null) {
			GameScene.removeFromMobLayer(text);
			text.killAndErase();
			text = null;
		}
	}
}
