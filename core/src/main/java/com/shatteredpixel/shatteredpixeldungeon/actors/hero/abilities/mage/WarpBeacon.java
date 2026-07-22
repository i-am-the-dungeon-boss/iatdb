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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class WarpBeacon extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	@Override
	public String targetingPrompt() {
		if (Dungeon.hero.buff(WarpBeaconTracker.class) == null
				&& Dungeon.hero.hasTalent(Talent.REMOTE_BEACON)) {
			return Messages.get(this, "prompt");
		}
		return super.targetingPrompt();
	}

	@Override
	public int targetedPos(Char user, int dst) {
		return dst;
	}

	@Override
	protected void activate(ClassArmor armor, UseContext ctx, Integer target) {
		Hero kit = ctx.kit;
		Char body = ctx.body;
		if (target == null) {
			return;
		}

		if (kit.buff(WarpBeaconTracker.class) != null) {
			final WarpBeaconTracker tracker = kit.buff(WarpBeaconTracker.class);
			if (!ctx.heroFX) {
				// Same as Hero window option "teleport" — no UI for Echo
				recallToBeacon(armor, ctx, tracker);
				return;
			}
			GameScene.show(new WndOptions(
					new Image(body.sprite),
					Messages.titleCase(name()),
					Messages.get(WarpBeacon.class, "window_desc", tracker.depth),
					Messages.get(WarpBeacon.class, "window_tele"),
					Messages.get(WarpBeacon.class, "window_clear"),
					Messages.get(WarpBeacon.class, "window_cancel")) {

				@Override
				protected void onSelect(int index) {
					if (index == 0) {
						recallToBeacon(armor, ctx, tracker);
					} else if (index == 1) {
						kit.buff(WarpBeaconTracker.class).detach();
					}
				}
			});

		} else {
			if (!Dungeon.level.mapped[target] && !Dungeon.level.visited[target]) {
				return;
			}

			if (Dungeon.level.distance(body.pos, target) > 4 * kit.pointsInTalent(Talent.REMOTE_BEACON)) {
				if (ctx.heroFX) {
					GLog.w(Messages.get(WarpBeacon.class, "too_far"));
				}
				return;
			}

			PathFinder.buildDistanceMap(target, BArray.or(Dungeon.level.passable, Dungeon.level.avoid, null));
			if (Dungeon.level.pit[target] ||
					(Dungeon.level.solid[target] && !Dungeon.level.passable[target]) ||
					!(Dungeon.level.passable[target] || Dungeon.level.avoid[target]) ||
					PathFinder.distance[body.pos] == Integer.MAX_VALUE) {
				if (ctx.heroFX) {
					GLog.w(Messages.get(WarpBeacon.class, "invalid_beacon"));
				}
				return;
			}

			WarpBeaconTracker tracker = new WarpBeaconTracker();
			tracker.pos = target;
			tracker.depth = Dungeon.depth;
			tracker.branch = Dungeon.branch;
			tracker.attachTo(kit);

			if (UseContext.canWorldFx(body)) {
				body.sprite.operate(target);
				Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
			}
			Invisibility.dispel(body);
			ctx.turns.spendAfterThrow(Actor.TICK);
		}
	}

	/** Shared Hero-window "teleport" / Echo auto-recall. */
	private void recallToBeacon(ClassArmor armor, UseContext ctx, WarpBeaconTracker tracker) {
		Hero kit = ctx.kit;
		Char body = ctx.body;

		if (tracker.depth != Dungeon.depth && !kit.hasTalent(Talent.LONGRANGE_WARP)) {
			if (ctx.heroFX) {
				GLog.w(Messages.get(WarpBeacon.class, "depths"));
			}
			return;
		}

		float chargeNeeded = chargeUse(kit);

		if (tracker.depth != Dungeon.depth) {
			chargeNeeded *= 1.833f - 0.333f * kit.pointsInTalent(Talent.LONGRANGE_WARP);
		}

		if (armor.charge < chargeNeeded) {
			if (ctx.heroFX) {
				GLog.w(Messages.get(ClassArmor.class, "low_charge"));
			}
			return;
		}

		armor.charge -= chargeNeeded;
		armor.updateQuickslot();

		if (tracker.depth == Dungeon.depth && tracker.branch == Dungeon.branch) {
			Char existing = Actor.findChar(tracker.pos);

			if (existing != null && existing != body) {
				if (kit.hasTalent(Talent.TELEFRAG)) {
					int heroHP = body.HP + body.shielding();
					int heroDmg = 5 * kit.pointsInTalent(Talent.TELEFRAG);
					body.damage(Math.min(heroDmg, heroHP - 1), WarpBeacon.this);

					int damage = Hero.heroDamageIntRange(10 * kit.pointsInTalent(Talent.TELEFRAG),
							15 * kit.pointsInTalent(Talent.TELEFRAG));
					if (UseContext.canWorldFx(existing)) {
						existing.sprite.flash();
						existing.sprite.bloodBurstA(existing.sprite.center(), damage);
						Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH);
						Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
					}
					existing.damage(damage, WarpBeacon.this);
				}

				if (existing.isAlive()) {
					Char toPush = Char.hasProp(existing, Char.Property.IMMOVABLE) ? body : existing;

					ArrayList<Integer> candidates = new ArrayList<>();
					for (int n : PathFinder.NEIGHBOURS8) {
						int cell = tracker.pos + n;
						if (!Dungeon.level.solid[cell] && Actor.findChar(cell) == null
								&& (!Char.hasProp(toPush, Char.Property.LARGE)
										|| Dungeon.level.openSpace[cell])) {
							candidates.add(cell);
						}
					}
					Random.shuffle(candidates);

					if (!candidates.isEmpty()) {
						ScrollOfTeleportation.appear(body, tracker.pos);
						if (ctx.heroFX) {
							Actor.add(new Pushing(toPush, toPush.pos, candidates.get(0)));
						}
						toPush.move(candidates.get(0), false);
						if (ctx.heroFX) {
							kit.next();
						}
					} else if (ctx.heroFX) {
						GLog.w(Messages.get(ScrollOfTeleportation.class, "no_tele"));
					}
				} else {
					ScrollOfTeleportation.appear(body, tracker.pos);
				}
			} else {
				ScrollOfTeleportation.appear(body, tracker.pos);
			}

			Invisibility.dispel(body);
			Dungeon.observe();
			if (ctx.heroFX) {
				GameScene.updateFog();
				kit.checkVisibleMobs();
				AttackIndicator.updateState();
			}

		} else {
			// Interfloor teleport is Hero-scene only
			if (!ctx.heroFX) {
				return;
			}
			if (!Dungeon.interfloorTeleportAllowed()) {
				GLog.w(Messages.get(ScrollOfTeleportation.class, "no_tele"));
				return;
			}

			Level.beforeTransition();
			Invisibility.dispel();
			InterlevelScene.mode = InterlevelScene.Mode.RETURN;
			InterlevelScene.returnDepth = tracker.depth;
			InterlevelScene.returnBranch = tracker.branch;
			InterlevelScene.returnPos = tracker.pos;
			Game.switchScene(InterlevelScene.class);
		}
	}

	public static class WarpBeaconTracker extends Buff {

		{
			revivePersists = true;
		}

		int pos;
		int depth;
		int branch;

		Emitter e;

		@Override
		public void fx(boolean on) {
			if (on && depth == Dungeon.depth) {
				e = CellEmitter.center(pos);
				e.pour(MagicMissile.WardParticle.UP, 0.05f);
			} else if (e != null)
				e.on = false;
		}

		public static final String POS = "pos";
		public static final String DEPTH = "depth";
		public static final String BRANCH = "branch";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(POS, pos);
			bundle.put(DEPTH, depth);
			bundle.put(BRANCH, branch);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			pos = bundle.getInt(POS);
			depth = bundle.getInt(DEPTH);
			branch = bundle.getInt(BRANCH);
		}
	}

	@Override
	public int icon() {
		return HeroIcon.WARP_BEACON;
	}

	@Override
	public Talent[] talents() {
		return new Talent[] { Talent.TELEFRAG, Talent.REMOTE_BEACON, Talent.LONGRANGE_WARP, Talent.HEROIC_ENERGY };
	}
}
