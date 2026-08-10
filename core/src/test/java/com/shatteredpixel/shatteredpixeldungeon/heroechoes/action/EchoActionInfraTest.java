package com.shatteredpixel.shatteredpixeldungeon.heroechoes.action;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.WarriorArmor;
import com.watabou.utils.Callback;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoActionInfraTest {

	@Test
	@DisplayName("EchoActionContext rejects a boss without a phantom kit")
	void echoActionContextRejectsBossWithoutPhantomKit() {
		EchoBoss boss = new EchoBoss();
		Assertions.assertThatThrownBy(() -> EchoActionContext.of(boss))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("phantom kit");
	}

	@Test
	@DisplayName("EchoKitBorrow restores kit position and sprite after synchronous failure")
	void echoKitBorrowRestoresAfterSynchronousFailure() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		EchoActionContext ctx = EchoActionContext.of(boss);
		Hero kit = ctx.kit;
		int kitPos = kit.pos;
		Assertions.assertThat(kit.sprite).isNull();

		Assertions.assertThatThrownBy(() -> EchoKitBorrow.run(ctx, () -> {
			Assertions.assertThat(kit.pos).isEqualTo(boss.pos);
			Assertions.assertThat(kit.sprite).isSameAs(boss.sprite);
			throw new IllegalStateException("sync fail");
		})).isInstanceOf(IllegalStateException.class);

		Assertions.assertThat(kit.pos).isEqualTo(kitPos);
		Assertions.assertThat(kit.sprite).isNull();
	}

	@Test
	@DisplayName("EchoKitBorrow restores kit position and sprite after deferred completion")
	void echoKitBorrowRestoresAfterDeferredCompletion() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		EchoActionContext ctx = EchoActionContext.of(boss);
		Hero kit = ctx.kit;
		int kitPos = kit.pos;

		Callback[] deferred = new Callback[1];
		EchoKitBorrow.runDeferred(ctx, after -> deferred[0] = after, () -> {
			Assertions.assertThat(kit.pos).isEqualTo(boss.pos);
			Assertions.assertThat(kit.sprite).isSameAs(boss.sprite);
		});

		Assertions.assertThat(kit.pos).isEqualTo(boss.pos);
		Assertions.assertThat(kit.sprite).isSameAs(boss.sprite);

		deferred[0].call();

		Assertions.assertThat(kit.pos).isEqualTo(kitPos);
		Assertions.assertThat(kit.sprite).isNull();
	}

	@Test
	@DisplayName("Echo action refusal clears boss busy state without spending a turn")
	void echoActionRefusalClearsBossBusyWithoutSpendingTurn() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		EchoActionContext ctx = EchoActionContext.of(boss);
		float before = boss.cooldown();

		ctx.busy();
		Assertions.assertThat(boss.isBusy()).isTrue();

		boolean ok = EchoActionSupport.refuse(ctx);

		Assertions.assertThat(ok).isFalse();
		Assertions.assertThat(boss.isBusy()).isFalse();
		Assertions.assertThat(boss.cooldown()).isEqualTo(before);
	}

	@Test
	@DisplayName("Echo action statuses affect body while resource trackers remain on kit")
	void echoActionStatusesAffectBodyWhileResourceTrackersRemainOnKit() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 1);
		Hero kit = boss.getEchoHero();
		WarriorArmor armor = new WarriorArmor();
		armor.charge = 100f;
		float chargeBefore = armor.charge;

		boolean ok = EchoArmorAbilityAdapter.activate(boss, armor, new Endure(), null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(boss.buff(Endure.EndureTracker.class)).isNotNull();
		Assertions.assertThat(kit.buff(Endure.EndureTracker.class)).isNull();
		Assertions.assertThat(armor.charge).isEqualTo(chargeBefore - 50f);
	}
}
