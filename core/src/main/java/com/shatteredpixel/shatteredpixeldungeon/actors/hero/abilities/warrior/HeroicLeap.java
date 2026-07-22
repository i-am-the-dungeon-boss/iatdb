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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class HeroicLeap extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	public float chargeUse(Hero hero) {
		float chargeUse = super.chargeUse(hero);
		if (hero.buff(DoubleJumpTracker.class) != null) {
			// reduced charge use by 16%/30%/41%/50%
			chargeUse *= Math.pow(0.84, hero.pointsInTalent(Talent.DOUBLE_JUMP));
		}
		return chargeUse;
	}

	@Override
	public void activate(ClassArmor armor, UseContext ctx, Integer target) {
		Char body = ctx.body;
		Hero kit = ctx.kit;
		if (target != null) {

			if (body.rooted) {
				if (ctx.heroFX) {
					PixelScene.shake(1, 1f);
				}
				return;
			}

			Ballistica route = new Ballistica(body.pos, target, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID);
			int cell = route.collisionPos;

			// can't occupy the same cell as another char, so move back one.
			int backTrace = route.dist - 1;
			while (Actor.findChar(cell) != null && cell != body.pos) {
				cell = route.path.get(backTrace);
				backTrace--;
			}

			armor.charge -= chargeUse(kit);
			armor.updateQuickslot();

			final int dest = cell;
			Runnable afterJump = new Runnable() {
				@Override
				public void run() {
					body.move(dest, false);
					Dungeon.observe();
					if (ctx.heroFX) {
						GameScene.updateFog();
					}

					for (int i : PathFinder.NEIGHBOURS8) {
						Char mob = Actor.findChar(body.pos + i);
						if (mob != null && mob != body && mob.alignment != Char.Alignment.ALLY) {
							if (kit.hasTalent(Talent.BODY_SLAM)) {
								int damage = Hero.heroDamageIntRange(kit.pointsInTalent(Talent.BODY_SLAM),
										4 * kit.pointsInTalent(Talent.BODY_SLAM));
								damage += Math.round(kit.drRoll() * 0.25f * kit.pointsInTalent(Talent.BODY_SLAM));
								damage -= mob.drRoll();
								mob.damage(damage, kit);
							}
							if (mob.pos == body.pos + i && kit.hasTalent(Talent.IMPACT_WAVE)) {
								Ballistica trajectory = new Ballistica(mob.pos, mob.pos + i, Ballistica.MAGIC_BOLT);
								int strength = 1 + kit.pointsInTalent(Talent.IMPACT_WAVE);
								WandOfBlastWave.throwChar(mob, trajectory, strength, true, true, HeroicLeap.this);
								if (Random.Int(4) < kit.pointsInTalent(Talent.IMPACT_WAVE)) {
									Buff.prolong(mob, Vulnerable.class, 5f);
								}
							}
						}
					}

					if (ctx.heroFX) {
						WandOfBlastWave.BlastWave.blast(dest);
						PixelScene.shake(2, 0.5f);
					}
					Invisibility.dispel(body);
					ctx.turns.spendAfterThrow(Actor.TICK);

					if (kit.buff(DoubleJumpTracker.class) != null) {
						kit.buff(DoubleJumpTracker.class).detach();
					} else {
						if (kit.hasTalent(Talent.DOUBLE_JUMP)) {
							Buff.affect(kit, DoubleJumpTracker.class, 3);
						}
					}
				}
			};

			if (UseContext.canWorldFx(body)) {
				body.sprite.jump(body.pos, cell, new Callback() {
					@Override
					public void call() {
						afterJump.run();
					}
				});
			} else {
				afterJump.run();
			}
		}
	}

	public static class DoubleJumpTracker extends FlavourBuff {
	};

	@Override
	public int icon() {
		return HeroIcon.HEROIC_LEAP;
	}

	@Override
	public Talent[] talents() {
		return new Talent[] { Talent.BODY_SLAM, Talent.IMPACT_WAVE, Talent.DOUBLE_JUMP, Talent.HEROIC_ENERGY };
	}
}
