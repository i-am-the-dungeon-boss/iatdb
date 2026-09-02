package com.shatteredpixel.shatteredpixeldungeon.village;

/**
 * One thing a standing figure is known for: the words, the colour they are
 * shouted in, and the sentence that explains them.
 *
 * <p>The three travel together because they are three faces of one fact. Split
 * apart — a title table here, a colour table there, a description table in the
 * inspect window — they drift the moment a new badge is added and somebody
 * updates two of the three.
 *
 * <p>The colour is per title rather than per figure: the square is read at a
 * glance, and a body shouting in gold is a different claim from one shouting in
 * crimson without anybody having to read the words.
 */
public final class VillageTitle {

	/** The shout: short, exclamatory, read from across the square. */
	public final String text;
	/** The colour it is shouted in, as {@code CharSprite} spells its statuses. */
	public final int color;
	/** The sentence in the inspect window that says what earned it. */
	public final String description;

	public VillageTitle(String text, int color, String description) {
		this.text = text;
		this.color = color;
		this.description = description;
	}
}
