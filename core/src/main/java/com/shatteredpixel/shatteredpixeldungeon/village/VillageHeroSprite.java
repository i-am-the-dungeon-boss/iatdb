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

package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;

/**
 * The town avatar's sprite: a warrior in tier-1 cloth, whatever class is being
 * played.
 *
 * <p>Exists so that {@code HeroSprite} needs to know nothing about the village.
 * It offers {@code appearanceClass()} and {@code appearanceTier()} as plain
 * "how is this hero drawn" questions, and the answers for town live here, in the
 * village package, alongside {@link VillageHero} which decides when to use them.
 * The dungeon side has no reference running the other way.
 *
 * <p>The appearance is deliberately <em>not</em> shared with the sprite used for
 * remote players. The two coincide today, but once players can pick an avatar
 * this one comes from the local player's own choice and a remote one from that
 * player's presence data — independent inputs, so a shared constant would claim
 * an invariant that is not real.
 *
 * <p>Both values are {@code static} on purpose: {@code HeroSprite}'s constructor
 * calls these accessors, which runs before any instance field of this class
 * would be assigned.
 */
public class VillageHeroSprite extends HeroSprite {

	private static final HeroClass APPEARANCE = HeroClass.WARRIOR;
	/** Cloth armour — {@code ClothArmor} is tier 1, and tier 0 is bare-chested. */
	private static final int TIER = 1;

	@Override
	protected HeroClass appearanceClass() {
		return APPEARANCE;
	}

	@Override
	protected int appearanceTier() {
		return TIER;
	}
}
