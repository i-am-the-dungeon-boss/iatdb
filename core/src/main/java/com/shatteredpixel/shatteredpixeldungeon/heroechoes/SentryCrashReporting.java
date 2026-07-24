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

import com.watabou.noosa.Game;
import io.sentry.Sentry;

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

	private static final Reporter DEFAULT = Sentry::captureException;

	private static Reporter reporter = DEFAULT;

	private SentryCrashReporting() {
	}

	public static void setReporter(Reporter next) {
		reporter = next != null ? next : DEFAULT;
	}

	public static void resetReporter() {
		reporter = DEFAULT;
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
