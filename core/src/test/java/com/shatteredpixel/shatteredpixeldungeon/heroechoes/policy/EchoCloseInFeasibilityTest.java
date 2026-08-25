package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Inferno;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

/**
 * {@code CLOSE_IN} is a virtual role: {@code *move_closer} always resolves, so
 * readiness used to be reported on item resolution alone. When a blob walls the
 * route off — {@code modifyPassable} treats every AoE DoT cell as impassable —
 * the positioning layer still won the turn and {@code getCloser} then silently
 * refused it, spending nothing. Readiness has to mean a step exists, the same
 * way {@code KEEP_DISTANCE} already asks {@code hasStepAwayFrom}.
 */
@ExtendWith(GdxTestExtension.class)
class EchoCloseInFeasibilityTest {

	@Test
	@DisplayName("CLOSE_IN is ready when a route to the hero exists")
	void readyOnOpenGround() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = closeInBoss(hero);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, closeInPolicy());

		Assertions.assertThat(status.isRoleReady(EchoRole.CLOSE_IN.id())).isTrue();
	}

	@Test
	@DisplayName("CLOSE_IN is not ready when a blob walls off every route")
	void notReadyWhenTheRouteIsWalledOff() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = closeInBoss(hero);
		burnColumn(3);

		EchoPolicyStatus status = EchoPolicyStatusBuilder.build(boss, closeInPolicy());

		Assertions.assertThat(status.selfStatuses).doesNotContain(EchoAoeDots.STATUS);
		Assertions.assertThat(boss.policyStepCloser(hero.pos)).isFalse();
		Assertions.assertThat(status.isRoleReady(EchoRole.CLOSE_IN.id())).isFalse();
	}

	/** Hero at (3,1), echo at (3,5): distance 4, open floor between them. */
	private static EchoBoss closeInBoss(Hero hero) {
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, closeInPolicy(), 5);
		boss.state = boss.HUNTING;
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);
		int width = Dungeon.level.width();
		hero.pos = 3 * width + 1;
		boss.pos = 3 * width + 5;
		Arrays.fill(boss.fieldOfView, true);
		Arrays.fill(Dungeon.level.heroFOV, true);
		boss.timeToNow();
		return boss;
	}

	/** Inferno down a whole column: no route past it, and none through it. */
	private static void burnColumn(int column) {
		int width = Dungeon.level.width();
		for (int row = 0; row < Dungeon.level.height(); row++) {
			Blob.seed(row * width + column, 120, Inferno.class);
		}
	}

	private static EchoPolicy closeInPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put(EchoRole.CLOSE_IN.id(), EchoTestSupport.capability("*move_closer")));
	}
}
