package com.shatteredpixel.shatteredpixeldungeon.items.wands;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GravityChaosTracker;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hex;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Levitation;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.watabou.utils.Random;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class CursedWandEchoBossTest {

	@BeforeEach
	void setUp() {
		EchoTestSupport.resetWorkflowState();
		CursedWand.setForcedEffect(null);
	}

	@AfterEach
	void tearDown() {
		CursedWand.setForcedEffect(null);
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("resolveCaster maps echo kit to EchoBoss body")
	void resolveCasterMapsKitToBody() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Assertions.assertThat(CursedWand.resolveCaster(boss.getEchoHero())).isSameAs(boss);
		Assertions.assertThat(CursedWand.resolveCaster(player)).isSameAs(player);
		Assertions.assertThat(CursedWand.resolveCaster(boss)).isSameAs(boss);
	}

	@Test
	@DisplayName("denies InterFloorTeleport AbortRetryFail Petrify HeroShapeShift for echo kit")
	void deniesEchoNonsenseEffectsForKit() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		Hero kit = boss.getEchoHero();
		Ballistica bolt = new Ballistica(boss.pos, player.pos, Ballistica.MAGIC_BOLT);

		Assertions.assertThat(new CursedWand.InterFloorTeleport().valid(null, kit, bolt, false))
				.isFalse();
		Assertions.assertThat(new CursedWand.AbortRetryFail().valid(null, kit, bolt, false))
				.isFalse();
		Assertions.assertThat(new CursedWand.Petrify().valid(null, kit, bolt, false))
				.isFalse();
		Assertions.assertThat(new CursedWand.HeroShapeShift().valid(null, kit, bolt, false))
				.isFalse();
	}

	@Test
	@DisplayName("randomValidRareEffect never returns InterFloorTeleport for echo kit")
	void randomValidRareNeverPicksInterFloorForEcho() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		Hero kit = boss.getEchoHero();
		Ballistica bolt = new Ballistica(boss.pos, player.pos, Ballistica.MAGIC_BOLT);

		for (int i = 0; i < 200; i++) {
			CursedWand.CursedEffect effect = CursedWand.randomValidRareEffect(null, kit, bolt, false);
			Assertions.assertThat(effect).isNotInstanceOf(CursedWand.InterFloorTeleport.class);
			Assertions.assertThat(effect).isNotInstanceOf(CursedWand.Petrify.class);
			Assertions.assertThat(effect).isNotInstanceOf(CursedWand.HeroShapeShift.class);
		}
	}

	@Test
	@DisplayName("randomValidVeryRareEffect never returns AbortRetryFail for echo kit")
	void randomValidVeryRareNeverPicksAbortRetryForEcho() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		Hero kit = boss.getEchoHero();
		Ballistica bolt = new Ballistica(boss.pos, player.pos, Ballistica.MAGIC_BOLT);

		for (int i = 0; i < 200; i++) {
			CursedWand.CursedEffect effect = CursedWand.randomValidVeryRareEffect(null, kit, bolt, false);
			Assertions.assertThat(effect).isNotInstanceOf(CursedWand.AbortRetryFail.class);
			Assertions.assertThat(effect).isNotInstanceOf(CursedWand.HeroShapeShift.class);
		}
	}

	@Test
	@DisplayName("BurnAndFreeze self side affects EchoBoss body not kit")
	void burnAndFreezeAffectsBodyNotKit() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		Hero kit = boss.getEchoHero();
		kit.pos = boss.pos;
		Ballistica bolt = new Ballistica(boss.pos, player.pos, Ballistica.MAGIC_BOLT);

		new CursedWand.BurnAndFreeze().effect(null, kit, bolt, false);

		boolean bodyHit = boss.buff(Frost.class) != null || boss.buff(Burning.class) != null;
		boolean kitHit = kit.buff(Frost.class) != null || kit.buff(Burning.class) != null;
		Assertions.assertThat(bodyHit).isTrue();
		Assertions.assertThat(kitHit).isFalse();
	}

	@Test
	@DisplayName("HealthTransfer positive heal lands on EchoBoss body")
	void healthTransferHealsBodyNotKit() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		Hero kit = boss.getEchoHero();
		kit.pos = boss.pos;
		boss.HP = 10;
		boss.HT = 200;
		kit.HP = 10;
		kit.HT = 200;
		int playerHp = player.HP;
		Ballistica bolt = new Ballistica(boss.pos, player.pos, Ballistica.MAGIC_BOLT);

		new CursedWand.HealthTransfer().effect(null, kit, bolt, true);

		Assertions.assertThat(boss.HP).isGreaterThan(10);
		Assertions.assertThat(kit.HP).isEqualTo(10);
		Assertions.assertThat(player.HP).isLessThan(playerHp);
	}

	@Test
	@DisplayName("Levitate fallback applies to EchoBoss body")
	void levitateFallbackAffectsBody() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		Hero kit = boss.getEchoHero();
		kit.pos = boss.pos;
		player.flying = true;
		Ballistica bolt = new Ballistica(boss.pos, player.pos, Ballistica.MAGIC_BOLT);

		new CursedWand.Levitate().effect(null, kit, bolt, false);

		Assertions.assertThat(boss.buff(Levitation.class)).isNotNull();
		Assertions.assertThat(kit.buff(Levitation.class)).isNull();
	}

	@Test
	@DisplayName("GravityChaos tracker attaches to EchoBoss body")
	void gravityChaosAttachesToBody() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		Hero kit = boss.getEchoHero();
		kit.pos = boss.pos;
		Ballistica bolt = new Ballistica(boss.pos, player.pos, Ballistica.MAGIC_BOLT);

		new CursedWand.GravityChaos().effect(null, kit, bolt, false);

		Assertions.assertThat(boss.buff(GravityChaosTracker.class)).isNotNull();
		Assertions.assertThat(kit.buff(GravityChaosTracker.class)).isNull();
	}

	@Test
	@DisplayName("CurseEquipment hexes bolt target when cast by echo kit")
	void curseEquipmentHexesTargetForEcho() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		Hero kit = boss.getEchoHero();
		kit.pos = boss.pos;
		Ballistica bolt = new Ballistica(boss.pos, player.pos, Ballistica.MAGIC_BOLT);

		new CursedWand.CurseEquipment().effect(null, kit, bolt, false);

		Assertions.assertThat(player.buff(Hex.class)).isNotNull();
	}

	@Test
	@DisplayName("RandomTeleport of caster moves EchoBoss body")
	void randomTeleportMovesBodyNotOnlyKit() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		Hero kit = boss.getEchoHero();
		kit.pos = boss.pos;
		int bossPosBefore = boss.pos;
		int empty = 0;
		Assertions.assertThat(Actor.findChar(empty)).isNull();
		Ballistica bolt = new Ballistica(boss.pos, empty, Ballistica.MAGIC_BOLT);

		// Prefer self-teleport branch: no char at collision (or Random fails target).
		Random.pushGenerator(1L);
		try {
			boolean ok = new CursedWand.RandomTeleport().effect(null, kit, bolt, false);
			Assertions.assertThat(ok).isTrue();
		} finally {
			Random.popGenerator();
		}

		Assertions.assertThat(boss.pos)
				.as("caster teleport must move on-stage body")
				.isNotEqualTo(bossPosBefore);
	}

	@Test
	@DisplayName("living hero still allows Petrify")
	void livingHeroStillAllowsPetrify() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		Ballistica bolt = new Ballistica(player.pos, boss.pos, Ballistica.MAGIC_BOLT);

		Assertions.assertThat(new CursedWand.Petrify().valid(null, player, bolt, false))
				.isTrue();
		Assertions.assertThat(Dungeon.hero).isSameAs(player);
	}
}
