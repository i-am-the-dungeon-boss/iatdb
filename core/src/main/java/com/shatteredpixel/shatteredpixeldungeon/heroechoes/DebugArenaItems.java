package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.DebugSettings;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.watabou.noosa.Game;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.List;

/** Instantiates every catalog {@link Item} for the debug arena floor. */
public final class DebugArenaItems {

	private DebugArenaItems() {
	}

	public static List<Item> createAll() {
		// Identity handlers are required before identify(); safe to re-init in debug.
		Scroll.initLabels();
		Potion.initColors();
		Ring.initGems();

		Hero previous = Dungeon.hero;
		boolean stubbedHero = previous == null || !previous.isAlive();
		if (stubbedHero) {
			Hero stub = new Hero();
			stub.HP = stub.HT = 1;
			Dungeon.hero = stub;
		}
		try {
			List<Item> items = new ArrayList<>();
			for (Catalog catalog : Catalog.values()) {
				if (catalog == Catalog.ENCHANTMENTS || catalog == Catalog.GLYPHS) {
					continue;
				}
				for (Class<?> cls : catalog.items()) {
					if (!Item.class.isAssignableFrom(cls)) {
						continue;
					}
					@SuppressWarnings("unchecked")
					Item item = Reflection.newInstance((Class<? extends Item>) cls);
					if (item == null) {
						throw new IllegalStateException("failed to instantiate catalog item: " + cls.getName());
					}
					item.identify();
					items.add(item);
				}
			}
			return items;
		} finally {
			if (stubbedHero) {
				Dungeon.hero = previous;
			}
		}
	}

	/**
	 * Clears all ground heaps and drops a fresh full catalog set. Debug builds
	 * only.
	 *
	 * @return number of items dropped, or 0 when unavailable
	 */
	public static int restockGround() {
		if (!DebugSettings.isDebugBuild() || Dungeon.level == null) {
			return 0;
		}
		Level level = Dungeon.level;
		for (Heap heap : level.heaps.valueList().toArray(new Heap[0])) {
			if (heap != null) {
				heap.destroy();
			}
		}

		List<Item> items = createAll();
		List<Integer> cells = dropCells(level);
		int dropped = 0;
		int limit = Math.min(items.size(), cells.size());
		for (int i = 0; i < limit; i++) {
			dropQuiet(level, items.get(i), cells.get(i));
			dropped++;
		}
		return dropped;
	}

	/**
	 * Drops without camera-dependent sprite placement so headless tests and live
	 * {@link GameScene} both work.
	 */
	static void dropQuiet(Level level, Item item, int cell) {
		Heap existing = level.heaps.get(cell);
		if (existing != null) {
			existing.drop(item);
			return;
		}
		Heap heap = new Heap();
		heap.seen = true;
		heap.pos = cell;
		heap.drop(item);
		level.heaps.put(cell, heap);
		if (Game.instance != null && ShatteredPixelDungeon.scene() instanceof GameScene) {
			GameScene.add(heap);
		} else if (heap.sprite == null) {
			heap.sprite = new ItemSprite();
		}
	}

	private static List<Integer> dropCells(Level level) {
		List<Integer> cells = new ArrayList<>();
		for (int i = 0; i < level.length(); i++) {
			int terr = level.map[i];
			if ((terr == Terrain.EMPTY || terr == Terrain.EMPTY_DECO || terr == Terrain.EMPTY_SP
					|| terr == Terrain.GRASS || terr == Terrain.EMBERS)
					&& level.passable[i]
					&& Actor.findChar(i) == null) {
				cells.add(i);
			}
		}
		return cells;
	}
}
