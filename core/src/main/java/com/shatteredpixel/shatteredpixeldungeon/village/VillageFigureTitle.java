package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

import java.util.ArrayList;
import java.util.List;

/**
 * What a standing figure is known for: the one-line boasts that float over it,
 * and the sentences behind them.
 *
 * <p>Short and shoutable — "Sewers Boss!", "Hero Slayer!" — because they are
 * read at a glance across the town square, unlike the descriptions in the
 * inspect window, which are read one at a time by somebody who stopped to look.
 * Both come from here, so a title and its explanation cannot drift apart.
 *
 * <p>Split from {@link VillageEcho} so the rule is a pure question about a
 * figure: the body it hangs over needs a sprite and a scene, the choice of words
 * does not.
 */
public final class VillageFigureTitle {

	/**
	 * The colour each title is shouted in.
	 *
	 * <p>Chosen for mutual distinction inside the group that is seen together:
	 * the depth posts run along the waterfront in depth order, the mentions
	 * cluster by the well, so what matters is that neighbours never share a hue.
	 */
	private static final int SEWERS = 0x88CC44;
	private static final int PRISON = 0xCC9955;
	private static final int CAVES = 0xFF8822;
	private static final int CITY = 0xAACCFF;
	private static final int HALLS = 0xFF3366;
	/**
	 * A title this build knows the words for but has no colour for: a depth with
	 * no region name, or a badge a newer server added wording for. Plain white,
	 * because an unrecognised honour should not borrow another one's meaning.
	 */
	private static final int UNCOLOURED = 0xFFFFFF;

	private static final int HIGHEST_KILLS = 0x44CCFF;
	private static final int MOST_BOSSES = 0xFFCC22;
	private static final int HERO_SLAYER = 0xFF4444;
	private static final int DUNGEON_OVERLORD = 0xBB66FF;
	private static final int ROOKIE_REAPER = 0x66FF99;

	private VillageFigureTitle() {
	}

	/**
	 * The one title a figure shouts, or {@code null} when there is nothing to
	 * say: an unheld depth post, or a figure whose every badge this build has no
	 * wording for.
	 *
	 * <p>One, not all: the shout is scenery, and a body cycling through four
	 * boasts over its own head is noise rather than a leaderboard. The rest are
	 * read in the inspect window, where {@link #heldBy} lists them.
	 */
	public static VillageTitle of(VillageFigure figure) {
		List<VillageTitle> titles = allOf(figure);
		return titles.isEmpty() ? null : titles.get(0);
	}

	/**
	 * Every title this one body has earned, best first: the post it holds, then
	 * its badges in the order the server sent them.
	 *
	 * <p>The post outranks the badges: holding a depth is why this body stands
	 * where it stands, and a depth holder with mentions is still the boss.
	 *
	 * <p>A title this build has no wording for is left out rather than half
	 * shown. A newer server may name badges this one has never heard of, and an
	 * untitled body reads better than the missing-key marker floating over the
	 * square.
	 */
	public static List<VillageTitle> allOf(VillageFigure figure) {
		ArrayList<VillageTitle> titles = new ArrayList<>();
		if (figure == null || figure.isEmptyPost()) {
			return titles;
		}
		if (figure.post == VillageFigure.Post.DEPTH) {
			VillageTitle depth = depthTitle(figure.depth);
			if (depth != null) {
				titles.add(depth);
			}
		}
		for (int i = 0; i < figure.badges.size(); i++) {
			VillageTitle badge = badgeTitle(figure.badges.get(i));
			if (badge != null) {
				titles.add(badge);
			}
		}
		return titles;
	}

	/**
	 * Every title the player behind a body has earned, anywhere in town, with
	 * this body's own first.
	 *
	 * <p>A player can stand in the square more than once — one echo holding a
	 * depth, another named as a mention — and the honours are the player's, not
	 * the individual echo's. Somebody who walks up to either body should learn
	 * the same thing about who they are looking at.
	 *
	 * <p>Matched on the displayed name, which is all a body carries: the village
	 * broadcast has no account id, and two bodies under one name are one player
	 * as far as anybody reading the square is concerned. A body with no name
	 * answers for itself alone rather than pooling with every other nameless one.
	 */
	public static List<VillageTitle> heldBy(VillageFigure figure, List<VillageFigure> village) {
		List<VillageTitle> titles = allOf(figure);
		if (figure == null || village == null
				|| figure.userName == null || figure.userName.isEmpty()) {
			return titles;
		}
		for (int i = 0; i < village.size(); i++) {
			VillageFigure other = village.get(i);
			if (other == figure || other == null
					|| !figure.userName.equals(other.userName)) {
				continue;
			}
			List<VillageTitle> theirs = allOf(other);
			for (int j = 0; j < theirs.size(); j++) {
				if (!says(titles, theirs.get(j).text)) {
					titles.add(theirs.get(j));
				}
			}
		}
		return titles;
	}

	/** Whether these titles already make that boast. */
	private static boolean says(List<VillageTitle> titles, String text) {
		for (int i = 0; i < titles.size(); i++) {
			if (titles.get(i).text.equals(text)) {
				return true;
			}
		}
		return false;
	}

	private static VillageTitle depthTitle(int depth) {
		String region = text("depth_" + depth);
		if (region != null) {
			return new VillageTitle(region, depthColor(depth), text("depth_desc_" + depth));
		}
		return new VillageTitle(
				Messages.get(VillageFigureTitle.class, "depth", depth),
				UNCOLOURED,
				Messages.get(VillageFigureTitle.class, "depth_desc", depth));
	}

	private static int depthColor(int depth) {
		switch (depth) {
			case 5:
				return SEWERS;
			case 10:
				return PRISON;
			case 15:
				return CAVES;
			case 20:
				return CITY;
			case 25:
				return HALLS;
			default:
				return UNCOLOURED;
		}
	}

	private static VillageTitle badgeTitle(VillageFigure.Badge badge) {
		String kind = badge.kind.replace('-', '_');
		String shout = text("title_" + kind);
		if (shout == null) {
			return null;
		}
		String key = "desc_" + kind;
		String description = badge.hasCount() ? text(key, badge.count) : text(key);
		return new VillageTitle(shout, badgeColor(badge.kind), description);
	}

	private static int badgeColor(String kind) {
		if ("highest-kills".equals(kind)) {
			return HIGHEST_KILLS;
		}
		if ("most-bosses".equals(kind)) {
			return MOST_BOSSES;
		}
		if ("hero-slayer".equals(kind)) {
			return HERO_SLAYER;
		}
		if ("dungeon-overlord".equals(kind)) {
			return DUNGEON_OVERLORD;
		}
		if ("rookie-reaper".equals(kind)) {
			return ROOKIE_REAPER;
		}
		return UNCOLOURED;
	}

	/** A message, or null when this build has no wording under that key. */
	private static String text(String key, Object... args) {
		String value = Messages.get(VillageFigureTitle.class, key, args);
		if (value == null || value.startsWith("!!!")) {
			return null;
		}
		return value;
	}
}
