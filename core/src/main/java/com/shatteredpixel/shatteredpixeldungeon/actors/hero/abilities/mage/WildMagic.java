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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.WondrousResin;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.tweeners.Delayer;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class WildMagic extends ArmorAbility {

	{
		baseChargeUse = 25f;
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
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

		ArrayList<Wand> wands = kit.belongings.getAllItems(Wand.class);
		Random.shuffle(wands);

		float chargeUsePerShot = 0.5f * (float) Math.pow(0.67f, kit.pointsInTalent(Talent.CONSERVED_MAGIC));

		for (Wand w : wands.toArray(new Wand[0])) {
			if (w.curCharges < 1 && w.partialCharge < chargeUsePerShot) {
				wands.remove(w);
			}
		}

		int maxWands = 4 + kit.pointsInTalent(Talent.FIRE_EVERYTHING);

		// second and third shots
		if (wands.size() < maxWands) {
			ArrayList<Wand> seconds = new ArrayList<>(wands);
			ArrayList<Wand> thirds = new ArrayList<>(wands);

			for (Wand w : wands) {
				float totalCharge = w.curCharges + w.partialCharge;
				if (totalCharge < 2 * chargeUsePerShot) {
					seconds.remove(w);
				}
				if (totalCharge < 3 * chargeUsePerShot
						|| Random.Int(4) >= kit.pointsInTalent(Talent.FIRE_EVERYTHING)) {
					thirds.remove(w);
				}
			}

			Random.shuffle(seconds);
			while (!seconds.isEmpty() && wands.size() < maxWands) {
				wands.add(seconds.remove(0));
			}

			Random.shuffle(thirds);
			while (!thirds.isEmpty() && wands.size() < maxWands) {
				wands.add(thirds.remove(0));
			}
		}

		if (wands.size() == 0) {
			if (ctx.heroFX) {
				GLog.w(Messages.get(this, "no_wands"));
			}
			return;
		}

		Random.shuffle(wands);

		Buff.affect(kit, WildMagicTracker.class, 0f);

		armor.charge -= chargeUse(kit);
		armor.updateQuickslot();

		zapWand(wands, ctx, target);

	}

	public static class WildMagicTracker extends FlavourBuff {
	};

	Actor wildMagicActor = null;

	private void zapWand(ArrayList<Wand> wands, UseContext ctx, int cell) {
		Hero kit = ctx.kit;
		Char body = ctx.body;
		Wand cur = wands.remove(0);

		Ballistica aim = new Ballistica(body.pos, cell, cur.collisionProperties(cell));

		if (body.sprite != null) {
			body.sprite.zap(cell);
		}

		float startTime = Game.timeTotal;
		if (cur.tryToZap(kit, cell)) {
			if (!cur.cursed) {
				if (body.sprite != null && body.sprite.parent != null) {
					cur.fx(aim, new Callback() {
						@Override
						public void call() {
							cur.onZap(aim);
							boolean alsoCursedZap = ctx.heroFX
									&& Random.Float() < WondrousResin.extraCurseEffectChance();
							if (ctx.heroFX && Game.timeTotal - startTime < 0.33f) {
								body.sprite.parent.add(new Delayer(0.33f - (Game.timeTotal - startTime)) {
									@Override
									protected void onComplete() {
										if (alsoCursedZap) {
											WondrousResin.forcePositive = true;
											CursedWand.cursedZap(cur,
													kit,
													new Ballistica(body.pos, cell, Ballistica.MAGIC_BOLT),
													new Callback() {
														@Override
														public void call() {
															WondrousResin.forcePositive = false;
															afterZap(cur, wands, ctx, cell);
														}
													});
										} else {
											afterZap(cur, wands, ctx, cell);
										}
									}
								});
							} else {
								if (alsoCursedZap) {
									WondrousResin.forcePositive = true;
									CursedWand.cursedZap(cur,
											kit,
											new Ballistica(body.pos, cell, Ballistica.MAGIC_BOLT),
											new Callback() {
												@Override
												public void call() {
													WondrousResin.forcePositive = false;
													afterZap(cur, wands, ctx, cell);
												}
											});
								} else {
									afterZap(cur, wands, ctx, cell);
								}
							}
						}
					});
				} else {
					cur.onZap(aim);
					afterZap(cur, wands, ctx, cell);
				}

			} else {
				if (body.sprite != null && body.sprite.parent != null) {
					CursedWand.cursedZap(cur,
							kit,
							new Ballistica(body.pos, cell, Ballistica.MAGIC_BOLT),
							new Callback() {
								@Override
								public void call() {
									if (ctx.heroFX && Game.timeTotal - startTime < 0.33f) {
										body.sprite.parent.add(new Delayer(0.33f - (Game.timeTotal - startTime)) {
											@Override
											protected void onComplete() {
												afterZap(cur, wands, ctx, cell);
											}
										});
									} else {
										afterZap(cur, wands, ctx, cell);
									}
								}
							});
				} else {
					cur.onZap(aim);
					afterZap(cur, wands, ctx, cell);
				}
			}
		} else {
			afterZap(cur, wands, ctx, cell);
		}
	}

	private void afterZap(Wand cur, ArrayList<Wand> wands, UseContext ctx, int target) {
		Hero kit = ctx.kit;
		cur.partialCharge -= 0.5f * (float) Math.pow(0.67f, kit.pointsInTalent(Talent.CONSERVED_MAGIC));
		if (cur.partialCharge < 0) {
			cur.partialCharge++;
			cur.curCharges--;
		}
		if (wildMagicActor != null) {
			wildMagicActor.next();
			wildMagicActor = null;
		}

		Char ch = Actor.findChar(target);
		if (!wands.isEmpty() && kit.isAlive()) {
			Actor.add(new Actor() {
				{
					actPriority = VFX_PRIO - 1;
				}

				@Override
				protected boolean act() {
					wildMagicActor = this;
					zapWand(wands, ctx, ch == null ? target : ch.pos);
					Actor.remove(this);
					return false;
				}
			});
			if (ctx.heroFX) {
				kit.next();
			}
		} else {
			if (kit.buff(WildMagicTracker.class) != null) {
				kit.buff(WildMagicTracker.class).detach();
			}
			if (ctx.heroFX) {
				Item.updateQuickslot();
			}
			Invisibility.dispel(ctx.body);
			if (Random.Int(4) >= kit.pointsInTalent(Talent.CONSERVED_MAGIC)) {
				ctx.turns.spendAfterThrow(Actor.TICK);
			} else if (ctx.heroFX) {
				kit.next();
			}
		}
	}

	@Override
	public int icon() {
		return HeroIcon.WILD_MAGIC;
	}

	@Override
	public Talent[] talents() {
		return new Talent[] { Talent.WILD_POWER, Talent.FIRE_EVERYTHING, Talent.CONSERVED_MAGIC, Talent.HEROIC_ENERGY };
	}
}
