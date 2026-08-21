package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.windows.EchoHeroLoader;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldNet;
import com.watabou.noosa.Game;
import com.watabou.utils.Base64Codec;
import com.watabou.utils.Bundle;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * The inspect tier: the hero behind one standing figure, fetched on demand.
 *
 * <p>The village broadcast carries appearance and facts for every body. The
 * bundle that rebuilds a whole {@link Hero} — stats, equipped kit, talents — is
 * an order of magnitude larger, and most visits to town open none of them, so it
 * is asked for one figure at a time and only when somebody is about to look.
 *
 * <p>Nothing here invents a hero. A bundle that will not decode leaves
 * {@link VillageEcho#getEchoHero()} null, and the inspect window falls back to
 * the plain mob view — which is the truth — rather than showing a figure with
 * empty tabs.
 */
public final class VillageEchoBundles {

	/** Figures already asked about, so a player staring at one does not spam the socket. */
	private static final HashSet<String> outstanding = new HashSet<>();

	private VillageEchoBundles() {
	}

	/**
	 * Asks for this figure's hero, unless it is already here or already asked for.
	 *
	 * <p>Called when the player is about to look at a body rather than when it is
	 * built, which is the whole point of the two tiers.
	 */
	public static void request(VillageEcho body) {
		if (body == null || body.getEchoHero() != null) {
			return;
		}
		String echoId = body.figure().echoId;
		if (echoId == null || echoId.isEmpty() || !outstanding.add(echoId)) {
			return;
		}
		WorldNet.requestEchoBundle(echoId);
	}

	/**
	 * A requested bundle came back. Restores the hero onto whichever body is
	 * standing for that echo, if any — a player can leave town, or the figures can
	 * change, between the question and the answer.
	 */
	public static void deliver(String echoId, String echoData) {
		outstanding.remove(echoId);
		VillageEcho body = standingFor(echoId);
		if (body == null) {
			return;
		}
		Echo echo = decode(echoData);
		if (echo == null) {
			return;
		}
		Hero hero = EchoHeroLoader.load(echo);
		if (hero != null) {
			body.setEchoHero(hero);
		}
	}

	/** Nothing outstanding follows the player out of town. */
	public static void clear() {
		outstanding.clear();
	}

	private static VillageEcho standingFor(String echoId) {
		if (echoId == null || echoId.isEmpty()) {
			return null;
		}
		ArrayList<VillageEcho> standing = VillageFigures.standing();
		for (int i = 0; i < standing.size(); i++) {
			VillageEcho body = standing.get(i);
			if (echoId.equals(body.figure().echoId)) {
				return body;
			}
		}
		return null;
	}

	/**
	 * Null rather than a throw: a bundle this build cannot read is a figure that
	 * stays uninspectable, not a reason to interrupt somebody standing in town.
	 */
	private static Echo decode(String echoData) {
		if (echoData == null || echoData.isEmpty()) {
			return null;
		}
		try {
			byte[] bytes = Base64Codec.decode(echoData);
			Bundle file = Bundle.read(new ByteArrayInputStream(bytes));
			if (file == null || !file.contains(Echo.BUNDLE_KEY)) {
				return null;
			}
			return Echo.fromFileBundle(file);
		} catch (Exception unreadable) {
			Game.reportException(unreadable);
			return null;
		}
	}
}
