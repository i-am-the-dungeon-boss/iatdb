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
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

public class Spear extends MeleeWeapon {

	{
		image = ItemSpriteSheet.SPEAR;
		hitSound = Assets.Sounds.HIT_STAB;
		hitSoundPitch = 0.9f;

		tier = 2;
		DLY = 1.5f; // 0.67x speed
		RCH = 2; // extra reach
	}

	@Override
	public int max(int lvl) {
		return Math.round(6.67f * (tier + 1)) + // 20 base, up from 15
				lvl * Math.round(1.33f * (tier + 1)); // +4 per level, up from +3
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	protected boolean duelistAbility(UseContext ctx, Integer target) {
		// +(9+2*lvl) damage, roughly +83% base damage, +80% scaling
		int dmgBoost = augment.damageFactor(9 + Math.round(2f * buffedLvl()));
		return spikeAbility(ctx, target, 1, dmgBoost, this);
	}

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		duelistAbility(UseContext.hero(hero), target);
	}

	@Override
	public String abilityInfo() {
		int dmgBoost = levelKnown ? 9 + Math.round(2f * buffedLvl()) : 9;
		if (levelKnown) {
			return Messages.get(this, "ability_desc", augment.damageFactor(min() + dmgBoost),
					augment.damageFactor(max() + dmgBoost));
		} else {
			return Messages.get(this, "typical_ability_desc", min(0) + dmgBoost, max(0) + dmgBoost);
		}
	}

	public String upgradeAbilityStat(int level) {
		int dmgBoost = 9 + Math.round(2f * level);
		return augment.damageFactor(min(level) + dmgBoost) + "-" + augment.damageFactor(max(level) + dmgBoost);
	}

	public static boolean spikeAbility(UseContext ctx, Integer target, float dmgMulti, int dmgBoost, MeleeWeapon wep) {
		if (target == null) {
			return false;
		}

		Char body = ctx.body;
		Hero kit = ctx.kit;

		Char enemy = Actor.findChar(target);
		boolean inFov = body.fieldOfView != null && target < body.fieldOfView.length
				? body.fieldOfView[target]
				: Dungeon.level.heroFOV[target];
		if (enemy == null || enemy == body || kit.isCharmedBy(enemy) || !inFov) {
			if (ctx.heroFX) {
				GLog.w(Messages.get(wep, "ability_no_target"));
			}
			return false;
		}

		int savedPos = kit.pos;
		CharSprite savedSprite = kit.sprite;
		boolean borrow = body != kit;
		if (borrow) {
			kit.pos = body.pos;
			kit.sprite = body.sprite;
		}
		try {
			kit.belongings.abilityWeapon = wep;
			if (Dungeon.level.adjacent(body.pos, enemy.pos) || !wep.canReach(kit, enemy.pos)) {
				if (ctx.heroFX) {
					GLog.w(Messages.get(wep, "ability_target_range"));
				}
				kit.belongings.abilityWeapon = null;
				return false;
			}
			kit.belongings.abilityWeapon = null;

			Callback doHit = new Callback() {
				@Override
				public void call() {
					wep.beforeAbilityUsed(ctx, enemy);
					if (ctx.heroFX) {
						AttackIndicator.target(enemy);
					}
					int oldPos = enemy.pos;
					if (kit.attack(enemy, dmgMulti, dmgBoost, Char.INFINITE_ACCURACY)) {
						if (enemy.isAlive() && enemy.pos == oldPos && !Pushing.pushingExistsForChar(enemy)) {
							Ballistica trajectory = new Ballistica(body.pos, enemy.pos, Ballistica.STOP_TARGET);
							trajectory = new Ballistica(trajectory.collisionPos,
									trajectory.path.get(trajectory.path.size() - 1), Ballistica.PROJECTILE);
							if (ctx.heroFX) {
								WandOfBlastWave.throwChar(enemy, trajectory, 1, true, false, kit);
							}
						} else if (!enemy.isAlive()) {
							wep.onAbilityKill(kit, enemy);
						}
						if (UseContext.canWorldFx(kit)) {
							Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
						}
					}
					Invisibility.dispel(body);
					if (ctx.heroFX) {
						kit.spendAndNext(kit.attackDelay());
					}
					wep.afterAbilityUsed(ctx);
				}
			};

			if (ctx.heroFX) {
				ctx.turns.busy();
			}
			if (ctx.heroFX && UseContext.canWorldFx(kit)) {
				kit.sprite.attack(enemy.pos, doHit);
			} else {
				if (UseContext.canWorldFx(kit)) {
					kit.sprite.attack(enemy.pos);
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

	public static void spikeAbility(Hero hero, Integer target, float dmgMulti, int dmgBoost, MeleeWeapon wep) {
		spikeAbility(UseContext.hero(hero), target, dmgMulti, dmgBoost, wep);
	}

}
