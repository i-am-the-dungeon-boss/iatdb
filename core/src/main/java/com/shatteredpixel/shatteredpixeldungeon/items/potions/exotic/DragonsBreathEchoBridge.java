package com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

/**
 * Same-package cone effect for Echo {@link com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoPotionAdapter}
 * and headless Hero tests. Hero UI keeps master CellSelector flow in
 * {@link PotionOfDragonsBreath}.
 */
public final class DragonsBreathEchoBridge {

	private DragonsBreathEchoBridge() {
	}

	/** Fire blobs, Burning, Cripple, doors — shared Hero/Echo cone resolve. */
	public static void applyCone(int sourcePos, int cell) {
		Ballistica bolt = new Ballistica(sourcePos, cell, Ballistica.WONT_STOP);
		ConeAOE cone = new ConeAOE(bolt, 6, 60,
				Ballistica.STOP_SOLID | Ballistica.STOP_TARGET | Ballistica.IGNORE_SOFT_SOLID);

		ArrayList<Integer> adjacentCells = new ArrayList<>();
		for (int c : cone.cells) {
			// ignore caster cell
			if (c == bolt.sourcePos) {
				continue;
			}

			// knock doors open
			if (Dungeon.level.map[c] == Terrain.DOOR) {
				Level.set(c, Terrain.OPEN_DOOR);
				GameScene.updateMap(c);
			}

			// only ignite cells directly near caster if they are flammable
			if (Dungeon.level.adjacent(bolt.sourcePos, c) && !Dungeon.level.flamable[c]) {
				adjacentCells.add(c);
			} else {
				GameScene.add(Blob.seed(c, 5, Fire.class));
			}

			Char ch = Actor.findChar(c);
			if (ch != null) {
				Buff.affect(ch, Burning.class).reignite(ch);
				Buff.prolong(ch, Cripple.class, 5f);
			}
		}

		// ignite cells that share a side with an adjacent cell, are flammable, and are
		// further from the source pos
		// This prevents short-range casts not igniting barricades or bookshelves
		for (int c : adjacentCells) {
			for (int i : PathFinder.NEIGHBOURS4) {
				if (Dungeon.level.trueDistance(c + i, bolt.sourcePos) > Dungeon.level.trueDistance(c, bolt.sourcePos)
						&& Dungeon.level.flamable[c + i]
						&& Fire.volumeAt(c + i, Fire.class) == 0) {
					GameScene.add(Blob.seed(c + i, 5, Fire.class));
				}
			}
		}
	}

	public static void playConeVfx(Char body, int cell, Callback onComplete) {
		if (body == null || body.sprite == null || body.sprite.parent == null) {
			if (onComplete != null) {
				onComplete.call();
			}
			return;
		}
		Ballistica bolt = new Ballistica(body.pos, cell, Ballistica.WONT_STOP);
		int dist = Math.min(bolt.dist, 6);
		ConeAOE cone = new ConeAOE(bolt, 6, 60,
				Ballistica.STOP_SOLID | Ballistica.STOP_TARGET | Ballistica.IGNORE_SOFT_SOLID);
		for (Ballistica ray : cone.outerRays) {
			((MagicMissile) body.sprite.parent.recycle(MagicMissile.class)).reset(
					MagicMissile.FIRE_CONE,
					body.sprite,
					ray.path.get(ray.dist),
					null);
		}
		MagicMissile.boltFromChar(
				body.sprite.parent,
				MagicMissile.FIRE_CONE,
				body.sprite,
				bolt.path.get(dist / 2),
				onComplete);
	}
}
