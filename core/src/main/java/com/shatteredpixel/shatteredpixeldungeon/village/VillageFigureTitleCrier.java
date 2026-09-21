package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.ui.NameTag;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.utils.PointF;

/**
 * Keeps a village shout showing, in the game's own good-news animation: the
 * same rising, fading text as "Level up!", shouted again every few seconds
 * rather than once.
 *
 * <p>
 * A {@code Gizmo} rather than a tag because the text itself is transient —
 * {@code FloatingText} lives for a second and dies — so what persists is the
 * thing that keeps asking for it. It rides the render clock, not the actor
 * clock: a title is a piece of scenery and must keep showing while the player
 * stands still and the turn counter does not move.
 *
 * <p>
 * Over a figure it holds its own text rather than going through
 * {@code CharSprite.showStatus}, which draws from a shared pool fixed at combat
 * size. A title sits directly above a {@link NameTag} and is read as part of
 * the
 * same label, so it is drawn a point under the name — close enough to belong to
 * it, small enough that the name stays the thing you read first.
 *
 * <p>
 * Over a cell — the dungeon stairs — there is no sprite to follow, so it
 * shouts from the tile centre instead.
 *
 * <p>
 * The colour comes from the title rather than from the combat statuses: every
 * honour shouts in its own, so a square full of figures reads as a leaderboard
 * from a distance instead of a wall of identical green.
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
	/** {@code -1} when the shout follows {@link #owner} instead of a tile. */
	private final int cell;
	private final VillageTitle title;
	/** Starts due, so a body that has just landed says what it is at once. */
	private float waited = PERIOD;
	private TitleText text;

	public VillageFigureTitleCrier(CharSprite owner, VillageTitle title) {
		this.owner = owner;
		this.cell = -1;
		this.title = title;
	}

	public VillageFigureTitleCrier(int cell, VillageTitle title) {
		this.owner = null;
		this.cell = cell;
		this.title = title;
	}

	@Override
	public void update() {
		super.update();
		if (cell >= 0) {
			shoutAtCell();
			return;
		}
		if (owner == null || owner.parent == null) {
			killAndErase();
			return;
		}
		if (!advance(Game.elapsed) || !owner.visible) {
			return;
		}
		// Same anchor CharSprite.showStatus uses: the top of the head, wherever
		// the sprite is standing now rather than where it was placed.
		shout(owner.destinationCenter().x,
				owner.destinationCenter().y - owner.height() / 2f);
	}

	private void shoutAtCell() {
		if (Dungeon.level == null) {
			return;
		}
		if (!advance(Game.elapsed)) {
			return;
		}
		PointF p = DungeonTilemap.tileCenterToWorld(cell);
		shout(p.x, p.y);
	}

	private void shout(float x, float y) {
		if (text == null) {
			text = new TitleText();
			GameScene.addToMobLayer(text);
		}
		text.reset(x, y, title.text, title.color, FloatingText.NO_ICON, true);
	}

	/**
	 * Whether this frame is a shout. Split from {@link #update()} so the cadence
	 * is a question about elapsed time rather than about a live scene.
	 *
	 * <p>
	 * The wait resets rather than carrying the remainder over: a frame that
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
