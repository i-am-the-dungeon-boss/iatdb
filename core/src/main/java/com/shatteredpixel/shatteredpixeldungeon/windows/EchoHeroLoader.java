package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoHeroSnapshot;

/** Restores a {@link Hero} view model from echo metadata for UI display. */
public final class EchoHeroLoader {

	private EchoHeroLoader() {}

	public static Hero load(Echo snapshot) {
		if (snapshot == null) {
			return null;
		}
		if (snapshot.echoData != null) {
			try {
				return EchoHeroSnapshot.restoreHero(snapshot);
			} catch (Throwable ignored) {
			}
		}
		return fallbackHero(snapshot);
	}

	public static HeroClass heroClass(Echo snapshot) {
		if (snapshot == null || snapshot.heroClass == null) {
			return HeroClass.WARRIOR;
		}
		try {
			return HeroClass.valueOf(snapshot.heroClass);
		} catch (IllegalArgumentException e) {
			return HeroClass.WARRIOR;
		}
	}

	public static int armorTier(Hero hero, Echo snapshot) {
		if (hero != null) {
			int tier = hero.tier();
			if (tier > 0) {
				return tier;
			}
		}
		if (snapshot == null || snapshot.lvl <= 0) {
			return 1;
		}
		return Math.min(5, 1 + snapshot.lvl / 6);
	}

	private static Hero fallbackHero(Echo snapshot) {
		Hero hero = new Hero();
		hero.live();
		hero.heroClass = heroClass(snapshot);
		hero.lvl = Math.max(1, snapshot.lvl);
		hero.HP = Math.max(1, snapshot.hp);
		hero.HT = Math.max(hero.HP, snapshot.ht);
		return hero;
	}
}
