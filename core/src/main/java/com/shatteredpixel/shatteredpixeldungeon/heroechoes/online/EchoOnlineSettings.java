package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;

public final class EchoOnlineSettings {

	private static Boolean testOnlineOverride;
	private static String testBackendUrlOverride;
	private static String testApiKeyOverride;

	private EchoOnlineSettings() {}

	public static boolean isOnlineEnabled() {
		if (testOnlineOverride != null) {
			return testOnlineOverride;
		}
		if (Dungeon.echoPlayMode != null && Dungeon.echoPlayMode != EchoPlayMode.NONE) {
			return Dungeon.echoPlayMode == EchoPlayMode.RANKED && isConfigured();
		}
		return true;
	}

	public static void setOnlineEnabled(boolean enabled) {
		testOnlineOverride = enabled;
	}

	public static String backendUrl() {
		if (testBackendUrlOverride != null) {
			return testBackendUrlOverride;
		}
		return EchoOnlineEnv.backendUrl();
	}

	public static void setBackendUrl(String url) {
		testBackendUrlOverride = url;
	}

	public static String apiKey() {
		if (testApiKeyOverride != null) {
			return testApiKeyOverride;
		}
		return EchoOnlineEnv.apiKey();
	}

	public static void setApiKey(String apiKey) {
		testApiKeyOverride = apiKey;
	}

	public static boolean isConfigured() {
		String url = backendUrl();
		return url != null && !url.trim().isEmpty();
	}

	public static boolean canSyncOnline() {
		return isOnlineEnabled() && isConfigured();
	}

	public static void resetForTests() {
		testOnlineOverride = null;
		testBackendUrlOverride = null;
		testApiKeyOverride = null;
		EchoOnlineEnv.resetForTests();
	}
}
