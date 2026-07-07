package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Routes combat-stat queries through a headless {@link Hero} snapshot.
 * <p>
 * Echo bosses keep a restored hero for stats/inventory only — it is never placed on
 * the level and has no sprite. Full {@link Hero} methods may update rendering or
 * audio; this delegate syncs {@link Hero#pos} and uses stat-safe entry points only.
 */
public final class EchoHeroCombatDelegate {

    private EchoHeroCombatDelegate() {}

    public static int damageRoll(Hero echoHero, int bossPos) {
        return withPosInt(echoHero, bossPos, echoHero::damageRoll);
    }

    public static int attackSkill(Hero echoHero, int bossPos, Char target) {
        return withPosInt(echoHero, bossPos, () -> echoHero.attackSkill(target));
    }

    public static int defenseSkill(Hero echoHero, int bossPos, Char enemy) {
        return withPosInt(echoHero, bossPos, () -> echoHero.defenseSkill(enemy));
    }

    public static int drRoll(Hero echoHero, int bossPos) {
        return withPosInt(echoHero, bossPos, echoHero::drRoll);
    }

    public static float attackDelay(Hero echoHero, int bossPos) {
        return withPos(echoHero, bossPos, echoHero::attackDelay);
    }

    public static float speed(Hero echoHero, int bossPos) {
        return withPos(echoHero, bossPos, echoHero::combatSpeed);
    }

    public static int attackProc(Hero echoHero, int bossPos, Char enemy, int damage) {
        return withPosInt(echoHero, bossPos, () -> echoHero.attackProc(enemy, damage));
    }

    public static int defenseProc(Hero echoHero, int bossPos, Char enemy, int damage) {
        return withPosInt(echoHero, bossPos, () -> echoHero.defenseProc(enemy, damage));
    }

    private static int withPosInt(Hero echoHero, int bossPos, IntSupplier action) {
        int savedPos = echoHero.pos;
        echoHero.pos = bossPos;
        try {
            return action.getAsInt();
        } finally {
            echoHero.pos = savedPos;
        }
    }

    private static <T> T withPos(Hero echoHero, int bossPos, Supplier<T> action) {
        int savedPos = echoHero.pos;
        echoHero.pos = bossPos;
        try {
            return action.get();
        } finally {
            echoHero.pos = savedPos;
        }
    }
}
