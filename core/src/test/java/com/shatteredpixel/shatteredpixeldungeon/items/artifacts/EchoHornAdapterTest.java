package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoHornAdapter;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoHornAdapterTest {

	@Test
	@DisplayName("Echo horn adapter snacks on the boss body Hunger and spends charge")
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

		boolean ok = EchoHornAdapter.snack(boss, horn);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(horn.charge).isEqualTo(chargeBefore - 1);
		Assertions.assertThat(hunger.hunger()).isLessThan(hungerBefore);
	}
}
