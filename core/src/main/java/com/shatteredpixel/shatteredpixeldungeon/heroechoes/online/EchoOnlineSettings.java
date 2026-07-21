package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ProjectLinks;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Hero Echoes online settings: ranked-mode gates, test overrides, and
 * connection values from environment variables / optional {@code .env} file /
 * build-time defaults (e.g. Android {@code BuildConfig}).
 * Precedence: test override &gt; process env &gt; dotenv &gt; build defaults.
 */
public final class EchoOnlineSettings {

	/** Production Hero Echoes host. Used as desktop build-default URL. */
	public static final String PRODUCTION_BACKEND_URL = ProjectLinks.BACKEND_URL;

	public static final String BACKEND_URL = "ECHO_BACKEND_URL";
	public static final String API_KEY = "ECHO_API_KEY";

	private static Boolean testOnlineOverride;
	private static String testBackendUrlOverride;
	private static String testApiKeyOverride;
	private static String buildDefaultBackendUrl;
	private static String buildDefaultApiKey;

	private static final Map<String, String> dotEnv = new HashMap<>();
	private static Function<String, String> systemEnvGetter = System::getenv;

	private EchoOnlineSettings() {
	}

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
		String fromEnvOrDotEnv = trimToEmpty(resolve(BACKEND_URL));
		if (!fromEnvOrDotEnv.isEmpty()) {
			return fromEnvOrDotEnv;
		}
		return trimToEmpty(buildDefaultBackendUrl);
	}

	public static void setBackendUrl(String url) {
		testBackendUrlOverride = url;
	}

	public static String apiKey() {
		if (testApiKeyOverride != null) {
			return testApiKeyOverride;
		}
		String fromEnvOrDotEnv = trimToEmpty(resolve(API_KEY));
		if (!fromEnvOrDotEnv.isEmpty()) {
			return fromEnvOrDotEnv;
		}
		return trimToEmpty(buildDefaultApiKey);
	}

	public static void setApiKey(String apiKey) {
		testApiKeyOverride = apiKey;
	}

	/**
	 * Platform/build defaults used when env and dotenv do not supply a value.
	 * Android debug/release inject these from {@code BuildConfig}.
	 */
	public static void setBuildDefaults(String backendUrl, String apiKey) {
		buildDefaultBackendUrl = backendUrl;
		buildDefaultApiKey = apiKey;
	}

	/** Maps desktop loopback hosts to the Android emulator host-loopback alias. */
	public static String forAndroidEmulatorLoopback(String url) {
		String value = trimToEmpty(url);
		if (value.isEmpty()) {
			return value;
		}
		return value
				.replace("://localhost", "://10.0.2.2")
				.replace("://127.0.0.1", "://10.0.2.2");
	}

	public static boolean isConfigured() {
		String url = backendUrl();
		return url != null && !url.trim().isEmpty();
	}

	public static boolean canSyncOnline() {
		return isOnlineEnabled() && isConfigured();
	}

	public static void loadDotEnv(File file) {
		dotEnv.clear();
		if (file == null || !file.isFile()) {
			return;
		}
		try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
			String line;
			while ((line = reader.readLine()) != null) {
				parseLine(line, dotEnv);
			}
		} catch (IOException ignored) {
			dotEnv.clear();
		}
	}

	public static void loadDefaultDotEnv() {
		for (File candidate : new File[] {
				new File(".env"),
				new File("../.env"),
				new File("../../.env")
		}) {
			if (candidate.isFile()) {
				loadDotEnv(candidate);
				return;
			}
		}
	}

	static void setEnvForTests(Function<String, String> getter) {
		systemEnvGetter = getter != null ? getter : System::getenv;
	}

	public static void resetForTests() {
		testOnlineOverride = null;
		testBackendUrlOverride = null;
		testApiKeyOverride = null;
		buildDefaultBackendUrl = null;
		buildDefaultApiKey = null;
		dotEnv.clear();
		systemEnvGetter = System::getenv;
	}

	private static String resolve(String key) {
		String fromSystem = systemEnvGetter.apply(key);
		if (fromSystem != null && !fromSystem.trim().isEmpty()) {
			return fromSystem;
		}
		return dotEnv.get(key);
	}

	private static void parseLine(String line, Map<String, String> target) {
		String trimmed = line.trim();
		if (trimmed.isEmpty() || trimmed.startsWith("#")) {
			return;
		}
		int separator = trimmed.indexOf('=');
		if (separator <= 0) {
			return;
		}
		String key = trimmed.substring(0, separator).trim();
		String value = trimmed.substring(separator + 1).trim();
		if ((value.startsWith("\"") && value.endsWith("\""))
				|| (value.startsWith("'") && value.endsWith("'"))) {
			value = value.substring(1, value.length() - 1);
		}
		target.put(key, value);
	}

	private static String trimToEmpty(String value) {
		if (value == null) {
			return "";
		}
		return value.trim();
	}
}
