package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
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

	@Test
	@DisplayName("No figure blocks the way in, the way down, or the path between them")
	void anchorsKeepTheRoadClear() {
		VillageLevel level = village();
		int pathX = VillageLevel.SIZE > 0 ? level.arrivalCell() % level.width() : 0;

		for (Integer cell : allAnchors(level)) {
			Assertions.assertThat(cell).isNotEqualTo(level.arrivalCell());
			Assertions.assertThat(cell).isNotEqualTo(level.dungeonEntrance());
			Assertions.assertThat(cell % level.width())
					.as("cell %d is off the path column", cell)
					.isNotEqualTo(pathX);
		}
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
	@DisplayName("A figure is not placed where another actor already stands")
	void occupiedAnchorsAreSkipped() {
		VillageLevel level = village();
		Actor.clear();

		int[] posts = VillageFigurePlacement.depthPosts(level);
		Assertions.assertThat(VillageFigurePlacement.isFree(level, posts[0])).isTrue();
	}
}
