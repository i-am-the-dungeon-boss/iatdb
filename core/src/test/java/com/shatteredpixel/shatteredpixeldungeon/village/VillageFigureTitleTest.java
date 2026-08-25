package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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

	@Test
	@DisplayName("A depth holder is titled by the region they hold, not by the number")
	void namesTheRegion() {
		Assertions.assertThat(VillageFigureTitle.of(depthPost(5, "5-1"))).isEqualTo("Sewers Boss!");
		Assertions.assertThat(VillageFigureTitle.of(depthPost(25, "25-1"))).isEqualTo("Halls Boss!");
	}

	@Test
	@DisplayName("A depth this build has no region name for still gets a title")
	void fallsBackToTheDepth() {
		Assertions.assertThat(VillageFigureTitle.of(depthPost(30, "30-1"))).isEqualTo("Depth 30 Boss!");
	}

	@Test
	@DisplayName("An empty post has nobody to title")
	void emptyPostHasNoTitle() {
		Assertions.assertThat(VillageFigureTitle.of(depthPost(5, null))).isNull();
	}

	@Test
	@DisplayName("A mention is titled by the badge that earned it")
	void namesTheBadge() {
		Assertions.assertThat(VillageFigureTitle.of(mention("hero-slayer", 12)))
				.isEqualTo("Hero Slayer!");
		Assertions.assertThat(VillageFigureTitle.of(mention("most-bosses", 4)))
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

		Assertions.assertThat(VillageFigureTitle.of(figure)).isEqualTo("Prison Boss!");
	}

	@Test
	@DisplayName("Null is not a figure")
	void nullIsNotAFigure() {
		Assertions.assertThat(VillageFigureTitle.of(null)).isNull();
	}
}
