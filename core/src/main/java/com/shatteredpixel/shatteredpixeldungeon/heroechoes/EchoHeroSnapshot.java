package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.watabou.utils.Bundle;

/** Captures and restores hero equipment for echo snapshots. */
public final class EchoHeroSnapshot {

	static final String WEAPON = "weapon";
	static final String ARMOR = "armor";
	static final String ARTIFACT = "artifact";
	static final String MISC = "misc";
	static final String RING = "ring";
	static final String SECOND_WEP = "second_wep";

	private EchoHeroSnapshot() {}

	public static boolean hasEquippedItems(Bundle echoData) {
		if (echoData == null) {
			return false;
		}
		return echoData.get(WEAPON) != null
				|| echoData.get(ARMOR) != null
				|| echoData.get(ARTIFACT) != null
				|| echoData.get(MISC) != null
				|| echoData.get(RING) != null
				|| echoData.get(SECOND_WEP) != null;
	}

	public static boolean heroHasEquippedItems(Hero hero) {
		if (hero == null) {
			return false;
		}
		return hero.belongings.weapon() != null
				|| hero.belongings.armor() != null
				|| hero.belongings.artifact() != null
				|| hero.belongings.misc() != null
				|| hero.belongings.ring() != null
				|| hero.belongings.secondWep() != null;
	}

	public static Bundle captureFromHero(Hero hero) {
		if (hero == null) {
			return null;
		}

		Bundle data = new Bundle();
		try {
			hero.storeInBundle(data);
		} catch (Throwable ignored) {
			data = new Bundle();
		}
		recordEquippedItems(hero, data);
		return data;
	}

	public static void recordEquippedItems(Hero hero, Bundle echoData) {
		if (hero == null || echoData == null) {
			return;
		}

		if (hero.belongings.weapon() != null && echoData.get(WEAPON) == null) {
			echoData.put(WEAPON, hero.belongings.weapon());
		}
		if (hero.belongings.armor() != null && echoData.get(ARMOR) == null) {
			echoData.put(ARMOR, hero.belongings.armor());
		}
		if (hero.belongings.artifact() != null && echoData.get(ARTIFACT) == null) {
			echoData.put(ARTIFACT, hero.belongings.artifact());
		}
		if (hero.belongings.misc() != null && echoData.get(MISC) == null) {
			echoData.put(MISC, hero.belongings.misc());
		}
		if (hero.belongings.ring() != null && echoData.get(RING) == null) {
			echoData.put(RING, hero.belongings.ring());
		}
		if (hero.belongings.secondWep() != null && echoData.get(SECOND_WEP) == null) {
			echoData.put(SECOND_WEP, hero.belongings.secondWep());
		}
	}

	public static Hero restoreHero(Echo echo) {
		if (echo == null || echo.echoData == null) {
			return null;
		}
		Hero hero = new Hero();
		hero.live();
		hero.restoreFromBundle(echo.echoData);
		return hero;
	}
}
