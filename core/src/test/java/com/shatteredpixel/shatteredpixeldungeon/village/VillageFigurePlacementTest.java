package com.shatteredpixel.shatteredpixeldungeon.village;

import java.util.ArrayDeque;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.VillageLevel;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;

@ExtendWith(GdxTestExtension.class)
class VillageFigurePlacementTest {

	@BeforeEach
	void onGroundLevel() {
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
		Dungeon.depth = VillageLevel.VILLAGE_DEPTH;
		Dungeon.branch = 0;
	}

	private VillageLevel village() {
		VillageLevel level = new VillageLevel();
		level.create();
		return level;
	}

	private ArrayList<Integer> allAnchors(VillageLevel level) {
		ArrayList<Integer> cells = new ArrayList<>();
		int[] depths = VillageFigurePlacement.depthPosts(level);
		for (int i = 0; i < depths.length; i++) {
			cells.add(depths[i]);
		}
		int[] mentions = VillageFigurePlacement.mentionPosts(level);
		for (int i = 0; i < mentions.length; i++) {
			cells.add(mentions[i]);
		}
		return cells;
	}

	@Test
	@DisplayName("There is a fixed spot for every depth and every mention")
	void oneAnchorPerFigure() {
		VillageLevel level = village();

		Assertions.assertThat(VillageFigurePlacement.depthPosts(level))
				.hasSize(VillageFigurePlacement.DEPTH_POSTS);
		Assertions.assertThat(VillageFigurePlacement.mentionPosts(level))
				.hasSize(VillageFigurePlacement.MENTION_POSTS);
	}

	@Test
	@DisplayName("No two figures are asked to stand in the same place")
	void anchorsAreDistinct() {
		VillageLevel level = village();

		ArrayList<Integer> cells = allAnchors(level);

		Assertions.assertThat(cells).doesNotHaveDuplicates();
	}

	@Test
	@DisplayName("Every figure stands somewhere a player could stand")
	void anchorsArePassable() {
		VillageLevel level = village();

		for (Integer cell : allAnchors(level)) {
			Assertions.assertThat(level.passable[cell])
					.as("cell %d is passable", cell)
					.isTrue();
			Assertions.assertThat(level.solid[cell])
					.as("cell %d is not solid", cell)
					.isFalse();
		}
	}

	/**
	 * This used to check that no figure stood on the path's column, which only
	 * meant anything while the path was a single straight run. The altar's
	 * walkway is a cross and the deepest echo stands squarely on its axis, so the
	 * column is no longer the thing to protect. What always mattered is that a
	 * town full of figures never walls the player in — and asserting that
	 * directly is stronger, since it also catches a figure plugging a doorway,
	 * which the column check never could.
	 */
	@Test
	@DisplayName("A village full of figures still lets the hero cross it")
	void anchorsNeverBlockTheWayThrough() {
		VillageLevel level = village();

		boolean[] blocked = new boolean[level.length()];
		for (Integer cell : allAnchors(level)) {
			Assertions.assertThat(cell).isNotEqualTo(level.arrivalCell());
			Assertions.assertThat(cell).isNotEqualTo(level.dungeonEntrance());
			blocked[cell] = true;
		}

		Assertions.assertThat(reachableAvoiding(level, level.arrivalCell(),
				level.dungeonEntrance(), blocked))
				.as("the dungeon gate is still reachable with every post taken")
				.isTrue();

		for (int cell = 0; cell < level.length(); cell++) {
			if (level.map[cell] != Terrain.DOOR) {
				continue;
			}
			Assertions.assertThat(reachableAvoiding(level, level.arrivalCell(), cell, blocked))
					.as("door at (%d,%d) is walled off once every post is taken",
							cell % level.width(), cell / level.width())
					.isTrue();
		}
	}

	/** Flood fill across passable cells, treating the given cells as occupied. */
	private boolean reachableAvoiding(VillageLevel level, int from, int to, boolean[] blocked) {
		boolean[] seen = new boolean[level.length()];
		java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
		queue.add(from);
		seen[from] = true;
		int[] steps = { -1, 1, -level.width(), level.width() };
		while (!queue.isEmpty()) {
			int cell = queue.poll();
			if (cell == to) {
				return true;
			}
			for (int step : steps) {
				int next = cell + step;
				if (next < 0 || next >= level.length() || seen[next] || blocked[next]) {
					continue;
				}
				if (level.passable[next]
						|| level.map[next] == Terrain.EXIT
						|| level.map[next] == Terrain.DOOR) {
					seen[next] = true;
					queue.add(next);
				}
			}
		}
		return false;
	}

	@Test
	@DisplayName("No figure stands on top of a villager who already lives there")
	void anchorsAvoidTheVillagers() {
		VillageLevel level = village();

		ArrayList<Integer> taken = new ArrayList<>();
		for (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob mob : level.mobs) {
			taken.add(mob.pos);
		}

		Assertions.assertThat(taken).as("create() places the villagers").isNotEmpty();
		Assertions.assertThat(allAnchors(level)).doesNotContainAnyElementsOf(taken);
	}

	@Test
	@DisplayName("The echo bosses stand on the altar, one to a quarter and the deepest at the throne")
	void depthPostsSitOnTheAltar() {
		VillageLevel level = village();

		int[] posts = VillageFigurePlacement.depthPosts(level);
		int[] expected = { EchoAltar.SEWERS, EchoAltar.PRISON, EchoAltar.CAVES, EchoAltar.CITY };

		for (int i = 0; i < posts.length; i++) {
			int x = posts[i] % level.width();
			int y = posts[i] / level.width();
			Assertions.assertThat(EchoAltar.inDisc(x, y))
					.as("post %d at (%d,%d) is on the altar", i, x, y)
					.isTrue();
		}

		for (int i = 0; i < expected.length; i++) {
			int x = posts[i] % level.width();
			int y = posts[i] / level.width();
			Assertions.assertThat(EchoAltar.quadrantOf(x, y))
					.as("post %d should stand on its own quarter", i)
					.isEqualTo(expected[i]);
		}

		int deepestX = posts[4] % level.width();
		int deepestY = posts[4] / level.width();
		Assertions.assertThat(EchoAltar.inDais(deepestX, deepestY))
				.as("the deepest echo stands on the raised centre")
				.isTrue();
	}

	@Test
	@DisplayName("The mentions keep their ring around the well, wherever the well now sits")
	void mentionPostsKeepTheirArrangement() {
		VillageLevel level = village();

		int well = -1;
		for (int cell = 0; cell < level.length(); cell++) {
			if (level.map[cell] == Terrain.WELL) {
				well = cell;
			}
		}
		Assertions.assertThat(well).as("the village still has a well").isNotEqualTo(-1);

		int wellX = well % level.width();
		int wellY = well / level.width();
		int[][] arrangement = { { 0, -2 }, { -2, -1 }, { 2, -1 }, { -2, 2 }, { 2, 2 } };

		int[] posts = VillageFigurePlacement.mentionPosts(level);
		for (int i = 0; i < posts.length; i++) {
			Assertions.assertThat(posts[i] % level.width() - wellX)
					.as("mention %d keeps its offset east of the well", i)
					.isEqualTo(arrangement[i][0]);
			Assertions.assertThat(posts[i] / level.width() - wellY)
					.as("mention %d keeps its offset south of the well", i)
					.isEqualTo(arrangement[i][1]);
		}
	}

	@Test
	@DisplayName("The mentions gather on a paved court, and the court joins the road")
	void theMentionsGatherOnAPavedCourt() {
		VillageLevel level = village();

		int[] posts = VillageFigurePlacement.mentionPosts(level);
		for (int i = 0; i < posts.length; i++) {
			Assertions.assertThat(level.map[posts[i]])
					.as("mention %d stands on the court's paving", i)
					.isEqualTo(Terrain.EMPTY_SP);
		}

		// and the court is not an island: from one of its cells the paving runs
		// unbroken to the avenue that serves the doors along the top of town
		Assertions.assertThat(pavingReaches(level, posts[0], level.cell(16, 11)))
				.as("the court's paving joins the avenue")
				.isTrue();
	}

	/** Whether paving runs unbroken from one cell to another, four ways. */
	private boolean pavingReaches(VillageLevel level, int from, int to) {
		boolean[] seen = new boolean[level.length()];
		ArrayDeque<Integer> queue = new ArrayDeque<>();
		queue.add(from);
		seen[from] = true;
		int[] step = { -level.width(), level.width(), -1, 1 };
		while (!queue.isEmpty()) {
			int cell = queue.remove();
			if (cell == to) {
				return true;
			}
			for (int i = 0; i < step.length; i++) {
				int next = cell + step[i];
				if (next < 0 || next >= level.length() || seen[next]) {
					continue;
				}
				if (level.map[next] != Terrain.EMPTY_SP) {
					continue;
				}
				seen[next] = true;
				queue.add(next);
			}
		}
		return false;
	}

	@Test
	@DisplayName("The deepest echo stands in the throne")
	void theDeepestEchoStandsInTheThrone() {
		VillageLevel level = village();

		int seat = level.cell(EchoAltar.THRONE_SEAT_X, EchoAltar.THRONE_SEAT_Y);
		Assertions.assertThat(level.passable[seat]).as("the seat is stood on").isTrue();

		int[] posts = VillageFigurePlacement.depthPosts(level);
		Assertions.assertThat(posts[posts.length - 1])
				.as("the deepest echo takes the chair")
				.isEqualTo(seat);
	}

	@Test
	@DisplayName("A figure is not placed where another actor already stands")
	void occupiedAnchorsAreSkipped() {
		VillageLevel level = village();
		Actor.clear();

		int[] posts = VillageFigurePlacement.depthPosts(level);
		Assertions.assertThat(VillageFigurePlacement.isFree(level, posts[0])).isTrue();
	}

}
