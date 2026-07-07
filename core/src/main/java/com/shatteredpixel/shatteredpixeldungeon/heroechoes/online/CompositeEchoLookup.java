package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.levels.EchoReplacementDecider;

import java.util.Optional;

public final class CompositeEchoLookup implements EchoReplacementDecider.EchoLookup {

	private final EchoClient client;
	private final EchoReplacementDecider.EchoLookup localLookup;
	private EchoPolicy lastFetchedPolicy;

	public CompositeEchoLookup(EchoClient client, EchoReplacementDecider.EchoLookup localLookup) {
		this.client = client;
		this.localLookup = localLookup;
	}

	@Override
	public Optional<Echo> findEchoForDepth(int depth) {
		lastFetchedPolicy = null;

		if (Dungeon.echoPlayMode == EchoPlayMode.RANKED) {
			return fetchRankedEcho(depth);
		}

		if (shouldTryOnlineFetch()) {
			try {
				Optional<EchoFetchResult> online = client.fetchEcho(depth, client.currentGameVersion());
				if (online.isPresent()) {
					lastFetchedPolicy = online.get().policy;
					return Optional.of(online.get().echo);
				}
			} catch (Exception ignored) {
				// fall through to local lookup
			}
		}

		return localLookup.findEchoForDepth(depth);
	}

	private Optional<Echo> fetchRankedEcho(int depth) {
		if (!EchoOnlineSettings.canSyncOnline()) {
			return Optional.empty();
		}
		try {
			Optional<EchoFetchResult> online = client.fetchEcho(depth, client.currentGameVersion());
			if (online.isPresent()) {
				lastFetchedPolicy = online.get().policy;
				return Optional.of(online.get().echo);
			}
		} catch (Exception ignored) {
		}
		return Optional.empty();
	}

	public Optional<EchoPolicy> getLastFetchedPolicy() {
		return Optional.ofNullable(lastFetchedPolicy);
	}

	private static boolean shouldTryOnlineFetch() {
		if (Dungeon.echoPlayMode == EchoPlayMode.SOLO) {
			return false;
		}
		return EchoOnlineSettings.canSyncOnline();
	}
}
