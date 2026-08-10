package com.shatteredpixel.shatteredpixeldungeon.heroechoes.action;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.EtherealChains;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.EtherealChainsEchoBridge;

/** Echo Ethereal Chains cast — delegates to {@link EtherealChainsEchoBridge}. */
public final class EchoChainsAdapter {

	private EchoChainsAdapter() {
	}

	public static boolean cast(EchoBoss boss, EtherealChains chains, int target) {
		return EtherealChainsEchoBridge.cast(boss, chains, target);
	}
}
