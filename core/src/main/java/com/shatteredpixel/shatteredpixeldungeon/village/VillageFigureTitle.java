package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

/**
 * The one-line boast that floats over a standing figure.
 *
 * <p>Short and shoutable — "Sewers Boss!", "Hero Slayer!" — because it is read
 * at a glance across the town square, unlike the badge sentences in the inspect
 * window, which are read one at a time by somebody who stopped to look.
 *
 * <p>Split from {@link VillageEcho} so the rule is a pure question about a
 * figure: the body it hangs over needs a sprite and a scene, the choice of words
 * does not.
 */
public final class VillageFigureTitle {

	private VillageFigureTitle() {
	}

	/**
	 * The title for a figure, or {@code null} when there is nothing to say: an
	 * unheld depth post, or a mention whose badge this build has no wording for.
	 * A newer server may name badges this one has never heard of, and an untitled
	 * body reads better than the missing-key marker floating over the square.
	 */
	public static String of(VillageFigure figure) {
		if (figure == null || figure.isEmptyPost()) {
			return null;
		}
		// The post outranks the badges: holding a depth is why this body stands
		// where it stands, and a depth holder with mentions is still the boss.
		if (figure.post == VillageFigure.Post.DEPTH) {
			String region = text("depth_" + figure.depth);
			return region != null ? region : Messages.get(VillageFigureTitle.class, "depth", figure.depth);
		}
		for (int i = 0; i < figure.badges.size(); i++) {
			String title = text("title_" + figure.badges.get(i).kind.replace('-', '_'));
			if (title != null) {
				return title;
			}
		}
		return null;
	}

	/** A message, or null when this build has no wording under that key. */
	private static String text(String key) {
		String value = Messages.get(VillageFigureTitle.class, key);
		if (value == null || value.startsWith("!!!")) {
			return null;
		}
		return value;
	}
}
