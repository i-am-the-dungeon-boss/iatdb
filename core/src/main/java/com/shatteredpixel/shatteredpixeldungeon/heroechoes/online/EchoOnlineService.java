package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoStorage;
import com.shatteredpixel.shatteredpixeldungeon.levels.EchoReplacementDecider;

/**
 * Factory for the online Hero Echoes integration layer.
 * Keeps HTTP/sync wiring out of gameplay classes.
 */
public final class EchoOnlineService {

	private static CompositeEchoLookup defaultLookup;

	private EchoOnlineService() {}

	public static EchoReplacementDecider.EchoLookup echoLookup() {
		if (defaultLookup == null) {
			defaultLookup = new CompositeEchoLookup(
					EchoClient.createDefault(),
					new EchoStorage()
			);
		}
		return defaultLookup;
	}

	public static void resetForTests() {
		defaultLookup = null;
		EchoOnlineSync.setDefaultForTests(null);
	}
}
