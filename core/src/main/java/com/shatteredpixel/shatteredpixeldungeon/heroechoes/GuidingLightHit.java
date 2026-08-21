package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.watabou.utils.Nullable;

/**
 * Who an {@code Illuminated} (Guiding Light) target is a guaranteed hit for.
 * <p>
 * Base-game {@code Mob#defenseSkill} asks {@code Dungeon.hero.heroClass ==
 * CLERIC}, which is only correct while the player is the one casting. An Echo
 * casts from a phantom kit {@link Hero} that {@code Dungeon.hero} knows nothing
 * about, and it attacks as either the {@link EchoBoss} body or the kit itself
 * depending on the path — so the question has to be asked of the attacker.
 * {@code Hero#defenseSkill} and {@code Mob#defenseSkill} both route here so the
 * two stay the same rule.
 */
public final class GuidingLightHit {

	private GuidingLightHit() {
	}

	/**
	 * True when {@code attacker} is the cleric the light belongs to and is
	 * swinging a weapon they have the STR for — the base-game weapon
	 * requirement, now asked of the echo kit as well as of the player.
	 */
	public static boolean isClericFreeHit(@Nullable Char attacker) {
		Hero cleric = attackingCleric(attacker);
		if (cleric == null) {
			return false;
		}
		KindOfWeapon weapon = cleric.belongings.attackingWeapon();
		return !(weapon instanceof Weapon) || ((Weapon) weapon).STRReq() <= cleric.STR();
	}

	/**
	 * True when {@code attacker} is not a cleric itself but fights alongside a
	 * cleric player. Base game hands allies the free hit with no weapon
	 * requirement; {@code Mob#defenseSkill} keeps that, {@code Hero} does not
	 * (the illuminated hero <em>is</em> {@code Dungeon.hero}, so it has no
	 * meaning there).
	 */
	public static boolean isClericAlly(@Nullable Char attacker) {
		return attackingCleric(attacker) == null
				&& Dungeon.hero != null
				&& Dungeon.hero.heroClass == HeroClass.CLERIC;
	}

	/** The cleric behind an attack: the hero itself, or an echo's kit. */
	@Nullable
	private static Hero attackingCleric(@Nullable Char attacker) {
		Hero hero = null;
		if (attacker instanceof Hero) {
			hero = (Hero) attacker;
		} else if (attacker instanceof EchoBoss) {
			hero = ((EchoBoss) attacker).getEchoHero();
		}
		return hero != null && hero.heroClass == HeroClass.CLERIC ? hero : null;
	}
}
