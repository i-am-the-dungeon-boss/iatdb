package com.shatteredpixel.shatteredpixeldungeon.heroechoes.action;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Dread;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Drowsy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Recharging;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.MirrorImage;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameSceneEchoBridge;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.InventoryScroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.InventoryScrollEchoBridge;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfLullaby;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMirrorImage;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRage;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTerror;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfAntiMagic;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfDread;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.sprites.EchoBossSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.noosa.particles.Emitter;

import java.util.ArrayList;

/**
 * Per-scroll Echo read effects — fork-owned. No Hero GLog, Catalog, or bag UI.
 */
public final class EchoScrollHandlers {

	private EchoScrollHandlers() {
	}

	public static boolean read(EchoActionContext ctx, Scroll scroll) {
		if (scroll instanceof ScrollOfUpgrade) {
			return InventoryScrollEchoBridge.readUpgrade((ScrollOfUpgrade) scroll, ctx);
		}
		if (scroll instanceof InventoryScroll) {
			return InventoryScrollEchoBridge.read((InventoryScroll) scroll, ctx);
		}
		if (scroll instanceof ScrollOfRecharging) {
			return recharging(ctx, (ScrollOfRecharging) scroll);
		}
		if (scroll instanceof ScrollOfTeleportation) {
			return teleportation(ctx, (ScrollOfTeleportation) scroll);
		}
		if (scroll instanceof ScrollOfLullaby) {
			return lullaby(ctx, (ScrollOfLullaby) scroll);
		}
		if (scroll instanceof ScrollOfRetribution) {
			return retribution(ctx, (ScrollOfRetribution) scroll);
		}
		if (scroll instanceof ScrollOfMagicMapping) {
			return magicMapping(ctx, (ScrollOfMagicMapping) scroll);
		}
		if (scroll instanceof ScrollOfMirrorImage) {
			return mirrorImage(ctx, (ScrollOfMirrorImage) scroll);
		}
		if (scroll instanceof ScrollOfRage) {
			return rage(ctx, (ScrollOfRage) scroll);
		}
		if (scroll instanceof ScrollOfTerror) {
			return terror(ctx, (ScrollOfTerror) scroll);
		}
		if (scroll instanceof ScrollOfPsionicBlast) {
			return psionicBlast(ctx, (ScrollOfPsionicBlast) scroll);
		}
		if (scroll instanceof ScrollOfDread) {
			return dread(ctx, (ScrollOfDread) scroll);
		}
		if (scroll instanceof ScrollOfAntiMagic) {
			return antiMagic(ctx, (ScrollOfAntiMagic) scroll);
		}
		return false;
	}

	static void readAnimation(EchoActionContext ctx) {
		Invisibility.dispel(ctx.body);
		if (ctx.canWorldFx()) {
			if (ctx.body.sprite instanceof EchoBossSprite) {
				((EchoBossSprite) ctx.body.sprite).read();
			} else {
				ctx.body.sprite.operate(ctx.body.pos);
			}
		}
	}

	private static boolean recharging(EchoActionContext ctx, ScrollOfRecharging scroll) {
		scroll.detach(ctx.kit.belongings.backpack);
		Buff.affect(ctx.body, Recharging.class, Recharging.DURATION);
		ScrollOfRecharging.charge(ctx.body);

		if (ctx.canWorldFx()) {
			Sample.INSTANCE.play(Assets.Sounds.READ);
			Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
			SpellSprite.show(ctx.body, SpellSprite.CHARGE);
		}

		readAnimation(ctx);
		return true;
	}

	private static boolean teleportation(EchoActionContext ctx, ScrollOfTeleportation scroll) {
		scroll.detach(ctx.kit.belongings.backpack);

		boolean teleported = echoTeleport(ctx);

		if (ctx.canWorldFx()) {
			Sample.INSTANCE.play(Assets.Sounds.READ);
		}
		if (teleported) {
			readAnimation(ctx);
		}
		return true;
	}

	private static boolean echoTeleport(EchoActionContext ctx) {
		if (ctx.canWorldFx() && GameSceneEchoBridge.hasScene()) {
			return ScrollOfTeleportation.teleportChar(ctx.body);
		}
		return echoTeleportHeadless(ctx.body);
	}

	private static boolean echoTeleportHeadless(Char ch) {
		if (Char.hasProp(ch, Char.Property.IMMOVABLE) || ch.isImmune(ScrollOfTeleportation.class)) {
			return false;
		}
		int count = 20;
		int pos;
		do {
			pos = Dungeon.level.randomRespawnCell(ch);
			if (count-- <= 0) {
				break;
			}
		} while (pos == -1 || Dungeon.level.secret[pos]);
		if (pos == -1) {
			for (int i = 0; i < Dungeon.level.length(); i++) {
				if (Dungeon.level.passable[i] && !Dungeon.level.secret[i]
						&& com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(i) == null) {
					pos = i;
					break;
				}
			}
		}
		if (pos == -1) {
			return false;
		}
		ch.move(pos, false);
		if (ch.pos == pos && ch.sprite != null) {
			ch.sprite.place(pos);
		}
		Dungeon.level.occupyCell(ch);
		Buff.detach(ch, Roots.class);
		return true;
	}

	private static boolean lullaby(EchoActionContext ctx, ScrollOfLullaby scroll) {
		scroll.detach(ctx.kit.belongings.backpack);

		if (ctx.canWorldFx()) {
			ctx.body.sprite.centerEmitter().start(Speck.factory(Speck.NOTE), 0.3f, 5);
			Sample.INSTANCE.play(Assets.Sounds.LULLABY);
		}

		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (Dungeon.level.heroFOV[mob.pos]) {
				Buff.affect(mob, Drowsy.class, Drowsy.DURATION);
				if (mob.sprite != null && mob.sprite.parent != null) {
					mob.sprite.centerEmitter().start(Speck.factory(Speck.NOTE), 0.3f, 5);
				}
			}
		}
		// Echo: also soothe the opposing Hero (not in level.mobs)
		ctx.forEachVisibleHostile(ch -> {
			if (ch instanceof Mob) {
				return; // already handled
			}
			Buff.affect(ch, Drowsy.class, Drowsy.DURATION);
			if (ch.sprite != null && ch.sprite.parent != null) {
				Emitter e = ch.sprite.centerEmitter();
				if (e != null) {
					e.start(Speck.factory(Speck.NOTE), 0.3f, 5);
				}
			}
		});

		Buff.affect(ctx.body, Drowsy.class, Drowsy.DURATION);

		readAnimation(ctx);
		return true;
	}

	private static boolean retribution(EchoActionContext ctx, ScrollOfRetribution scroll) {
		scroll.detach(ctx.kit.belongings.backpack);

		if (ctx.canWorldFx()) {
			GameScene.flash(0x80FFFFFF);
			Sample.INSTANCE.play(Assets.Sounds.BLAST);
		}

		float hpPercent = (ctx.body.HT - ctx.body.HP) / (float) (ctx.body.HT);
		float power = Math.min(4f, 4.45f * hpPercent);

		ArrayList<Char> targets = new ArrayList<>();
		ctx.forEachVisibleHostile(targets::add);

		for (Char ch : targets) {
			ch.damage(Math.round(ch.HT / 10f + (ch.HP * power * 0.225f)), scroll);
			if (ch.isAlive()) {
				Buff.prolong(ch, Blindness.class, Blindness.DURATION);
			}
		}

		Buff.prolong(ctx.body, Weakness.class, Weakness.DURATION);
		Buff.prolong(ctx.body, Blindness.class, Blindness.DURATION);
		Dungeon.observe();

		readAnimation(ctx);
		return true;
	}

	private static boolean magicMapping(EchoActionContext ctx, ScrollOfMagicMapping scroll) {
		scroll.detach(ctx.kit.belongings.backpack);
		int length = Dungeon.level.length();
		int[] map = Dungeon.level.map;
		boolean[] mapped = Dungeon.level.mapped;
		boolean[] discoverable = Dungeon.level.discoverable;

		boolean noticed = false;

		for (int i = 0; i < length; i++) {

			int terr = map[i];

			if (discoverable[i]) {

				mapped[i] = true;
				if ((Terrain.flags[terr] & Terrain.SECRET) != 0) {

					Dungeon.level.discover(i);

					if (Dungeon.level.heroFOV[i]) {
						ScrollOfMagicMapping.discover(i);
						noticed = true;
					}
				}
			}
		}

		if (ctx.canWorldFx()) {
			if (noticed) {
				Sample.INSTANCE.play(Assets.Sounds.SECRET);
			}
			SpellSprite.show(ctx.body, SpellSprite.MAP);
			Sample.INSTANCE.play(Assets.Sounds.READ);
		}

		readAnimation(ctx);
		return true;
	}

	private static boolean mirrorImage(EchoActionContext ctx, ScrollOfMirrorImage scroll) {
		scroll.detach(ctx.kit.belongings.backpack);
		if (ctx.canWorldFx() && GameSceneEchoBridge.hasScene()) {
			ScrollOfMirrorImage.spawnImages(ctx.kit, ctx.body.pos, 2);
		} else {
			echoSpawnImagesHeadless(ctx.kit, ctx.body.pos, 2);
		}

		if (ctx.canWorldFx()) {
			Sample.INSTANCE.play(Assets.Sounds.READ);
		}

		readAnimation(ctx);
		return true;
	}

	private static void echoSpawnImagesHeadless(Hero hero, int pos, int nImages) {
		ArrayList<Integer> respawnPoints = new ArrayList<>();

		for (int i = 0; i < PathFinder.NEIGHBOURS9.length; i++) {
			int p = pos + PathFinder.NEIGHBOURS9[i];
			if (com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(p) == null
					&& Dungeon.level.passable[p]) {
				respawnPoints.add(p);
			}
		}

		while (nImages > 0 && respawnPoints.size() > 0) {
			int index = Random.index(respawnPoints);

			MirrorImage mob = new MirrorImage();
			mob.duplicate(hero);
			GameScene.add(mob);
			mob.move(respawnPoints.get(index), false);
			if (mob.sprite != null) {
				mob.sprite.place(mob.pos);
			}
			Dungeon.level.occupyCell(mob);

			respawnPoints.remove(index);
			nImages--;
		}
	}

	private static boolean rage(EchoActionContext ctx, ScrollOfRage scroll) {
		scroll.detach(ctx.kit.belongings.backpack);
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			mob.beckon(ctx.body.pos);
			if (mob.alignment != Char.Alignment.ALLY && Dungeon.level.heroFOV[mob.pos]) {
				Buff.prolong(mob, Amok.class, 5f);
			}
		}

		if (ctx.canWorldFx()) {
			if (ctx.body.sprite != null) {
				ctx.body.sprite.centerEmitter().start(Speck.factory(Speck.SCREAM), 0.3f, 3);
			}
			Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
		}

		readAnimation(ctx);
		return true;
	}

	private static boolean terror(EchoActionContext ctx, ScrollOfTerror scroll) {
		scroll.detach(ctx.kit.belongings.backpack);

		if (ctx.canWorldFx()) {
			new Flare(5, 32).color(0xFF0000, true).show(ctx.body.sprite, 2f);
			Sample.INSTANCE.play(Assets.Sounds.READ);
		}

		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (mob.alignment != Char.Alignment.ALLY && Dungeon.level.heroFOV[mob.pos]) {
				Buff.affect(mob, Terror.class, Terror.DURATION).object = ctx.body.id();

				if (mob.buff(Terror.class) != null) {
					// applied
				}
			}
		}

		readAnimation(ctx);
		return true;
	}

	private static boolean psionicBlast(EchoActionContext ctx, ScrollOfPsionicBlast scroll) {
		scroll.detach(ctx.kit.belongings.backpack);

		if (ctx.canWorldFx()) {
			GameScene.flash(0x80FFFFFF);
			Sample.INSTANCE.play(Assets.Sounds.BLAST);
		}

		ArrayList<Char> targets = new ArrayList<>();
		ctx.forEachVisibleHostile(targets::add);

		for (Char ch : targets) {
			ch.damage(Math.round(ch.HT / 2f + ch.HP / 2f), scroll);
			if (ch.isAlive()) {
				Buff.prolong(ch, Blindness.class, Blindness.DURATION);
			}
		}

		ctx.body.damage(Math.max(0, Math.round(ctx.body.HT * (0.5f * (float) Math.pow(0.9, targets.size())))), scroll);
		if (ctx.body.isAlive()) {
			Buff.prolong(ctx.body, Blindness.class, Blindness.DURATION);
			Buff.prolong(ctx.body, Weakness.class, Weakness.DURATION * 5f);
			readAnimation(ctx);
		}

		return true;
	}

	private static boolean dread(EchoActionContext ctx, ScrollOfDread scroll) {
		scroll.detach(ctx.kit.belongings.backpack);

		if (ctx.canWorldFx()) {
			new Flare(5, 32).color(0xFF0000, true).show(ctx.body.sprite, 2f);
			Sample.INSTANCE.play(Assets.Sounds.READ);
		}

		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (mob.alignment != Char.Alignment.ALLY && Dungeon.level.heroFOV[mob.pos]) {
				if (!mob.isImmune(Dread.class)) {
					Buff.affect(mob, Dread.class).object = ctx.body.id();
				} else {
					Buff.affect(mob, Terror.class, Terror.DURATION).object = ctx.body.id();
				}
			}
		}

		readAnimation(ctx);
		return true;
	}

	private static boolean antiMagic(EchoActionContext ctx, ScrollOfAntiMagic scroll) {
		scroll.detach(ctx.kit.belongings.backpack);
		Buff.affect(ctx.body, MagicImmune.class, MagicImmune.DURATION);

		if (ctx.canWorldFx()) {
			new Flare(5, 32).color(0x00FF00, true).show(ctx.body.sprite, 2f);
			scroll.identify();
		}

		readAnimation(ctx);
		return true;
	}
}
