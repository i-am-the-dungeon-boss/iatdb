package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.watabou.noosa.Game;

public final class EchoBackendProbe {

	private static Boolean testReachableOverride;
	private static volatile Boolean lastReachable;

	private EchoBackendProbe() {}

	public static boolean canStartRanked() {
		if (!EchoOnlineSettings.isConfigured()) {
			return false;
		}
		if (testReachableOverride != null) {
			return testReachableOverride;
		}
		return Boolean.TRUE.equals(lastReachable);
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
			boolean healthy = false;
			try {
				healthy = EchoClient.createDefault().checkHealth();
			} catch (Exception ignored) {
			}
			lastReachable = healthy;
			runOnRenderThread(onComplete);
		}, "echo-backend-probe").start();
	}

	public static void setReachableForTests(Boolean reachable) {
		testReachableOverride = reachable;
		lastReachable = reachable;
	}

	public static void resetForTests() {
		testReachableOverride = null;
		lastReachable = null;
	}

	private static void runOnRenderThread(Runnable onComplete) {
		if (onComplete == null) {
			return;
		}
		Game.runOnRenderThread(() -> onComplete.run());
	}
}
