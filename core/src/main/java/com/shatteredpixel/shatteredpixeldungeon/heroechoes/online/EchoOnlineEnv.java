package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Hero Echoes connection settings loaded from environment variables and an optional {@code .env} file.
 * Process environment variables take precedence over values from the file.
 */
public final class EchoOnlineEnv {

	public static final String BACKEND_URL = "ECHO_BACKEND_URL";
	public static final String API_KEY = "ECHO_API_KEY";

	private static final Map<String, String> dotEnv = new HashMap<>();
	private static Function<String, String> envGetter = EchoOnlineEnv::resolve;

	private EchoOnlineEnv() {}

	public static String backendUrl() {
		return trimToEmpty(envGetter.apply(BACKEND_URL));
	}

	public static String apiKey() {
		return trimToEmpty(envGetter.apply(API_KEY));
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
		envGetter = getter != null ? getter : EchoOnlineEnv::resolve;
	}

	static void resetForTests() {
		dotEnv.clear();
		envGetter = EchoOnlineEnv::resolve;
	}

	private static String resolve(String key) {
		String fromSystem = System.getenv(key);
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
