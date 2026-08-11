/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;

/**
 * The town avatar: a {@link Hero} in everything the presentation layer touches,
 * with none of the run underneath it.
 *
 * <p>It has to <em>be</em> a hero — {@code HeroSprite}, {@code StatusPane},
 * {@code Toolbar}, {@code GameScene} and {@code CellSelector} all read
 * {@code Dungeon.hero}, so a parallel town object would mean forking every one
 * of them. Movement, examining, interacting and the whole {@code handle(int)}
 * funnel are inherited untouched; that is what "the same controls" means.
 *
 * <p>What is overridden is only the run machinery: no hunger, no regeneration,
 * no damage, no death, no experience. The village is peaceful because of its
 * content — every mob there is an NPC and there are no heaps — not because the
 * hero was defanged.
 */
public class VillageHero extends Hero {

	public VillageHero(HeroClass heroClass) {
		super();
		this.heroClass = heroClass == null ? HeroClass.WARRIOR : heroClass;

		// Talent tiers must exist even though the town avatar never levels:
		// Hero.talentPointsSpent does talents.get(tier - 1), and WndHero is
		// reachable from the status-pane avatar in town. Empty tiers grant
		// nothing, since points come from levelling.
		Talent.initClassTalents(this);
	}

	/** No {@code Regeneration}, no {@code Hunger}: neither belongs in town. */
	@Override
	public void live() {
		// deliberately empty
	}

	@Override
	public void damage(int dmg, Object src) {
		// nothing in the village can hurt the town avatar
	}

	@Override
	public void die(Object cause) {
		// no rankings, no resurrect window, no deleted save: there is no run here
	}

	@Override
	public boolean isAlive() {
		return true;
	}

	@Override
	public void earnExp(int exp, Class source) {
		// the town avatar stays at level 1 with no experience
	}

	@Override
	public boolean search(boolean intentional) {
		// the village map is fully revealed; the search button is hidden in town
		return false;
	}

}
