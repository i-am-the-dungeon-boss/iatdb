package com.shatteredpixel.shatteredpixeldungeon.heroechoes.action;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Daze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.AssassinsBlade;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.BattleAxe;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Crossbow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Cudgel;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dirk;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Flail;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gauntlet;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Glaive;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gloves;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greataxe;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatshield;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.HandAxe;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Katana;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Longsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Mace;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeaponEchoBridge;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Quarterstaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Rapier;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RoundShield;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RunicBlade;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sai;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Scimitar;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sickle;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Spear;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WarHammer;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WarScythe;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Whip;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

/**
 * Echo duelist weapon abilities — fork-owned. Validates kit/charges/target,
 * then runs ability bodies formerly shared with Hero via temporary seams.
 */
public final class EchoDuelistAdapter {

	private EchoDuelistAdapter() {
	}

	public static boolean useAbility(EchoBoss boss, MeleeWeapon weapon, Integer target) {
		if (boss == null || weapon == null) {
			return false;
		}
		EchoActionContext ctx = EchoActionContext.of(boss);
		Hero kit = ctx.stats();
		if (!weapon.isEquipped(kit)) {
			return false;
		}
		if (kit.heroClass != HeroClass.DUELIST) {
			return false;
		}
		if (weapon.STRReq() > kit.STR()) {
			return false;
		}
		MeleeWeapon.Charger charger = Buff.affect(kit, MeleeWeapon.Charger.class);
		if ((charger.charges + charger.partialCharge) < weapon.abilityChargeUse(kit, null)) {
			return false;
		}
		if (weapon.targetingPrompt() != null && target == null) {
			return false;
		}
		// Do not busy before validating — refused targets must leave the boss ready.
		// Successful abilities busy themselves before async VFX / spendAndNext.
		return dispatchAbility(ctx, weapon, target);
	}

	private static boolean dispatchAbility(EchoActionContext ctx, MeleeWeapon weapon, Integer target) {
		if (weapon instanceof Scimitar) {
			return scimitar(ctx, weapon);
		}
		if (weapon instanceof Quarterstaff) {
			return quarterstaff(ctx, weapon);
		}
		if (weapon instanceof RoundShield) {
			return guard(ctx, weapon, 5 + weapon.buffedLvl());
		}
		if (weapon instanceof Greatshield) {
			return guard(ctx, weapon, 3 + weapon.buffedLvl());
		}
		if (weapon instanceof Rapier) {
			int dmgBoost = weapon.augment.damageFactor(5 + Math.round(1.5f * weapon.buffedLvl()));
			return lunge(ctx, target, 1, dmgBoost, weapon);
		}
		if (weapon instanceof Katana) {
			int dmgBoost = weapon.augment.damageFactor(8 + Math.round(2f * weapon.buffedLvl()));
			return lunge(ctx, target, 1, dmgBoost, weapon);
		}
		if (weapon instanceof Spear) {
			int dmgBoost = weapon.augment.damageFactor(9 + Math.round(2f * weapon.buffedLvl()));
			return spike(ctx, target, 1, dmgBoost, weapon);
		}
		if (weapon instanceof Glaive) {
			int dmgBoost = weapon.augment.damageFactor(12 + Math.round(2.5f * weapon.buffedLvl()));
			return spike(ctx, target, 1, dmgBoost, weapon);
		}
		if (weapon instanceof WornShortsword) {
			int dmgBoost = weapon.augment.damageFactor(3 + weapon.buffedLvl());
			return cleave(ctx, target, 1, dmgBoost, weapon);
		}
		if (weapon instanceof Shortsword) {
			int dmgBoost = weapon.augment.damageFactor(4 + weapon.buffedLvl());
			return cleave(ctx, target, 1, dmgBoost, weapon);
		}
		if (weapon instanceof Longsword) {
			int dmgBoost = weapon.augment.damageFactor(6 + weapon.buffedLvl());
			return cleave(ctx, target, 1, dmgBoost, weapon);
		}
		if (weapon instanceof Greatsword) {
			int dmgBoost = weapon.augment.damageFactor(7 + weapon.buffedLvl());
			return cleave(ctx, target, 1, dmgBoost, weapon);
		}
		if (weapon instanceof Sword) {
			int dmgBoost = weapon.augment.damageFactor(5 + weapon.buffedLvl());
			return cleave(ctx, target, 1, dmgBoost, weapon);
		}
		if (weapon instanceof Sai) {
			int dmgBoost = weapon.augment.damageFactor(4 + weapon.buffedLvl());
			return comboStrike(ctx, target, 0, dmgBoost, weapon);
		}
		if (weapon instanceof Gloves) {
			int dmgBoost = weapon.augment.damageFactor(3 + weapon.buffedLvl());
			return comboStrike(ctx, target, 0, dmgBoost, weapon);
		}
		if (weapon instanceof Gauntlet) {
			int dmgBoost = weapon.augment.damageFactor(5 + weapon.buffedLvl());
			return comboStrike(ctx, target, 0, dmgBoost, weapon);
		}
		if (weapon instanceof Sickle) {
			int bleedAmt = weapon.augment.damageFactor(Math.round(15f + 2.5f * weapon.buffedLvl()));
			return harvest(ctx, target, 0f, bleedAmt, weapon);
		}
		if (weapon instanceof WarScythe) {
			int bleedAmt = weapon.augment.damageFactor(Math.round(30f + 4.5f * weapon.buffedLvl()));
			return harvest(ctx, target, 0f, bleedAmt, weapon);
		}
		if (weapon instanceof Mace) {
			int dmgBoost = weapon.augment.damageFactor(5 + Math.round(1.5f * weapon.buffedLvl()));
			return heavyBlow(ctx, target, 1, dmgBoost, weapon);
		}
		if (weapon instanceof HandAxe) {
			int dmgBoost = weapon.augment.damageFactor(4 + Math.round(1.5f * weapon.buffedLvl()));
			return heavyBlow(ctx, target, 1, dmgBoost, weapon);
		}
		if (weapon instanceof BattleAxe) {
			int dmgBoost = weapon.augment.damageFactor(5 + Math.round(1.5f * weapon.buffedLvl()));
			return heavyBlow(ctx, target, 1, dmgBoost, weapon);
		}
		if (weapon instanceof Cudgel) {
			int dmgBoost = weapon.augment.damageFactor(3 + Math.round(1.5f * weapon.buffedLvl()));
			return heavyBlow(ctx, target, 1, dmgBoost, weapon);
		}
		if (weapon instanceof WarHammer) {
			int dmgBoost = weapon.augment.damageFactor(6 + Math.round(1.5f * weapon.buffedLvl()));
			return heavyBlow(ctx, target, 1, dmgBoost, weapon);
		}
		if (weapon instanceof Dagger) {
			return sneak(ctx, target, 5, 2 + weapon.buffedLvl(), weapon);
		}
		if (weapon instanceof Dirk) {
			return sneak(ctx, target, 4, 2 + weapon.buffedLvl(), weapon);
		}
		if (weapon instanceof AssassinsBlade) {
			return sneak(ctx, target, 3, 2 + weapon.buffedLvl(), weapon);
		}
		if (weapon instanceof Whip) {
			return whip(ctx, (Whip) weapon);
		}
		if (weapon instanceof RunicBlade) {
			return runicSlash(ctx, (RunicBlade) weapon, target);
		}
		if (weapon instanceof Flail) {
			return spin(ctx, (Flail) weapon);
		}
		if (weapon instanceof Crossbow) {
			return chargedShot(ctx, (Crossbow) weapon);
		}
		if (weapon instanceof Greataxe) {
			return greataxe(ctx, (Greataxe) weapon, target);
		}
		return false;
	}

	private static boolean inFov(EchoActionContext ctx, int cell) {
		if (ctx.body.fieldOfView != null && cell < ctx.body.fieldOfView.length) {
			return ctx.body.fieldOfView[cell];
		}
		return Dungeon.level.heroFOV[cell];
	}

	private static boolean scimitar(EchoActionContext ctx, MeleeWeapon wep) {
		MeleeWeaponEchoBridge.beforeAbilityUsed(wep, ctx, null);
		// 1 turn less as using the ability is instant
		Buff.prolong(ctx.body, Scimitar.SwordDance.class, 3 + wep.buffedLvl());
		if (ctx.canWorldFx()) {
			ctx.body.sprite.operate(ctx.body.pos);
		}
		MeleeWeaponEchoBridge.afterAbilityUsed(wep, ctx);
		return true;
	}

	private static boolean quarterstaff(EchoActionContext ctx, MeleeWeapon wep) {
		MeleeWeaponEchoBridge.beforeAbilityUsed(wep, ctx, null);
		// 1 turn less as using the ability is instant
		Buff.prolong(ctx.body, Quarterstaff.DefensiveStance.class, 3 + wep.buffedLvl());
		if (ctx.canWorldFx()) {
			ctx.body.sprite.operate(ctx.body.pos);
		}
		MeleeWeaponEchoBridge.afterAbilityUsed(wep, ctx);
		return true;
	}

	private static boolean guard(EchoActionContext ctx, MeleeWeapon wep, int duration) {
		MeleeWeaponEchoBridge.beforeAbilityUsed(wep, ctx, null);
		Buff.prolong(ctx.body, RoundShield.GuardTracker.class, duration).hasBlocked = false;
		if (ctx.canWorldFx()) {
			ctx.body.sprite.operate(ctx.body.pos);
		}
		MeleeWeaponEchoBridge.afterAbilityUsed(wep, ctx);
		return true;
	}

	private static boolean chargedShot(EchoActionContext ctx, Crossbow wep) {
		Hero kit = ctx.stats();
		if (kit.buff(Crossbow.ChargedShot.class) != null) {
			return false;
		}
		MeleeWeaponEchoBridge.beforeAbilityUsed(wep, ctx, null);
		Buff.affect(kit, Crossbow.ChargedShot.class);
		if (ctx.canWorldFx()) {
			ctx.body.sprite.operate(ctx.body.pos);
		}
		MeleeWeaponEchoBridge.afterAbilityUsed(wep, ctx);
		return true;
	}

	private static boolean spin(EchoActionContext ctx, Flail wep) {
		Hero kit = ctx.stats();
		Flail.SpinAbilityTracker spin = kit.buff(Flail.SpinAbilityTracker.class);
		if (spin != null && spin.spins >= 3) {
			return false;
		}
		MeleeWeaponEchoBridge.beforeAbilityUsed(wep, ctx, null);
		if (spin == null) {
			spin = Buff.affect(kit, Flail.SpinAbilityTracker.class, 3f);
		}
		spin.spins++;
		Buff.prolong(kit, Flail.SpinAbilityTracker.class, 3f);
		if (ctx.canWorldFx()) {
			Sample.INSTANCE.play(Assets.Sounds.CHAINS, 1, 1, 0.9f + 0.1f * spin.spins);
			ctx.body.sprite.operate(ctx.body.pos);
		}
		MeleeWeaponEchoBridge.afterAbilityUsed(wep, ctx);
		return true;
	}

	private static boolean lunge(EchoActionContext ctx, Integer target, float dmgMulti, int dmgBoost, MeleeWeapon wep) {
		if (target == null) {
			return false;
		}
		Char body = ctx.body;
		Hero kit = ctx.stats();
		Char enemy = Actor.findChar(target);
		// duelist can lunge out of FOV, but this wastes the ability instead of
		// cancelling if there is no target
		if (inFov(ctx, target)) {
			if (enemy == null || enemy == body || kit.isCharmedBy(enemy)) {
				return false;
			}
		}
		if (body.rooted || Dungeon.level.distance(body.pos, target) < 2
				|| Dungeon.level.distance(body.pos, target) - 1 > wep.reachFactor(kit)) {
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
			return false;
		}
		final int dest = lungeCell;
		final Char foe = enemy;
		Callback finishLunge = new Callback() {
			@Override
			public void call() {
				body.move(dest, false);
				EchoKitBorrow.run(ctx, new Runnable() {
					@Override
					public void run() {
						kit.belongings.abilityWeapon = wep;
						if (foe != null && kit.canAttack(foe)) {
							Callback doHit = new Callback() {
								@Override
								public void call() {
									MeleeWeaponEchoBridge.beforeAbilityUsed(wep, ctx, foe);
									if (kit.attack(foe, dmgMulti, dmgBoost, Char.INFINITE_ACCURACY)) {
										if (ctx.canWorldFx()) {
											Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
										}
										if (!foe.isAlive()) {
											MeleeWeapon.onAbilityKill(kit, foe);
										}
									}
									Invisibility.dispel(body);
									MeleeWeaponEchoBridge.afterAbilityUsed(wep, ctx);
								}
							};
							echoAttackFxThenHit(ctx, foe.pos, doHit);
						} else {
							MeleeWeapon.Charger charger = Buff.affect(kit, MeleeWeapon.Charger.class);
							charger.partialCharge -= 1;
							while (charger.partialCharge < 0 && charger.charges > 0) {
								charger.charges--;
								charger.partialCharge++;
							}
						}
					}
				});
			}
		};
		if (ctx.canWorldFx()) {
			Sample.INSTANCE.play(Assets.Sounds.MISS);
			body.sprite.jump(body.pos, dest, 0, 0.1f, finishLunge);
		} else {
			finishLunge.call();
		}
		return true;
	}

	/**
	 * Shared cleave for Echo. Kit is borrowed onto body pos/sprite for
	 * canAttack / attack (same pattern as SpectralBlades).
	 */
	private static boolean cleave(EchoActionContext ctx, Integer target, float dmgMulti, int dmgBoost,
			MeleeWeapon wep) {
		if (target == null) {
			return false;
		}
		Char body = ctx.body;
		Hero kit = ctx.stats();
		Char enemy = Actor.findChar(target);
		if (enemy == null || enemy == body || kit.isCharmedBy(enemy) || !inFov(ctx, target)) {
			return false;
		}
		return runBorrowed(ctx, new BorrowedAction() {
			@Override
			public void run(Hero kit, Char body) {
				kit.belongings.abilityWeapon = wep;
				if (!kit.canAttack(enemy)) {
					kit.belongings.abilityWeapon = null;
					throw new AbilityRefused();
				}
				kit.belongings.abilityWeapon = null;
				Callback doHit = new Callback() {
					@Override
					public void call() {
						MeleeWeaponEchoBridge.beforeAbilityUsed(wep, ctx, enemy);
						if (kit.attack(enemy, dmgMulti, dmgBoost, Char.INFINITE_ACCURACY)) {
							if (ctx.canWorldFx()) {
								Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
							}
						}
						Invisibility.dispel(body);
						if (!enemy.isAlive()) {
							MeleeWeapon.onAbilityKill(kit, enemy);
							if (kit.buff(Sword.CleaveTracker.class) != null) {
								kit.buff(Sword.CleaveTracker.class).detach();
							} else {
								Buff.prolong(kit, Sword.CleaveTracker.class, 4f); // 1 less as attack was instant
							}
						} else if (kit.buff(Sword.CleaveTracker.class) != null) {
							kit.buff(Sword.CleaveTracker.class).detach();
						}
						MeleeWeaponEchoBridge.afterAbilityUsed(wep, ctx);
					}
				};
				echoAttackFxThenHit(ctx, enemy.pos, doHit);
			}
		});
	}

	private static boolean spike(EchoActionContext ctx, Integer target, float dmgMulti, int dmgBoost, MeleeWeapon wep) {
		if (target == null) {
			return false;
		}
		Char body = ctx.body;
		Hero kit = ctx.stats();
		Char enemy = Actor.findChar(target);
		if (enemy == null || enemy == body || kit.isCharmedBy(enemy) || !inFov(ctx, target)) {
			return false;
		}
		return runBorrowed(ctx, new BorrowedAction() {
			@Override
			public void run(Hero kit, Char body) {
				kit.belongings.abilityWeapon = wep;
				if (Dungeon.level.adjacent(body.pos, enemy.pos) || !wep.canReach(kit, enemy.pos)) {
					kit.belongings.abilityWeapon = null;
					throw new AbilityRefused();
				}
				kit.belongings.abilityWeapon = null;
				Callback doHit = new Callback() {
					@Override
					public void call() {
						MeleeWeaponEchoBridge.beforeAbilityUsed(wep, ctx, enemy);
						int oldPos = enemy.pos;
						if (kit.attack(enemy, dmgMulti, dmgBoost, Char.INFINITE_ACCURACY)) {
							if (enemy.isAlive() && enemy.pos == oldPos && !Pushing.pushingExistsForChar(enemy)) {
								// Hero-only knockback VFX; skip for Echo headless paths.
							} else if (!enemy.isAlive()) {
								MeleeWeapon.onAbilityKill(kit, enemy);
							}
							if (ctx.canWorldFx()) {
								Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
							}
						}
						Invisibility.dispel(body);
						MeleeWeaponEchoBridge.afterAbilityUsed(wep, ctx);
					}
				};
				echoAttackFxThenHit(ctx, enemy.pos, doHit);
			}
		});
	}

	private static boolean heavyBlow(EchoActionContext ctx, Integer target, float dmgMulti, int dmgBoost,
			MeleeWeapon wep) {
		if (target == null) {
			return false;
		}
		Char body = ctx.body;
		Hero kit = ctx.stats();
		Char enemy = Actor.findChar(target);
		if (enemy == null || enemy == body || kit.isCharmedBy(enemy) || !inFov(ctx, target)) {
			return false;
		}
		return runBorrowed(ctx, new BorrowedAction() {
			@Override
			public void run(Hero kit, Char body) {
				kit.belongings.abilityWeapon = wep;
				if (!kit.canAttack(enemy)) {
					kit.belongings.abilityWeapon = null;
					throw new AbilityRefused();
				}
				kit.belongings.abilityWeapon = null;
				float finalDmgMulti = dmgMulti;
				int finalDmgBoost = dmgBoost;
				if (enemy instanceof Mob && !((Mob) enemy).surprisedBy(kit)) {
					finalDmgMulti = Math.min(1, finalDmgMulti);
					finalDmgBoost = 0;
				}
				final float hitMulti = finalDmgMulti;
				final int hitBoost = finalDmgBoost;
				Callback doHit = new Callback() {
					@Override
					public void call() {
						MeleeWeaponEchoBridge.beforeAbilityUsed(wep, ctx, enemy);
						if (kit.attack(enemy, hitMulti, hitBoost, Char.INFINITE_ACCURACY)) {
							if (ctx.canWorldFx()) {
								Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
							}
							if (enemy.isAlive()) {
								Buff.affect(enemy, Daze.class, Daze.DURATION);
							} else {
								MeleeWeapon.onAbilityKill(kit, enemy);
							}
						}
						Invisibility.dispel(body);
						MeleeWeaponEchoBridge.afterAbilityUsed(wep, ctx);
					}
				};
				echoAttackFxThenHit(ctx, enemy.pos, doHit);
			}
		});
	}

	private static boolean comboStrike(EchoActionContext ctx, Integer target, float multiPerHit, int boostPerHit,
			MeleeWeapon wep) {
		if (target == null) {
			return false;
		}
		Char body = ctx.body;
		Hero kit = ctx.stats();
		Char enemy = Actor.findChar(target);
		if (enemy == null || enemy == body || kit.isCharmedBy(enemy) || !inFov(ctx, target)) {
			return false;
		}
		return runBorrowed(ctx, new BorrowedAction() {
			@Override
			public void run(Hero kit, Char body) {
				kit.belongings.abilityWeapon = wep;
				if (!kit.canAttack(enemy)) {
					kit.belongings.abilityWeapon = null;
					throw new AbilityRefused();
				}
				kit.belongings.abilityWeapon = null;
				Callback doHit = new Callback() {
					@Override
					public void call() {
						MeleeWeaponEchoBridge.beforeAbilityUsed(wep, ctx, enemy);
						int recentHits = 0;
						Sai.ComboStrikeTracker buff = kit.buff(Sai.ComboStrikeTracker.class);
						if (buff != null) {
							recentHits = buff.hits;
							buff.detach();
						}
						boolean hit = kit.attack(enemy, 1f + multiPerHit * recentHits, boostPerHit * recentHits,
								Char.INFINITE_ACCURACY);
						if (hit && !enemy.isAlive()) {
							MeleeWeapon.onAbilityKill(kit, enemy);
						}
						Invisibility.dispel(body);
						if (recentHits >= 2 && hit && ctx.canWorldFx()) {
							Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
						}
						MeleeWeaponEchoBridge.afterAbilityUsed(wep, ctx);
					}
				};
				echoAttackFxThenHit(ctx, enemy.pos, doHit);
			}
		});
	}

	private static boolean harvest(EchoActionContext ctx, Integer target, float bleedMulti, int bleedBoost,
			MeleeWeapon wep) {
		if (target == null) {
			return false;
		}
		Char body = ctx.body;
		Hero kit = ctx.stats();
		Char enemy = Actor.findChar(target);
		if (enemy == null || enemy == body || kit.isCharmedBy(enemy) || !inFov(ctx, target)) {
			return false;
		}
		return runBorrowed(ctx, new BorrowedAction() {
			@Override
			public void run(Hero kit, Char body) {
				kit.belongings.abilityWeapon = wep;
				if (!kit.canAttack(enemy)) {
					kit.belongings.abilityWeapon = null;
					throw new AbilityRefused();
				}
				kit.belongings.abilityWeapon = null;
				Callback doHit = new Callback() {
					@Override
					public void call() {
						MeleeWeaponEchoBridge.beforeAbilityUsed(wep, ctx, enemy);
						Buff.affect(enemy, Sickle.HarvestBleedTracker.class, 0);
						if (kit.attack(enemy, bleedMulti, bleedBoost, Char.INFINITE_ACCURACY)) {
							if (ctx.canWorldFx()) {
								Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
							}
						}
						Invisibility.dispel(body);
						if (!enemy.isAlive()) {
							MeleeWeapon.onAbilityKill(kit, enemy);
						}
						MeleeWeaponEchoBridge.afterAbilityUsed(wep, ctx);
					}
				};
				echoAttackFxThenHit(ctx, enemy.pos, doHit);
			}
		});
	}

	private static boolean sneak(EchoActionContext ctx, Integer target, int maxDist, int invisTurns, MeleeWeapon wep) {
		if (target == null) {
			return false;
		}
		Char body = ctx.body;
		PathFinder.buildDistanceMap(body.pos, BArray.or(Dungeon.level.passable, Dungeon.level.avoid, null), maxDist);
		if (PathFinder.distance[target] == Integer.MAX_VALUE || !inFov(ctx, target) || body.rooted) {
			return false;
		}
		if (Actor.findChar(target) != null) {
			return false;
		}
		MeleeWeaponEchoBridge.beforeAbilityUsed(wep, ctx, null);
		Buff.prolong(body, Invisibility.class, invisTurns - 1);
		if (body.sprite != null) {
			body.sprite.turnTo(body.pos, target);
		}
		body.move(target, false);
		if (ctx.canWorldFx()) {
			Sample.INSTANCE.play(Assets.Sounds.PUFF);
			if (Game.instance != null) {
				CellEmitter.get(body.pos).burst(Speck.factory(Speck.WOOL), 6);
			}
		}
		MeleeWeaponEchoBridge.afterAbilityUsed(wep, ctx);
		return true;
	}

	private static boolean whip(EchoActionContext ctx, Whip wep) {
		return runBorrowed(ctx, new BorrowedAction() {
			@Override
			public void run(Hero kit, Char body) {
				ArrayList<Char> targets = new ArrayList<>();
				Char closest = null;
				kit.belongings.abilityWeapon = wep;
				for (Char ch : Actor.chars()) {
					if (ch != body
							&& ch.alignment != body.alignment
							&& !kit.isCharmedBy(ch)
							&& inFov(ctx, ch.pos)
							&& kit.canAttack(ch)) {
						targets.add(ch);
						if (closest == null || Dungeon.level.trueDistance(body.pos, closest.pos) > Dungeon.level
								.trueDistance(body.pos, ch.pos)) {
							closest = ch;
						}
					}
				}
				kit.belongings.abilityWeapon = null;
				if (targets.isEmpty()) {
					throw new AbilityRefused();
				}
				Char finalClosest = closest;
				Callback doHit = new Callback() {
					@Override
					public void call() {
						MeleeWeaponEchoBridge.beforeAbilityUsed(wep, ctx, finalClosest);
						for (Char ch : targets) {
							kit.attack(ch, 1, 0, Char.INFINITE_ACCURACY);
							if (!ch.isAlive()) {
								MeleeWeapon.onAbilityKill(kit, ch);
							}
						}
						Invisibility.dispel(body);
						MeleeWeaponEchoBridge.afterAbilityUsed(wep, ctx);
					}
				};
				echoAttackFxThenHit(ctx, body.pos, doHit);
			}
		});
	}

	private static boolean runicSlash(EchoActionContext ctx, RunicBlade wep, Integer target) {
		if (target == null) {
			return false;
		}
		Char body = ctx.body;
		Hero kit = ctx.stats();
		Char enemy = Actor.findChar(target);
		if (enemy == null || enemy == body || kit.isCharmedBy(enemy) || !inFov(ctx, target)) {
			return false;
		}
		RunicBlade.RunicSlashTracker tracker = Buff.affect(kit, RunicBlade.RunicSlashTracker.class);
		tracker.boost = 3f + 0.50f * wep.buffedLvl();
		return runBorrowed(ctx, new BorrowedAction() {
			@Override
			public void run(Hero kit, Char body) {
				kit.belongings.abilityWeapon = wep;
				if (!kit.canAttack(enemy)) {
					tracker.detach();
					kit.belongings.abilityWeapon = null;
					throw new AbilityRefused();
				}
				kit.belongings.abilityWeapon = null;
				Callback doHit = new Callback() {
					@Override
					public void call() {
						MeleeWeaponEchoBridge.beforeAbilityUsed(wep, ctx, enemy);
						if (kit.attack(enemy, 1f, 0, Char.INFINITE_ACCURACY)) {
							if (ctx.canWorldFx()) {
								Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
							}
							if (!enemy.isAlive()) {
								MeleeWeapon.onAbilityKill(kit, enemy);
							}
						}
						tracker.detach();
						Invisibility.dispel(body);
						MeleeWeaponEchoBridge.afterAbilityUsed(wep, ctx);
					}
				};
				echoAttackFxThenHit(ctx, enemy.pos, doHit);
			}
		});
	}

	private static boolean greataxe(EchoActionContext ctx, Greataxe wep, Integer target) {
		Char body = ctx.body;
		if (body.HP / (float) body.HT >= 0.5f) {
			return false;
		}
		if (target == null) {
			return false;
		}
		Hero kit = ctx.stats();
		Char enemy = Actor.findChar(target);
		if (enemy == null || enemy == body || kit.isCharmedBy(enemy) || !inFov(ctx, target)) {
			return false;
		}
		int dmgBoost = wep.augment.damageFactor(5 + Math.round(1.5f * wep.buffedLvl()));
		return runBorrowed(ctx, new BorrowedAction() {
			@Override
			public void run(Hero kit, Char body) {
				kit.belongings.abilityWeapon = wep;
				if (!kit.canAttack(enemy)) {
					kit.belongings.abilityWeapon = null;
					throw new AbilityRefused();
				}
				kit.belongings.abilityWeapon = null;
				Callback doHit = new Callback() {
					@Override
					public void call() {
						MeleeWeaponEchoBridge.beforeAbilityUsed(wep, ctx, enemy);
						if (kit.attack(enemy, 1, dmgBoost, Char.INFINITE_ACCURACY)) {
							if (ctx.canWorldFx()) {
								Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
							}
							if (!enemy.isAlive()) {
								MeleeWeapon.onAbilityKill(kit, enemy);
							}
						}
						Invisibility.dispel(body);
						MeleeWeaponEchoBridge.afterAbilityUsed(wep, ctx);
					}
				};
				echoAttackFxThenHit(ctx, enemy.pos, doHit);
			}
		});
	}

	private static boolean runBorrowed(EchoActionContext ctx, BorrowedAction action) {
		try {
			EchoKitBorrow.run(ctx, new Runnable() {
				@Override
				public void run() {
					action.run(ctx.stats(), ctx.body);
				}
			});
			return true;
		} catch (AbilityRefused refused) {
			return false;
		}
	}

	/**
	 * Echo world FX: play the swing without waiting, then hit while kit is still
	 * borrowed. Waiting on {@code attack(cell, callback)} restores the headless
	 * kit before the hit and NPEs in {@link Char#attack}.
	 */
	private static void echoAttackFxThenHit(EchoActionContext ctx, int cell, Callback doHit) {
		if (ctx.canWorldFx()) {
			ctx.body.sprite.attack(cell);
		}
		doHit.call();
	}

	private interface BorrowedAction {
		void run(Hero kit, Char body);
	}

	/** Internal signal for borrow helpers — not thrown outside the adapter. */
	private static final class AbilityRefused extends RuntimeException {
	}

}
