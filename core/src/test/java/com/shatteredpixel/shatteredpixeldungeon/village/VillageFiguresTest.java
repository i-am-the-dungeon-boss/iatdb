package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.levels.VillageLevel;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;

@ExtendWith(GdxTestExtension.class)
class VillageFiguresTest {

	@BeforeEach
	void inTown() {
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
		Dungeon.depth = VillageLevel.VILLAGE_DEPTH;
		Dungeon.branch = 0;
		// Mob.destroy does hero bookkeeping, and town always has one standing in it.
		Dungeon.hero = new Hero();
		VillageFigures.clear();
	}

	@AfterEach
	void leaveTown() {
		VillageFigures.clear();
		Dungeon.level = null;
		Dungeon.hero = null;
		Actor.clear();
	}

	private VillageLevel village() {
		VillageLevel level = new VillageLevel();
		level.create();
		Dungeon.level = level;
		return level;
	}

	private static VillageFigure depthPost(int depth, String echoId) {
		VillageFigure figure = new VillageFigure();
		figure.post = VillageFigure.Post.DEPTH;
		figure.depth = depth;
		figure.echoId = echoId;
		figure.userName = "Player" + echoId;
		figure.heroClass = HeroClass.WARRIOR.name();
		figure.armorTier = 2;
		figure.lvl = 10;
		figure.hp = 20;
		figure.ht = 25;
		return figure;
	}

	private static VillageFigure mention(String echoId, String kind) {
		VillageFigure figure = new VillageFigure();
		figure.post = VillageFigure.Post.MENTION;
		figure.depth = 12;
		figure.echoId = echoId;
		figure.userName = "Player" + echoId;
		figure.heroClass = HeroClass.MAGE.name();
		figure.armorTier = 4;
		figure.lvl = 18;
		figure.hp = 30;
		figure.ht = 30;
		figure.badges.add(new VillageFigure.Badge(kind, VillageFigure.Badge.NO_COUNT));
		return figure;
	}

	private static ArrayList<VillageFigure> list(VillageFigure... figures) {
		ArrayList<VillageFigure> out = new ArrayList<>();
		for (int i = 0; i < figures.length; i++) {
			out.add(figures[i]);
		}
		return out;
	}

	private static ArrayList<VillageEcho> bodies(VillageFigure... figures) {
		ArrayList<VillageEcho> out = new ArrayList<>();
		for (int i = 0; i < figures.length; i++) {
			out.add(new VillageEcho(figures[i]));
		}
		return out;
	}

	@Test
	@DisplayName("The first push adds everything, because nothing is standing yet")
	void firstPushAddsEverything() {
		VillageFigures.Diff diff = VillageFigures.diff(
				new ArrayList<>(),
				list(depthPost(5, "a"), mention("b", "hero-slayer")));

		Assertions.assertThat(diff.added).hasSize(2);
		Assertions.assertThat(diff.kept).isEmpty();
		Assertions.assertThat(diff.removed).isEmpty();
	}

	@Test
	@DisplayName("An unchanged figure is kept, so it does not blink on every refresh")
	void unchangedFiguresAreKept() {
		ArrayList<VillageEcho> standing = bodies(depthPost(5, "a"), mention("b", "hero-slayer"));

		VillageFigures.Diff diff = VillageFigures.diff(
				standing,
				list(depthPost(5, "a"), mention("b", "hero-slayer")));

		Assertions.assertThat(diff.kept).hasSize(2);
		Assertions.assertThat(diff.added).isEmpty();
		Assertions.assertThat(diff.removed).isEmpty();
	}

	@Test
	@DisplayName("A depth changing hands replaces that body and leaves the others alone")
	void depthChangingHandsReplacesOnlyThatBody() {
		ArrayList<VillageEcho> standing = bodies(depthPost(5, "a"), depthPost(10, "b"));

		VillageFigures.Diff diff = VillageFigures.diff(
				standing,
				list(depthPost(5, "c"), depthPost(10, "b")));

		Assertions.assertThat(diff.removed).hasSize(1);
		Assertions.assertThat(diff.removed.get(0).figure().echoId).isEqualTo("a");
		Assertions.assertThat(diff.added).hasSize(1);
		Assertions.assertThat(diff.added.get(0).echoId).isEqualTo("c");
		Assertions.assertThat(diff.kept).hasSize(1);
		Assertions.assertThat(diff.kept.get(0).figure().echoId).isEqualTo("b");
	}

	@Test
	@DisplayName("The same echo gaining a kill is replaced, because the figure now says something new")
	void changedFactsReplaceTheBody() {
		ArrayList<VillageEcho> standing = bodies(depthPost(5, "a"));
		VillageFigure updated = depthPost(5, "a");
		updated.killCount = 3;

		VillageFigures.Diff diff = VillageFigures.diff(standing, list(updated));

		Assertions.assertThat(diff.kept).isEmpty();
		Assertions.assertThat(diff.removed).hasSize(1);
		Assertions.assertThat(diff.added).hasSize(1);
	}

	@Test
	@DisplayName("A mention that drops off the board is removed")
	void droppedMentionIsRemoved() {
		ArrayList<VillageEcho> standing = bodies(mention("a", "hero-slayer"), mention("b", "most-bosses"));

		VillageFigures.Diff diff = VillageFigures.diff(standing, list(mention("a", "hero-slayer")));

		Assertions.assertThat(diff.removed).hasSize(1);
		Assertions.assertThat(diff.removed.get(0).figure().echoId).isEqualTo("b");
		Assertions.assertThat(diff.kept).hasSize(1);
	}

	@Test
	@DisplayName("A mention keeping its echo but gaining a badge is replaced, so the new reason shows")
	void newBadgeReplacesTheBody() {
		ArrayList<VillageEcho> standing = bodies(mention("a", "hero-slayer"));
		VillageFigure both = mention("a", "hero-slayer");
		both.badges.add(new VillageFigure.Badge("first-depth-5", VillageFigure.Badge.NO_COUNT));

		VillageFigures.Diff diff = VillageFigures.diff(standing, list(both));

		Assertions.assertThat(diff.kept).isEmpty();
		Assertions.assertThat(diff.added).hasSize(1);
	}

	@Test
	@DisplayName("An empty depth post is its own body, and stays put until somebody takes the depth")
	void emptyDepthPostIsStable() {
		VillageFigure empty = new VillageFigure();
		empty.post = VillageFigure.Post.DEPTH;
		empty.depth = 20;

		VillageFigure stillEmpty = new VillageFigure();
		stillEmpty.post = VillageFigure.Post.DEPTH;
		stillEmpty.depth = 20;

		VillageFigures.Diff diff = VillageFigures.diff(bodies(empty), list(stillEmpty));

		Assertions.assertThat(diff.kept).hasSize(1);
		Assertions.assertThat(diff.added).isEmpty();
	}

	@Test
	@DisplayName("Somebody taking an empty depth replaces the empty post")
	void takingAnEmptyDepthReplacesThePost() {
		VillageFigure empty = new VillageFigure();
		empty.post = VillageFigure.Post.DEPTH;
		empty.depth = 20;

		VillageFigures.Diff diff = VillageFigures.diff(bodies(empty), list(depthPost(20, "z")));

		Assertions.assertThat(diff.removed).hasSize(1);
		Assertions.assertThat(diff.added).hasSize(1);
		Assertions.assertThat(diff.added.get(0).echoId).isEqualTo("z");
	}

	@Test
	@DisplayName("An empty push clears the village rather than leaving ghosts behind")
	void emptyPushRemovesEverything() {
		ArrayList<VillageEcho> standing = bodies(depthPost(5, "a"), mention("b", "hero-slayer"));

		VillageFigures.Diff diff = VillageFigures.diff(standing, new ArrayList<>());

		Assertions.assertThat(diff.removed).hasSize(2);
		Assertions.assertThat(diff.kept).isEmpty();
	}
	@Test
	@DisplayName("stands a pushed body in the village and puts it on the actor clock")
	void applyStandsABody() {
		VillageLevel level = village();

		VillageFigures.apply(level, list(depthPost(5, "5-1")));

		Assertions.assertThat(VillageFigures.standing()).hasSize(1);
		VillageEcho body = VillageFigures.standing().get(0);
		Assertions.assertThat(level.mobs).contains(body);
		Assertions.assertThat(Actor.all()).contains(body);
	}

	@Test
	@DisplayName("takes a body off the map and the clock when the push drops it")
	void applyRemovesADroppedBody() {
		VillageLevel level = village();
		VillageFigures.apply(level, list(depthPost(5, "5-1")));
		VillageEcho gone = VillageFigures.standing().get(0);

		VillageFigures.apply(level, list());

		Assertions.assertThat(VillageFigures.standing()).isEmpty();
		Assertions.assertThat(level.mobs).doesNotContain(gone);
		Assertions.assertThat(Actor.all()).doesNotContain(gone);
	}

	@Test
	@DisplayName("leaves an unchanged body standing rather than rebuilding it")
	void applyKeepsAnUnchangedBody() {
		VillageLevel level = village();
		VillageFigures.apply(level, list(depthPost(5, "5-1")));
		VillageEcho first = VillageFigures.standing().get(0);

		VillageFigures.apply(level, list(depthPost(5, "5-1")));

		Assertions.assertThat(VillageFigures.standing()).containsExactly(first);
	}

	@Test
	@DisplayName("ignores a push that arrives with no village built")
	void applyIgnoresAMissingLevel() {
		VillageFigures.apply(null, list(depthPost(5, "5-1")));

		Assertions.assertThat(VillageFigures.standing()).isEmpty();
	}
}
