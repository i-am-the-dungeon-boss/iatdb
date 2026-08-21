package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;

/**
 * A {@link com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob} that stands
 * for somebody else's hero, and can therefore be inspected as one.
 *
 * <p>Implemented by the {@code EchoBoss} the player fights and by the standing
 * figures in the village, which have nothing else in common: one is a boss on
 * the Actor clock, the other is passive scenery. What they share is that their
 * inspect window should show the echoed hero's stats, kit and talents rather
 * than the mob's own, and that the hero in question is emphatically not
 * {@code Dungeon.hero}.
 *
 * <p>The window reads health from the {@code Mob} itself, so an implementor is
 * expected to carry HP and HT that mean something about the echo — for a boss
 * that is the fight in progress, for a village figure the hero as they were
 * recorded.
 */
public interface EchoInspectable {

	/**
	 * The restored hero, or null when there is nothing to show — an echo whose
	 * bundle is missing, or one whose data has not arrived yet.
	 */
	Hero getEchoHero();

	/** The echo metadata behind this body, or null when it is not known. */
	Echo getEcho();
}
