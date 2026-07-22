/*
 * I am the Dungeon Boss
 * Copyright (C) 2026 Dungeon Boss
 */

package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

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
}
