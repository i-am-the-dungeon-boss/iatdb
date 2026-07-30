package com.shatteredpixel.shatteredpixeldungeon.items.bombs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicyChoice;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicyStatusBuilder;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoRoleExecutor;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import com.watabou.utils.Reflection;
import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@ExtendWith(GdxTestExtension.class)
class BombKindsLightThrowAsTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
	}

	static Stream<Class<? extends Bomb>> catalogBombs() {
		List<Class<? extends Bomb>> kinds = new ArrayList<>();
		for (Class<?> cls : Catalog.BOMBS.items()) {
			if (Bomb.class.isAssignableFrom(cls)) {
				@SuppressWarnings("unchecked")
				Class<? extends Bomb> bombCls = (Class<? extends Bomb>) cls;
				kinds.add(bombCls);
			}
		}
		return kinds.stream();
	}

	@ParameterizedTest(name = "Echo {0} throwAs lights fuse")
	@MethodSource("catalogBombs")
	@DisplayName("Echo throwAs lights fuse for every catalog bomb kind")
	void echoThrowAsLightsFuseForEveryBombKind(Class<? extends Bomb> bombClass) {
		Hero player = EchoTestSupport.warriorHero();
		Bomb bomb = Reflection.newInstance(bombClass);
		Assertions.assertThat(bomb).isNotNull();
		bomb.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Bomb kitBomb = boss.getEchoHero().belongings.getItem(bombClass);
		Assertions.assertThat(kitBomb).isNotNull();
		Assertions.assertThat(kitBomb.throwAs(UseContext.echo(boss), player.pos)).isTrue();

		Bomb landed = findBombAt(player.pos, bombClass);
		Assertions.assertThat(landed)
				.as("%s must land lit (LIGHTTHROW), not as a plain throw", bombClass.getSimpleName())
				.isNotNull();
		Assertions.assertThat(landed.fuse)
				.as("%s fuse must be lit", bombClass.getSimpleName())
				.isNotNull();
	}

	@ParameterizedTest(name = "EchoRoleExecutor BOMB role lights {0}")
	@MethodSource("catalogBombs")
	@DisplayName("EchoRoleExecutor BOMB role light-throws every catalog bomb kind")
	void echoRoleExecutorBombRoleLightsEveryKind(Class<? extends Bomb> bombClass) {
		Hero player = EchoTestSupport.warriorHero();
		String id = bombClass.getSimpleName();
		EchoPolicy policy = EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("BOMB", new JSONObject()
						.put("pick", "FIRST_LEGAL")
						.put("items", new JSONArray().put(id)))
				.put("MELEE", EchoTestSupport.capability("*melee")));
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(player, policy, 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		boss.fieldOfView = new boolean[Dungeon.level.length()];
		java.util.Arrays.fill(boss.fieldOfView, true);

		Bomb bomb = Reflection.newInstance(bombClass);
		Assertions.assertThat(bomb).isNotNull();
		bomb.collect(boss.getEchoHero().belongings.backpack);

		boolean spent = EchoRoleExecutor.execute(
				boss,
				policy,
				EchoPolicyStatusBuilder.build(boss, policy),
				new EchoPolicyChoice("BOMB", "default", null, id));

		Assertions.assertThat(spent).isTrue();
		Bomb landed = findBombAt(player.pos, bombClass);
		Assertions.assertThat(landed).isNotNull();
		Assertions.assertThat(landed.fuse)
				.as("%s via BOMB role must light fuse", id)
				.isNotNull();
	}

	private static Bomb findBombAt(int cell, Class<? extends Bomb> bombClass) {
		Heap heap = Dungeon.level.heaps.get(cell);
		if (heap == null) {
			return null;
		}
		for (com.shatteredpixel.shatteredpixeldungeon.items.Item i : heap.items) {
			if (bombClass.isInstance(i)) {
				return (Bomb) i;
			}
		}
		return null;
	}
}
