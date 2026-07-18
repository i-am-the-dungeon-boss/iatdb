package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.watabou.noosa.Game;

public final class EchoBackendProbe {

	/** 2 attempts total (initial + 1 automatic retry). */
	static final int PROBE_ATTEMPTS = 2;

	/** Testable backoff between probe retries; production default 300ms. */
	static long probeRetryDelayMs = 300L;

	private static Boolean testReachableOverride;
	private static volatile Boolean lastReachable;

	private EchoBackendProbe() {
	}

	/** Backend URL configured and last health probe succeeded. */
	public static boolean isOnlineReady() {
		if (!EchoOnlineSettings.isConfigured()) {
			return false;
		}
		if (testReachableOverride != null) {
			return testReachableOverride;
		}
		return Boolean.TRUE.equals(lastReachable);
	}

	/** Message key under TitleScene when {@link #isOnlineReady()} is false. */
	public static String offlineMessageKey() {
		return EchoOnlineSettings.isConfigured() ? "offline_unreachable" : "offline_unconfigured";
	}

	public static void probeAsync(Runnable onComplete) {
		if (!EchoOnlineSettings.isConfigured()) {
			lastReachable = false;
			runOnRenderThread(onComplete);
			return;
		}
		if (testReachableOverride != null) {
			lastReachable = testReachableOverride;
			runOnRenderThread(onComplete);
			return;
		}

		lastReachable = null;
		new Thread(() -> {
			boolean healthy = checkHealthWithRetry(EchoClient.createDefault());
			lastReachable = healthy;
			runOnRenderThread(onComplete);
		}, "echo-backend-probe").start();
	}

	/** Runs health check with one automatic retry after a short delay. */
	static boolean checkHealthWithRetry(EchoClient client) {
		for (int attempt = 0; attempt < PROBE_ATTEMPTS; attempt++) {
			if (attempt > 0) {
				sleepRetryDelay();
			}
			try {
				if (client.checkHealth()) {
					return true;
				}
			} catch (Exception ignored) {
			}
		}
		return false;
	}

	public static void setReachableForTests(Boolean reachable) {
		testReachableOverride = reachable;
		lastReachable = reachable;
	}

	public static void resetForTests() {
		testReachableOverride = null;
		lastReachable = null;
		probeRetryDelayMs = 300L;
	}

	private static void sleepRetryDelay() {
		if (probeRetryDelayMs <= 0) {
			return;
		}
		try {
			Thread.sleep(probeRetryDelayMs);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
	}

	private static void runOnRenderThread(Runnable onComplete) {
		if (onComplete == null) {
			return;
		}
		Game.runOnRenderThread(() -> onComplete.run());
	}
}
