package com.shatteredpixel.shatteredpixeldungeon.heroechoes.action;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlentyEchoBridge;

/** Echo horn snack/eat — delegates to {@link HornOfPlentyEchoBridge}. */
public final class EchoHornAdapter {

	private EchoHornAdapter() {
	}

	public static boolean snack(EchoBoss boss, HornOfPlenty horn) {
		return HornOfPlentyEchoBridge.snack(boss, horn, 1);
	}
}
