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

/**
 * Thin hook from {@link com.watabou.noosa.Game#reportException} to Sentry.
 * Capture without {@link Sentry#init} is a no-op (e.g. iOS until wired).
 * INDEV / debug builds never report.
 */
public final class SentryCrashReporting {

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
	 * Matches {@link com.watabou.utils.DeviceCompat#isDebug()} without NPE on null
	 * version.
	 */
	static boolean isDevBuild() {
		return Game.version != null && Game.version.contains("INDEV");
	}
}
