package com.shatteredpixel.shatteredpixeldungeon.utils;

import com.watabou.utils.Signal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Captures {@link GLog} lines for tests. Register in {@code @BeforeEach},
 * {@link #stop()} in {@code @AfterEach}.
 */
public final class GLogCapture implements Signal.Listener<String> {

	private final List<String> lines = new ArrayList<>();

	public void start() {
		lines.clear();
		GLog.update.add(this);
	}

	public void stop() {
		GLog.update.remove(this);
		lines.clear();
	}

	@Override
	public boolean onSignal(String message) {
		if (message != null && !GLog.NEW_LINE.equals(message)) {
			lines.add(message);
		}
		return false;
	}

	public List<String> lines() {
		return Collections.unmodifiableList(lines);
	}

	public boolean isEmpty() {
		return lines.isEmpty();
	}
}
