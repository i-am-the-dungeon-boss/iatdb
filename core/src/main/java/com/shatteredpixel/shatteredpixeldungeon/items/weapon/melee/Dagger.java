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
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;

public class Dagger extends MeleeWeapon {

	{
		image = ItemSpriteSheet.DAGGER;
		hitSound = Assets.Sounds.HIT_STAB;
		hitSoundPitch = 1.1f;

		tier = 1;

		bones = false;
	}

	@Override
	public int max(int lvl) {
		return 4 * (tier + 1) + // 8 base, down from 10
				lvl * (tier + 1); // scaling unchanged
	}

	@Override
	public int damageRoll(Char owner) {
		if (owner instanceof Hero) {
			Hero hero = (Hero) owner;
			Char enemy = hero.attackTarget();
			if (enemy instanceof Mob && ((Mob) enemy).surprisedBy(hero)) {
				// deals 75% toward max to max on surprise, instead of min to max.
				int diff = max() - min();
				int damage = augment.damageFactor(Hero.heroDamageIntRange(
						min() + Math.round(diff * 0.75f),
						max()));
				int exStr = hero.STR() - STRReq();
				if (exStr > 0) {
					damage += Hero.heroDamageIntRange(0, exStr);
				}
				return damage;
			}
		}
		return super.damageRoll(owner);
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	public boolean useTargeting() {
		return false;
	}

	@Override
	protected boolean duelistAbility(UseContext ctx, Integer target) {
		return sneakAbility(ctx, target, 5, 2 + buffedLvl(), this);
	}

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		duelistAbility(UseContext.hero(hero), target);
	}

	@Override
	public String abilityInfo() {
		if (levelKnown) {
			return Messages.get(this, "ability_desc", 2 + buffedLvl());
		} else {
			return Messages.get(this, "typical_ability_desc", 2);
		}
	}

	@Override
	public String upgradeAbilityStat(int level) {
		return Integer.toString(2 + level);
	}

	public static boolean sneakAbility(UseContext ctx, Integer target, int maxDist, int invisTurns, MeleeWeapon wep) {
		if (target == null) {
			return false;
		}

		Char body = ctx.body;

		PathFinder.buildDistanceMap(body.pos, BArray.or(Dungeon.level.passable, Dungeon.level.avoid, null), maxDist);
		boolean inFov = body.fieldOfView != null && target < body.fieldOfView.length
				? body.fieldOfView[target]
				: Dungeon.level.heroFOV[target];
		if (PathFinder.distance[target] == Integer.MAX_VALUE || !inFov || body.rooted) {
			if (ctx.heroFX) {
				GLog.w(Messages.get(wep, "ability_target_range"));
				if (body.rooted)
					PixelScene.shake(1, 1f);
			}
			return false;
		}

		if (Actor.findChar(target) != null) {
			if (ctx.heroFX) {
				GLog.w(Messages.get(wep, "ability_occupied"));
			}
			return false;
		}

		wep.beforeAbilityUsed(ctx, null);
		Buff.prolong(body, Invisibility.class, invisTurns - 1);

		if (body.sprite != null) {
			body.sprite.turnTo(body.pos, target);
		}
		body.move(target, false);
		if (UseContext.canWorldFx(body)) {
			Sample.INSTANCE.play(Assets.Sounds.PUFF);
			if (Game.instance != null && Game.scene() instanceof GameScene) {
				CellEmitter.get(body.pos).burst(Speck.factory(Speck.WOOL), 6);
			}
		}
		if (ctx.heroFX) {
			Dungeon.observe();
			GameScene.updateFog();
			if (body instanceof Hero) {
				((Hero) body).checkVisibleMobs();
			}
		}

		if (body instanceof Hero) {
			((Hero) body).next();
		}
		wep.afterAbilityUsed(ctx);
		return true;
	}

	public static void sneakAbility(Hero hero, Integer target, int maxDist, int invisTurns, MeleeWeapon wep) {
		sneakAbility(UseContext.hero(hero), target, maxDist, invisTurns, wep);
	}
}
