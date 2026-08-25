package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Inferno;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invulnerability;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicy;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.watabou.utils.PathFinder;

import java.util.Arrays;

/**
 * One invariant, every rung: if {@link EchoBoss#act()} says the turn was taken,
 * the actor's clock moved. A rung that returns {@code true} without spending
 * re-enters {@code act()} at the same game time, which is an infinite loop the
 * player sees as a frozen boss.
 */
@ExtendWith(GdxTestExtension.class)
class EchoBossTurnSpendTest {

	private static final Offset<Float> EPS = Offset.offset(0.0001f);

	@Test
	@DisplayName("the AoE last-resort escape pays for the step it takes")
	void aoeLastResortSpendsTheTurn() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = meleeOnlyBoss(hero, 2);
		engulf(boss.pos);
		int start = boss.pos;

		Assertions.assertThat(boss.act()).isTrue();

		Assertions.assertThat(boss.pos).isNotEqualTo(start);
		Assertions.assertThat(boss.cooldown()).isCloseTo(1f / boss.speed(), EPS);
	}

	@Test
	@DisplayName("a paralysed echo pays one tick")
	void paralysedSpendsATick() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = meleeOnlyBoss(hero, 2);
		Buff.affect(boss, Paralysis.class, 10f);

		Assertions.assertThat(boss.act()).isTrue();

		Assertions.assertThat(boss.cooldown()).isCloseTo(Actor.TICK, EPS);
	}

	@Test
	@DisplayName("the untouchable floor's disengage step pays the kite delay")
	void untouchableRunPaysTheKiteDelay() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = meleeOnlyBoss(hero, 1);
		// Untouchable hero: MELEE is gated, so the Java floor owns the turn.
		Buff.affect(hero, Invulnerability.class, 3f);
		int start = boss.pos;

		Assertions.assertThat(boss.act()).isTrue();

		Assertions.assertThat(boss.pos).isNotEqualTo(start);
		Assertions.assertThat(Dungeon.level.distance(boss.pos, hero.pos)).isGreaterThan(1);
		Assertions.assertThat(boss.cooldown()).isCloseTo(boss.kiteStepDelay(), EPS);
	}

	@Test
	@DisplayName("a deferred VFX turn does not leave the next turn unpaid")
	void deferredVfxFlagDoesNotLeakIntoTheNextTurn() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = meleeOnlyBoss(hero, 2);

		// Turn N: a throw hands the turn to its projectile callback, which pays.
		boss.busy();
		boss.spendAndNext(Actor.TICK);
		Assertions.assertThat(boss.isBusy()).isFalse();

		// Turn N+1: a rung that never enters runPlan must still spend.
		boss.timeToNow();
		Buff.affect(boss, Paralysis.class, 10f);

		Assertions.assertThat(boss.act()).isTrue();

		Assertions.assertThat(boss.cooldown()).isCloseTo(Actor.TICK, EPS);
	}

	/** Warrior echo on a melee-only playbook: nothing else can claim the turn. */
	private static EchoBoss meleeOnlyBoss(Hero hero, int offset) {
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, meleePolicy(), 5);
		boss.state = boss.HUNTING;
		EchoTestSupport.installEchoBossLevel(hero, boss, offset);
		Arrays.fill(boss.fieldOfView, true);
		Arrays.fill(Dungeon.level.heroFOV, true);
		boss.timeToNow();
		return boss;
	}

	private static EchoPolicy meleePolicy() {
		JSONObject root = new JSONObject(EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("MELEE", EchoTestSupport.capability("*melee"))).root().toString());
		root.put("selection", new JSONObject()
				.put("order", new JSONArray().put("default"))
				.put("default_roles", new JSONArray().put("MELEE")));
		return EchoPolicy.fromJson(root);
	}

	/** Infernal Brew pattern: the cell and every neighbour of it. */
	private static void engulf(int cell) {
		Blob.seed(cell, 120, Inferno.class);
		for (int i : PathFinder.NEIGHBOURS8) {
			if (Dungeon.level.insideMap(cell + i)) {
				Blob.seed(cell + i, 120, Inferno.class);
			}
		}
	}
}
