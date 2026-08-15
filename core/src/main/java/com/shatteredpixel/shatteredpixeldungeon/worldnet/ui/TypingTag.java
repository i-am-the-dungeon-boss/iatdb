/*
 * I am the Dungeon Boss
 * Copyright (C) 2026 Dungeon Boss
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.worldnet.ui;

import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Visual;

/**
 * An ellipsis held over a player who is composing a chat message.
 *
 * <p>Sits where that player's {@link ChatBubble} will appear, so the indicator
 * reads as the message on its way rather than as a second, unrelated label.
 *
 * <p>Unlike a bubble this has no lifespan: it is raised and taken down by
 * {@link RemotePlayers} as the roster reports the flag turning over. That is
 * deliberate — a timeout here would either blink out on a slow typist or hang
 * over somebody who has already sent, and the server clears the flag on send
 * anyway.
 */
public class TypingTag extends RenderedTextBlock {

	private static final int SIZE = 7;
	/** Clears the name tag, which sits directly on the sprite's head. */
	private static final float GAP = 10f;
	/** Seconds per dot, so the ellipsis fills and restarts about once a second. */
	private static final float STEP = 0.35f;

	private static final String[] FRAMES = { ".", "..", "..." };

	private final Visual owner;
	private float elapsed;
	private int frame = -1;

	public TypingTag(Visual owner) {
		// Rendered at screen resolution and scaled back down, as NameTag does:
		// asking for SIZE directly and then shrinking smears the glyphs.
		super(SIZE * PixelScene.defaultZoom);
		this.owner = owner;
		zoom(1 / (float) PixelScene.defaultZoom);
		hardlight(0xCCEEFF);
		setHightlighting(false);
		advance(0);
	}

	@Override
	public void update() {
		super.update();
		if (owner == null) {
			killAndErase();
			return;
		}

		elapsed += Game.elapsed;
		// A while rather than an if: a frame that took longer than a step should
		// land on the right dot, not fall behind by one for the rest of the run.
		while (elapsed >= STEP) {
			elapsed -= STEP;
			advance((frame + 1) % FRAMES.length);
		}

		visible = owner.visible;
		// setPos rather than assigning x/y: this is a Component, so its glyphs
		// only follow when the layout is re-run.
		setPos(
				PixelScene.align(Camera.main, owner.x + owner.width() / 2f - width() / 2f),
				PixelScene.align(Camera.main, owner.y - height() - GAP));
	}

	/**
	 * Re-renders the ellipsis at {@code next} dots.
	 *
	 * <p>Guarded because {@code text} rebuilds the glyph layout, and doing that
	 * every frame for a label that changes three times a second is most of the
	 * cost of having one of these on screen at all.
	 */
	private void advance(int next) {
		if (frame == next) {
			return;
		}
		frame = next;
		text(FRAMES[next]);
	}
}
