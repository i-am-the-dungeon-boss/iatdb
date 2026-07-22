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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

public class Sword extends MeleeWeapon {

	{
		image = ItemSpriteSheet.SWORD;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1f;

		tier = 3;
	}

	@Override
	protected int baseChargeUse(Hero hero, Char target) {
		if (hero.buff(Sword.CleaveTracker.class) != null) {
			return 0;
		} else {
			return 1;
		}
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	protected boolean duelistAbility(UseContext ctx, Integer target) {
		int dmgBoost = augment.damageFactor(5 + buffedLvl());
		return Sword.cleaveAbility(ctx, target, 1, dmgBoost, this);
	}

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		duelistAbility(UseContext.hero(hero), target);
	}

	@Override
	public String abilityInfo() {
		int dmgBoost = levelKnown ? 5 + buffedLvl() : 5;
		if (levelKnown) {
			return Messages.get(this, "ability_desc", augment.damageFactor(min() + dmgBoost),
					augment.damageFactor(max() + dmgBoost));
		} else {
			return Messages.get(this, "typical_ability_desc", min(0) + dmgBoost, max(0) + dmgBoost);
		}
	}

	public String upgradeAbilityStat(int level) {
		int dmgBoost = 5 + level;
		return augment.damageFactor(min(level) + dmgBoost) + "-" + augment.damageFactor(max(level) + dmgBoost);
	}

	/**
	 * Shared cleave for Hero and Echo. Kit is borrowed onto body pos/sprite for
	 * canAttack / attack (same pattern as SpectralBlades).
	 */
	public static boolean cleaveAbility(UseContext ctx, Integer target, float dmgMulti, int dmgBoost, MeleeWeapon wep) {
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
			if (!kit.canAttack(enemy)) {
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
					if (kit.attack(enemy, dmgMulti, dmgBoost, Char.INFINITE_ACCURACY)) {
						if (UseContext.canWorldFx(kit)) {
							Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
						}
					}

					Invisibility.dispel(body);

					if (!enemy.isAlive()) {
						if (ctx.heroFX) {
							kit.next();
						}
						wep.onAbilityKill(kit, enemy);
						if (kit.buff(CleaveTracker.class) != null) {
							kit.buff(CleaveTracker.class).detach();
						} else {
							Buff.prolong(kit, CleaveTracker.class, 4f); // 1 less as attack was instant
						}
					} else {
						if (ctx.heroFX) {
							kit.spendAndNext(kit.attackDelay());
						}
						if (kit.buff(CleaveTracker.class) != null) {
							kit.buff(CleaveTracker.class).detach();
						}
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

	/**
	 * @deprecated use
	 *             {@link #cleaveAbility(UseContext, Integer, float, int, MeleeWeapon)}
	 */
	@Deprecated
	public static void cleaveAbility(Hero hero, Integer target, float dmgMulti, int dmgBoost, MeleeWeapon wep) {
		cleaveAbility(UseContext.hero(hero), target, dmgMulti, dmgBoost, wep);
	}

	public static class CleaveTracker extends FlavourBuff {

		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.DUEL_CLEAVE;
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (5 - visualcooldown()) / 5);
		}
	}

}
