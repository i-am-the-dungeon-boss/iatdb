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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RoundShield;
import com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

public class Invisibility extends FlavourBuff {

	public static final float DURATION = 20f;

	{
		type = buffType.POSITIVE;
		announced = true;
	}

	@Override
	public boolean attachTo(Char target) {
		if (super.attachTo(target)) {
			target.invisible++;
			if (target instanceof Hero && ((Hero) target).subClass == HeroSubClass.ASSASSIN) {
				Buff.affect(target, Preparation.class);
			}
			if (target instanceof Hero && ((Hero) target).hasTalent(Talent.PROTECTIVE_SHADOWS)) {
				Buff.affect(target, Talent.ProtectiveShadowsTracker.class);
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void detach() {
		if (target.invisible > 0)
			target.invisible--;
		super.detach();
	}

	@Override
	public int icon() {
		return BuffIndicator.INVISIBLE;
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (DURATION - visualcooldown()) / DURATION);
	}

	@Override
	public void fx(boolean on) {
		if (on)
			target.sprite.add(CharSprite.State.INVISIBLE);
		else if (target.invisible == 0)
			target.sprite.remove(CharSprite.State.INVISIBLE);
	}

	public static void dispel() {
		if (Dungeon.hero == null)
			return;

		dispel(Dungeon.hero);
	}

	/**
	 * Throwing is a reveal. The act of hurling something gives away where the
	 * thrower is standing, so every throwable — missile weapon, spirit bow
	 * arrow, potion, bomb — breaks invisibility whether or not it connects.
	 *
	 * <p>Every throw entry point routes through here once the throw has
	 * resolved, never before it: the shot itself is still fired from stealth,
	 * so surprise-attack accuracy and {@link Preparation} damage apply as
	 * usual, and only then is the thrower exposed.
	 *
	 * <p>{@code thrower} is the char that actually stands on the level. For an
	 * Echo that is the {@code EchoBoss} body, never its off-stage phantom kit —
	 * dispelling the kit reveals nobody.
	 */
	public static void dispelOnThrow(Char thrower) {
		if (thrower == null) {
			return;
		}
		dispel(thrower);
	}

	public static void dispel(Char ch) {

		for (Buff invis : ch.buffs(Invisibility.class)) {
			invis.detach();
		}
		// Use instanceof — Char.buff(Class) requires exact class equality, which is
		// brittle for CloakOfShadows' non-static cloakStealth inner class.
		for (Buff b : ch.buffs().toArray(new Buff[0])) {
			if (b instanceof CloakOfShadows.cloakStealth) {
				((CloakOfShadows.cloakStealth) b).dispel();
			}
		}

		// these aren't forms of invisibility, but do dispel at the same time as it.
		TimekeepersHourglass.timeFreeze timeFreeze = ch.buff(TimekeepersHourglass.timeFreeze.class);
		if (timeFreeze != null) {
			timeFreeze.detach();
		}

		Preparation prep = ch.buff(Preparation.class);
		if (prep != null) {
			prep.detach();
		}

		Swiftthistle.TimeBubble bubble = ch.buff(Swiftthistle.TimeBubble.class);
		if (bubble != null) {
			bubble.detach();
		}

		RoundShield.GuardTracker guard = ch.buff(RoundShield.GuardTracker.class);
		if (guard != null && guard.hasBlocked) {
			guard.detach();
		}

		// Some sprites fully un-render while stealthed (EchoBossSprite sets
		// visible=false and alpha 0), and the buff's own fx(false) is skipped
		// whenever the reveal came from something other than an Invisibility
		// detach. Force the state off here so no caller has to remember to.
		if (ch.invisible <= 0 && ch.sprite != null) {
			ch.sprite.remove(CharSprite.State.INVISIBLE);
		}
	}
}
