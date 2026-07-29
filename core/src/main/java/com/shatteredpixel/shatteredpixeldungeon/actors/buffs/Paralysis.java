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

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class Paralysis extends FlavourBuff {

	public static final float DURATION = 10f;

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	@Override
	public boolean attachTo(Char target) {
		if (super.attachTo(target)) {
			target.paralysed++;
			return true;
		} else {
			return false;
		}
	}

	public void processDamage(int damage) {
		if (target == null)
			return;
		ParalysisResist resist = target.buff(ParalysisResist.class);
		if (resist == null) {
			resist = Buff.affect(target, ParalysisResist.class);
		}
		resist.damage += damage;
		if (Random.NormalIntRange(0, resist.damage) >= Random.NormalIntRange(0, target.HP)) {
			if (Dungeon.level.heroFOV[target.pos]) {
				target.sprite.showStatus(CharSprite.NEUTRAL, Messages.get(this, "out"));
			}
			detach();
		}
	}

	@Override
	public void detach() {
		Char ch = target;
		super.detach();
		if (ch != null && ch.paralysed > 0) {
			ch.paralysed--;
		}
		if (ch instanceof Hero || ch instanceof EchoBoss) {
			Buff.prolong(ch, Immunity.class, Immunity.DURATION);
		}
	}

	@Override
	public int icon() {
		return BuffIndicator.PARALYSIS;
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (DURATION - visualcooldown()) / DURATION);
	}

	@Override
	public void fx(boolean on) {
		if (on)
			target.sprite.add(CharSprite.State.PARALYSED);
		else if (target.paralysed <= 1)
			target.sprite.remove(CharSprite.State.PARALYSED);
	}

	/**
	 * Brief lockout after paralysis so Hero / EchoBoss are not immediately
	 * re-stunned.
	 */
	public static class Immunity extends FlavourBuff {

		public static final float DURATION = 3f;

		{
			type = buffType.POSITIVE;
			immunities.add(Paralysis.class);
			immunities.add(ParalyticGas.class);
		}

		@Override
		public int icon() {
			return BuffIndicator.IMMUNITY;
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (DURATION - visualcooldown()) / DURATION);
		}
	}

	public static class ParalysisResist extends Buff {

		{
			type = buffType.POSITIVE;
		}

		private int damage;

		@Override
		public boolean act() {
			if (target.buff(Paralysis.class) == null) {
				damage -= Math.ceil(damage / 10f);
				if (damage <= 0)
					detach();
			}
			spend(TICK);
			return true;
		}

		private static final String DAMAGE = "damage";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(DAMAGE, damage);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			damage = bundle.getInt(DAMAGE);
		}
	}
}
