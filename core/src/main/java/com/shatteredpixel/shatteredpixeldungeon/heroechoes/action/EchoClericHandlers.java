package com.shatteredpixel.shatteredpixeldungeon.heroechoes.action;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AdrenalineSurge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArcaneArmor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtifactRecharge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barkskin;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bless;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Dread;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FireImbue;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GreaterHaste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LifeLink;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Light;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LostInventory;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ShieldBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ToxicImbue;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WellFed;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.AuraOfProtection;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BeamingRay;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BlessSpell;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BodyForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Cleanse;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpellEchoBridge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.DivineSense;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Flash;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HallowedGround;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyIntuition;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyLance;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Judgement;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.LayOnHands;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.LifeLinkSpell;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.MnemonicPrayer;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Radiance;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ShieldOfLight;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Smite;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.SpiritForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Sunray;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.effects.Enchanting;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Kinetic;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundlable;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Per-spell Echo cleric effects — fork-owned. No Hero GLog, QuickSlot, or
 * talent UI riders.
 */
public final class EchoClericHandlers {

	private EchoClericHandlers() {
	}

	public static boolean cast(EchoActionContext ctx, HolyTome tome, ClericSpell spell, Integer target) {
		if (spell instanceof GuidingLight) {
			return guidingLight(ctx, tome, (GuidingLight) spell, target);
		}
		if (spell instanceof Sunray) {
			return sunray(ctx, tome, (Sunray) spell, target);
		}
		if (spell instanceof Smite) {
			return smite(ctx, tome, (Smite) spell, target);
		}
		if (spell instanceof HolyLance) {
			return holyLance(ctx, tome, (HolyLance) spell, target);
		}
		if (spell instanceof HallowedGround) {
			return hallowedGround(ctx, tome, (HallowedGround) spell, target);
		}
		if (spell instanceof Flash) {
			return flash(ctx, tome, (Flash) spell, target);
		}
		if (spell instanceof LayOnHands) {
			return layOnHands(ctx, tome, (LayOnHands) spell, target);
		}
		if (spell instanceof BlessSpell) {
			return bless(ctx, tome, (BlessSpell) spell, target);
		}
		if (spell instanceof ShieldOfLight) {
			return shieldOfLight(ctx, tome, (ShieldOfLight) spell, target);
		}
		if (spell instanceof MnemonicPrayer) {
			return mnemonicPrayer(ctx, tome, (MnemonicPrayer) spell, target);
		}
		if (spell instanceof HolyWeapon) {
			return holyWeapon(ctx, tome, (HolyWeapon) spell);
		}
		if (spell instanceof HolyWard) {
			return holyWard(ctx, tome, (HolyWard) spell);
		}
		if (spell instanceof Cleanse) {
			return cleanse(ctx, tome, (Cleanse) spell);
		}
		if (spell instanceof Radiance) {
			return radiance(ctx, tome, (Radiance) spell);
		}
		if (spell instanceof DivineSense) {
			return divineSense(ctx, tome, (DivineSense) spell);
		}
		if (spell instanceof AuraOfProtection) {
			return auraOfProtection(ctx, tome, (AuraOfProtection) spell);
		}
		if (spell instanceof Judgement) {
			return judgement(ctx, tome, (Judgement) spell);
		}
		if (spell instanceof HolyIntuition) {
			return holyIntuition(ctx, tome, (HolyIntuition) spell);
		}
		if (spell instanceof BodyForm) {
			return bodyForm(ctx, tome, (BodyForm) spell);
		}
		return false;
	}

	/**
	 * Shared targeted cast for Echo and headless callers. Fires MagicMissile when
	 * the body sprite has a scene parent; otherwise applies the hit immediately.
	 */
	private static boolean guidingLight(EchoActionContext ctx, HolyTome tome, GuidingLight spell, Integer target) {
		if (!EchoActionSupport.hasTarget(ctx, target) || Dungeon.level == null) {
			return false;
		}
		Ballistica aim = new Ballistica(ctx.body.pos, target, spell.targetingFlags());
		if (Actor.findChar(aim.collisionPos) == ctx.body) {
			return false;
		}
		Runnable applyHit = () -> {
			Char ch = Actor.findChar(aim.collisionPos);
			if (ch != null) {
				ch.damage(Hero.heroDamageIntRange(2, 8), spell);
				if (ctx.canWorldFx() && ch.sprite != null) {
					Sample.INSTANCE.play(Assets.Sounds.HIT_MAGIC, 1, Random.Float(0.87f, 1.15f));
					ch.sprite.burst(0xFFFFFF44, 3);
				}
				if (ch.isAlive()) {
					Buff.affect(ch, GuidingLight.Illuminated.class);
					Buff.affect(ch, GuidingLight.WasIlluminatedTracker.class);
				}
			} else {
				Dungeon.level.pressCell(aim.collisionPos);
			}
			ClericSpellEchoBridge.onSpellCast(spell, ctx, tome);
			if (ctx.kit.subClass == HeroSubClass.PRIEST
					&& ctx.kit.buff(GuidingLight.GuidingLightPriestCooldown.class) == null) {
				Buff.prolong(ctx.kit, GuidingLight.GuidingLightPriestCooldown.class, 50f);
			}
		};
		if (ctx.canWorldFx()) {
			Sample.INSTANCE.play(Assets.Sounds.ZAP);
			ctx.body.sprite.zap(target);
			MagicMissile.boltFromChar(ctx.body.sprite.parent, MagicMissile.LIGHT_MISSILE, ctx.body.sprite,
					aim.collisionPos, applyHit::run);
		} else {
			applyHit.run();
		}
		return true;
	}

	private static boolean sunray(EchoActionContext ctx, HolyTome tome, Sunray spell, Integer target) {
		if (!EchoActionSupport.hasTarget(ctx, target) || Dungeon.level == null) {
			return false;
		}
		Hero kit = ctx.kit;
		Ballistica aim = new Ballistica(ctx.body.pos, target, spell.targetingFlags());
		if (Actor.findChar(aim.collisionPos) == ctx.body) {
			return false;
		}
		if (ctx.canWorldFx()) {
			Sample.INSTANCE.play(Assets.Sounds.RAY);
			ctx.body.sprite.zap(target);
			ctx.body.sprite.parent.add(
					new Beam.SunRay(ctx.body.sprite.center(),
							DungeonTilemap.raisedTileCenterToWorld(aim.collisionPos)));
		}
		Char ch = Actor.findChar(aim.collisionPos);
		if (ch != null) {
			if (ctx.canWorldFx() && ch.sprite != null) {
				ch.sprite.burst(0xFFFFFF44, 5);
			}
			applySunrayHit(spell, kit, ch);
		}
		ClericSpellEchoBridge.onSpellCast(spell, ctx, tome);
		return true;
	}

	private static void applySunrayHit(Sunray spell, Hero kit, Char ch) {
		if (Char.hasProp(ch, Char.Property.UNDEAD) || Char.hasProp(ch, Char.Property.DEMONIC)) {
			if (kit.pointsInTalent(Talent.SUNRAY) == 2) {
				ch.damage(12, spell);
			} else {
				ch.damage(8, spell);
			}
		} else {
			if (kit.pointsInTalent(Talent.SUNRAY) == 2) {
				ch.damage(Hero.heroDamageIntRange(6, 12), spell);
			} else {
				ch.damage(Hero.heroDamageIntRange(4, 8), spell);
			}
		}
		if (ch.isAlive()) {
			if (ch.buff(Blindness.class) != null && ch.buff(Sunray.SunRayRecentlyBlindedTracker.class) != null) {
				Buff.prolong(ch, Paralysis.class, 2f + 2f * kit.pointsInTalent(Talent.SUNRAY));
				ch.buff(Sunray.SunRayRecentlyBlindedTracker.class).detach();
			} else if (ch.buff(Sunray.SunRayUsedTracker.class) == null) {
				Buff.prolong(ch, Blindness.class, 2f + 2f * kit.pointsInTalent(Talent.SUNRAY));
				Buff.prolong(ch, Sunray.SunRayRecentlyBlindedTracker.class,
						2f + 2f * kit.pointsInTalent(Talent.SUNRAY));
				Buff.affect(ch, Sunray.SunRayUsedTracker.class);
			}
			if (kit.subClass == HeroSubClass.PRIEST) {
				Buff.affect(ch, GuidingLight.Illuminated.class);
			}
		}
	}

	private static boolean smite(EchoActionContext ctx, HolyTome tome, Smite spell, Integer target) {
		if (!EchoActionSupport.hasTarget(ctx, target) || Dungeon.level == null) {
			return false;
		}
		Char enemy = Actor.findChar(target);
		if (enemy == null || enemy == ctx.body) {
			return false;
		}
		Smite.SmiteTracker tracker = Buff.affect(ctx.kit, Smite.SmiteTracker.class);
		final boolean[] ok = { false };
		EchoKitBorrow.run(ctx, () -> {
			boolean inFov = ctx.body.fieldOfView != null && target < ctx.body.fieldOfView.length
					? ctx.body.fieldOfView[target]
					: Dungeon.level.heroFOV[target];
			if (ctx.kit.isCharmedBy(enemy) || !inFov || !ctx.kit.canAttack(enemy)) {
				tracker.detach();
				return;
			}
			ok[0] = true;
			float accMult = 1;
			if (!(ctx.kit.belongings.attackingWeapon() instanceof Weapon)
					|| ((Weapon) ctx.kit.belongings.attackingWeapon()).STRReq() <= ctx.kit.STR()) {
				accMult = Char.INFINITE_ACCURACY;
			}
			final float accuracy = accMult;
			// Visual swing only — hit while still borrowed (do not wait on attack
			// callback).
			if (ctx.canWorldFx()) {
				ctx.kit.sprite.attack(enemy.pos);
			}
			if (ctx.kit.attack(enemy, 1, 0, accuracy)) {
				if (ctx.canWorldFx()) {
					Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
				}
				if (enemy.sprite != null && enemy.sprite.parent != null) {
					enemy.sprite.burst(0xFFFFFFFF, 10);
				}
			}
			tracker.detach();
			Invisibility.dispel(ctx.body);
			ClericSpellEchoBridge.onSpellCast(spell, ctx, tome);
		});
		return ok[0];
	}

	private static boolean holyLance(EchoActionContext ctx, HolyTome tome, HolyLance spell, Integer target) {
		if (!EchoActionSupport.hasTarget(ctx, target) || Dungeon.level == null) {
			return false;
		}
		Ballistica aim = new Ballistica(ctx.body.pos, target, spell.targetingFlags());
		if (Actor.findChar(aim.collisionPos) == ctx.body) {
			return false;
		}
		Runnable applyHit = () -> {
			Char enemy = Actor.findChar(aim.collisionPos);
			if (enemy != null) {
				int min = 15 + 15 * ctx.kit.pointsInTalent(Talent.HOLY_LANCE);
				int max = Math.round(27.5f + 27.5f * ctx.kit.pointsInTalent(Talent.HOLY_LANCE));
				if (Char.hasProp(enemy, Char.Property.UNDEAD) || Char.hasProp(enemy, Char.Property.DEMONIC)) {
					min = max;
				}
				enemy.damage(Hero.heroDamageIntRange(min, max), spell);
				if (enemy.sprite != null && enemy.sprite.parent != null) {
					Sample.INSTANCE.play(Assets.Sounds.HIT_MAGIC, 1, Random.Float(0.8f, 1f));
					Sample.INSTANCE.play(Assets.Sounds.HIT_STAB, 1, Random.Float(0.8f, 1f));
					enemy.sprite.burst(0xFFFFFFFF, 10);
				}
				if (enemy.isActive()) {
					Buff.affect(enemy, GuidingLight.Illuminated.class);
				}
			} else {
				Dungeon.level.pressCell(aim.collisionPos);
			}
			FlavourBuff.affect(ctx.kit, HolyLance.LanceCooldown.class, 30f);
			ClericSpellEchoBridge.onSpellCast(spell, ctx, tome);
		};
		if (ctx.canWorldFx()) {
			Sample.INSTANCE.play(Assets.Sounds.ZAP);
			ctx.body.sprite.zap(target);
			Char enemy = Actor.findChar(aim.collisionPos);
			if (enemy != null && enemy.sprite != null) {
				((MissileSprite) ctx.body.sprite.parent.recycle(MissileSprite.class)).reset(
						ctx.body.sprite, enemy.sprite, new HolyLance.HolyLanceVFX(), applyHit::run);
			} else {
				((MissileSprite) ctx.body.sprite.parent.recycle(MissileSprite.class)).reset(
						ctx.body.sprite, target, new HolyLance.HolyLanceVFX(), applyHit::run);
			}
		} else {
			applyHit.run();
		}
		return true;
	}

	private static boolean hallowedGround(EchoActionContext ctx, HolyTome tome, HallowedGround spell, Integer target) {
		if (!EchoActionSupport.hasTarget(ctx, target) || Dungeon.level == null) {
			return false;
		}
		boolean inFov = ctx.body.fieldOfView != null && target < ctx.body.fieldOfView.length
				? ctx.body.fieldOfView[target]
				: Dungeon.level.heroFOV[target];
		if (Dungeon.level.solid[target] || !inFov) {
			return false;
		}
		ArrayList<Char> affected = new ArrayList<>();
		PathFinder.buildDistanceMap(target, BArray.not(Dungeon.level.solid, null),
				ctx.kit.pointsInTalent(Talent.HALLOWED_GROUND));
		for (int i = 0; i < Dungeon.level.length(); i++) {
			if (PathFinder.distance[i] != Integer.MAX_VALUE) {
				int c = Dungeon.level.map[i];
				if (c == Terrain.EMPTY || c == Terrain.EMBERS || c == Terrain.EMPTY_DECO) {
					Level.set(i, Terrain.GRASS);
					GameScene.updateMap(i);
				}
				GameScene.add(Blob.seed(i, 20, HallowedGround.HallowedTerrain.class));
				Char ch = Actor.findChar(i);
				if (ch != null) {
					affected.add(ch);
				}
			}
		}
		Char ally = PowerOfMany.getPoweredAlly();
		if (ally != null && ally.buff(LifeLinkSpell.LifeLinkSpellBuff.class) != null) {
			if (affected.contains(ctx.body) && !affected.contains(ally)) {
				affected.add(ally);
			} else if (!affected.contains(ctx.body) && affected.contains(ally)) {
				affected.add(ctx.body);
			}
		}
		for (Char ch : affected) {
			hallowedGroundAffect(ch, ctx.body);
		}
		ClericSpellEchoBridge.onSpellCast(spell, ctx, tome);
		return true;
	}

	private static void hallowedGroundAffect(Char ch, Char self) {
		if (ch.alignment == Char.Alignment.ALLY) {
			if (ch == self || ch.HP == ch.HT) {
				int barrierToGive = Math.min(15, 30 - ch.shielding());
				Buff.affect(ch, Barrier.class).incShield(barrierToGive);
				if (ch.sprite != null) {
					ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(barrierToGive),
							FloatingText.SHIELDING);
				}
			} else {
				int barrier = 15 - (ch.HT - ch.HP);
				barrier = Math.max(barrier, 0);
				ch.HP += 15 - barrier;
				if (ch.sprite != null) {
					ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(15 - barrier),
							FloatingText.HEALING);
				}
				if (barrier > 0) {
					Buff.affect(ch, Barrier.class).incShield(barrier);
					if (ch.sprite != null) {
						ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(barrier),
								FloatingText.SHIELDING);
					}
				}
			}
		} else if (!ch.flying) {
			Buff.affect(ch, GuidingLight.Illuminated.class);
			Buff.affect(ch, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots.class, 2f);
		}
	}

	private static boolean flash(EchoActionContext ctx, HolyTome tome, Flash spell, Integer target) {
		if (!EchoActionSupport.hasTarget(ctx, target) || Dungeon.level == null) {
			return false;
		}
		Hero kit = ctx.kit;
		if (Dungeon.level.solid[target] || (!Dungeon.level.mapped[target] && !Dungeon.level.visited[target])
				|| Dungeon.level.distance(ctx.body.pos, target) > 2 + kit.pointsInTalent(Talent.FLASH)) {
			return false;
		}
		if (!ScrollOfTeleportation.teleportToLocation(ctx.body, target)) {
			return false;
		}
		AscendedForm.AscendBuff ascend = kit.buff(AscendedForm.AscendBuff.class);
		if (ascend != null) {
			ascend.flashCasts++;
		}
		ClericSpellEchoBridge.onSpellCast(spell, ctx, tome);
		return true;
	}

	private static boolean layOnHands(EchoActionContext ctx, HolyTome tome, LayOnHands spell, Integer target) {
		if (!EchoActionSupport.hasTarget(ctx, target) || Dungeon.level == null) {
			return false;
		}
		if (Dungeon.level.distance(ctx.body.pos, target) > 1) {
			return false;
		}
		Char ch = Actor.findChar(target);
		if (ch == null) {
			return false;
		}
		layOnHandsAffect(ctx.kit, ch, ctx.body);
		Char ally = PowerOfMany.getPoweredAlly();
		if (ally != null && ally.buff(LifeLinkSpell.LifeLinkSpellBuff.class) != null) {
			if (ch == ctx.body) {
				layOnHandsAffect(ctx.kit, ally, ctx.body);
			} else if (ally == ch) {
				layOnHandsAffect(ctx.kit, ctx.body, ctx.body);
			}
		}
		ClericSpellEchoBridge.onSpellCast(spell, ctx, tome);
		return true;
	}

	private static void layOnHandsAffect(Hero kit, Char ch, Char self) {
		int totalHeal = 10 + 5 * kit.pointsInTalent(Talent.LAY_ON_HANDS);
		int totalBarrier = 0;
		if (ch == self) {
			Barrier barrier = Buff.affect(ch, Barrier.class);
			totalBarrier = totalHeal;
			totalBarrier = Math.min(3 * totalHeal - barrier.shielding(), totalBarrier);
			totalBarrier = Math.max(0, totalBarrier);
			Buff.affect(ch, Barrier.class).incShield(totalBarrier);
			if (ch.sprite != null) {
				ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(totalBarrier),
						FloatingText.SHIELDING);
			}
		} else {
			if (ch.HT - ch.HP < totalHeal) {
				totalBarrier = totalHeal - (ch.HT - ch.HP);
				if (ch.HP != ch.HT) {
					ch.HP = ch.HT;
					if (ch.sprite != null) {
						ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(totalHeal - totalBarrier),
								FloatingText.HEALING);
					}
				}
				if (totalBarrier > 0) {
					Barrier barrier = Buff.affect(ch, Barrier.class);
					totalBarrier = Math.min(3 * totalHeal - barrier.shielding(), totalBarrier);
					totalBarrier = Math.max(0, totalBarrier);
					if (totalBarrier > 0) {
						barrier.incShield(totalBarrier);
						if (ch.sprite != null) {
							ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(totalBarrier),
									FloatingText.SHIELDING);
						}
					}
				}
			} else {
				ch.HP = ch.HP + totalHeal;
				if (ch.sprite != null) {
					ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(totalHeal),
							FloatingText.HEALING);
				}
			}
		}
	}

	private static boolean bless(EchoActionContext ctx, HolyTome tome, BlessSpell spell, Integer target) {
		if (!EchoActionSupport.hasTarget(ctx, target) || Dungeon.level == null) {
			return false;
		}
		Char ch = Actor.findChar(target);
		boolean inFov = ctx.body.fieldOfView != null && target < ctx.body.fieldOfView.length
				? ctx.body.fieldOfView[target]
				: Dungeon.level.heroFOV[target];
		if (ch == null || !inFov) {
			return false;
		}
		blessAffect(spell, ctx.kit, ch, ctx.body);
		Char ally = PowerOfMany.getPoweredAlly();
		if (ally != null && ally.buff(LifeLinkSpell.LifeLinkSpellBuff.class) != null) {
			if (ch == ctx.body) {
				blessAffect(spell, ctx.kit, ally, ctx.body);
			} else if (ally == ch) {
				blessAffect(spell, ctx.kit, ctx.body, ctx.body);
			}
		}
		ClericSpellEchoBridge.onSpellCast(spell, ctx, tome);
		return true;
	}

	private static void blessAffect(BlessSpell spell, Hero kit, Char ch, Char self) {
		if (ch.sprite != null && ch.sprite.parent != null) {
			new Flare(6, 32).color(0xFFFF00, true).show(ch.sprite, 2f);
		}
		if (ch == self) {
			Buff.prolong(ch, Bless.class, 2f + 4 * kit.pointsInTalent(Talent.BLESS));
			Buff.affect(ch, Barrier.class).setShield(5 + 5 * kit.pointsInTalent(Talent.BLESS));
			if (ch.sprite != null) {
				ch.sprite.showStatusWithIcon(CharSprite.POSITIVE,
						Integer.toString(5 + 5 * kit.pointsInTalent(Talent.BLESS)), FloatingText.SHIELDING);
			}
		} else {
			Buff.prolong(ch, Bless.class, 5f + 5 * kit.pointsInTalent(Talent.BLESS));
			int totalHeal = 5 + 5 * kit.pointsInTalent(Talent.BLESS);
			if (ch.HT - ch.HP < totalHeal) {
				int barrier = totalHeal - (ch.HT - ch.HP);
				barrier = Math.max(barrier, 0);
				if (ch.HP != ch.HT) {
					ch.HP = ch.HT;
					if (ch.sprite != null) {
						ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(totalHeal - barrier),
								FloatingText.HEALING);
					}
				}
				if (barrier > 0) {
					Buff.affect(ch, Barrier.class).setShield(barrier);
					if (ch.sprite != null) {
						ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(barrier),
								FloatingText.SHIELDING);
					}
				}
			} else {
				ch.HP = ch.HP + totalHeal;
				if (ch.sprite != null) {
					ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(totalHeal),
							FloatingText.HEALING);
				}
			}
		}
		if (ch.alignment != Char.Alignment.ALLY && kit.subClass == HeroSubClass.PRIEST) {
			Buff.affect(ch, GuidingLight.Illuminated.class);
		}
	}

	private static boolean shieldOfLight(EchoActionContext ctx, HolyTome tome, ShieldOfLight spell, Integer target) {
		if (!EchoActionSupport.hasTarget(ctx, target) || Dungeon.level == null) {
			return false;
		}
		Char ch = Actor.findChar(target);
		boolean inFov = ctx.body.fieldOfView != null && target < ctx.body.fieldOfView.length
				? ctx.body.fieldOfView[target]
				: Dungeon.level.heroFOV[target];
		if (ch == null || ch.alignment == ctx.body.alignment || !inFov) {
			return false;
		}
		Buff.prolong(ctx.body, ShieldOfLight.ShieldOfLightTracker.class, 4f).object = ch.id();
		if (ctx.kit.subClass == HeroSubClass.PRIEST) {
			Buff.affect(ch, GuidingLight.Illuminated.class);
		}
		Char ally = PowerOfMany.getPoweredAlly();
		if (ally != null && ally.buff(LifeLinkSpell.LifeLinkSpellBuff.class) != null) {
			Buff.prolong(ally, ShieldOfLight.ShieldOfLightTracker.class, 3f).object = ch.id();
		}
		ClericSpellEchoBridge.onSpellCast(spell, ctx, tome);
		return true;
	}

	@SuppressWarnings("unchecked")
	private static boolean mnemonicPrayer(EchoActionContext ctx, HolyTome tome, MnemonicPrayer spell, Integer target) {
		if (!EchoActionSupport.hasTarget(ctx, target) || Dungeon.level == null) {
			return false;
		}
		Char ch = Actor.findChar(target);
		boolean inFov = ctx.body.fieldOfView != null && target < ctx.body.fieldOfView.length
				? ctx.body.fieldOfView[target]
				: Dungeon.level.heroFOV[target];
		if (ch == null || !inFov) {
			return false;
		}
		float extension = 2 + ctx.kit.pointsInTalent(Talent.MNEMONIC_PRAYER);
		mnemonicAffect(ch, extension, ch.alignment == ctx.body.alignment);
		Char ally = PowerOfMany.getPoweredAlly();
		if (ally != null && ally.buff(LifeLinkSpell.LifeLinkSpellBuff.class) != null) {
			if (ch == ctx.body) {
				mnemonicAffect(ally, extension, ally.alignment == ctx.body.alignment);
			} else if (ch == ally) {
				mnemonicAffect(ctx.body, extension, true);
			}
		}
		ClericSpellEchoBridge.onSpellCast(spell, ctx, tome);
		return true;
	}

	@SuppressWarnings("unchecked")
	private static void mnemonicAffect(Char ch, float extension, boolean allyOfCaster) {
		if (allyOfCaster) {
			Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
			if (ch.sprite != null) {
				Emitter e = ch.sprite.emitter();
				if (e != null) {
					e.start(Speck.factory(Speck.UP), 0.15f, 4);
				}
			}
			for (Buff b : ch.buffs()) {
				if (b.type != Buff.buffType.POSITIVE || b.mnemonicExtended
						|| b.icon() == com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.NONE) {
					continue;
				}
				if (b instanceof AscendedForm.AscendBuff
						|| b instanceof BodyForm.BodyFormBuff || b instanceof SpiritForm.SpiritFormBuff
						|| b instanceof PowerOfMany.PowerBuff || b instanceof BeamingRay.BeamingRayBoost
						|| b instanceof LifeLink || b instanceof LifeLinkSpell.LifeLinkSpellBuff) {
					continue;
				}
				if (b instanceof FlavourBuff) {
					Buff.affect(ch, (Class<? extends FlavourBuff>) b.getClass(), extension);
				} else if (b instanceof AdrenalineSurge) {
					((AdrenalineSurge) b).delay(extension);
				} else if (b instanceof ArcaneArmor) {
					((ArcaneArmor) b).delay(extension);
				} else if (b instanceof ArtifactRecharge) {
					((ArtifactRecharge) b).extend(extension);
				} else if (b instanceof Barkskin) {
					((Barkskin) b).delay(extension);
				} else if (b instanceof FireImbue) {
					((FireImbue) b).extend(extension);
				} else if (b instanceof GreaterHaste) {
					((GreaterHaste) b).extend(extension);
				} else if (b instanceof Healing) {
					((Healing) b).increaseHeal((int) extension);
				} else if (b instanceof ToxicImbue) {
					((ToxicImbue) b).extend(extension);
				} else if (b instanceof WellFed) {
					((WellFed) b).extend(extension);
				} else if (b instanceof ElixirOfAquaticRejuvenation.AquaHealing) {
					((ElixirOfAquaticRejuvenation.AquaHealing) b).extend(extension);
				} else if (b instanceof ScrollOfChallenge.ChallengeArena) {
					((ScrollOfChallenge.ChallengeArena) b).extend(extension);
				} else if (b instanceof ShieldBuff) {
					((ShieldBuff) b).delay(extension);
				} else if (b instanceof Kinetic.ConservedDamage) {
					((Kinetic.ConservedDamage) b).delay(extension);
				} else if (b instanceof Sungrass.Health) {
					((Sungrass.Health) b).boost((int) extension);
				}
				b.mnemonicExtended = true;
			}
		} else {
			Sample.INSTANCE.play(Assets.Sounds.DEBUFF);
			if (ch.sprite != null) {
				Emitter e = ch.sprite.emitter();
				if (e != null) {
					e.start(Speck.factory(Speck.DOWN), 0.15f, 4);
				}
			}
			Buff.affect(ch, GuidingLight.Illuminated.class);
			for (Buff b : ch.buffs()) {
				if (b.type != Buff.buffType.NEGATIVE || b.mnemonicExtended) {
					continue;
				}
				if (b instanceof FlavourBuff) {
					Buff.affect(ch, (Class<? extends FlavourBuff>) b.getClass(), extension);
				} else if (b instanceof Bleeding) {
					((Bleeding) b).extend(extension);
				} else if (b instanceof Burning) {
					((Burning) b).extend(extension);
				} else if (b instanceof Corrosion) {
					((Corrosion) b).extend(extension);
				} else if (b instanceof Dread) {
					((Dread) b).extend(extension);
				} else if (b instanceof Ooze) {
					((Ooze) b).extend(extension);
				} else if (b instanceof Poison) {
					((Poison) b).extend(extension);
				} else if (b instanceof Viscosity.DeferedDamage) {
					((Viscosity.DeferedDamage) b).extend(extension);
				}
				b.mnemonicExtended = true;
			}
		}
	}

	private static boolean holyWeapon(EchoActionContext ctx, HolyTome tome, HolyWeapon spell) {
		Buff.affect(ctx.body, HolyWeapon.HolyWepBuff.class, 50f);
		if (ctx.canWorldFx()) {
			Sample.INSTANCE.play(Assets.Sounds.READ);
			ctx.body.sprite.operate(ctx.body.pos);
			if (ctx.kit.belongings.weapon() != null) {
				Enchanting.show(ctx.kit, ctx.kit.belongings.weapon());
			}
		}
		ClericSpellEchoBridge.onSpellCast(spell, ctx, tome);
		return true;
	}

	private static boolean holyWard(EchoActionContext ctx, HolyTome tome, HolyWard spell) {
		Buff.affect(ctx.body, HolyWard.HolyArmBuff.class, 50f);
		if (ctx.canWorldFx()) {
			Sample.INSTANCE.play(Assets.Sounds.READ);
			ctx.body.sprite.operate(ctx.body.pos);
			if (ctx.kit.belongings.armor() != null) {
				Enchanting.show(ctx.kit, ctx.kit.belongings.armor());
			}
		}
		ClericSpellEchoBridge.onSpellCast(spell, ctx, tome);
		return true;
	}

	private static boolean cleanse(EchoActionContext ctx, HolyTome tome, Cleanse spell) {
		Hero kit = ctx.kit;
		ArrayList<Char> affected = new ArrayList<>();
		affected.add(ctx.body);
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (Dungeon.level.heroFOV[mob.pos] && mob.alignment == Char.Alignment.ALLY) {
				affected.add(mob);
			}
		}
		Char ally = PowerOfMany.getPoweredAlly();
		if (ally != null && ally.buff(LifeLinkSpell.LifeLinkSpellBuff.class) != null
				&& !affected.contains(ally)) {
			affected.add(ally);
		}
		for (Char ch : affected) {
			for (Buff b : ch.buffs()) {
				if (b.type == Buff.buffType.NEGATIVE
						&& !(b instanceof AllyBuff)
						&& !(b instanceof LostInventory)) {
					b.detach();
				}
			}
			if (kit.pointsInTalent(Talent.CLEANSE) > 1) {
				Buff.prolong(ch, PotionOfCleansing.Cleanse.class, 2 * (kit.pointsInTalent(Talent.CLEANSE) - 1));
			}
			Buff.affect(ch, Barrier.class).setShield(10 * kit.pointsInTalent(Talent.CLEANSE));
			if (ch.sprite != null && ch.sprite.parent != null) {
				new Flare(6, 32).color(0xFF4CD2, true).show(ch.sprite, 2f);
			}
		}
		if (ctx.canWorldFx()) {
			ctx.body.sprite.operate(ctx.body.pos);
			Sample.INSTANCE.play(Assets.Sounds.READ);
		}
		ClericSpellEchoBridge.onSpellCast(spell, ctx, tome);
		return true;
	}

	private static boolean radiance(EchoActionContext ctx, HolyTome tome, Radiance spell) {
		if (ctx.canWorldFx()) {
			GameScene.flash(0x80FFFFFF);
			Sample.INSTANCE.play(Assets.Sounds.BLAST);
			ctx.body.sprite.operate(ctx.body.pos);
		}
		if (Dungeon.level.viewDistance < 6) {
			Buff.prolong(ctx.body, Light.class, Dungeon.isChallenged(Challenges.DARKNESS) ? 20 : 100);
		}
		ctx.forEachVisibleHostile(ch -> {
			if (ch.buff(GuidingLight.Illuminated.class) != null) {
				ch.damage(ctx.kit.lvl + 5, GuidingLight.class);
			} else {
				Buff.affect(ch, GuidingLight.Illuminated.class);
				Buff.affect(ch, GuidingLight.WasIlluminatedTracker.class);
			}
			if (ch.isActive()) {
				Buff.affect(ch, Paralysis.class, 3f);
			}
		});
		ClericSpellEchoBridge.onSpellCast(spell, ctx, tome);
		return true;
	}

	private static boolean divineSense(EchoActionContext ctx, HolyTome tome, DivineSense spell) {
		Buff.prolong(ctx.body, DivineSense.DivineSenseTracker.class, DivineSense.DivineSenseTracker.DURATION);
		Dungeon.observe();
		if (ctx.canWorldFx()) {
			Sample.INSTANCE.play(Assets.Sounds.READ);
			SpellSprite.show(ctx.body, SpellSprite.VISION);
			ctx.body.sprite.operate(ctx.body.pos);
		}
		Char ally = PowerOfMany.getPoweredAlly();
		if (ally != null && ally.buff(LifeLinkSpell.LifeLinkSpellBuff.class) != null) {
			Buff.prolong(ally, DivineSense.DivineSenseTracker.class, DivineSense.DivineSenseTracker.DURATION);
			if (ally.sprite != null && ally.sprite.parent != null) {
				SpellSprite.show(ally, SpellSprite.VISION);
			}
		}
		ClericSpellEchoBridge.onSpellCast(spell, ctx, tome);
		return true;
	}

	private static boolean auraOfProtection(EchoActionContext ctx, HolyTome tome, AuraOfProtection spell) {
		Buff.affect(ctx.body, AuraOfProtection.AuraBuff.class, AuraOfProtection.AuraBuff.DURATION);
		if (ctx.canWorldFx()) {
			Sample.INSTANCE.play(Assets.Sounds.READ);
			ctx.body.sprite.operate(ctx.body.pos);
		}
		ClericSpellEchoBridge.onSpellCast(spell, ctx, tome);
		return true;
	}

	private static boolean judgement(EchoActionContext ctx, HolyTome tome, Judgement spell) {
		if (!spell.canCast(ctx.kit)) {
			return false;
		}
		AscendedForm.AscendBuff ascend = ctx.kit.buff(AscendedForm.AscendBuff.class);
		int damageBase = 5 + 5 * ctx.kit.pointsInTalent(Talent.JUDGEMENT);
		if (ascend != null) {
			damageBase += Math.round(damageBase * ascend.spellCasts / 3f);
		}
		boolean[] inFov = ctx.body.fieldOfView != null ? ctx.body.fieldOfView : Dungeon.level.heroFOV;
		for (Char ch : Actor.chars()) {
			if (ch.alignment != ctx.body.alignment
					&& ch.pos >= 0 && ch.pos < inFov.length && inFov[ch.pos]) {
				ch.damage(Hero.heroDamageIntRange(damageBase, 2 * damageBase), spell);
				if (ctx.kit.subClass == HeroSubClass.PRIEST) {
					Buff.affect(ch, GuidingLight.Illuminated.class);
				}
			}
		}
		if (ascend != null) {
			ascend.spellCasts = 0;
		}
		ClericSpellEchoBridge.onSpellCast(spell, ctx, tome);
		return true;
	}

	/**
	 * Echo auto-pick apply. Default bridges to {@link #onItemSelected} — subclasses
	 * with Hero-only VFX/spend should override.
	 */
	private static boolean holyIntuition(EchoActionContext ctx, HolyTome tome, HolyIntuition spell) {
		Item pick = ClericSpellEchoBridge.firstUsableItem(spell, ctx.kit);
		if (pick == null) {
			return false;
		}
		pick.cursedKnown = true;
		ClericSpellEchoBridge.onSpellCast(spell, ctx, tome);
		return true;
	}

	/**
	 * Echo: apply BodyFormBuff from equipped weapon enchantment or armor glyph.
	 * Hero: opens
	 * {@link com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity.WndItemtypeSelect}.
	 */
	private static boolean bodyForm(EchoActionContext ctx, HolyTome tome, BodyForm spell) {
		Bundlable effect = null;
		if (ctx.kit.belongings.weapon() instanceof Weapon) {
			Weapon.Enchantment ench = ((Weapon) ctx.kit.belongings.weapon()).enchantment;
			if (ench != null) {
				effect = ench;
			}
		}
		if (effect == null && ctx.kit.belongings.armor() != null) {
			Armor.Glyph glyph = ctx.kit.belongings.armor().glyph;
			if (glyph != null) {
				effect = glyph;
			}
		}
		if (effect == null) {
			return false;
		}
		Buff.prolong(ctx.body, BodyForm.BodyFormBuff.class, BodyForm.duration()).setEffect(effect);
		ClericSpellEchoBridge.onSpellCast(spell, ctx, tome);
		return true;
	}

}
