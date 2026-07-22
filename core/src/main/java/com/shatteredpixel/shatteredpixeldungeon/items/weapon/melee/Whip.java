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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

import java.util.ArrayList;

public class Whip extends MeleeWeapon {

	{
		image = ItemSpriteSheet.WHIP;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1.1f;

		tier = 3;
		RCH = 3; // lots of extra reach
	}

	@Override
	public int max(int lvl) {
		return 5 * (tier) + // 15 base, down from 20
				lvl * (tier); // +3 per level, down from +4
	}

	@Override
	protected boolean duelistAbility(UseContext ctx, Integer target) {
		Char body = ctx.body;
		Hero kit = ctx.kit;

		ArrayList<Char> targets = new ArrayList<>();
		Char closest = null;

		int savedPos = kit.pos;
		CharSprite savedSprite = kit.sprite;
		boolean borrow = body != kit;
		if (borrow) {
			kit.pos = body.pos;
			kit.sprite = body.sprite;
		}
		try {
			kit.belongings.abilityWeapon = this;
			for (Char ch : Actor.chars()) {
				boolean inFov = body.fieldOfView != null && ch.pos < body.fieldOfView.length
						? body.fieldOfView[ch.pos]
						: Dungeon.level.heroFOV[ch.pos];
				if (ch != body
						&& ch.alignment != body.alignment
						&& !kit.isCharmedBy(ch)
						&& inFov
						&& kit.canAttack(ch)) {
					targets.add(ch);
					if (closest == null || Dungeon.level.trueDistance(body.pos, closest.pos) > Dungeon.level
							.trueDistance(body.pos, ch.pos)) {
						closest = ch;
					}
				}
			}
			kit.belongings.abilityWeapon = null;

			if (targets.isEmpty()) {
				if (ctx.heroFX) {
					GLog.w(Messages.get(this, "ability_no_target"));
				}
				return false;
			}

			if (ctx.heroFX) {
				throwSound();
			}
			Char finalClosest = closest;
			Callback doHit = new Callback() {
				@Override
				public void call() {
					beforeAbilityUsed(ctx, finalClosest);
					for (Char ch : targets) {
						kit.attack(ch, 1, 0, Char.INFINITE_ACCURACY);
						if (!ch.isAlive()) {
							onAbilityKill(kit, ch);
						}
					}
					Invisibility.dispel(body);
					if (ctx.heroFX) {
						kit.spendAndNext(kit.attackDelay());
					}
					afterAbilityUsed(ctx);
				}
			};

			if (ctx.heroFX) {
				ctx.turns.busy();
			}
			if (ctx.heroFX && UseContext.canWorldFx(kit)) {
				kit.sprite.attack(body.pos, doHit);
			} else {
				if (UseContext.canWorldFx(kit)) {
					kit.sprite.attack(body.pos);
				}
				doHit.call();
			}
			return true;
		} finally {
			if (borrow) {
				kit.pos = savedPos;
				kit.sprite = savedSprite;
			}
		}
	}

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		duelistAbility(UseContext.hero(hero), target);
	}

	@Override
	public String abilityInfo() {
		if (levelKnown) {
			return Messages.get(this, "ability_desc", augment.damageFactor(min()), augment.damageFactor(max()));
		} else {
			return Messages.get(this, "typical_ability_desc", min(0), max(0));
		}
	}

	public String upgradeAbilityStat(int level) {
		return augment.damageFactor(min(level)) + "-" + augment.damageFactor(max(level));
	}
}
