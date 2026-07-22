package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.DebugSettings;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.DebugArenaItems;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoBossSpawner;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Debug-only sandbox: open room, every catalog item on the floor, and a mirror
 * {@link EchoBoss} of the current hero.
 */
public class DebugArenaLevel extends Level {

	private static final int SIZE = 34;

	{
		color1 = 0x534f3e;
		color2 = 0xb9d661;
		viewDistance = SIZE;
	}

	@Override
	public void playLevelMusic() {
		Music.INSTANCE.play(Assets.Music.SEWERS_1, true);
	}

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_SEWERS;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_SEWERS;
	}

	@Override
	protected boolean build() {
		setSize(SIZE, SIZE);
		Arrays.fill(map, Terrain.WALL);
		Painter.fill(this, 1, 1, SIZE - 2, SIZE - 2, Terrain.EMPTY);

		int mid = SIZE / 2;
		int entrance = (SIZE - 2) * width() + mid;
		map[entrance] = Terrain.ENTRANCE;
		transitions.add(new LevelTransition(this, entrance, LevelTransition.Type.REGULAR_ENTRANCE));

		feeling = Feeling.NONE;
		viewDistance = SIZE;
		return true;
	}

	@Override
	public void create() {
		super.create();
		Arrays.fill(visited, true);
		Arrays.fill(mapped, true);
	}

	@Override
	public Mob createMob() {
		return null;
	}

	@Override
	protected void createMobs() {
		if (!DebugSettings.isDebugBuild() || Dungeon.hero == null) {
			return;
		}
		Echo echo = Echo.fromHero(
				Dungeon.hero,
				Dungeon.depth,
				Game.version != null ? Game.version : "debug",
				Dungeon.seed);
		EchoPolicy policy = EchoPolicy.fallback();
		Dungeon.armPendingEchoBoss(echo, policy);

		EchoBoss boss = EchoBossSpawner.create(Dungeon.depth);
		boss.pos = mirrorCell();
		mobs.add(boss);
	}

	@Override
	protected void createItems() {
		if (!DebugSettings.isDebugBuild()) {
			return;
		}
		List<Item> items = DebugArenaItems.createAll();
		List<Integer> cells = floorCells();
		int limit = Math.min(items.size(), cells.size());
		for (int i = 0; i < limit; i++) {
			drop(items.get(i), cells.get(i));
		}
	}

	@Override
	public Actor addRespawner() {
		return null;
	}

	@Override
	public int randomRespawnCell(Char ch) {
		ArrayList<Integer> candidates = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS8) {
			int cell = entrance() + i;
			if (passable[cell]
					&& Actor.findChar(cell) == null
					&& (!Char.hasProp(ch, Char.Property.LARGE) || openSpace[cell])) {
				candidates.add(cell);
			}
		}
		if (candidates.isEmpty()) {
			return entrance();
		}
		return Random.element(candidates);
	}

	int mirrorCell() {
		int mid = SIZE / 2;
		return 2 * width() + mid;
	}

	private List<Integer> floorCells() {
		List<Integer> cells = new ArrayList<>();
		int entrance = entrance();
		int mirror = mirrorCell();
		for (int i = 0; i < length(); i++) {
			if (map[i] == Terrain.EMPTY && i != entrance && i != mirror) {
				cells.add(i);
			}
		}
		return cells;
	}
}
