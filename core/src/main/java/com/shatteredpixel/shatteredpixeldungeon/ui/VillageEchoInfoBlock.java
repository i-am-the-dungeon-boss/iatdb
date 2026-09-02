package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.village.VillageEcho;
import com.shatteredpixel.shatteredpixeldungeon.village.VillageTitle;

import java.util.ArrayList;
import java.util.List;

/**
 * A village figure's inspect text, with every honour boasted in its own colour.
 *
 * <p>One format for all of them: the boast, coloured, then the sentence behind
 * it in the window's own colour. Only the boast is painted — a whole paragraph
 * in crimson is a wall rather than a label, and the eye needs the plain text to
 * fall back to.
 *
 * <p>{@link RenderedTextBlock} paints one highlight colour per block, so each
 * honour is a block of its own carrying that honour's colour, and the boast is
 * wrapped in the highlight markers the block already understands. Stacking them
 * rather than flowing one long string is what buys the per-honour colour.
 *
 * <p>Why bother: the square is read by colour before it is read by words, a
 * crimson Halls Boss against a gold Boss Slayer. Somebody who walks up to a
 * figure and opens it should be reading the same leaderboard.
 */
public class VillageEchoInfoBlock extends RenderedTextBlock {

	/** The window's own text colour: what the opening sentence is left at. */
	public static final int PLAIN = -1;

	/** Blank line between paragraphs, matching the gap a "\n\n" would leave. */
	private static final float PARAGRAPH_GAP = 6f;

	/** One line of the window: its text, and the colour its boast is painted. */
	public static class Paragraph {
		/** Highlight markers included, so the boast is the part that is coloured. */
		public final String text;
		/** The boast's colour, or {@link #PLAIN} for a line that has no boast. */
		public final int highlight;

		Paragraph(String text, int highlight) {
			this.text = text;
			this.highlight = highlight;
		}
	}

	private final ArrayList<RenderedTextBlock> paragraphs = new ArrayList<>();
	private int wrapWidth;

	/**
	 * What the window says about a figure, line by line.
	 *
	 * <p>Static and free of any scene so the format — one honour per line, boast
	 * first, only the boast coloured — can be checked without a window to draw it
	 * in.
	 */
	public static List<Paragraph> paragraphsOf(VillageEcho echo) {
		ArrayList<Paragraph> lines = new ArrayList<>();
		lines.add(new Paragraph(echo.preamble(), PLAIN));
		List<VillageTitle> titles = echo.titles();
		for (int i = 0; i < titles.size(); i++) {
			VillageTitle title = titles.get(i);
			if (title.description != null) {
				lines.add(new Paragraph("_" + title.text + "_ " + title.description, title.color));
			}
		}
		return lines;
	}

	public VillageEchoInfoBlock(VillageEcho echo, int size, int wrapWidth) {
		super(size);
		this.wrapWidth = wrapWidth;

		List<Paragraph> lines = paragraphsOf(echo);
		for (int i = 0; i < lines.size(); i++) {
			addParagraph(lines.get(i), size);
		}
		layout();
	}

	private void addParagraph(Paragraph paragraph, int size) {
		RenderedTextBlock block = PixelScene.renderTextBlock(size);
		// Set before the text: highlighting is applied while the words are being
		// built, so a colour chosen afterwards would need a rebuild.
		if (paragraph.highlight == PLAIN) {
			block.setHightlighting(false);
		} else {
			block.setHightlighting(true, paragraph.highlight);
		}
		block.text(paragraph.text, wrapWidth);
		paragraphs.add(block);
		add(block);
	}

	/** Re-wraps every paragraph: the windows widen themselves to fit landscape. */
	@Override
	public void maxWidth(int maxWidth) {
		if (wrapWidth == maxWidth) {
			return;
		}
		wrapWidth = maxWidth;
		for (int i = 0; i < paragraphs.size(); i++) {
			paragraphs.get(i).maxWidth(maxWidth);
		}
		layout();
	}

	@Override
	public int maxWidth() {
		return wrapWidth;
	}

	/** Stacked, not flowed: paragraphs are separate blocks with a blank line between. */
	@Override
	protected synchronized void layout() {
		float pos = y;
		width = 0;
		nLines = 0;
		for (int i = 0; i < paragraphs.size(); i++) {
			RenderedTextBlock block = paragraphs.get(i);
			block.setPos(x, pos);
			pos = block.bottom() + PARAGRAPH_GAP;
			width = Math.max(width, block.width());
			nLines += block.nLines;
		}
		height = paragraphs.isEmpty() ? 0 : pos - PARAGRAPH_GAP - y;
	}
}
