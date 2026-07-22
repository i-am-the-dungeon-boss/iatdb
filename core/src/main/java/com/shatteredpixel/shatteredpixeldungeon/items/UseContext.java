package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;

/**
 * Call-site context for shared item execute APIs ({@link Item#throwAs},
 * {@link com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand#zapAs},
 * {@link com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility#activateAs},
 * {@link com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility#activateAs},
 * {@link com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll#readAs},
 * {@link com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell#castAs}).
 * Splits body / kit / turn ownership so Hero (all self) and EchoBoss
 * (boss body, phantom kit, AI turn) share one path.
 */
public final class UseContext {

	/** Position, sprite, and self-buff target. */
	public final Char body;
	/** Belongings, charges, and Hero-shaped formulas ({@link Item#curUser}). */
	public final Hero kit;
	/** Turn spend / busy policy. */
	public final TurnOwner turns;
	/** QuickSlot, identify, Seer Shot, and other Hero-only riders. */
	public final boolean heroFX;

	public UseContext(Char body, Hero kit, TurnOwner turns, boolean heroFX) {
		this.body = body;
		this.kit = kit;
		this.turns = turns;
		this.heroFX = heroFX;
	}

	/** Player Hero: body = kit = hero; spendAndNext; heroFX on. */
	public static UseContext hero(Hero hero) {
		return new UseContext(hero, hero, TurnOwner.hero(hero), true);
	}

	/** EchoBoss: body = boss, kit = phantom echo hero; no-op turns; heroFX off. */
	public static UseContext echo(EchoBoss boss) {
		return new UseContext(boss, boss.getEchoHero(), TurnOwner.NO_OP, false);
	}

	/**
	 * World VFX (attack / jump / zap / MagicMissile / emitters) when the body's
	 * sprite is attached to a scene. Independent of {@link #heroFX} (QuickSlot /
	 * GLog / turn spend / talent riders).
	 */
	public boolean canWorldFx() {
		return canWorldFx(body);
	}

	/**
	 * Same as {@link #canWorldFx()} for a specific char (e.g. kit after sprite
	 * borrow).
	 */
	public static boolean canWorldFx(Char ch) {
		return ch != null && ch.sprite != null && ch.sprite.parent != null;
	}

	/** Turn ownership for throw/zap/activate spend. */
	public interface TurnOwner {

		void busy();

		void spendAfterThrow(float delay);

		TurnOwner NO_OP = new TurnOwner() {
			@Override
			public void busy() {
			}

			@Override
			public void spendAfterThrow(float delay) {
			}
		};

		static TurnOwner hero(Hero hero) {
			return new TurnOwner() {
				@Override
				public void busy() {
					hero.busy();
				}

				@Override
				public void spendAfterThrow(float delay) {
					if (hero.buff(Talent.LethalMomentumTracker.class) != null) {
						hero.buff(Talent.LethalMomentumTracker.class).detach();
						hero.next();
					} else {
						hero.spendAndNext(delay);
					}
				}
			};
		}
	}
}
