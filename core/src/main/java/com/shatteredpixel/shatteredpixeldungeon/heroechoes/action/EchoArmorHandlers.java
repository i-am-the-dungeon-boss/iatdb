package com.shatteredpixel.shatteredpixeldungeon.heroechoes.action;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Electricity;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Freezing;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Adrenaline;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BlobImmunity;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combo;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Doom;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Dread;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hex;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Light;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PrismaticGuard;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Recharging;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ShieldBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Sleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.Ratmogrify;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Challenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Feint;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.NaturesPower;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpectralBlades;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.ElementalBlast;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WarpBeacon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WildMagic;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.DeathMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.ShadowClone;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.SmokeBomb;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Shockwave;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BodyForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Stasis;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SmokeParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.WondrousResin;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorruption;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorrosion;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfDisintegration;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfPrismaticLight;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfTransfusion;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blooming;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Chilling;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Corrupting;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Elastic;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Grim;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Kinetic;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Lucky;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Projecting;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Shocking;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Unstable;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Vampiric;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Annoying;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Dazzling;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Displacing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Explosive;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Friendly;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Polarized;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Sacrificial;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Wayward;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Shuriken;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.Door;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MirrorSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.noosa.tweeners.Delayer;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Per-ability Echo armor effects — fork-owned. No Hero GLog, CellSelector, or
 * UI riders.
 */
public final class EchoArmorHandlers {

	private EchoArmorHandlers() {
	}

	public static boolean activate(EchoActionContext ctx, ClassArmor armor, ArmorAbility ability, Integer target) {
		if (ability instanceof Endure) {
			return endure(ctx, armor, (Endure) ability, target);
		}
		if (ability instanceof NaturesPower) {
			return naturesPower(ctx, armor, (NaturesPower) ability, target);
		}
		if (ability instanceof AscendedForm) {
			return ascendedForm(ctx, armor, (AscendedForm) ability, target);
		}
		if (ability instanceof Feint) {
			return feint(ctx, armor, (Feint) ability, target);
		}
		if (ability instanceof Shockwave) {
			return shockwave(ctx, armor, (Shockwave) ability, target);
		}
		if (ability instanceof DeathMark) {
			return deathMark(ctx, armor, (DeathMark) ability, target);
		}
		if (ability instanceof SpectralBlades) {
			return spectralBlades(ctx, armor, (SpectralBlades) ability, target);
		}
		if (ability instanceof Challenge) {
			return challenge(ctx, armor, (Challenge) ability, target);
		}
		if (ability instanceof ElementalStrike) {
			return elementalStrike(ctx, armor, (ElementalStrike) ability, target);
		}
		if (ability instanceof HeroicLeap) {
			return heroicLeap(ctx, armor, (HeroicLeap) ability, target);
		}
		if (ability instanceof SmokeBomb) {
			return smokeBomb(ctx, armor, (SmokeBomb) ability, target);
		}
		if (ability instanceof WarpBeacon) {
			return warpBeacon(ctx, armor, (WarpBeacon) ability, target);
		}
		if (ability instanceof ShadowClone) {
			return shadowClone(ctx, armor, (ShadowClone) ability, target);
		}
		if (ability instanceof SpiritHawk) {
			return spiritHawk(ctx, armor, (SpiritHawk) ability, target);
		}
		if (ability instanceof PowerOfMany) {
			return powerOfMany(ctx, armor, (PowerOfMany) ability, target);
		}
		if (ability instanceof ElementalBlast) {
			return elementalBlast(ctx, armor, (ElementalBlast) ability, target);
		}
		if (ability instanceof WildMagic) {
			return wildMagic(ctx, armor, (WildMagic) ability, target);
		}
		if (ability instanceof Trinity) {
			return trinity(ctx, armor, (Trinity) ability, target);
		}
		if (ability instanceof Ratmogrify) {
			return ratmogrify(ctx, armor, (Ratmogrify) ability, target);
		}
		return false;
	}

	private static boolean endure(EchoActionContext ctx, ClassArmor armor, Endure ability, Integer target) {
		Char body = ctx.body;

		if (body.buff(Endure.EndureTracker.class) != null) {
			body.buff(Endure.EndureTracker.class).detach();
		}
		Buff.prolong(body, Endure.EndureTracker.class, 12f);

		Combo combo = ctx.stats().buff(Combo.class);
		if (combo != null) {
			combo.addTime(3f);
		}
		if (EchoActionContext.canWorldFx(body)) {
			body.sprite.operate(body.pos);
		}

		armor.charge -= ability.chargeUse(ctx.stats());
		armor.updateQuickslot();
		Invisibility.dispel(body);
		ctx.complete(3f);
		return true;
	}

	private static boolean naturesPower(EchoActionContext ctx, ClassArmor armor, NaturesPower ability, Integer target) {
		Char body = ctx.body;
		Buff.prolong(body, NaturesPower.naturesPowerTracker.class, NaturesPower.naturesPowerTracker.DURATION);
		body.buff(NaturesPower.naturesPowerTracker.class).extensionsLeft = 2;
		if (EchoActionContext.canWorldFx(body)) {
			body.sprite.operate(body.pos);
			Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
			Emitter e = body.sprite.emitter();
			if (e != null) {
				e.burst(LeafParticle.GENERAL, 10);
			}
		}

		armor.charge -= ability.chargeUse(ctx.stats());
		armor.updateQuickslot();
		Invisibility.dispel(body);
		ctx.complete(Actor.TICK);
		return true;
	}

	private static boolean ascendedForm(EchoActionContext ctx, ClassArmor armor, AscendedForm ability, Integer target) {
		Char body = ctx.body;

		Buff.affect(body, AscendedForm.AscendBuff.class).reset();
		if (EchoActionContext.canWorldFx(body)) {
			body.sprite.operate(body.pos);
			Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
			new Flare(6, 48).color(0xFFFF00, true).show(body.sprite, 2f);
		}

		armor.charge -= ability.chargeUse(ctx.stats());
		armor.updateQuickslot();
		Invisibility.dispel(body);
		ctx.complete(Actor.TICK);
		return true;
	}

	private static boolean shockwave(EchoActionContext ctx, ClassArmor armor, Shockwave ability, Integer target) {
		Char body = ctx.body;
		Hero kit = ctx.stats();
		if (target == null) {
			ctx.cancel();
			return false;
		}
		if (target == body.pos) {
			ctx.cancel();
			return false;
		}

		armor.charge -= ability.chargeUse(kit);
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
				ctx.complete(Actor.TICK);

			}
		};

		if (EchoActionContext.canWorldFx(body)) {
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
		return true;
	}

	private static boolean deathMark(EchoActionContext ctx, ClassArmor armor, DeathMark ability, Integer target) {
		Hero kit = ctx.stats();
		if (target == null) {
			ctx.cancel();
			return false;
		}

		Char ch = Actor.findChar(target);

		if (ch == null || !Dungeon.level.heroFOV[target]) {
			ctx.cancel();
			return false;
		} else if (ch.alignment == ctx.body.alignment) {
			ctx.cancel();
			return false;
		}

		Buff.affect(ch, DeathMark.DeathMarkTracker.class, DeathMark.DeathMarkTracker.DURATION).setInitialHP(ch.HP);

		armor.charge -= ability.chargeUse(kit);
		armor.updateQuickslot();
		if (EchoActionContext.canWorldFx(ctx.body)) {
			ctx.body.sprite.zap(target);
		}

		ctx.complete(Actor.TICK);

		if (kit.buff(DeathMark.DoubleMarkTracker.class) != null) {
			kit.buff(DeathMark.DoubleMarkTracker.class).detach();
		} else if (kit.hasTalent(Talent.DOUBLE_MARK)) {
			Buff.affect(kit, DeathMark.DoubleMarkTracker.class, 0.01f);
		}
		return true;
	}

	private static boolean challenge(EchoActionContext ctx, ClassArmor armor, Challenge ability, Integer target) {
		Char body = ctx.body;
		Hero kit = ctx.stats();
		if (target == null) {
			ctx.cancel();
			return false;
		}

		Char targetCh = Actor.findChar(target);
		boolean[] fov = body.fieldOfView != null ? body.fieldOfView : Dungeon.level.heroFOV;
		if (targetCh == null || fov == null || !fov[target]) {
			ctx.cancel();
			return false;
		}

		if (body.buff(Challenge.DuelParticipant.class) != null) {
			ctx.cancel();
			return false;
		}

		if (targetCh.alignment == body.alignment
				&& !(targetCh instanceof Mimic && targetCh.alignment == Char.Alignment.NEUTRAL)) {
			ctx.cancel();
			return false;
		}

		boolean[] passable = BArray.or(Dungeon.level.passable, Dungeon.level.avoid, null);
		for (Char c : Actor.chars()) {
			if (c != body)
				passable[c.pos] = false;
		}
		PathFinder.buildDistanceMap(targetCh.pos, passable);
		int[] reachable = PathFinder.distance.clone();

		int blinkpos = body.pos;
		if (kit.hasTalent(Talent.CLOSE_THE_GAP) && !body.rooted) {

			int blinkrange = 1 + kit.pointsInTalent(Talent.CLOSE_THE_GAP);
			PathFinder.buildDistanceMap(body.pos, BArray.or(Dungeon.level.passable, Dungeon.level.avoid, null),
					blinkrange);

			for (int i = 0; i < PathFinder.distance.length; i++) {
				if (PathFinder.distance[i] == Integer.MAX_VALUE
						|| reachable[i] == Integer.MAX_VALUE
						|| (!Dungeon.level.passable[i] && !(body.flying && Dungeon.level.avoid[i]))
						|| i == targetCh.pos) {
					continue;
				}

				if (Dungeon.level.distance(i, targetCh.pos) < Dungeon.level.distance(blinkpos, targetCh.pos)) {
					blinkpos = i;
				} else if (Dungeon.level.distance(i, targetCh.pos) == Dungeon.level.distance(blinkpos, targetCh.pos)) {
					if (Dungeon.level.trueDistance(i, body.pos) < Dungeon.level.trueDistance(blinkpos, body.pos)) {
						blinkpos = i;
					}
				}
			}
		}

		if (reachable[blinkpos] == Integer.MAX_VALUE) {
			ctx.cancel();
			return false;
		}

		if (Dungeon.level.distance(blinkpos, targetCh.pos) > 5) {
			ctx.cancel();
			return false;
		}

		if (blinkpos != body.pos) {
			body.move(blinkpos, false);
			// prevents the body from being interrupted by seeing new enemies
			Dungeon.observe();
			if (EchoActionContext.canWorldFx(body)) {
				CellEmitter.get(body.pos).burst(Speck.factory(Speck.WOOL), 6);
				Sample.INSTANCE.play(Assets.Sounds.PUFF);
			}
		}

		boolean bossTarget = Char.hasProp(targetCh, Char.Property.BOSS);
		for (Char toFreeze : Actor.chars()) {
			// Exclude both duelists — Hero casters are ALLY so they skipped the
			// alignment filter; EchoBoss is ENEMY and must not freeze itself
			// (Challenge.SpectatorFreeze blocks attaching Challenge.DuelParticipant).
			// (SpectatorFreeze blocks attaching DuelParticipant).
			if (toFreeze != targetCh && toFreeze != body
					&& toFreeze.alignment != Char.Alignment.ALLY && !(toFreeze instanceof NPC)
					&& (!bossTarget || !(Char.hasProp(targetCh, Char.Property.BOSS)
							|| Char.hasProp(targetCh, Char.Property.BOSS_MINION)))) {
				Actor.delayChar(toFreeze, Challenge.DuelParticipant.DURATION);
				Buff.affect(toFreeze, Challenge.SpectatorFreeze.class, Challenge.DuelParticipant.DURATION);
			}
		}

		Buff.affect(targetCh, Challenge.DuelParticipant.class);
		Buff.affect(body, Challenge.DuelParticipant.class);
		if (targetCh instanceof Mob) {
			((Mob) targetCh).aggro(body);
		}

		if (EchoActionContext.canWorldFx(body)) {
			GameScene.flash(0x80FFFFFF);
			Sample.INSTANCE.play(Assets.Sounds.DESCEND);
		}

		armor.charge -= ability.chargeUse(kit);
		armor.updateQuickslot();
		if (EchoActionContext.canWorldFx(body)) {
			body.sprite.zap(target);
		}
		Invisibility.dispel(body);
		ctx.complete(Actor.TICK);

		if (kit.buff(Challenge.EliminationMatchTracker.class) != null) {
			kit.buff(Challenge.EliminationMatchTracker.class).detach();
		}
		return true;
	}

	private static boolean heroicLeap(EchoActionContext ctx, ClassArmor armor, HeroicLeap ability, Integer target) {
		Char body = ctx.body;
		Hero kit = ctx.stats();
		if (target != null) {

			if (body.rooted) {
				ctx.cancel();
				return false;
			}

			Ballistica route = new Ballistica(body.pos, target, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID);
			int cell = route.collisionPos;

			// can't occupy the same cell as another char, so move back one.
			int backTrace = route.dist - 1;
			while (Actor.findChar(cell) != null && cell != body.pos) {
				cell = route.path.get(backTrace);
				backTrace--;
			}

			armor.charge -= ability.chargeUse(kit);
			armor.updateQuickslot();

			final int dest = cell;
			Runnable afterJump = new Runnable() {
				@Override
				public void run() {
					body.move(dest, false);
					Dungeon.observe();
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
								WandOfBlastWave.throwChar(mob, trajectory, strength, true, true, ability);
								if (Random.Int(4) < kit.pointsInTalent(Talent.IMPACT_WAVE)) {
									Buff.prolong(mob, Vulnerable.class, 5f);
								}
							}
						}
					}
					Invisibility.dispel(body);
					ctx.complete(Actor.TICK);

					if (kit.buff(HeroicLeap.DoubleJumpTracker.class) != null) {
						kit.buff(HeroicLeap.DoubleJumpTracker.class).detach();
					} else {
						if (kit.hasTalent(Talent.DOUBLE_JUMP)) {
							Buff.affect(kit, HeroicLeap.DoubleJumpTracker.class, 3);
						}
					}
				}
			};

			if (EchoActionContext.canWorldFx(body)) {
				body.sprite.jump(body.pos, cell, new Callback() {
					@Override
					public void call() {
						afterJump.run();
					}
				});
			} else {
				afterJump.run();
			}
		} else {
			ctx.cancel();
			return false;
		}
		return true;
	}

	private static boolean smokeBomb(EchoActionContext ctx, ClassArmor armor, SmokeBomb ability, Integer target) {
		Char body = ctx.body;
		Hero kit = ctx.stats();
		if (target != null) {

			if (target != body.pos && body.rooted) {
				ctx.cancel();
				return false;
			}

			PathFinder.buildDistanceMap(body.pos, BArray.or(Dungeon.level.passable, Dungeon.level.avoid, null), 6);

			if (PathFinder.distance[target] == Integer.MAX_VALUE ||
					!Dungeon.level.heroFOV[target] ||
					(target != body.pos && Actor.findChar(target) != null)) {
				ctx.cancel();
				return false;
			}

			armor.charge -= ability.chargeUse(kit);
			armor.updateQuickslot();

			boolean shadowStepping = kit.invisible > 0 && kit.hasTalent(Talent.SHADOW_STEP);

			if (!shadowStepping) {
				for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
					if (Dungeon.level.adjacent(mob.pos, body.pos) && mob.alignment != Char.Alignment.ALLY) {
						Buff.prolong(mob, Blindness.class, Blindness.DURATION / 2f);
						if (mob.state == mob.HUNTING)
							mob.state = mob.WANDERING;
						if (EchoActionContext.canWorldFx(mob)) {
							mob.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 4);
						}
					}
				}

				if (kit.hasTalent(Talent.BODY_REPLACEMENT)) {
					for (Char ch : Actor.chars()) {
						if (ch instanceof SmokeBomb.NinjaLog) {
							ch.die(null);
						}
					}

					SmokeBomb.NinjaLog n = new SmokeBomb.NinjaLog();
					n.pos = body.pos;
					GameScene.add(n);
					Dungeon.level.occupyCell(n);
				}

				if (kit.hasTalent(Talent.HASTY_RETREAT)) {
					// effectively 1/2/3/4 turns
					float duration = 0.67f + kit.pointsInTalent(Talent.HASTY_RETREAT);
					Buff.affect(body, Haste.class, duration);
					Buff.affect(body, Invisibility.class, duration);
				}
			}

			if (EchoActionContext.canWorldFx(body)) {
				CellEmitter.get(body.pos).burst(Speck.factory(Speck.WOOL), 10);
				Sample.INSTANCE.play(Assets.Sounds.PUFF);
			}
			ScrollOfTeleportation.appear(body, target);
			Dungeon.level.occupyCell(body);
			Dungeon.observe();
			ctx.complete(Actor.TICK);
		} else {
			ctx.cancel();
			return false;
		}
		return true;
	}

	private static boolean shadowClone(EchoActionContext ctx, ClassArmor armor, ShadowClone ability, Integer target) {
		Char body = ctx.body;
		Hero kit = ctx.stats();
		ShadowClone.ShadowAlly ally = ShadowClone.getShadowAlly();

		if (ally != null) {
			if (target == null) {
				return false;
			}
			ally.directTocell(target);
			return true;
		}

		ArrayList<Integer> spawnPoints = new ArrayList<>();
		for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
			int p = body.pos + PathFinder.NEIGHBOURS8[i];
			if (Actor.findChar(p) == null && Dungeon.level.passable[p]) {
				spawnPoints.add(p);
			}
		}

		if (spawnPoints.isEmpty()) {
			ctx.cancel();
			return false;
		}

		armor.charge -= ability.chargeUse(kit);
		armor.updateQuickslot();

		ally = new ShadowClone.ShadowAlly(kit.lvl);
		ally.pos = Random.element(spawnPoints);
		GameScene.add(ally);
		if (ally.sprite == null) {
			Actor.add(ally);
		}

		ShadowClone.ShadowAlly.appear(ally, ally.pos);

		Invisibility.dispel(body);
		ctx.complete(Actor.TICK);
		return true;
	}

	private static boolean spiritHawk(EchoActionContext ctx, ClassArmor armor, SpiritHawk ability, Integer target) {
		Char body = ctx.body;
		Hero kit = ctx.stats();
		SpiritHawk.HawkAlly ally = SpiritHawk.getHawk();

		if (ally != null) {
			if (target == null) {
				return false;
			}
			ally.directTocell(target);
			return true;
		}

		ArrayList<Integer> spawnPoints = new ArrayList<>();
		for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
			int p = body.pos + PathFinder.NEIGHBOURS8[i];
			if (Actor.findChar(p) == null && (Dungeon.level.passable[p] || Dungeon.level.avoid[p])) {
				spawnPoints.add(p);
			}
		}

		if (spawnPoints.isEmpty()) {
			ctx.cancel();
			return false;
		}

		armor.charge -= ability.chargeUse(kit);
		armor.updateQuickslot();

		ally = new SpiritHawk.HawkAlly();
		ally.pos = Random.element(spawnPoints);
		GameScene.add(ally);
		if (ally.sprite == null) {
			Actor.add(ally);
		}

		ScrollOfTeleportation.appear(ally, ally.pos);
		Dungeon.observe();

		Invisibility.dispel(body);
		ctx.complete(Actor.TICK);
		return true;
	}

	private static boolean powerOfMany(EchoActionContext ctx, ClassArmor armor, PowerOfMany ability, Integer target) {
		Char body = ctx.body;
		Hero kit = ctx.stats();

		Char ally = PowerOfMany.getPoweredAlly();

		boolean allyExists = ally != null;

		if (kit.buff(PrismaticGuard.class) != null
				&& kit.buff(PrismaticGuard.class).isEmpowered()) {
			allyExists = true;
		}

		if (kit.buff(WandOfLivingEarth.RockArmor.class) != null
				&& kit.buff(WandOfLivingEarth.RockArmor.class).isEmpowered()) {
			allyExists = true;
		}

		if (Stasis.getStasisAlly() != null) {
			allyExists = true;
		}

		if (ally instanceof PowerOfMany.LightAlly) {
			if (target == null) {
				return false;
			} else {
				((PowerOfMany.LightAlly) ally).directTocell(target);
			}
		} else if (allyExists) {
			ctx.cancel();
		} else {
			if (target == null) {
				ctx.cancel();
				return false;
			}

			boolean[] fov = body.fieldOfView != null ? body.fieldOfView : Dungeon.level.heroFOV;
			if (fov == null || !fov[target]) {
				ctx.cancel();
				return false;
			}

			// pre-calculate as cost becomes 0 if light ally starts to exist
			float chargeCost = ability.chargeUse(kit);

			Char ch = Actor.findChar(target);
			if (ch != null) {
				// Allies of the caster — not hardcoded ALLY (EchoBoss is ENEMY).
				if (ch.alignment != body.alignment || ch == body) {
					ctx.cancel();
					return false;
				}
			} else {

				if (!Dungeon.level.passable[target] || Dungeon.level.avoid[target]) {
					ctx.cancel();
					return false;
				}

				ch = new PowerOfMany.LightAlly(kit.lvl);
				ch.pos = target;
				GameScene.add((Mob) ch);
				// Headless tests have no GameScene — still register the actor
				if (ch.sprite == null) {
					Actor.add(ch);
				}
				ScrollOfTeleportation.appear(ch, ch.pos);
			}

			Buff.affect(ch, PowerOfMany.PowerBuff.class, 100f);
			Buff.affect(ch, Barrier.class).setShield(25);

			armor.charge -= chargeCost;
			armor.updateQuickslot();

			if (EchoActionContext.canWorldFx(body)) {
				body.sprite.zap(target);
				Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
			}
			Invisibility.dispel(body);
			ctx.complete(Actor.TICK);

		}
		return true;
	}

	private static boolean ratmogrify(EchoActionContext ctx, ClassArmor armor, Ratmogrify ability, Integer target) {
		Char body = ctx.body;
		Hero kit = ctx.stats();

		if (target == null) {
			ctx.cancel();
			return false;
		}

		Char ch = Actor.findChar(target);

		if (ch == null || !Dungeon.level.heroFOV[target]) {
			ctx.cancel();
			return false;
		} else if (ch == body) {
			if (!kit.hasTalent(Talent.RATFORCEMENTS)) {
				ctx.cancel();
				return false;
			} else {
				ArrayList<Integer> spawnPoints = new ArrayList<>();

				for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
					int p = body.pos + PathFinder.NEIGHBOURS8[i];
					if (Actor.findChar(p) == null && Dungeon.level.passable[p]) {
						spawnPoints.add(p);
					}
				}

				int ratsToSpawn = kit.pointsInTalent(Talent.RATFORCEMENTS);

				while (ratsToSpawn > 0 && spawnPoints.size() > 0) {
					int index = Random.index(spawnPoints);

					Rat rat = new Rat();
					rat.alignment = Char.Alignment.ALLY;
					rat.state = rat.HUNTING;
					Buff.affect(rat, AscensionChallenge.AscensionBuffBlocker.class);
					GameScene.add(rat);
					ScrollOfTeleportation.appear(rat, spawnPoints.get(index));

					spawnPoints.remove(index);
					ratsToSpawn--;
				}

			}
		} else if (ch.alignment != Char.Alignment.ENEMY || !(ch instanceof Mob) || ch instanceof Rat) {
			ctx.cancel();
			return false;
		} else if (ch instanceof Ratmogrify.TransmogRat) {
			if (((Ratmogrify.TransmogRat) ch).allied || !kit.hasTalent(Talent.RATLOMACY)) {
				ctx.cancel();
				return false;
			} else {
				((Ratmogrify.TransmogRat) ch).makeAlly();
				if (EchoActionContext.canWorldFx(ch)) {
					ch.sprite.emitter().start(Speck.factory(Speck.HEART), 0.2f, 5);
					Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
				}
				if (kit.pointsInTalent(Talent.RATLOMACY) > 1) {
					Buff.affect(ch, Adrenaline.class, 2 * (kit.pointsInTalent(Talent.RATLOMACY) - 1));
				}
			}
		} else if (Char.hasProp(ch, Char.Property.MINIBOSS) || Char.hasProp(ch, Char.Property.BOSS)) {
			ctx.cancel();
			return false;
		} else {
			Ratmogrify.TransmogRat rat = new Ratmogrify.TransmogRat();
			rat.setup((Mob) ch);
			rat.pos = ch.pos;

			// preserve some buffs
			HashSet<Buff> persistentBuffs = new HashSet<>();
			for (Buff b : ch.buffs()) {
				if (b.revivePersists) {
					persistentBuffs.add(b);
				}
			}

			Actor.remove(ch);
			if (ch.sprite != null) {
				ch.sprite.killAndErase();
			}
			Dungeon.level.mobs.remove(ch);

			for (Buff b : persistentBuffs) {
				ch.add(b);
			}

			GameScene.add(rat);
			// Headless tests have no GameScene — still register the actor
			if (rat.sprite == null) {
				Actor.add(rat);
			}
			if (EchoActionContext.canWorldFx(rat)) {
				CellEmitter.get(rat.pos).burst(Speck.factory(Speck.WOOL), 4);
				Sample.INSTANCE.play(Assets.Sounds.PUFF);
			}

			// for rare cases where a buff was keeping a mob alive (e.g. gnoll brute rage)
			if (!rat.isAlive()) {
				rat.die(ability);
			} else {
				Dungeon.level.occupyCell(rat);
			}
		}

		armor.charge -= ability.chargeUse(kit);
		armor.updateQuickslot();
		Invisibility.dispel(body);
		ctx.complete(Actor.TICK);
		return true;
	}

	/** Same enemy resolution for Hero (TargetHealthIndicator) and Echo (player). */
	private static Char resolveFeintEnemy(EchoActionContext ctx, Char body) {
		Char fromUi = TargetHealthIndicator.instance != null
				? TargetHealthIndicator.instance.target()
				: null;
		if (fromUi != null && fromUi != body
				&& fromUi.alignment != body.alignment) {
			return fromUi;
		}
		if (Dungeon.hero != null && Dungeon.hero.isAlive() && Dungeon.hero != body) {
			return Dungeon.hero;
		}
		return null;
	}

	private static boolean feint(EchoActionContext ctx, ClassArmor armor, Feint ability, Integer target) {
		Char body = ctx.body;
		Hero kit = ctx.stats();
		if (target == null) {
			ctx.cancel();
			return false;
		}

		if (!Dungeon.level.adjacent(body.pos, target)) {
			ctx.cancel();
			return false;
		}

		if (body.rooted) {
			ctx.cancel();
			return false;
		}

		if (Dungeon.level.solid[target] || Actor.findChar(target) != null) {
			ctx.cancel();
			return false;
		}

		if (EchoActionContext.canWorldFx(body)) {
			Sample.INSTANCE.play(Assets.Sounds.MISS);
		}
		int from = body.pos;
		if (Dungeon.level.map[from] == Terrain.OPEN_DOOR) {
			Door.leave(from);
		}

		Feint.AfterImage image = new Feint.AfterImage();
		image.pos = from;
		image.alignment = body.alignment;
		GameScene.add(image);
		// Headless tests have no GameScene — still register the actor for aggro/defense
		if (image.sprite == null) {
			Actor.add(image);
		}
		image.syncOwner(kit, body);

		Invisibility.dispel(body);
		if (EchoActionContext.canWorldFx(body)) {
			body.pos = target;
			Dungeon.level.occupyCell(body);
			body.sprite.jump(from, target, 0, 0.1f, null);
		} else {
			body.move(target, false);
		}
		ctx.complete(1f);

		int imageAttackPos;
		Char enemyTarget = resolveFeintEnemy(ctx, body);
		if (enemyTarget != null) {
			imageAttackPos = enemyTarget.pos;
		} else {
			imageAttackPos = image.pos + (image.pos - target);
		}
		if (EchoActionContext.canWorldFx(body) && image.sprite != null) {
			// do a purely visual attack
			body.sprite.parent.add(new Delayer(0f) {
				@Override
				protected void onComplete() {
					image.sprite.attack(imageAttackPos, new Callback() {
						@Override
						public void call() {
							// do nothing, attack is purely visual
						}
					});
				}
			});
		}

		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])) {
			if ((m.isTargeting(body) && m.state == m.HUNTING) ||
					(m.alignment == Char.Alignment.ENEMY && m.state != m.PASSIVE
							&& Dungeon.level.distance(m.pos, image.pos) <= 2)) {
				m.aggro(image);
			}
		}

		armor.charge -= ability.chargeUse(kit);
		armor.updateQuickslot();
		return true;
	}

	private static boolean spectralBlades(EchoActionContext ctx, ClassArmor armor, SpectralBlades ability,
			Integer target) {
		Char body = ctx.body;
		Hero kit = ctx.stats();
		if (target == null) {
			ctx.cancel();
			return false;
		}
		if (Actor.findChar(target) == body) {
			ctx.cancel();
			return false;
		}
		int savedPos = kit.pos;
		boolean borrow = body != kit;
		final boolean[] restored = { !borrow };
		if (borrow) {
			kit.pos = body.pos;
		}
		boolean keepBorrowed = false;
		try {
			Ballistica b = new Ballistica(body.pos, target, Ballistica.WONT_STOP);
			final HashSet<Char> targets = new HashSet<>();

			Char enemy = findChar(b, body, 2 * kit.pointsInTalent(Talent.PROJECTING_BLADES), targets);

			if (enemy == null || !body.fieldOfView[enemy.pos]) {
				ctx.cancel();
				return false;
			}

			targets.add(enemy);

			if (kit.hasTalent(Talent.FAN_OF_BLADES)) {
				ConeAOE cone = new ConeAOE(b, 30 * kit.pointsInTalent(Talent.FAN_OF_BLADES));
				for (Ballistica ray : cone.rays) {
					Char toAdd = findChar(ray, body, 2 * kit.pointsInTalent(Talent.PROJECTING_BLADES), targets);
					if (toAdd != null && body.fieldOfView[toAdd.pos]) {
						targets.add(toAdd);
					}
				}
				while (targets.size() > 1 + kit.pointsInTalent(Talent.FAN_OF_BLADES)) {
					Char furthest = null;
					for (Char ch : targets) {
						if (furthest == null) {
							furthest = ch;
						} else if (Dungeon.level.trueDistance(enemy.pos, ch.pos) > Dungeon.level.trueDistance(enemy.pos,
								furthest.pos)) {
							furthest = ch;
						}
					}
					targets.remove(furthest);
				}
			}

			armor.charge -= ability.chargeUse(kit);
			armor.updateQuickslot();

			Item proto = new Shuriken();

			final HashSet<Callback> callbacks = new HashSet<>();
			final Char primaryEnemy = enemy;

			Runnable restoreKit = new Runnable() {
				@Override
				public void run() {
					if (restored[0]) {
						return;
					}
					kit.pos = savedPos;
					restored[0] = true;
				}
			};

			Runnable finish = new Runnable() {
				@Override
				public void run() {
					try {
						Invisibility.dispel(body);
						ctx.complete(kit.attackDelay());
					} finally {
						restoreKit.run();
					}
				}
			};

			for (Char ch : targets) {
				Callback callback = new Callback() {
					@Override
					public void call() {
						float dmgMulti = ch == primaryEnemy ? 1f : 0.5f;
						float accmulti = 1f + 0.25f * kit.pointsInTalent(Talent.PROJECTING_BLADES);
						if (kit.hasTalent(Talent.SPIRIT_BLADES)) {
							Buff.affect(kit, Talent.SpiritBladesTracker.class, 0f);
						}
						kit.attack(ch, dmgMulti, 0, accmulti);
						callbacks.remove(this);
						if (callbacks.isEmpty()) {
							finish.run();
						}
					}
				};

				callbacks.add(callback);
				if (body.sprite != null && body.sprite.parent != null) {
					MissileSprite m = ((MissileSprite) body.sprite.parent.recycle(MissileSprite.class));
					m.reset(body.sprite, ch.pos, proto, callback);
					m.hardlight(0.6f, 1f, 1f);
					m.alpha(0.8f);
					keepBorrowed = true;
				} else {
					callback.call();
				}
			}

			if (body.sprite != null) {
				body.sprite.zap(primaryEnemy.pos);
			}
		} finally {
			if (!keepBorrowed) {
				if (!restored[0] && borrow) {
					kit.pos = savedPos;
					restored[0] = true;
				}
			}
		}
		return true;
	}

	private static Char findChar(Ballistica path, Char user, int wallPenetration, HashSet<Char> existingTargets) {
		for (int cell : path.path) {
			Char ch = Actor.findChar(cell);
			if (ch != null) {
				if (ch == user || existingTargets.contains(ch) || ch.alignment == user.alignment) {
					continue;
				} else {
					return ch;
				}
			}
			if (Dungeon.level.solid[cell]) {
				wallPenetration--;
				if (wallPenetration < 0) {
					return null;
				}
			}
		}
		return null;
	}

	private static boolean elementalStrike(EchoActionContext ctx, ClassArmor armor, ElementalStrike ability,
			Integer target) {
		Char body = ctx.body;
		Hero kit = ctx.stats();
		if (target == null) {
			return false;
		}

		armor.charge -= ability.chargeUse(kit);
		armor.updateQuickslot();

		int savedPos = kit.pos;
		boolean borrow = body != kit;
		if (borrow) {
			kit.pos = body.pos;
		}
		try {
			Ballistica aim = new Ballistica(body.pos, target, Ballistica.WONT_STOP);

			int maxDist = 4 + kit.pointsInTalent(Talent.ELEMENTAL_REACH);
			int dist = Math.min(aim.dist, maxDist);

			ConeAOE cone = new ConeAOE(aim,
					dist,
					65 + 10 * kit.pointsInTalent(Talent.ELEMENTAL_REACH),
					Ballistica.STOP_SOLID | Ballistica.STOP_TARGET);

			KindOfWeapon w = kit.belongings.weapon();
			Weapon.Enchantment enchantment = null;
			if (w instanceof MeleeWeapon) {
				enchantment = ((MeleeWeapon) w).enchantment;
			}
			Class<? extends Weapon.Enchantment> enchCls = null;
			if (enchantment != null) {
				enchCls = enchantment.getClass();
			}

			Weapon.Enchantment finalEnchantment = enchantment;
			final int strikeTarget = target;
			Callback applyStrike = new Callback() {
				@Override
				public void call() {

					Char enemy = Actor.findChar(strikeTarget);

					if (enemy != null) {
						if (kit.isCharmedBy(enemy)) {
							enemy = null;
						} else if (enemy.alignment == body.alignment) {
							enemy = null;
						} else if (!kit.canAttack(enemy)) {
							enemy = null;
						}
					}

					ability.preAttackEffect(cone, kit, body, finalEnchantment);

					if (enemy != null) {

						ability.setOldEnemyPos(enemy.pos);
						if (kit.attack(enemy, 1, 0, Char.INFINITE_ACCURACY)) {
							if (EchoActionContext.canWorldFx(body)) {
								Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
							}
						}
					}

					ability.perCellEffect(cone, body, finalEnchantment);

					ability.perCharEffect(cone, kit, body, enemy, finalEnchantment);

					Invisibility.dispel(body);
					ctx.complete(kit.attackDelay());
				}
			};

			if (EchoActionContext.canWorldFx(body)) {
				// cast to cells at the tip, rather than all cells, better performance.
				for (Ballistica ray : cone.outerRays) {
					((MagicMissile) body.sprite.parent.recycle(MagicMissile.class)).reset(
							ElementalStrike.effectTypes.get(enchCls),
							body.sprite,
							ray.path.get(ray.dist),
							null);
				}

				// Visual swing only — applyStrike while kit is still borrowed.
				body.sprite.attack(strikeTarget);
				Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
			}
			applyStrike.call();
		} finally {
			if (borrow) {
				kit.pos = savedPos;
			}
		}
		return true;
	}

	/** Shared Hero-window "teleport" / Echo auto-recall. */
	private static boolean recallToBeacon(EchoActionContext ctx, ClassArmor armor, WarpBeacon ability,
			WarpBeacon.WarpBeaconTracker tracker) {
		Hero kit = ctx.stats();
		Char body = ctx.body;

		// Echo: same-floor only (no interfloor scene switch)
		// Interfloor teleport is Hero-scene only
		// Same as Hero window option "teleport" — no UI for Echo
		if (tracker.depth != Dungeon.depth || tracker.branch != Dungeon.branch) {
			ctx.cancel();
			return false;
		}
		if (!kit.hasTalent(Talent.LONGRANGE_WARP) && tracker.depth != Dungeon.depth) {
			ctx.cancel();
			return false;
		}

		float chargeNeeded = ability.chargeUse(kit);
		if (armor.charge < chargeNeeded) {
			ctx.cancel();
			return false;
		}

		armor.charge -= chargeNeeded;
		armor.updateQuickslot();

		Char existing = Actor.findChar(tracker.pos);
		if (existing != null && existing != body) {
			if (kit.hasTalent(Talent.TELEFRAG)) {
				int heroHP = body.HP + body.shielding();
				int heroDmg = 5 * kit.pointsInTalent(Talent.TELEFRAG);
				body.damage(Math.min(heroDmg, heroHP - 1), ability);

				int damage = Hero.heroDamageIntRange(10 * kit.pointsInTalent(Talent.TELEFRAG),
						15 * kit.pointsInTalent(Talent.TELEFRAG));
				if (EchoActionContext.canWorldFx(existing)) {
					existing.sprite.flash();
					existing.sprite.bloodBurstA(existing.sprite.center(), damage);
					Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH);
					Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
				}
				existing.damage(damage, ability);
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
				if (candidates.isEmpty()) {
					ctx.cancel();
					return false;
				}
				ScrollOfTeleportation.appear(body, tracker.pos);
				toPush.move(candidates.get(0), false);
			} else {
				ScrollOfTeleportation.appear(body, tracker.pos);
			}
		} else {
			ScrollOfTeleportation.appear(body, tracker.pos);
		}

		Invisibility.dispel(body);
		Dungeon.observe();
		ctx.complete(Actor.TICK);
		return true;
	}

	private static boolean warpBeacon(EchoActionContext ctx, ClassArmor armor, WarpBeacon ability, Integer target) {
		Hero kit = ctx.stats();
		Char body = ctx.body;
		if (target == null) {
			ctx.cancel();
			return false;
		}

		if (kit.buff(WarpBeacon.WarpBeaconTracker.class) != null) {
			WarpBeacon.WarpBeaconTracker tracker = kit.buff(WarpBeacon.WarpBeaconTracker.class);
			return recallToBeacon(ctx, armor, ability, tracker);
		}

		if (!Dungeon.level.mapped[target] && !Dungeon.level.visited[target]) {
			ctx.cancel();
			return false;
		}
		if (Dungeon.level.distance(body.pos, target) > 4 * kit.pointsInTalent(Talent.REMOTE_BEACON)) {
			ctx.cancel();
			return false;
		}

		PathFinder.buildDistanceMap(target, BArray.or(Dungeon.level.passable, Dungeon.level.avoid, null));
		if (Dungeon.level.pit[target]
				|| (Dungeon.level.solid[target] && !Dungeon.level.passable[target])
				|| !(Dungeon.level.passable[target] || Dungeon.level.avoid[target])
				|| PathFinder.distance[body.pos] == Integer.MAX_VALUE) {
			ctx.cancel();
			return false;
		}

		WarpBeacon.WarpBeaconTracker tracker = new WarpBeacon.WarpBeaconTracker();
		tracker.pos = target;
		tracker.depth = Dungeon.depth;
		tracker.branch = Dungeon.branch;
		tracker.attachTo(kit);

		if (EchoActionContext.canWorldFx(body)) {
			body.sprite.operate(target);
			Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
		}
		Invisibility.dispel(body);
		ctx.complete(Actor.TICK);
		return true;
	}

	private static void zapWand(WildMagic ability, ArrayList<Wand> wands, EchoActionContext ctx, int cell) {
		Hero kit = ctx.stats();
		Char body = ctx.body;
		Wand cur = wands.remove(0);

		Ballistica aim = new Ballistica(body.pos, cell, cur.collisionProperties(cell));

		// Keep borrow until fx / cursedZap completion (same as EchoWandAdapter).
		final boolean[] zapped = { false };
		Callback afterFx = EchoKitBorrow.defer(ctx, new Callback() {
			@Override
			public void call() {
				if (zapped[0]) {
					cur.onZap(aim);
				}
				afterZap(ability, cur, wands, ctx, cell);
			}
		});

		cur.setCurrent(kit);
		if (ctx.canWorldFx()) {
			ctx.body.sprite.zap(cell);
		}

		if (cur.tryToZap(kit, cell)) {
			if (!cur.cursed) {
				zapped[0] = true;
				if (EchoActionContext.canWorldFx(kit)) {
					cur.fx(aim, afterFx);
				} else {
					afterFx.call();
				}
			} else {
				if (EchoActionContext.canWorldFx(kit)) {
					CursedWand.cursedZap(cur,
							kit,
							new Ballistica(body.pos, cell, Ballistica.MAGIC_BOLT),
							afterFx);
				} else {
					zapped[0] = true;
					afterFx.call();
				}
			}
		} else {
			afterFx.call();
		}
	}

	private static void afterZap(WildMagic ability, Wand cur, ArrayList<Wand> wands, EchoActionContext ctx,
			int target) {
		Hero kit = ctx.stats();
		cur.partialCharge -= 0.5f * (float) Math.pow(0.67f, kit.pointsInTalent(Talent.CONSERVED_MAGIC));
		if (cur.partialCharge < 0) {
			cur.partialCharge++;
			cur.curCharges--;
		}

		Char ch = Actor.findChar(target);
		if (!wands.isEmpty() && kit.isAlive()) {
			// Echo has no Hero ready/next wake — drain the chain synchronously.
			zapWand(ability, wands, ctx, ch == null ? target : ch.pos);
		} else {
			if (kit.buff(WildMagic.WildMagicTracker.class) != null) {
				kit.buff(WildMagic.WildMagicTracker.class).detach();
			}
			Invisibility.dispel(ctx.body);
			ctx.complete(Actor.TICK);
		}
	}

	private static boolean wildMagic(EchoActionContext ctx, ClassArmor armor, WildMagic ability, Integer target) {
		Char body = ctx.body;
		Hero kit = ctx.stats();
		if (target == null) {
			ctx.cancel();
			return false;
		}

		if (target == body.pos) {

			ctx.cancel();
			return false;
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

			ctx.cancel();
			return false;
		}

		Random.shuffle(wands);

		Buff.affect(kit, WildMagic.WildMagicTracker.class, 0f);

		armor.charge -= ability.chargeUse(kit);
		armor.updateQuickslot();

		zapWand(ability, wands, ctx, target);
		return true;
	}

	private static boolean elementalBlast(EchoActionContext ctx, ClassArmor armor, ElementalBlast ability,
			Integer target) {
		Char body = ctx.body;
		Hero kit = ctx.stats();
		Ballistica aim;
		// The direction of the aim only matters if it goes outside the map
		// So we try to aim in the cardinal direction that has the most space
		int x = body.pos % Dungeon.level.width();
		int y = body.pos / Dungeon.level.width();

		if (Math.max(x, Dungeon.level.width() - x) >= Math.max(y, Dungeon.level.height() - y)) {
			if (x > Dungeon.level.width() / 2) {
				aim = new Ballistica(body.pos, body.pos - 1, Ballistica.WONT_STOP);
			} else {
				aim = new Ballistica(body.pos, body.pos + 1, Ballistica.WONT_STOP);
			}
		} else {
			if (y > Dungeon.level.height() / 2) {
				aim = new Ballistica(body.pos, body.pos - Dungeon.level.width(), Ballistica.WONT_STOP);
			} else {
				aim = new Ballistica(body.pos, body.pos + Dungeon.level.width(), Ballistica.WONT_STOP);
			}
		}

		Class<? extends Wand> wandCls = null;
		if (kit.belongings.getItem(MagesStaff.class) != null) {
			wandCls = kit.belongings.getItem(MagesStaff.class).wandClass();
		}

		if (wandCls == null) {
			ctx.cancel();
			return false;
		}

		int aoeSize = 4 + kit.pointsInTalent(Talent.BLAST_RADIUS);

		int projectileProps = Ballistica.STOP_SOLID | Ballistica.STOP_TARGET;

		// ### Special Projectile Properties ###
		// *** Wand of Disintegration ***
		if (wandCls == WandOfDisintegration.class) {
			projectileProps = Ballistica.STOP_TARGET;

			// *** Wand of Fireblast ***
		} else if (wandCls == WandOfFireblast.class) {
			projectileProps = projectileProps | Ballistica.IGNORE_SOFT_SOLID;

			// *** Wand of Warding ***
		} else if (wandCls == WandOfWarding.class) {
			projectileProps = Ballistica.STOP_TARGET;

		}

		ConeAOE aoe = new ConeAOE(aim, aoeSize, 360, projectileProps);

		final float effectMulti = 1f + 0.25f * kit.pointsInTalent(Talent.ELEMENTAL_POWER);

		Class<? extends Wand> finalWandCls = wandCls;
		Callback applyBlast = new Callback() {
			@Override
			public void call() {

				int charsHit = 0;
				Freezing freeze = (Freezing) Dungeon.level.blobs.get(Freezing.class);
				Fire fire = (Fire) Dungeon.level.blobs.get(Fire.class);
				for (int cell : aoe.cells) {

					// ### Cell effects ###
					// *** Wand of Lightning ***
					if (finalWandCls == WandOfLightning.class) {
						if (Dungeon.level.water[cell]) {
							GameScene.add(Blob.seed(cell, 4, Electricity.class));
						}

						// *** Wand of Fireblast ***
					} else if (finalWandCls == WandOfFireblast.class) {
						if (Dungeon.level.map[cell] == Terrain.DOOR) {
							Level.set(cell, Terrain.OPEN_DOOR);
							GameScene.updateMap(cell);
						}
						if (freeze != null) {
							freeze.clear(cell);
						}
						if (Dungeon.level.flamable[cell]) {
							GameScene.add(Blob.seed(cell, 4, Fire.class));
						}

						// *** Wand of Frost ***
					} else if (finalWandCls == WandOfFrost.class) {
						if (fire != null) {
							fire.clear(cell);
						}

						// *** Wand of Prismatic Light ***
					} else if (finalWandCls == WandOfPrismaticLight.class) {
						for (int n : PathFinder.NEIGHBOURS9) {
							int c = cell + n;

							if (Dungeon.level.discoverable[c]) {
								Dungeon.level.mapped[c] = true;
							}

							int terr = Dungeon.level.map[c];
							if ((Terrain.flags[terr] & Terrain.SECRET) != 0) {

								Dungeon.level.discover(c);

								GameScene.discoverTile(c, terr);
								ScrollOfMagicMapping.discover(c);

							}
						}

						// *** Wand of Regrowth ***
					} else if (finalWandCls == WandOfRegrowth.class) {
						// TODO: spend 3 charges worth of regrowth energy from staff?
						int t = Dungeon.level.map[cell];
						if (Random.Float() < 0.33f * effectMulti) {
							if ((t == Terrain.EMPTY || t == Terrain.EMPTY_DECO || t == Terrain.EMBERS
									|| t == Terrain.GRASS || t == Terrain.FURROWED_GRASS)
									&& Dungeon.level.plants.get(cell) == null) {
								Level.set(cell, Terrain.HIGH_GRASS);
								GameScene.updateMap(cell);
							}
						}
					}

					// ### Deal damage ###
					Char mob = Actor.findChar(cell);
					int damage = Math.round(Hero.heroDamageIntRange(15, 25)
							* effectMulti
							* ElementalBlast.damageFactors.get(finalWandCls));

					// Hostility is relative to the casting body (Hero ALLY vs EchoBoss ENEMY)
					if (mob != null && damage > 0 && mob.alignment != body.alignment) {
						mob.damage(damage, Reflection.newInstance(finalWandCls));
						charsHit++;
					}

					// ### Other Char Effects ###
					if (mob != null && mob != body) {
						// *** Wand of Lightning ***
						if (finalWandCls == WandOfLightning.class) {
							if (mob.isAlive() && mob.alignment != body.alignment) {
								Buff.affect(mob, Paralysis.class, effectMulti * Paralysis.DURATION / 2);
							}

							// *** Wand of Fireblast ***
						} else if (finalWandCls == WandOfFireblast.class) {
							if (mob.isAlive() && mob.alignment != body.alignment) {
								Buff.affect(mob, Burning.class).reignite(mob);
							}

							// *** Wand of Corrosion ***
						} else if (finalWandCls == WandOfCorrosion.class) {
							if (mob.isAlive() && mob.alignment != body.alignment) {
								Buff.affect(mob, Corrosion.class).set(4, Math.round(6 * effectMulti));
								charsHit++;
							}

							// *** Wand of Blast Wave ***
						} else if (finalWandCls == WandOfBlastWave.class) {
							if (mob.alignment != body.alignment) {
								Ballistica blastAim = new Ballistica(body.pos, mob.pos, Ballistica.WONT_STOP);
								int knockback = aoeSize + 1 - (int) Dungeon.level.trueDistance(body.pos, mob.pos);
								knockback *= effectMulti;
								WandOfBlastWave.throwChar(mob,
										new Ballistica(mob.pos, blastAim.collisionPos, Ballistica.MAGIC_BOLT),
										knockback,
										true,
										true,
										ability);
							}

							// *** Wand of Frost ***
						} else if (finalWandCls == WandOfFrost.class) {
							if (mob.isAlive() && mob.alignment != body.alignment) {
								Buff.affect(mob, Frost.class, effectMulti * Frost.DURATION);
							}

							// *** Wand of Prismatic Light ***
						} else if (finalWandCls == WandOfPrismaticLight.class) {
							if (mob.isAlive() && mob.alignment != body.alignment) {
								Buff.prolong(mob, Blindness.class, effectMulti * Blindness.DURATION / 2);
								charsHit++;
							}

							// *** Wand of Warding ***
						} else if (finalWandCls == WandOfWarding.class) {
							if (mob instanceof WandOfWarding.Ward) {
								((WandOfWarding.Ward) mob).wandHeal(0, effectMulti);
								charsHit++;
							}

							// *** Wand of Transfusion ***
						} else if (finalWandCls == WandOfTransfusion.class) {
							if (mob.alignment == body.alignment || mob.buff(Charm.class) != null) {
								int healing = Math.round(10 * effectMulti);
								int shielding = (mob.HP + healing) - mob.HT;
								if (shielding > 0) {
									healing -= shielding;
									Buff.affect(mob, Barrier.class).setShield(shielding);
								} else {
									shielding = 0;
								}
								mob.HP += healing;

								if (EchoActionContext.canWorldFx(mob)) {
									mob.sprite.emitter().burst(Speck.factory(Speck.HEALING), 4);

									if (healing > 0) {
										mob.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healing),
												FloatingText.HEALING);
									}
									if (shielding > 0) {
										mob.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shielding),
												FloatingText.SHIELDING);
									}
								}
							} else {
								if (!mob.properties().contains(Char.Property.UNDEAD)) {
									Charm charm = Buff.affect(mob, Charm.class, effectMulti * Charm.DURATION / 2f);
									charm.object = kit.id();
									charm.ignoreHeroAllies = true;
									if (EchoActionContext.canWorldFx(mob)) {
										mob.sprite.centerEmitter().start(Speck.factory(Speck.HEART), 0.2f, 3);
									}
								} else {
									damage = Math.round(Hero.heroDamageIntRange(15, 25) * effectMulti);
									mob.damage(damage, Reflection.newInstance(finalWandCls));
									if (EchoActionContext.canWorldFx(mob)) {
										mob.sprite.emitter().start(ShadowParticle.UP, 0.05f, 10);
									}
								}
							}
							charsHit++;

							// *** Wand of Corruption ***
						} else if (finalWandCls == WandOfCorruption.class) {
							if (mob.isAlive() && mob.alignment != body.alignment) {
								Buff.prolong(mob, Amok.class, effectMulti * 5f);
								charsHit++;
							}

							// *** Wand of Regrowth ***
						} else if (finalWandCls == WandOfRegrowth.class) {
							if (mob.alignment != body.alignment) {
								Buff.prolong(mob, Roots.class, effectMulti * Roots.DURATION);
								charsHit++;
							}
						}
					}

				}

				// ### Self-Effects ###
				// *** Wand of Magic Missile ***
				if (finalWandCls == WandOfMagicMissile.class) {
					Buff.affect(body, Recharging.class, effectMulti * Recharging.DURATION / 2f);
					if (EchoActionContext.canWorldFx(body)) {
						SpellSprite.show(body, SpellSprite.CHARGE);
					}

					// *** Wand of Living Earth ***
				} else if (finalWandCls == WandOfLivingEarth.class && charsHit > 0) {
					for (Mob m : Dungeon.level.mobs) {
						if (m instanceof WandOfLivingEarth.EarthGuardian) {
							((WandOfLivingEarth.EarthGuardian) m).setInfo(kit, 0,
									Math.round(effectMulti * charsHit * 5));
							if (EchoActionContext.canWorldFx(m)) {
								m.sprite.centerEmitter().burst(MagicMissile.EarthParticle.ATTRACT, 8 + charsHit);
							}
							break;
						}
					}

					// *** Wand of Frost ***
				} else if (finalWandCls == WandOfFrost.class) {
					if ((body.buff(Burning.class)) != null) {
						body.buff(Burning.class).detach();
					}

					// *** Wand of Prismatic Light ***
				} else if (finalWandCls == WandOfPrismaticLight.class) {
					if (Dungeon.isChallenged(Challenges.DARKNESS)) {
						Buff.prolong(body, Light.class, effectMulti * 10f);
					} else {
						Buff.prolong(body, Light.class, effectMulti * 50f);
					}

				}

				charsHit = Math.min(4 + kit.pointsInTalent(Talent.REACTIVE_BARRIER), charsHit);
				if (charsHit > 0 && kit.hasTalent(Talent.REACTIVE_BARRIER)) {
					int shielding = Math.round(charsHit * 2.5f * kit.pointsInTalent(Talent.REACTIVE_BARRIER));
					if (EchoActionContext.canWorldFx(body)) {
						body.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shielding),
								FloatingText.SHIELDING);
					}
					Buff.affect(body, Barrier.class).setShield(shielding);
				}

				Invisibility.dispel(body);
				ctx.complete(Actor.TICK);
			}
		};

		if (EchoActionContext.canWorldFx(body)) {
			for (Ballistica ray : aoe.outerRays) {
				((MagicMissile) body.sprite.parent.recycle(MagicMissile.class)).reset(
						ElementalBlast.effectTypes.get(wandCls),
						body.sprite,
						ray.path.get(ray.dist),
						null);
			}

			// cast a ray 2/3 the way, and do effects
			((MagicMissile) body.sprite.parent.recycle(MagicMissile.class)).reset(
					ElementalBlast.effectTypes.get(wandCls),
					body.sprite,
					aim.path.get(Math.min(aoeSize / 2, aim.path.size() - 1)),
					applyBlast);
		} else {
			applyBlast.call();
		}

		if (EchoActionContext.canWorldFx(body)) {
			body.sprite.operate(body.pos);
			Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
		}

		armor.charge -= ability.chargeUse(kit);
		armor.updateQuickslot();
		return true;
	}

	private static boolean trinity(EchoActionContext ctx, ClassArmor armor, Trinity ability, Integer target) {
		Bundlable bodyForm = ability.bodyFormForEcho();
		Bundlable mindForm = ability.mindFormForEcho();
		Bundlable spiritForm = ability.spiritFormForEcho();
		if (bodyForm == null && mindForm == null && spiritForm == null) {
			return false;
		}
		if (bodyForm == null) {
			return false;
		}
		// Do not use trinityChargeUsePerEffect — it reads Dungeon.hero.armorAbility
		float cost = ability.chargeUse(ctx.stats());
		Class<?> cls = bodyForm.getClass();
		if (Weapon.Enchantment.class.isAssignableFrom(cls) || Armor.Glyph.class.isAssignableFrom(cls)) {
			for (Class<?> ench : Weapon.Enchantment.rare) {
				if (ench.equals(cls)) {
					cost *= 2;
					break;
				}
			}
			for (Class<?> glyph : Armor.Glyph.rare) {
				if (glyph.equals(cls)) {
					cost *= 2;
					break;
				}
			}
		}
		if (armor.charge < cost) {
			return false;
		}
		Buff.prolong(ctx.body, BodyForm.BodyFormBuff.class, BodyForm.duration()).setEffect(bodyForm);
		armor.charge -= cost;
		armor.updateQuickslot();
		Invisibility.dispel(ctx.body);
		ctx.complete(Actor.TICK);
		return true;
	}

}
