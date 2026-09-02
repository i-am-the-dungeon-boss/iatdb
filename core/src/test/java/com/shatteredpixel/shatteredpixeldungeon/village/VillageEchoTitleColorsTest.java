package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.VillageEchoInfoBlock;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

/**
 * The inspect window is the same leaderboard as the square: an honour is read by
 * its colour before it is read by its words. Only the boast is coloured, though
 * — a whole paragraph in crimson is a wall, not a label.
 */
@ExtendWith(GdxTestExtension.class)
@DisplayName("A village figure's honours in the inspect window")
class VillageEchoTitleColorsTest {

	private static VillageEcho bossWithMentions() {
		VillageFigure figure = new VillageFigure();
		figure.post = VillageFigure.Post.DEPTH;
		figure.depth = 10;
		figure.echoId = "10-1";
		figure.userName = "Somebody";
		figure.badges.add(new VillageFigure.Badge("hero-slayer", 47));
		figure.badges.add(new VillageFigure.Badge("most-bosses", 171));
		return new VillageEcho(figure);
	}

	@Test
	@DisplayName("The opening sentence is the figure's own, not one of its honours")
	void preambleIsNotAnHonour() {
		VillageEcho echo = bossWithMentions();

		Assertions.assertThat(echo.preamble())
				.isEqualTo(Messages.get(VillageEcho.class, "desc"));
		Assertions.assertThat(echo.description()).startsWith(echo.preamble());
	}

	@Test
	@DisplayName("Every honour reads as one format: the boast, then what earned it")
	void honoursShareOneFormat() {
		VillageEcho echo = bossWithMentions();
		List<VillageTitle> titles = echo.titles();

		List<VillageEchoInfoBlock.Paragraph> paragraphs = VillageEchoInfoBlock.paragraphsOf(echo);

		Assertions.assertThat(paragraphs).hasSize(titles.size() + 1);
		Assertions.assertThat(paragraphs.get(0).text).isEqualTo(echo.preamble());
		Assertions.assertThat(paragraphs.get(0).highlight).isEqualTo(VillageEchoInfoBlock.PLAIN);
		for (int i = 0; i < titles.size(); i++) {
			VillageTitle title = titles.get(i);
			VillageEchoInfoBlock.Paragraph paragraph = paragraphs.get(i + 1);
			Assertions.assertThat(paragraph.text)
					.isEqualTo("_" + title.text + "_ " + title.description);
			Assertions.assertThat(paragraph.highlight).isEqualTo(title.color);
		}
	}

	@Test
	@DisplayName("Only the boast is coloured: the description is left at the window's own colour")
	void onlyTheBoastIsColoured() {
		VillageEcho echo = bossWithMentions();

		List<VillageEchoInfoBlock.Paragraph> paragraphs = VillageEchoInfoBlock.paragraphsOf(echo);

		for (int i = 1; i < paragraphs.size(); i++) {
			String text = paragraphs.get(i).text;
			// Exactly two markers, opening the line and closing the boast. A third
			// would reopen highlighting somewhere inside the description.
			Assertions.assertThat(countUnderscores(text)).isEqualTo(2);
			Assertions.assertThat(text).startsWith("_");
			Assertions.assertThat(text.substring(text.lastIndexOf('_'))).startsWith("_ ");
		}
	}

	@Test
	@DisplayName("A description never repeats the boast that already opens the line")
	void descriptionsDoNotRepeatTheirTitle() {
		VillageEcho echo = bossWithMentions();
		List<VillageTitle> titles = echo.titles();

		for (int i = 0; i < titles.size(); i++) {
			VillageTitle title = titles.get(i);
			String boast = title.text.substring(0, title.text.length() - 1);
			Assertions.assertThat(title.description).doesNotContain(boast);
		}
	}

	@Test
	@DisplayName("The flat description reads the same way, boast first")
	void flatDescriptionKeepsTheFormat() {
		VillageEcho echo = bossWithMentions();
		List<VillageTitle> titles = echo.titles();

		String description = echo.description();

		for (int i = 0; i < titles.size(); i++) {
			Assertions.assertThat(description)
					.contains(titles.get(i).text + " " + titles.get(i).description);
		}
	}

	@Test
	@DisplayName("An honour this build cannot explain contributes no paragraph")
	void undescribedHonoursAreNotDrawn() {
		VillageFigure figure = new VillageFigure();
		figure.post = VillageFigure.Post.MENTION;
		figure.echoId = "m-1";
		figure.badges.add(new VillageFigure.Badge("invented-later", 1));
		VillageEcho echo = new VillageEcho(figure);

		Assertions.assertThat(VillageEchoInfoBlock.paragraphsOf(echo)).hasSize(1);
	}

	private static int countUnderscores(String text) {
		int count = 0;
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) == '_') {
				count++;
			}
		}
		return count;
	}
}
