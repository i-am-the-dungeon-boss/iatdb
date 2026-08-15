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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.PointF;

/**
 * Another player, drawn in the village.
 *
 * <p>Extends {@link CharSprite} but deliberately never calls {@code link()}, so
 * {@code ch} stays null: there is no {@code Char}, no collision, no combat and
 * no turn scheduling. {@code MobSprite} would have been the closer-looking base
 * but its {@code update()} casts {@code ch} to {@code Mob} without a guard.
 *
 * <p>Movement is interpolated here rather than through {@code CharSprite.move},
 * whose water branch dereferences {@code ch.flying} and would crash on a null
 * char the moment a remote player walked through the village stream.
 */
public class RemotePlayerSprite extends CharSprite {

	/**
	 * How another player is drawn: a warrior in tier-1 cloth, whatever class they
	 * are actually playing.
	 *
	 * <p>Held here rather than shared with {@code HeroSprite}, even though the
	 * local hero currently uses the same two values. They are separate inputs
	 * that happen to coincide: once players can pick an avatar, a remote one
	 * comes from that player's presence data and the local one from your own
	 * saved choice. Hoisting them into a single constant would only have to be
	 * unpicked, and would claim an invariant that is not real.
	 */
	private static final HeroClass APPEARANCE = HeroClass.WARRIOR;
	/** Cloth armour — {@code ClothArmor} is tier 1, and tier 0 is bare-chested. */
	private static final int TIER = 1;
	private static final int RUN_FRAMERATE = 20;
	/** Slightly slower than a local step, so a 1–2s poll interval reads as walking. */
	private static final float GLIDE_SPEED = 40f;

	private PointF target;

	public RemotePlayerSprite() {
		super();

		texture(APPEARANCE.spritesheet());
		TextureFilm film = HeroSprite.film(TIER);

		idle = new Animation(1, true);
		idle.frames(film, 0, 0, 0, 1, 0, 0, 1, 1);

		run = new Animation(RUN_FRAMERATE, true);
		run.frames(film, 2, 3, 4, 5, 6, 7);

		die = new Animation(20, false);
		die.frames(film, 8, 9, 10, 11, 12, 11);

		renderShadow = true;
		idle();
	}

	@Override
	public void move(int from, int to) {
		turnTo(from, to);
		play(run);
		target = worldToCamera(to);
		isMoving = true;
	}

	@Override
	public void update() {
		super.update();
		if (target == null) {
			return;
		}

		float step = GLIDE_SPEED * com.watabou.noosa.Game.elapsed;
		float dx = target.x - x;
		float dy = target.y - y;
		float distance = (float) Math.sqrt(dx * dx + dy * dy);

		if (distance <= step || distance == 0f) {
			x = target.x;
			y = target.y;
			target = null;
			isMoving = false;
			idle();
			return;
		}

		x += dx / distance * step;
		y += dy / distance * step;
	}

	/** Drops any in-flight glide so a re-placed avatar does not slide across the map. */
	@Override
	public void place(int cell) {
		target = null;
		isMoving = false;
		super.place(cell);
	}
}
