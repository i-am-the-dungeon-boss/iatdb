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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

public class Rapier extends MeleeWeapon {

	{
		image = ItemSpriteSheet.RAPIER;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1.3f;

		tier = 1;

		bones = false;
	}

	@Override
	public int max(int lvl) {
		return 4 * (tier + 1) + // 8 base, down from 10
				lvl * (tier + 1); // scaling unchanged
	}

	@Override
	public int defenseFactor(Char owner) {
		return 1; // 1 extra defence
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	protected boolean duelistAbility(UseContext ctx, Integer target) {
		// +(5+1.5*lvl) damage, roughly +111% base damage, +100% scaling
		int dmgBoost = augment.damageFactor(5 + Math.round(1.5f * buffedLvl()));
		return lungeAbility(ctx, target, 1, dmgBoost, this);
	}

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		duelistAbility(UseContext.hero(hero), target);
	}

	@Override
	public String abilityInfo() {
		int dmgBoost = levelKnown ? 5 + Math.round(1.5f * buffedLvl()) : 5;
		if (levelKnown) {
			return Messages.get(this, "ability_desc", augment.damageFactor(min() + dmgBoost),
					augment.damageFactor(max() + dmgBoost));
		} else {
			return Messages.get(this, "typical_ability_desc", min(0) + dmgBoost, max(0) + dmgBoost);
		}
	}

	public String upgradeAbilityStat(int level) {
		int dmgBoost = 5 + Math.round(1.5f * level);
		return augment.damageFactor(min(level) + dmgBoost) + "-" + augment.damageFactor(max(level) + dmgBoost);
	}

	public static boolean lungeAbility(UseContext ctx, Integer target, float dmgMulti, int dmgBoost, MeleeWeapon wep) {
		if (target == null) {
			return false;
		}

		Char body = ctx.body;
		Hero kit = ctx.kit;

		Char enemy = Actor.findChar(target);
		boolean inFov = body.fieldOfView != null && target < body.fieldOfView.length
				? body.fieldOfView[target]
				: Dungeon.level.heroFOV[target];
		// duelist can lunge out of FOV, but this wastes the ability instead of
		// cancelling if there is no target
		if (inFov) {
			if (enemy == null || enemy == body || kit.isCharmedBy(enemy)) {
				if (ctx.heroFX) {
					GLog.w(Messages.get(wep, "ability_no_target"));
				}
				return false;
			}
		}

		if (body.rooted || Dungeon.level.distance(body.pos, target) < 2
				|| Dungeon.level.distance(body.pos, target) - 1 > wep.reachFactor(kit)) {
			if (ctx.heroFX) {
				GLog.w(Messages.get(wep, "ability_target_range"));
				if (body.rooted)
					PixelScene.shake(1, 1f);
			}
			return false;
		}

		int lungeCell = -1;
		for (int i : PathFinder.NEIGHBOURS8) {
			int cell = body.pos + i;
			if (cell < 0 || cell >= Dungeon.level.length()) {
				continue;
			}
			if (Dungeon.level.distance(cell, target) <= wep.reachFactor(kit)
					&& Actor.findChar(cell) == null
					&& (Dungeon.level.passable[cell] || (Dungeon.level.avoid[cell] && body.flying))) {
				if (lungeCell == -1
						|| Dungeon.level.trueDistance(cell, target) < Dungeon.level.trueDistance(lungeCell, target)) {
					lungeCell = cell;
				}
			}
		}

		if (lungeCell == -1) {
			if (ctx.heroFX) {
				GLog.w(Messages.get(wep, "ability_target_range"));
			}
			return false;
		}

		final int dest = lungeCell;
		final Char foe = enemy;

		if (ctx.heroFX) {
			ctx.turns.busy();
		}

		Runnable finishLunge = () -> {
			body.move(dest, false);
			if (ctx.heroFX) {
				Dungeon.observe();
			}

			int savedPos = kit.pos;
			CharSprite savedSprite = kit.sprite;
			boolean borrow = body != kit;
			if (borrow) {
				kit.pos = body.pos;
				kit.sprite = body.sprite;
			}
			try {
				kit.belongings.abilityWeapon = wep; // set this early so we can check canAttack
				if (foe != null && kit.canAttack(foe)) {
					Runnable doHit = () -> {
						wep.beforeAbilityUsed(ctx, foe);
						if (ctx.heroFX) {
							AttackIndicator.target(foe);
						}
						if (kit.attack(foe, dmgMulti, dmgBoost, Char.INFINITE_ACCURACY)) {
							if (UseContext.canWorldFx(kit)) {
								Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
							}
							if (!foe.isAlive()) {
								wep.onAbilityKill(kit, foe);
							}
						}
						Invisibility.dispel(body);
						if (ctx.heroFX) {
							kit.spendAndNext(kit.attackDelay());
						}
						wep.afterAbilityUsed(ctx);
					};
					if (ctx.heroFX && UseContext.canWorldFx(kit)) {
						kit.sprite.attack(foe.pos, doHit::run);
					} else {
						if (UseContext.canWorldFx(kit)) {
							kit.sprite.attack(foe.pos);
						}
						doHit.run();
					}
				} else {
					// spends charge but otherwise does not count as an ability use
					Charger charger = Buff.affect(kit, Charger.class);
					charger.partialCharge -= 1;
					while (charger.partialCharge < 0 && charger.charges > 0) {
						charger.charges--;
						charger.partialCharge++;
					}
					if (ctx.heroFX) {
						updateQuickslot();
						GLog.w(Messages.get(Rapier.class, "ability_no_target"));
						kit.spendAndNext(1 / kit.speed());
					}
				}
			} finally {
				if (borrow) {
					kit.pos = savedPos;
					kit.sprite = savedSprite;
				}
			}
		};

		if (UseContext.canWorldFx(body)) {
			Sample.INSTANCE.play(Assets.Sounds.MISS);
			body.sprite.jump(body.pos, dest, 0, 0.1f, finishLunge::run);
		} else {
			finishLunge.run();
		}
		return true;
	}

	public static void lungeAbility(Hero hero, Integer target, float dmgMulti, int dmgBoost, MeleeWeapon wep) {
		lungeAbility(UseContext.hero(hero), target, dmgMulti, dmgBoost, wep);
	}
}
