package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoStorage;
import com.shatteredpixel.shatteredpixeldungeon.levels.EchoReplacementDecider;

public final class CompositeEchoLookup implements EchoReplacementDecider.EchoLookup {

	/** 2 attempts total (initial + 1 automatic retry). */
	static final int RANKED_ATTEMPTS = 2;

	/** Testable backoff between ranked auto-retries; production default 300ms. */
	static long rankedRetryDelayMs = 300L;

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
		rankedRetryDelayMs = 300L;
		EchoOnlineSync.setDefaultForTests(null);
	}

	@Override
	public EchoLookupOutcome findEchoForDepth(int depth) {
		if (Dungeon.echoPlayMode == EchoPlayMode.RANKED) {
			return fetchRankedEcho(depth);
		}
		return fetchLocalEcho(depth);
	}

	private EchoLookupOutcome fetchLocalEcho(int depth) {
		try {
			EchoLookupOutcome local = localLookup.findEchoForDepth(depth);
			if (!local.isFound()) {
				return local;
			}
			if (Dungeon.echoPlayMode != EchoPlayMode.SOLO) {
				return local;
			}
			if (!EchoOnlineSettings.isConfigured()) {
				return EchoLookupOutcome.error(EchoLookupFailureKind.UNAVAILABLE);
			}
			Echo echo = local.result.echo;
			EchoLookupOutcome last = EchoLookupOutcome.error(EchoLookupFailureKind.NETWORK);
			for (int attempt = 0; attempt < RANKED_ATTEMPTS; attempt++) {
				if (attempt > 0) {
					sleepRetryDelay();
				}
				EchoPolicy remotePolicy = client.fetchEchoPolicy(echo);
				if (remotePolicy == null) {
					last = EchoLookupOutcome.error(EchoLookupFailureKind.NETWORK);
					continue;
				}
				if (!remotePolicy.isSupported()) {
					last = EchoLookupOutcome.error(EchoLookupFailureKind.DECODE);
					continue;
				}
				if (localLookup instanceof EchoStorage) {
					((EchoStorage) localLookup).save(echo, remotePolicy);
				}
				return EchoLookupOutcome.found(new EchoFetchResult(echo, remotePolicy));
			}
			return last;
		} catch (Exception unexpected) {
			return EchoLookupOutcome.error(EchoLookupFailureKind.UNKNOWN);
		}
	}

	private EchoLookupOutcome fetchRankedEcho(int depth) {
		if (!EchoOnlineSettings.canSyncOnline()) {
			return EchoLookupOutcome.error(EchoLookupFailureKind.UNAVAILABLE);
		}
		EchoLookupOutcome last = EchoLookupOutcome.error(EchoLookupFailureKind.UNKNOWN);
		for (int attempt = 0; attempt < RANKED_ATTEMPTS; attempt++) {
			if (attempt > 0) {
				sleepRetryDelay();
			}
			last = client.fetchEcho(depth);
			if (!last.isError()) {
				return last;
			}
		}
		return last;
	}

	private static void sleepRetryDelay() {
		if (rankedRetryDelayMs <= 0) {
			return;
		}
		try {
			Thread.sleep(rankedRetryDelayMs);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
	}
}
