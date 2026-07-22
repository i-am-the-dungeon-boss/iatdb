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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combo;
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
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;

public class Sai extends MeleeWeapon {

	{
		image = ItemSpriteSheet.SAI;
		hitSound = Assets.Sounds.HIT_STAB;
		hitSoundPitch = 1.3f;

		tier = 3;
		DLY = 0.5f; // 2x speed
	}

	@Override
	public int max(int lvl) {
		return Math.round(2.5f * (tier + 1)) + // 10 base, down from 20
				lvl * Math.round(0.5f * (tier + 1)); // +2 per level, down from +4
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	protected boolean duelistAbility(UseContext ctx, Integer target) {
		int dmgBoost = augment.damageFactor(4 + buffedLvl());
		return comboStrikeAbility(ctx, target, 0, dmgBoost, this);
	}

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		duelistAbility(UseContext.hero(hero), target);
	}

	@Override
	public String abilityInfo() {
		int dmgBoost = levelKnown ? 4 + buffedLvl() : 4;
		if (levelKnown) {
			return Messages.get(this, "ability_desc", augment.damageFactor(dmgBoost));
		} else {
			return Messages.get(this, "typical_ability_desc", augment.damageFactor(dmgBoost));
		}
	}

	public String upgradeAbilityStat(int level) {
		return "+" + augment.damageFactor(4 + level);
	}

	public static boolean comboStrikeAbility(UseContext ctx, Integer target, float multiPerHit, int boostPerHit,
			MeleeWeapon wep) {
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

					int recentHits = 0;
					ComboStrikeTracker buff = kit.buff(ComboStrikeTracker.class);
					if (buff != null) {
						recentHits = buff.hits;
						buff.detach();
					}

					boolean hit = kit.attack(enemy, 1f + multiPerHit * recentHits, boostPerHit * recentHits,
							Char.INFINITE_ACCURACY);
					if (hit && !enemy.isAlive()) {
						wep.onAbilityKill(kit, enemy);
					}

					Invisibility.dispel(body);
					if (ctx.heroFX) {
						kit.spendAndNext(kit.attackDelay());
					}
					if (recentHits >= 2 && hit && UseContext.canWorldFx(kit)) {
						Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
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

	public static void comboStrikeAbility(Hero hero, Integer target, float multiPerHit, int boostPerHit,
			MeleeWeapon wep) {
		comboStrikeAbility(UseContext.hero(hero), target, multiPerHit, boostPerHit, wep);
	}

	public static class ComboStrikeTracker extends Buff {

		{
			type = buffType.POSITIVE;
		}

		public static int DURATION = 5;
		private float comboTime = 0f;
		public int hits = 0;

		@Override
		public int icon() {
			if (Dungeon.hero.belongings.weapon() instanceof Gloves
					|| Dungeon.hero.belongings.weapon() instanceof Sai
					|| Dungeon.hero.belongings.weapon() instanceof Gauntlet
					|| Dungeon.hero.belongings.secondWep() instanceof Gloves
					|| Dungeon.hero.belongings.secondWep() instanceof Sai
					|| Dungeon.hero.belongings.secondWep() instanceof Gauntlet) {
				return BuffIndicator.DUEL_COMBO;
			} else {
				return BuffIndicator.NONE;
			}
		}

		@Override
		public boolean act() {
			comboTime -= TICK;
			spend(TICK);
			if (comboTime <= 0) {
				detach();
			}
			return true;
		}

		public void addHit() {
			hits++;
			comboTime = 5f;

			if (hits >= 2 && icon() != BuffIndicator.NONE) {
				GLog.p(Messages.get(Combo.class, "combo", hits));
			}
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (DURATION - comboTime) / DURATION);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString((int) comboTime);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", hits, dispTurns(comboTime));
		}

		private static final String TIME = "combo_time";
		public static String RECENT_HITS = "recent_hits";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(TIME, comboTime);
			bundle.put(RECENT_HITS, hits);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			comboTime = bundle.getInt(TIME);
			hits = bundle.getInt(RECENT_HITS);
		}
	}

}
