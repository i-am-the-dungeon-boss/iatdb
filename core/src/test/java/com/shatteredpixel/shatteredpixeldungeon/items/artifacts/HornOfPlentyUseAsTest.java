package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class HornOfPlentyUseAsTest {

	@AfterEach
	void cleanup() {
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("Echo useAs snacks on the boss body Hunger and spends charge")
	void echoSnackSatisfiesBossHunger() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		HornOfPlenty horn = new HornOfPlenty();
		horn.charge = 3;
		kit.belongings.artifact = horn;
		horn.activate(kit);

		Hunger hunger = boss.buff(Hunger.class);
		if (hunger == null) {
			hunger = new Hunger();
			hunger.attachTo(boss);
		}
		hunger.satisfy(-Hunger.STARVING);
		int hungerBefore = hunger.hunger();
		int chargeBefore = horn.charge;

		boolean ok = horn.useAs(UseContext.echo(boss));

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(horn.charge).isEqualTo(chargeBefore - 1);
		Assertions.assertThat(hunger.hunger()).isLessThan(hungerBefore);
	}
}
