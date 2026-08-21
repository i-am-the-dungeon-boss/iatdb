package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.levels.VillageLevel;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.ui.NameTag;
import com.watabou.noosa.Gizmo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * The standing figures in town: what the village has been told to show, and the
 * bodies currently showing it.
 *
 * <p>Sits beside {@code RemotePlayers} rather than reusing it — same push
 * source, same render-thread-only discipline, different content. Like presence,
 * a later push {@link #diff reconciles} against what is already standing instead
 * of tearing the village down and rebuilding it, so a figure nobody displaced
 * does not blink every time somebody else uploads an echo.
 *
 * <p>Nothing here fetches. The figures arrive on the world channel, which town
 * always joins, and a body's echoed hero is requested only when a player
 * inspects it.
 */
public final class VillageFigures {

	private static final ArrayList<VillageEcho> standing = new ArrayList<>();
	/** The labels and criers over each body, so they come down with it. */
	private static final Map<VillageEcho, ArrayList<Gizmo>> labels = new HashMap<>();

	private VillageFigures() {
	}

	/** What a push changes: bodies to take away, figures to put down, bodies to leave. */
	public static class Diff {
		public final ArrayList<VillageEcho> removed = new ArrayList<>();
		public final ArrayList<VillageFigure> added = new ArrayList<>();
		public final ArrayList<VillageEcho> kept = new ArrayList<>();
	}

	/**
	 * Works out the smallest change from what is standing to what was just sent.
	 *
	 * <p>Bodies are matched by the spot they hold, not by identity: a depth post
	 * is the same post whoever is holding it, so depth 5 changing hands is one
	 * body swapped rather than the waterfront re-laid. A body is kept only when
	 * everything it displays is unchanged — a new badge or one more kill is
	 * something the figure now says, so it is replaced rather than left stale.
	 */
	public static Diff diff(ArrayList<VillageEcho> current, ArrayList<VillageFigure> desired) {
		Diff diff = new Diff();
		ArrayList<VillageEcho> unmatched = new ArrayList<>(current);

		for (int i = 0; i < desired.size(); i++) {
			VillageFigure figure = desired.get(i);
			VillageEcho match = null;
			for (int j = 0; j < unmatched.size(); j++) {
				if (spotKey(unmatched.get(j).figure()).equals(spotKey(figure))) {
					match = unmatched.get(j);
					break;
				}
			}

			if (match == null) {
				diff.added.add(figure);
			} else {
				unmatched.remove(match);
				if (sameFigure(match.figure(), figure)) {
					diff.kept.add(match);
				} else {
					diff.removed.add(match);
					diff.added.add(figure);
				}
			}
		}

		diff.removed.addAll(unmatched);
		return diff;
	}

	/**
	 * The spot a figure occupies. A depth post belongs to its depth whoever holds
	 * it — including nobody — while a mention belongs to the echo that earned it,
	 * because the mention cluster has no fixed order to defend.
	 */
	private static String spotKey(VillageFigure figure) {
		if (figure.post == VillageFigure.Post.DEPTH) {
			return "depth:" + figure.depth;
		}
		return "mention:" + figure.echoId;
	}

	private static boolean sameFigure(VillageFigure a, VillageFigure b) {
		if (!equal(a.echoId, b.echoId)
				|| !equal(a.userName, b.userName)
				|| !equal(a.heroClass, b.heroClass)
				|| a.armorTier != b.armorTier
				|| a.lvl != b.lvl
				|| a.hp != b.hp
				|| a.ht != b.ht
				|| a.killCount != b.killCount
				|| a.timestamp != b.timestamp
				|| a.badges.size() != b.badges.size()) {
			return false;
		}
		for (int i = 0; i < a.badges.size(); i++) {
			VillageFigure.Badge left = a.badges.get(i);
			VillageFigure.Badge right = b.badges.get(i);
			if (!equal(left.kind, right.kind) || left.count != right.count) {
				return false;
			}
		}
		return true;
	}

	private static boolean equal(String a, String b) {
		return a == null ? b == null : a.equals(b);
	}

	/** The bodies currently standing in town. */
	public static ArrayList<VillageEcho> standing() {
		return new ArrayList<>(standing);
	}

	/**
	 * Applies a push to a built village.
	 *
	 * <p>Render thread only, and only in town: a push that arrives while the
	 * player is elsewhere is dropped rather than queued, because the figures are
	 * re-sent on the next greeting anyway.
	 */
	public static void apply(VillageLevel level, ArrayList<VillageFigure> figures) {
		if (level == null || figures == null) {
			return;
		}

		Diff diff = diff(standing, figures);

		for (int i = 0; i < diff.removed.size(); i++) {
			VillageEcho body = diff.removed.get(i);
			standing.remove(body);
			takeDownLabels(body);
			level.mobs.remove(body);
			body.destroy();
			if (body.sprite != null) {
				body.sprite.killAndErase();
			}
		}

		int[] depthPosts = VillageFigurePlacement.depthPosts(level);
		int[] mentionPosts = VillageFigurePlacement.mentionPosts(level);
		int nextDepth = 0;
		int nextMention = 0;

		for (int i = 0; i < diff.added.size(); i++) {
			VillageFigure figure = diff.added.get(i);
			int[] posts = figure.post == VillageFigure.Post.DEPTH ? depthPosts : mentionPosts;
			int index = figure.post == VillageFigure.Post.DEPTH ? nextDepth++ : nextMention++;
			if (index >= posts.length) {
				// more figures than the village has spots for; the server decides
				// how many there are, so this is a mismatch worth ignoring rather
				// than crowding bodies onto each other
				continue;
			}

			int cell = posts[index];
			if (!VillageFigurePlacement.isFree(level, cell)) {
				continue;
			}

			VillageEcho body = new VillageEcho(figure);
			body.pos = cell;
			level.mobs.add(body);
			Actor.add(body);
			standing.add(body);
			// The village is already on screen when a push lands — generation's own
			// sprite pass ran long ago — so a body added now has to make its own.
			GameScene.addSprite(body);
			raiseLabels(body);
		}
	}

	/**
	 * Hangs the body's name over its head, and starts it shouting its title.
	 *
	 * <p>Who they are and what they did, readable from across the square, so the
	 * village reads as a leaderboard without anybody having to walk up to every
	 * figure and open a window. The name is a standing tag because it is an
	 * identity; the title is the game's own good-news animation, the same rising
	 * text as "Level up!", because it is an announcement.
	 *
	 * <p>Nothing to hang them on outside a live scene: {@code GameScene} has not
	 * linked a sprite to the body, so there is no head to follow.
	 */
	private static void raiseLabels(VillageEcho body) {
		if (body.sprite == null) {
			return;
		}
		ArrayList<Gizmo> raised = new ArrayList<>();
		String name = body.figure().userName;
		if (name != null && !name.isEmpty()) {
			NameTag nameTag = new NameTag(body.sprite, name);
			GameScene.addToMobLayer(nameTag);
			raised.add(nameTag);
		}
		String title = VillageFigureTitle.of(body.figure());
		if (title != null) {
			VillageFigureTitleCrier crier = new VillageFigureTitleCrier(body.sprite, title);
			GameScene.addToMobLayer(crier);
			raised.add(crier);
		}
		if (!raised.isEmpty()) {
			labels.put(body, raised);
		}
	}

	private static void takeDownLabels(VillageEcho body) {
		ArrayList<Gizmo> raised = labels.remove(body);
		if (raised == null) {
			return;
		}
		for (int i = 0; i < raised.size(); i++) {
			GameScene.removeFromMobLayer(raised.get(i));
			raised.get(i).killAndErase();
		}
	}

	/** Nothing follows the player out of town. */
	public static void clear() {
		standing.clear();
		labels.clear();
	}
}
