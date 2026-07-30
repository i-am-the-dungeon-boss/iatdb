package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invulnerability;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoBossAnkhTest {

	@Test
	@DisplayName("blessed ankh revives EchoBoss at quarter HP with invulnerability")
	void blessedAnkhRevivesWithInvulnerability() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoPolicy.fallback(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		giveAnkh(boss, true);
		Buff.affect(boss, Poison.class).set(4);
		boss.HP = 0;

		boss.die(hero);

		Assertions.assertThat(boss.isAlive()).isTrue();
		Assertions.assertThat(boss.HP).isEqualTo(boss.HT / 4);
		Assertions.assertThat(boss.buff(Invulnerability.class)).isNotNull();
		Assertions.assertThat(boss.buff(Poison.class)).isNull();
		Assertions.assertThat(boss.getEchoHero().belongings.getAllItems(Ankh.class)).isEmpty();
	}

	@Test
	@DisplayName("unblessed ankh revives EchoBoss at quarter HP without invulnerability")
	void unblessedAnkhRevivesWithoutInvulnerability() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoPolicy.fallback(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		giveAnkh(boss, false);
		boss.HP = 0;

		boss.die(hero);

		Assertions.assertThat(boss.isAlive()).isTrue();
		Assertions.assertThat(boss.HP).isEqualTo(boss.HT / 4);
		Assertions.assertThat(boss.buff(Invulnerability.class)).isNull();
		Assertions.assertThat(boss.getEchoHero().belongings.getAllItems(Ankh.class)).isEmpty();
	}

	@Test
	@DisplayName("EchoBoss prefers blessed ankh when both are present")
	void prefersBlessedAnkhWhenBothPresent() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoPolicy.fallback(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		giveAnkh(boss, false);
		giveAnkh(boss, true);
		boss.HP = 0;

		boss.die(hero);

		Assertions.assertThat(boss.isAlive()).isTrue();
		Assertions.assertThat(boss.buff(Invulnerability.class)).isNotNull();
		java.util.List<Ankh> remaining = boss.getEchoHero().belongings.getAllItems(Ankh.class);
		Assertions.assertThat(remaining).hasSize(1);
		Assertions.assertThat(remaining.get(0).isBlessed()).isFalse();
	}

	@Test
	@DisplayName("EchoBoss dies normally when kit has no ankh")
	void diesNormallyWithoutAnkh() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, EchoPolicy.fallback(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		boss.HP = 0;

		boss.die(hero);

		Assertions.assertThat(boss.isAlive()).isFalse();
	}

	private static void giveAnkh(EchoBoss boss, boolean blessed) {
		Ankh ankh = new Ankh();
		if (blessed) {
			ankh.bless();
		}
		boss.getEchoHero().belongings.backpack.items.add(ankh);
	}
}
