/*
 * I am the Dungeon Boss
 * Copyright (C) 2026 Dungeon Boss
 */

package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.watabou.noosa.Game;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class SentryCrashReportingTest {

	@AfterEach
	void resetReporter() {
		SentryCrashReporting.resetReporter();
	}

	@Test
	@DisplayName("report forwards throwable to the installed reporter")
	void reportForwardsThrowable() {
		List<Throwable> captured = new ArrayList<>();
		SentryCrashReporting.setReporter(captured::add);

		RuntimeException boom = new RuntimeException("boom");
		SentryCrashReporting.report(boom);

		Assertions.assertThat(captured).containsExactly(boom);
	}

	@Test
	@DisplayName("report ignores null throwable")
	void reportIgnoresNull() {
		List<Throwable> captured = new ArrayList<>();
		SentryCrashReporting.setReporter(captured::add);

		SentryCrashReporting.report(null);

		Assertions.assertThat(captured).isEmpty();
	}

	@Test
	@DisplayName("report does not send to Sentry on INDEV builds")
	void reportSkippedOnIndevBuilds() {
		String previous = Game.version;
		Game.version = "1.0.0-INDEV";
		try {
			List<Throwable> captured = new ArrayList<>();
			SentryCrashReporting.setReporter(captured::add);

			SentryCrashReporting.report(new RuntimeException("dev boom"));

			Assertions.assertThat(captured).isEmpty();
		} finally {
			Game.version = previous;
		}
	}

	@Test
	@DisplayName("report still sends on release builds")
	void reportSendsOnReleaseBuilds() {
		String previous = Game.version;
		Game.version = "1.0.0";
		try {
			List<Throwable> captured = new ArrayList<>();
			SentryCrashReporting.setReporter(captured::add);
			RuntimeException boom = new RuntimeException("prod boom");

			SentryCrashReporting.report(boom);

			Assertions.assertThat(captured).containsExactly(boom);
		} finally {
			Game.version = previous;
		}
	}
}
