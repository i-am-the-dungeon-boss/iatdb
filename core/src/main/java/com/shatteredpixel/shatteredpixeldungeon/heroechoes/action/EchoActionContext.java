package com.shatteredpixel.shatteredpixeldungeon.heroechoes.action;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;

/**
 * Validated Echo execute context: real boss body + phantom kit. No Hero factory
 * and no {@code heroFX} flag — Hero keeps upstream entry points.
 */
public final class EchoActionContext {

	/** RoboVM-safe stand-in for a per-hostile effect. */
	public interface HostileEffect {
		void accept(Char ch);
	}

	public final EchoBoss body;

	/**
	 * The phantom kit: gear, talents, and stats, never on stage. It is a
	 * {@code Hero} because base-game math demands one, which also makes it a
	 * {@code Char} that world-facing code will happily draw on — the same defect
	 * shipped three times (ANDROID-21 plus a third copy in the runestone
	 * bridge). Private so every use has to name its intent through
	 * {@link #gear()} or {@link #stats()}.
	 */
	private final Hero kit;

	private EchoActionContext(EchoBoss body, Hero kit) {
		this.body = body;
		this.kit = kit;
	}

	public static EchoActionContext of(EchoBoss boss) {
		if (boss == null) {
			throw new IllegalArgumentException("EchoActionContext requires an EchoBoss");
		}
		Hero kit = boss.getEchoHero();
		if (kit == null) {
			throw new IllegalArgumentException("EchoActionContext rejects a boss without a phantom kit");
		}
		return new EchoActionContext(boss, kit);
	}

	/** The kit's inventory — the overwhelmingly common reason to reach for it. */
	public Belongings gear() {
		return kit.belongings;
	}

	/**
	 * The kit as a calculator, for base-game APIs that will not take anything
	 * but a {@code Hero} — {@code chargeUse}, {@code canCast}, {@code tryToZap},
	 * {@code isEquipped}, {@code reachFactor}.
	 *
	 * <p><b>Never pass this to world or VFX code.</b> It is off stage; its
	 * sprite is mirrored from {@link #body}, so drawing on it puts the effect on
	 * a char the player is not looking at. Pass {@code body} instead, or use the
	 * {@code ...Fx} methods below. Guarded by {@code EchoKitEscapeTest}.
	 */
	public Hero stats() {
		return kit;
	}

	public boolean canWorldFx() {
		return canWorldFx(body);
	}

	public static boolean canWorldFx(Char ch) {
		return Char.canWorldFx(ch);
	}

	public void busy() {
		body.busy();
	}

	public void complete(float delay) {
		body.spendAndNext(delay);
	}

	public void cancel() {
		body.cancelBusy();
	}

	/**
	 * Visible chars hostile to {@link #body} — includes the living Hero when an
	 * Echo reads/casts AoE that should hit the player.
	 */
	public void forEachVisibleHostile(HostileEffect effect) {
		if (effect == null || body == null || Dungeon.level == null) {
			return;
		}
		boolean[] fov = body.fieldOfView != null ? body.fieldOfView : Dungeon.level.heroFOV;
		if (fov == null) {
			return;
		}
		for (Char ch : Actor.chars()) {
			if (ch == null || ch == body || ch.alignment == body.alignment) {
				continue;
			}
			if (ch.pos < 0 || ch.pos >= fov.length || !fov[ch.pos]) {
				continue;
			}
			effect.accept(ch);
		}
	}
}
