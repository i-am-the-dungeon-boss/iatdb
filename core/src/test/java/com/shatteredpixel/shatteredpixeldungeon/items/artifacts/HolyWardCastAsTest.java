package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyIntuition;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard;
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
class HolyWardCastAsTest {

	@AfterEach
	void cleanup() {
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("Hero castAs applies HolyArmBuff on the hero and spends charge")
	void heroCastAsBuffsHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.CLERIC.initHero(hero);
		hero.lvl = 6;
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		HolyTome tome = (HolyTome) hero.belongings.artifact;
		tome.directCharge(5f);
		String chargeBefore = tome.status();
		float before = hero.cooldown();

		// Headless tests have no sprite parent group; hide VFX that needs it.
		hero.sprite.visible = false;

		boolean ok = tome.castAs(UseContext.hero(hero), HolyWard.INSTANCE, null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(hero.buff(HolyWard.HolyArmBuff.class)).isNotNull();
		Assertions.assertThat(tome.status()).isNotEqualTo(chargeBefore);
		Assertions.assertThat(hero.cooldown()).isGreaterThanOrEqualTo(before);
	}

	@Test
	@DisplayName("Echo castAs applies HolyArmBuff on the boss body not the kit")
	void echoCastAsBuffsBossBody() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		HolyTome tome = clericKitTome(boss);
		String chargeBefore = tome.status();

		boolean ok = tome.castAs(UseContext.echo(boss), HolyWard.INSTANCE, null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(boss.buff(HolyWard.HolyArmBuff.class)).isNotNull();
		Assertions.assertThat(boss.getEchoHero().buff(HolyWard.HolyArmBuff.class)).isNull();
		Assertions.assertThat(tome.status()).isNotEqualTo(chargeBefore);
	}

	@Test
	@DisplayName("Echo castAs HolyIntuition reveals curse on an unknown kit weapon")
	void echoHolyIntuitionRevealsCurse() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		HolyTome tome = clericKitTome(boss);
		Hero kit = boss.getEchoHero();
		kit.upgradeTalent(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.HOLY_INTUITION);
		com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword sword = new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword();
		sword.cursed = true;
		kit.belongings.weapon = sword;
		Assertions.assertThat(sword.cursedKnown).isFalse();
		String chargeBefore = tome.status();

		boolean ok = tome.castAs(UseContext.echo(boss), HolyIntuition.INSTANCE, null);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(sword.cursedKnown).isTrue();
		Assertions.assertThat(tome.status()).isNotEqualTo(chargeBefore);
	}

	private static HolyTome clericKitTome(EchoBoss boss) {
		Hero previous = Dungeon.hero;
		Hero kit = boss.getEchoHero();
		Dungeon.hero = kit;
		HeroClass.CLERIC.initHero(kit);
		kit.lvl = 6;
		Dungeon.hero = previous;
		HolyTome tome = (HolyTome) kit.belongings.artifact;
		tome.directCharge(5f);
		return tome;
	}
}
