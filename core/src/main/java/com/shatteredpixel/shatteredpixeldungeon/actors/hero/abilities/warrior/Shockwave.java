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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combo;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

public class Shockwave extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	public int targetedPos(Char user, int dst) {
		return new Ballistica(user.pos, dst, Ballistica.STOP_SOLID | Ballistica.STOP_TARGET).collisionPos;
	}

	@Override
	protected void activate(ClassArmor armor, UseContext ctx, Integer target) {
		Char body = ctx.body;
		Hero kit = ctx.kit;
		if (target == null) {
			return;
		}
		if (target == body.pos) {
			if (ctx.heroFX) {
				GLog.w(Messages.get(this, "self_target"));
			}
			return;
		}

		armor.charge -= chargeUse(kit);
		armor.updateQuickslot();

		Ballistica aim = new Ballistica(body.pos, target, Ballistica.WONT_STOP);

		int maxDist = 5 + kit.pointsInTalent(Talent.EXPANDING_WAVE);
		int dist = Math.min(aim.dist, maxDist);

		ConeAOE cone = new ConeAOE(aim,
				dist,
				60 + 15 * kit.pointsInTalent(Talent.EXPANDING_WAVE),
				Ballistica.STOP_SOLID | Ballistica.STOP_TARGET);

		Callback applyEffect = new Callback() {
			@Override
			public void call() {

				for (int cell : cone.cells) {

					Char ch = Actor.findChar(cell);
					if (ch != null && ch.alignment != body.alignment) {
						int scalingStr = kit.STR() - 10;
						int damage = Hero.heroDamageIntRange(5 + scalingStr, 10 + 2 * scalingStr);
						damage = Math.round(damage * (1f + 0.2f * kit.pointsInTalent(Talent.SHOCK_FORCE)));
						damage -= ch.drRoll();

						if (kit.pointsInTalent(Talent.STRIKING_WAVE) == 4) {
							Buff.affect(kit, Talent.StrikingWaveTracker.class, 0f);
						}

						if (Random.Int(10) < 3 * kit.pointsInTalent(Talent.STRIKING_WAVE)) {
							boolean wasEnemy = ch.alignment == Char.Alignment.ENEMY
									|| (ch instanceof Mimic && ch.alignment == Char.Alignment.NEUTRAL);
							damage = kit.attackProc(ch, damage);
							ch.damage(damage, kit);
							if (kit.subClass == HeroSubClass.GLADIATOR && wasEnemy) {
								Buff.affect(kit, Combo.class).hit(ch);
							}
						} else {
							ch.damage(damage, kit);
						}
						if (ch.isAlive()) {
							if (Random.Int(4) < kit.pointsInTalent(Talent.SHOCK_FORCE)) {
								Buff.affect(ch, Paralysis.class, 5f);
							} else {
								Buff.affect(ch, Cripple.class, 5f);
							}
						}

					}
				}

				Invisibility.dispel(body);
				ctx.turns.spendAfterThrow(Actor.TICK);

			}
		};

		if (UseContext.canWorldFx(body)) {
			// cast to cells at the tip, rather than all cells, better performance.
			for (Ballistica ray : cone.outerRays) {
				((MagicMissile) body.sprite.parent.recycle(MagicMissile.class)).reset(
						MagicMissile.FORCE_CONE,
						body.sprite,
						ray.path.get(ray.dist),
						null);
			}

			body.sprite.zap(target);
			Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.5f);
			PixelScene.shake(2, 0.5f);
			// final zap at 2/3 distance, for timing of the actual effect
			MagicMissile.boltFromChar(body.sprite.parent,
					MagicMissile.FORCE_CONE,
					body.sprite,
					cone.coreRay.path.get(dist * 2 / 3),
					applyEffect);
		} else {
			applyEffect.call();
		}
	}

	@Override
	public int icon() {
		return HeroIcon.SHOCKWAVE;
	}

	@Override
	public Talent[] talents() {
		return new Talent[] { Talent.EXPANDING_WAVE, Talent.STRIKING_WAVE, Talent.SHOCK_FORCE, Talent.HEROIC_ENERGY };
	}
}
