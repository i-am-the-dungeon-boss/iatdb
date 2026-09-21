package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.village.VillageFigure;
import com.watabou.noosa.TextureFilm;

/**
 * A standing figure in the village, drawn as the hero it echoes — or as the
 * unknown-warrior placeholder when a depth post is still empty.
 *
 * <p>
 * Idle only. Kept apart from {@link EchoBossSprite}, which carries
 * invisibility handling and the zap, fly and read poses a body that never moves
 * has no use for.
 *
 * <p>
 * This is a third independent source of hero appearance, alongside the local
 * avatar and {@code RemotePlayerSprite}. They are not hoisted into one shared
 * constant even where they coincide: a figure's look comes from the echo that
 * earned the spot, a remote player's from that player's presence, and the local
 * one from your own saved choice. Merging them would claim an invariant that is
 * not real.
 */
public class VillageEchoSprite extends MobSprite {

	/** Cloth armour. Tier 0 is bare-chested, so an unknown tier draws as tier 1. */
	public static final int FALLBACK_TIER = 1;

	public VillageEchoSprite() {
		this(HeroClass.WARRIOR, VillageFigure.UNKNOWN_TIER);
	}

	public VillageEchoSprite(HeroClass heroClass, int armorTier) {
		this(heroClass.spritesheet(), armorTier);
	}

	/**
	 * The black warrior with a white question mark: a depth post nobody holds
	 * yet. Same film layout as the real warrior sheet, so idle frames line up.
	 */
	public static VillageEchoSprite emptyPost() {
		return new VillageEchoSprite(Assets.Sprites.WARRIOR_UNKNOWN, FALLBACK_TIER);
	}

	private VillageEchoSprite(String sheet, int armorTier) {
		super();

		texture(sheet);
		TextureFilm film = HeroSprite.film(tierFor(armorTier));

		idle = new Animation(1, true);
		idle.frames(film, 0, 0, 0, 1, 0, 0, 1, 1);

		// a figure never walks or dies, but CharSprite expects both to exist
		run = new Animation(20, true);
		run.frames(film, 0);

		die = new Animation(20, false);
		die.frames(film, 0);

		renderShadow = true;
		idle();
	}

	/**
	 * The tier to draw. An echo whose armour could not be decoded stores no tier
	 * at all rather than a hollow 1, so the fallback is made here, where it is a
	 * display decision, instead of being baked into the stored data.
	 */
	public static int tierFor(int armorTier) {
		if (armorTier < 1 || armorTier > 6) {
			return FALLBACK_TIER;
		}
		return armorTier;
	}
}
