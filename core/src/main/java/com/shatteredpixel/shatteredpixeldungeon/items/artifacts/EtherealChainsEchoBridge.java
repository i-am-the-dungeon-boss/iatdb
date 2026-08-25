package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.effects.Chains;
import com.shatteredpixel.shatteredpixeldungeon.effects.Effects;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoActionContext;
import com.shatteredpixel.shatteredpixeldungeon.levels.MiningLevel;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;

/**
 * Same-package bridge for Echo Ethereal Chains cast with immediate relocation.
 */
public final class EtherealChainsEchoBridge {

	private EtherealChainsEchoBridge() {
	}

	public static boolean cast(EchoBoss boss, EtherealChains chains, int target) {
		if (boss == null || chains == null || target < 0) {
			return false;
		}
		EchoActionContext ctx = EchoActionContext.of(boss);
		if (ctx.body.buff(MagicImmune.class) != null) {
			return false;
		}
		if (!chains.isEquipped(ctx.stats())) {
			return false;
		}
		if (chains.charge < 1) {
			return false;
		}
		if (chains.cursed) {
			return false;
		}

		Char body = ctx.body;
		PathFinder.buildDistanceMap(target, BArray.or(Dungeon.level.passable, Dungeon.level.avoid, null));
		if (!(Dungeon.level instanceof MiningLevel) && PathFinder.distance[body.pos] == Integer.MAX_VALUE) {
			return false;
		}

		Ballistica chain = new Ballistica(body.pos, target, Ballistica.STOP_TARGET);
		Char enemy = Actor.findChar(chain.collisionPos);
		if (enemy != null && enemy != body) {
			return chainEnemy(ctx, chains, chain, enemy);
		}
		return chainLocation(ctx, chains, chain);
	}

	private static boolean chainEnemy(EchoActionContext ctx, EtherealChains chains, Ballistica chain, Char enemy) {
		if (enemy.properties().contains(Char.Property.IMMOVABLE)) {
			return false;
		}

		int bestPos = -1;
		for (int i : chain.subPath(1, chain.dist)) {
			if (!Dungeon.level.solid[i]
					&& Actor.findChar(i) == null
					&& (!Char.hasProp(enemy, Char.Property.LARGE) || Dungeon.level.openSpace[i])) {
				bestPos = i;
				break;
			}
		}
		if (bestPos == -1) {
			return false;
		}

		int chargeUse = Dungeon.level.distance(enemy.pos, bestPos);
		if (chargeUse > chains.charge) {
			return false;
		}

		if (ctx.canWorldFx() && enemy.sprite != null) {
			Sample.INSTANCE.play(Assets.Sounds.CHAINS);
			ctx.body.sprite.parent.add(new Chains(ctx.body.sprite.center(),
					enemy.sprite.center(),
					Effects.Type.ETHEREAL_CHAIN,
					null));
		}

		enemy.move(bestPos, false);
		chains.charge -= chargeUse;
		Invisibility.dispel(ctx.body);
		chains.artifactProc(enemy, chains.visiblyUpgraded(), chargeUse);
		chains.updateQuickslot();
		return true;
	}

	private static boolean chainLocation(EchoActionContext ctx, EtherealChains chains, Ballistica chain) {
		Char body = ctx.body;
		if (body.rooted) {
			return false;
		}
		if (Dungeon.level.solid[chain.collisionPos]
				|| !(Dungeon.level.passable[chain.collisionPos] || Dungeon.level.avoid[chain.collisionPos])) {
			return false;
		}
		boolean solidFound = false;
		for (int i : PathFinder.NEIGHBOURS8) {
			if (Dungeon.level.solid[chain.collisionPos + i]) {
				solidFound = true;
				break;
			}
		}
		if (!solidFound) {
			return false;
		}

		int newPos = chain.collisionPos;
		int chargeUse = Dungeon.level.distance(body.pos, newPos);
		if (chargeUse > chains.charge) {
			return false;
		}

		if (ctx.canWorldFx()) {
			Sample.INSTANCE.play(Assets.Sounds.CHAINS);
			body.sprite.parent.add(new Chains(body.sprite.center(),
					DungeonTilemap.raisedTileCenterToWorld(newPos),
					Effects.Type.ETHEREAL_CHAIN,
					null));
		}

		body.move(newPos, false);
		chains.charge -= chargeUse;
		Invisibility.dispel(body);
		chains.updateQuickslot();
		return true;
	}
}
