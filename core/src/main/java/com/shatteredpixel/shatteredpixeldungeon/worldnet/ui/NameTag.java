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
import com.watabou.noosa.Visual;

/**
 * A floating display name above a remote player.
 *
 * <p>Nothing like this existed — {@code FloatingText} is transient and
 * {@code CharHealthIndicator} needs a real {@code Char} — so this follows
 * {@code EmoIcon}'s pattern instead: re-anchor to the owner every frame rather
 * than trying to parent to it.
 */
public class NameTag extends RenderedTextBlock {

	/** Point size of a floating label. Public so a village title can match it. */
	public static final int SIZE = 7;
	/** Pixels above the sprite's head. */
	private static final float GAP = 2f;

	private final Visual owner;

	public NameTag(Visual owner, String name) {
		// Rendered at screen resolution and scaled back down, the way
		// FloatingText does it: asking for SIZE directly and then shrinking
		// would render the glyphs at a fraction of a pixel and smear them.
		super(name, SIZE * PixelScene.defaultZoom);
		this.owner = owner;
		// World-space text is otherwise rendered at UI scale and looks huge.
		zoom(1 / (float) PixelScene.defaultZoom);
		hardlight(0xCCEEFF);
	}

	@Override
	public void update() {
		super.update();
		if (owner == null) {
			return;
		}
		visible = owner.visible;
		// setPos rather than assigning x/y: this is a Component, so its glyphs
		// only follow when the layout is re-run.
		setPos(
				PixelScene.align(Camera.main, owner.x + owner.width() / 2f - width() / 2f),
				PixelScene.align(Camera.main, owner.y - height() - GAP));
	}
}
