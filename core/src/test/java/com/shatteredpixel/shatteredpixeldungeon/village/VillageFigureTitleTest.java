package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@ExtendWith(GdxTestExtension.class)
class VillageFigureTitleTest {

	private static VillageFigure depthPost(int depth, String echoId) {
		VillageFigure figure = new VillageFigure();
		figure.post = VillageFigure.Post.DEPTH;
		figure.depth = depth;
		figure.echoId = echoId;
		return figure;
	}

	private static VillageFigure mention(String kind, int count) {
		VillageFigure figure = new VillageFigure();
		figure.post = VillageFigure.Post.MENTION;
		figure.echoId = "m-1";
		figure.badges.add(new VillageFigure.Badge(kind, count));
		return figure;
	}

	private static String textOf(VillageTitle title) {
		return title == null ? null : title.text;
	}

	@Test
	@DisplayName("A depth holder is titled by the region they hold, not by the number")
	void namesTheRegion() {
		Assertions.assertThat(textOf(VillageFigureTitle.of(depthPost(5, "5-1")))).isEqualTo("Sewers Boss!");
		Assertions.assertThat(textOf(VillageFigureTitle.of(depthPost(25, "25-1")))).isEqualTo("Halls Boss!");
	}

	@Test
	@DisplayName("A depth this build has no region name for still gets a title")
	void fallsBackToTheDepth() {
		Assertions.assertThat(textOf(VillageFigureTitle.of(depthPost(30, "30-1")))).isEqualTo("Depth 30 Boss!");
	}

	@Test
	@DisplayName("An empty post still shouts the region, as unclaimed")
	void emptyPostShoutsUnclaimed() {
		VillageTitle sewers = VillageFigureTitle.of(depthPost(5, null));
		VillageTitle halls = VillageFigureTitle.of(depthPost(25, null));
		VillageTitle unknown = VillageFigureTitle.of(depthPost(30, null));

		Assertions.assertThat(textOf(sewers)).isEqualTo("Unclaimed Sewers!");
		Assertions.assertThat(textOf(halls)).isEqualTo("Unclaimed Halls!");
		Assertions.assertThat(textOf(unknown)).isEqualTo("Unclaimed Depth 30!");
		Assertions.assertThat(sewers.description)
				.isEqualTo("The Sewers have no reigning echo. The first hero to hold this depth takes the post.");
		Assertions.assertThat(unknown.description).isEqualTo("Nobody has claimed this floor yet.");
		Assertions.assertThat(sewers.color)
				.isEqualTo(VillageFigureTitle.of(depthPost(5, "5-1")).color);
	}

	@Test
	@DisplayName("A mention is titled by the badge that earned it")
	void namesTheBadge() {
		Assertions.assertThat(textOf(VillageFigureTitle.of(mention("hero-slayer", 12))))
				.isEqualTo("Hero Slayer!");
		Assertions.assertThat(textOf(VillageFigureTitle.of(mention("most-bosses", 4))))
				.isEqualTo("Boss Slayer!");
	}

	@Test
	@DisplayName("A badge kind this build has never heard of leaves the body untitled")
	void unknownBadgeHasNoTitle() {
		Assertions.assertThat(VillageFigureTitle.of(mention("invented-later", 1))).isNull();
	}

	@Test
	@DisplayName("The post comes first: a depth holder who is also a mention is titled by the depth")
	void depthWins() {
		VillageFigure figure = depthPost(10, "10-1");
		figure.badges.add(new VillageFigure.Badge("hero-slayer", 3));

		Assertions.assertThat(textOf(VillageFigureTitle.of(figure))).isEqualTo("Prison Boss!");
	}

	@Test
	@DisplayName("Null is not a figure")
	void nullIsNotAFigure() {
		Assertions.assertThat(VillageFigureTitle.of(null)).isNull();
	}

	@Test
	@DisplayName("Every title a figure has earned carries its own explanation")
	void allTitlesAreDescribed() {
		VillageFigure figure = depthPost(10, "10-1");
		figure.badges.add(new VillageFigure.Badge("hero-slayer", 3));
		figure.badges.add(new VillageFigure.Badge("most-bosses", 7));

		List<VillageTitle> titles = VillageFigureTitle.allOf(figure);

		Assertions.assertThat(titles).hasSize(3);
		Assertions.assertThat(titles.get(0).text).isEqualTo("Prison Boss!");
		Assertions.assertThat(titles.get(1).text).isEqualTo("Hero Slayer!");
		Assertions.assertThat(titles.get(2).text).isEqualTo("Boss Slayer!");
		for (int i = 0; i < titles.size(); i++) {
			Assertions.assertThat(titles.get(i).description).isNotBlank();
			Assertions.assertThat(titles.get(i).description).doesNotContain("!!!");
		}
		Assertions.assertThat(titles.get(1).description).contains("3");
		Assertions.assertThat(titles.get(2).description).contains("7");
	}

	@Test
	@DisplayName("A counted honour leads with its number, right after the boast")
	void countsComeFirst() {
		String[] counted = { "most-bosses", "hero-slayer", "dungeon-overlord", "rookie-reaper" };
		for (int i = 0; i < counted.length; i++) {
			VillageTitle title = VillageFigureTitle.of(mention(counted[i], 171));

			// The number is the claim: how many, then what it was. It sits where
			// the eye lands after the coloured boast rather than buried mid-line.
			Assertions.assertThat(title.description).startsWith("171");
		}
	}

	@Test
	@DisplayName("A title this build has no wording for is left out rather than half-shown")
	void unknownTitlesAreLeftOut() {
		VillageFigure figure = mention("hero-slayer", 3);
		figure.badges.add(new VillageFigure.Badge("invented-later", 1));

		List<VillageTitle> titles = VillageFigureTitle.allOf(figure);

		Assertions.assertThat(titles).hasSize(1);
		Assertions.assertThat(titles.get(0).text).isEqualTo("Hero Slayer!");
	}

	@Test
	@DisplayName("Each title is shouted in its own colour, so the square reads as a leaderboard")
	void everyTitleHasItsOwnColour() {
		ArrayList<VillageFigure> figures = new ArrayList<>();
		figures.add(depthPost(5, "5-1"));
		figures.add(depthPost(10, "10-1"));
		figures.add(depthPost(15, "15-1"));
		figures.add(depthPost(20, "20-1"));
		figures.add(depthPost(25, "25-1"));
		figures.add(mention("highest-kills", VillageFigure.Badge.NO_COUNT));
		figures.add(mention("most-bosses", 1));
		figures.add(mention("hero-slayer", 1));
		figures.add(mention("dungeon-overlord", 1));
		figures.add(mention("rookie-reaper", 1));

		HashSet<Integer> colours = new HashSet<>();
		for (int i = 0; i < figures.size(); i++) {
			colours.add(VillageFigureTitle.of(figures.get(i)).color);
		}

		Assertions.assertThat(colours).hasSize(figures.size());
	}

	@Test
	@DisplayName("A player standing twice carries both bodies' titles on either one")
	void titlesFollowThePlayerNotTheBody() {
		VillageFigure boss = depthPost(10, "10-1");
		boss.userName = "Marwan";
		VillageFigure mention = mention("hero-slayer", 3);
		mention.userName = "Marwan";
		VillageFigure stranger = mention("most-bosses", 9);
		stranger.echoId = "m-2";
		stranger.userName = "Somebody Else";

		ArrayList<VillageFigure> village = new ArrayList<>();
		village.add(boss);
		village.add(mention);
		village.add(stranger);

		List<VillageTitle> onTheBoss = VillageFigureTitle.heldBy(boss, village);
		List<VillageTitle> onTheMention = VillageFigureTitle.heldBy(mention, village);

		Assertions.assertThat(titleTexts(onTheBoss)).containsExactly("Prison Boss!", "Hero Slayer!");
		Assertions.assertThat(titleTexts(onTheMention)).containsExactly("Hero Slayer!", "Prison Boss!");
	}

	@Test
	@DisplayName("A nameless body answers for itself alone")
	void anUnnamedBodyIsNotEverybody() {
		VillageFigure boss = depthPost(10, "10-1");
		VillageFigure other = mention("hero-slayer", 3);

		ArrayList<VillageFigure> village = new ArrayList<>();
		village.add(boss);
		village.add(other);

		Assertions.assertThat(titleTexts(VillageFigureTitle.heldBy(boss, village)))
				.containsExactly("Prison Boss!");
	}

	@Test
	@DisplayName("The same title earned by two of a player's bodies is said once")
	void aRepeatedTitleIsNotSaidTwice() {
		VillageFigure first = mention("hero-slayer", 3);
		first.userName = "Marwan";
		VillageFigure second = mention("hero-slayer", 3);
		second.echoId = "m-2";
		second.userName = "Marwan";

		ArrayList<VillageFigure> village = new ArrayList<>();
		village.add(first);
		village.add(second);

		Assertions.assertThat(titleTexts(VillageFigureTitle.heldBy(first, village)))
				.containsExactly("Hero Slayer!");
	}

	private static List<String> titleTexts(List<VillageTitle> titles) {
		ArrayList<String> texts = new ArrayList<>();
		for (int i = 0; i < titles.size(); i++) {
			texts.add(titles.get(i).text);
		}
		return texts;
	}
}
