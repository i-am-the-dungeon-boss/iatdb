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

package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.watabou.noosa.Image;

/**
 * Title-screen Village button icon: the village's own wall.
 *
 * <p>Lifted from the city tile sheet {@code VillageLevel.tilesTex()} draws the
 * town in, so the button shows the same stone the player walks past, and stays
 * in step with the tiles if they are ever retouched. Its own file rather than a
 * cell of the shared icon atlas, framed with the dark outline the other title
 * icons carry.
 */
public final class TitleVillageIcon {

	/** Height of every other icon on the title screen. */
	private static final float ICON_SIZE = 16f;

	private TitleVillageIcon() {
	}

	public static String asset() {
		return Assets.Interfaces.VILLAGE;
	}

	/**
	 * Uniform scale bringing a texture of {@code sourceSize} down to icon size.
	 * A missing or empty texture reports zero, which is left unscaled rather
	 * than divided by.
	 */
	public static float scaleFor(float sourceSize) {
		if (sourceSize <= 0f) {
			return 1f;
		}
		return ICON_SIZE / sourceSize;
	}

	public static Image get() {
		Image icon = new Image(asset());
		icon.scale.set(scaleFor(icon.height));
		return icon;
	}
}
