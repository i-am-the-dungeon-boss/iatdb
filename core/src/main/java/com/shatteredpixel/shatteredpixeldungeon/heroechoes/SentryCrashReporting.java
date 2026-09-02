/*
 * I am the Dungeon Boss
 * Copyright (C) 2026 Dungeon Boss
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPlayerAuth;
import com.watabou.noosa.Game;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import io.sentry.protocol.User;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Thin hook from {@link com.watabou.noosa.Game#reportException} to Sentry, plus
 * shared launcher init. Capture without {@link Sentry#init} is a no-op.
 * INDEV / debug builds never report.
 */
public final class SentryCrashReporting {

	public static final String PROPERTIES_RESOURCE = "sentry.properties";

	/** Flush window for crash-time capture before process exit. */
	public static final long CRASH_FLUSH_TIMEOUT_MS = 2000L;

	@FunctionalInterface
	public interface Reporter {
		void report(Throwable throwable);
	}

	/** Sent when the player files a report without typing anything. */
	public static final String DEFAULT_USER_REPORT = "Player-reported error (no description)";

	/**
	 * Player-filed reports are not crashes: they are triaged separately from real
	 * errors, so they go out at WARNING.
	 */
	public static final SentryLevel USER_REPORT_LEVEL = SentryLevel.WARNING;

	@FunctionalInterface
	public interface MessageReporter {
		/** {@code username} is the reporting player's name, empty when unknown. */
		void report(String message, String username);
	}

	private static final Reporter DEFAULT = Sentry::captureException;

	private static final MessageReporter DEFAULT_MESSAGE = (message, username) ->
			Sentry.withScope(scope -> {
				if (!username.isEmpty()) {
					User user = new User();
					user.setUsername(username);
					scope.setUser(user);
				}
				Sentry.captureMessage(message, USER_REPORT_LEVEL);
			});

	private static Reporter reporter = DEFAULT;

	private static MessageReporter messageReporter = DEFAULT_MESSAGE;

	private SentryCrashReporting() {
	}

	public static void setReporter(Reporter next) {
		reporter = next != null ? next : DEFAULT;
	}

	public static void resetReporter() {
		reporter = DEFAULT;
	}

	public static void setMessageReporter(MessageReporter next) {
		messageReporter = next != null ? next : DEFAULT_MESSAGE;
	}

	public static void resetMessageReporter() {
		messageReporter = DEFAULT_MESSAGE;
	}

	/**
	 * Files a player-initiated report as a Sentry event at
	 * {@link #USER_REPORT_LEVEL}, attributed to the reporting player. The
	 * description is optional: blank or null reports still go out, tagged with
	 * {@link #DEFAULT_USER_REPORT}, since the surrounding scope (version, run state)
	 * is the useful part. INDEV builds never report.
	 */
	public static void reportUserMessage(String message) {
		reportUserMessage(message, currentUsername());
	}

	/** As {@link #reportUserMessage(String)}, with an explicit reporter name. */
	public static void reportUserMessage(String message, String username) {
		if (isDevBuild()) {
			return;
		}
		String trimmed = message != null ? message.trim() : "";
		String name = username != null ? username.trim() : "";
		messageReporter.report(trimmed.isEmpty() ? DEFAULT_USER_REPORT : trimmed, name);
	}

	/**
	 * The reporting player's name, empty when there is no session and no local
	 * name; settings access must never block a report.
	 */
	private static String currentUsername() {
		try {
			String name = EchoPlayerAuth.preferredUsername();
			return name != null ? name : "";
		} catch (Throwable ignored) {
			return "";
		}
	}

	public static void report(Throwable throwable) {
		if (throwable == null || isDevBuild()) {
			return;
		}
		reporter.report(throwable);
	}

	/**
	 * Capture then block briefly so the transport can ship before a crash exit.
	 * Used from uncaught-exception handlers.
	 */
	public static void reportAndFlush(Throwable throwable) {
		report(throwable);
		if (throwable == null || isDevBuild()) {
			return;
		}
		Sentry.flush(CRASH_FLUSH_TIMEOUT_MS);
	}

	/**
	 * Init Sentry for a release launcher. Sets DSN from classpath
	 * {@code sentry.properties} explicitly (RoboVM cannot rely on cwd discovery).
	 * No-op for INDEV versions or when DSN is missing.
	 * <p>
	 * On iOS, callers must invoke this inside
	 * {@code org.robovm.rt.Signals.installSignals(..., true)} so RoboVM keeps its
	 * NPE signal handlers.
	 */
	public static void initForRelease(String platform, String version, int versionCode) {
		String dsn = readClasspathDsn();
		if (!shouldInit(platform, version, dsn)) {
			return;
		}
		try {
			Sentry.init(options -> {
				options.setDsn(dsn.trim());
				// DSN already comes from classpath; avoid cwd/file discovery.
				options.setEnableExternalConfiguration(false);
				options.setTag("platform", platform);
				options.setSendDefaultPii(true);
				options.setTracesSampleRate(1.0);
				// Launchers install their own handler that calls reportAndFlush.
				// Avoid a second handler that can fight RoboVM signal setup.
				options.setEnableUncaughtExceptionHandler(false);
			});
			Sentry.configureScope(scope -> {
				if (version != null) {
					scope.setTag("app.version", version);
				}
				scope.setTag("app.version_code", String.valueOf(versionCode));
			});
		} catch (Throwable ignored) {
			// Crash reporting must never abort app launch.
		}
	}

	static boolean shouldInit(String version, String dsn) {
		if (version != null && version.contains("INDEV")) {
			return false;
		}
		return dsn != null && !dsn.trim().isEmpty();
	}

	/**
	 * Same gates as {@link #shouldInit(String, String)}; platform is recorded as a
	 * tag.
	 */
	static boolean shouldInit(String platform, String version, String dsn) {
		return shouldInit(version, dsn);
	}

	static String readClasspathDsn() {
		try (InputStream in = SentryCrashReporting.class.getClassLoader()
				.getResourceAsStream(PROPERTIES_RESOURCE)) {
			return readDsn(in);
		} catch (IOException e) {
			return "";
		}
	}

	/**
	 * Parses {@code dsn=} from a {@code sentry.properties} stream; empty if
	 * missing.
	 */
	static String readDsn(InputStream in) throws IOException {
		if (in == null) {
			return "";
		}
		Properties props = new Properties();
		props.load(in);
		String dsn = props.getProperty("dsn");
		return dsn != null ? dsn.trim() : "";
	}

	/**
	 * Matches {@link com.watabou.utils.DeviceCompat#isDebug()} without NPE on null
	 * version.
	 */
	static boolean isDevBuild() {
		return Game.version != null && Game.version.contains("INDEV");
	}
}
