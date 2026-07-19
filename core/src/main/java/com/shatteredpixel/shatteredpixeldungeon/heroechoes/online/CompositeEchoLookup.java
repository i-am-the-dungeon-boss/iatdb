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
		EchoLookupOutcome outcome;
		if (Dungeon.echoPlayMode == EchoPlayMode.RANKED) {
			outcome = fetchRankedEcho(depth);
		} else {
			outcome = fetchLocalEcho(depth);
		}
		if (outcome.isFound() && outcome.result.policy == null) {
			return EchoLookupOutcome.notFound();
		}
		return outcome;
	}

	private EchoLookupOutcome fetchLocalEcho(int depth) {
		try {
			EchoLookupOutcome local = localLookup.findEchoForDepth(depth);
			if (!local.isFound()) {
				return local;
			}
			if (Dungeon.echoPlayMode != EchoPlayMode.SOLO || !EchoOnlineSettings.isConfigured()) {
				return local;
			}
			Echo echo = local.result.echo;
			EchoPolicy remotePolicy = client.fetchEchoPolicy(echo.heroClass, echo.lvl);
			if (remotePolicy == null || !remotePolicy.isSupported()) {
				return local;
			}
			if (localLookup instanceof EchoStorage) {
				((EchoStorage) localLookup).save(echo, remotePolicy);
			}
			return EchoLookupOutcome.found(new EchoFetchResult(echo, remotePolicy));
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
