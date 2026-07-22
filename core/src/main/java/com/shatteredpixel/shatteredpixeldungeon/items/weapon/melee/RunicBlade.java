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
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

public class RunicBlade extends MeleeWeapon {

	{
		image = ItemSpriteSheet.RUNIC_BLADE;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1f;

		tier = 4;
	}

	// Essentially it's a tier 4 weapon, with tier 3 base max damage, and tier 5
	// scaling.
	// equal to tier 4 in damage at +5

	@Override
	public int max(int lvl) {
		return 5 * (tier) + // 20 base, down from 25
				Math.round(lvl * (tier + 2)); // +6 per level, up from +5
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	protected boolean duelistAbility(UseContext ctx, Integer target) {
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
				GLog.w(Messages.get(this, "ability_no_target"));
			}
			return false;
		}

		RunicSlashTracker tracker = Buff.affect(kit, RunicSlashTracker.class);
		tracker.boost = 3f + 0.50f * buffedLvl();

		int savedPos = kit.pos;
		CharSprite savedSprite = kit.sprite;
		boolean borrow = body != kit;
		if (borrow) {
			kit.pos = body.pos;
			kit.sprite = body.sprite;
		}
		try {
			kit.belongings.abilityWeapon = this;
			if (!kit.canAttack(enemy)) {
				if (ctx.heroFX) {
					GLog.w(Messages.get(this, "ability_target_range"));
				}
				tracker.detach();
				kit.belongings.abilityWeapon = null;
				return false;
			}
			kit.belongings.abilityWeapon = null;

			Callback doHit = new Callback() {
				@Override
				public void call() {
					beforeAbilityUsed(ctx, enemy);
					if (ctx.heroFX) {
						AttackIndicator.target(enemy);
					}
					if (kit.attack(enemy, 1f, 0, Char.INFINITE_ACCURACY)) {
						if (UseContext.canWorldFx(kit)) {
							Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
						}
						if (!enemy.isAlive()) {
							onAbilityKill(kit, enemy);
						}
					}
					tracker.detach();
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

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		duelistAbility(UseContext.hero(hero), target);
	}

	@Override
	public String abilityInfo() {
		if (levelKnown) {
			return Messages.get(this, "ability_desc", 300 + 50 * buffedLvl());
		} else {
			return Messages.get(this, "typical_ability_desc", 300);
		}
	}

	@Override
	public String upgradeAbilityStat(int level) {
		return "+" + (300 + 50 * level) + "%";
	}

	public static class RunicSlashTracker extends FlavourBuff {

		public float boost = 2f;

	};

}
