package com.shatteredpixel.shatteredpixeldungeon.heroechoes.action;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Daze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Degrade;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hex;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;

import java.util.HashSet;

/**
 * Moves combat-proc buffs off the phantom Echo kit onto the on-stage body after
 * {@code withEchoHeroCombat}. Kit identity buffs (Hunger, chargers, …) stay
 * put.
 */
public final class EchoCombatBuffTransfer {

	private EchoCombatBuffTransfer() {
	}

	private static final Class<?>[] TRANSFER = {
			Burning.class,
			Chill.class,
			Frost.class,
			Ooze.class,
			Corrosion.class,
			Paralysis.class,
			Cripple.class,
			Roots.class,
			Charm.class,
			Bleeding.class,
			Poison.class,
			Hex.class,
			Weakness.class,
			Vulnerable.class,
			Degrade.class,
			Slow.class,
			Daze.class,
	};

	public static HashSet<Buff> snapshot(Char kit) {
		HashSet<Buff> before = new HashSet<>();
		if (kit == null) {
			return before;
		}
		for (Buff b : kit.buffs()) {
			before.add(b);
		}
		return before;
	}

	public static void moveNewCombatBuffs(Char kit, Char body, HashSet<Buff> before) {
		if (kit == null || body == null || before == null) {
			return;
		}
		for (Buff b : kit.buffs().toArray(new Buff[0])) {
			if (before.contains(b) || !shouldTransfer(b)) {
				continue;
			}
			b.detach();
			if (body.buff(b.getClass()) != null) {
				continue;
			}
			b.attachTo(body);
		}
	}

	private static boolean shouldTransfer(Buff b) {
		Class<?> cls = b.getClass();
		for (Class<?> transfer : TRANSFER) {
			if (transfer == cls) {
				return true;
			}
		}
		return false;
	}
}
