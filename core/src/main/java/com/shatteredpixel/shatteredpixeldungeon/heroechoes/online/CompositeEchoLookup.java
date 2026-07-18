package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoStorage;
import com.shatteredpixel.shatteredpixeldungeon.levels.EchoReplacementDecider;

import java.util.Optional;

public final class CompositeEchoLookup implements EchoReplacementDecider.EchoLookup {

	private static EchoReplacementDecider.EchoLookup lookup;

	private final EchoClient client;
	private final EchoReplacementDecider.EchoLookup localLookup;

	public CompositeEchoLookup(EchoClient client, EchoReplacementDecider.EchoLookup localLookup) {
		this.client = client;
		this.localLookup = localLookup;
	}

	public static EchoReplacementDecider.EchoLookup echoLookup() {
		if (lookup == null) {
			lookup = new CompositeEchoLookup(
					EchoClient.createDefault(),
					new EchoStorage());
		}
		return lookup;
	}

	/** Test-only override for the process-wide echo lookup. */
	public static void setEchoLookupForTests(EchoReplacementDecider.EchoLookup override) {
		lookup = override;
	}

	public static void resetForTests() {
		lookup = null;
		EchoOnlineSync.setDefaultForTests(null);
	}

	@Override
	public Optional<EchoFetchResult> findEchoForDepth(int depth) {
		Optional<EchoFetchResult> result;
		if (Dungeon.echoPlayMode == EchoPlayMode.RANKED) {
			result = fetchRankedEcho(depth);
		} else {
			result = localLookup.findEchoForDepth(depth);
		}
		return result.filter(fetched -> fetched.policy != null);
	}

	private Optional<EchoFetchResult> fetchRankedEcho(int depth) {
		if (!EchoOnlineSettings.canSyncOnline()) {
			return Optional.empty();
		}
		try {
			return client.fetchEcho(depth);
		} catch (Exception ignored) {
			return Optional.empty();
		}
	}
}
