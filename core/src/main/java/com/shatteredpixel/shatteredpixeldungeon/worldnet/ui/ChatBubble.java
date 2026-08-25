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

import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Visual;

/**
 * A spoken chat line, held above the speaker's head.
 *
 * <p>{@code FloatingText} was the first home for this but is the wrong shape: it
 * is a single drifting line with no wrap, so a 240-character message ran off
 * both edges of the screen. This stays put over its owner, wraps to a fixed
 * width, and outlives a damage number so a line can actually be read.
 */
public class ChatBubble extends RenderedTextBlock {

	private static final int SIZE = 6;
	/** Widest a bubble gets, in world pixels — roughly four dungeon tiles. */
	private static final int MAX_WIDTH = 64;
	/** Long enough to read a full-length message. */
	private static final float LIFESPAN = 5f;
	private static final float FADE = 1f;
	/** Clears the name tag, which sits directly on the sprite's head. */
	private static final float GAP = 10f;

	private final Visual owner;
	private float timeLeft = LIFESPAN;

	public ChatBubble(Visual owner, String text) {
		// Rendered at screen resolution and scaled back down; asking for SIZE
		// directly and then shrinking would smear the glyphs.
		super(SIZE * PixelScene.defaultZoom);
		this.owner = owner;
		zoom(1 / (float) PixelScene.defaultZoom);
		// The width bound is applied pre-zoom, hence the same scaling.
		text(text, MAX_WIDTH * PixelScene.defaultZoom);
		align(CENTER_ALIGN);
		setHightlighting(false);
	}

	@Override
	public void update() {
		super.update();
		if (owner == null) {
			killAndErase();
			return;
		}

		if ((timeLeft -= Game.elapsed) <= 0) {
			killAndErase();
			return;
		}
		if (timeLeft < FADE) {
			alpha(timeLeft / FADE);
		}

		visible = owner.visible;
		// setPos rather than assigning x/y: this is a Component, so its glyphs
		// only follow when the layout is re-run.
		setPos(
				PixelScene.align(Camera.main, owner.x + owner.width() / 2f - width() / 2f),
				PixelScene.align(Camera.main, owner.y - height() - GAP));
	}

	/** Raises a bubble over {@code owner}, in the same layer as the avatars. */
	public static void show(Visual owner, String text) {
		if (owner == null || text == null || text.trim().isEmpty()) {
			return;
		}
		GameScene.addToMobLayer(new ChatBubble(owner, text.trim()));
	}
}
