package com.shatteredpixel.shatteredpixeldungeon.village;

import java.util.ArrayList;

/**
 * One standing body in the village: a player's echo that earned a spot, or an
 * empty depth post waiting for one.
 *
 * <p>Appearance and facts only. The hero behind it — stats, equipped kit,
 * talents — lives in the echo's data bundle, which is fetched for a single
 * figure when somebody inspects it rather than broadcast for all of them. Ten
 * gzipped hero bundles on every greeting would be the heaviest thing town ever
 * pulls, for data most visits never open.
 */
public class VillageFigure {

	/** Which of the two rows on the site this body stands for. */
	public enum Post {
		/** The current boss holding a depth. Has a fixed spot on the waterfront. */
		DEPTH,
		/** An honorable mention. Stands in the cluster by the well. */
		MENTION
	}

	/** A reason this body is standing here. One figure can have several. */
	public static class Badge {
		/** Matches the site's own kind strings, so the two surfaces cannot drift. */
		public final String kind;
		/** The number the kind carries (echoes, kills), or {@link #NO_COUNT}. */
		public final int count;

		public static final int NO_COUNT = -1;

		public Badge(String kind, int count) {
			this.kind = kind;
			this.count = count;
		}

		public boolean hasCount() {
			return count != NO_COUNT;
		}
	}

	/** Armour tier is unknown — the renderer falls back to tier-1 cloth. */
	public static final int UNKNOWN_TIER = 0;

	public Post post = Post.MENTION;
	/** The depth this body holds, for a {@link Post#DEPTH} body. */
	public int depth;
	/** Identity, and the key the inspect bundle is requested by. Null when the post is empty. */
	public String echoId;
	public String userName;
	/** Hero class name, as the game's own enum spells it. */
	public String heroClass;
	public int armorTier = UNKNOWN_TIER;
	public int lvl;
	public int hp;
	public int ht;
	public int killCount;
	public long timestamp;

	public final ArrayList<Badge> badges = new ArrayList<>();

	/** True when this is a depth post with nobody holding it yet. */
	public boolean isEmptyPost() {
		return echoId == null;
	}
}
