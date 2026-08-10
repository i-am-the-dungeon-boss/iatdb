package com.shatteredpixel.shatteredpixeldungeon.heroechoes.action;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadowsEchoBridge;

/** Echo cloak stealth toggle — delegates to {@link CloakOfShadowsEchoBridge}. */
public final class EchoCloakAdapter {

	private EchoCloakAdapter() {
	}

	public static boolean toggleStealth(EchoBoss boss, CloakOfShadows cloak) {
		return CloakOfShadowsEchoBridge.toggleStealth(boss, cloak);
	}
}
