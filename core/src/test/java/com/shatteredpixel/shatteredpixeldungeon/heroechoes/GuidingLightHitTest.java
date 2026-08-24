package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class GuidingLightHitTest {

	private static EchoBoss clericEcho() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(player, EchoPolicy.fallback(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		boss.getEchoHero().heroClass = HeroClass.CLERIC;
		return boss;
	}

	@Test
	@DisplayName("an echo whose kit is a cleric gets the Illuminated free hit")
	void echoClericKitIsAFreeHit() {
		EchoBoss boss = clericEcho();

		Assertions.assertThat(GuidingLightHit.isClericFreeHit(boss)).isTrue();
		Assertions.assertThat(GuidingLightHit.isClericFreeHit(boss.getEchoHero())).isTrue();
	}

	@Test
	@DisplayName("an echo whose kit is not a cleric does not")
	void nonClericEchoIsNotAFreeHit() {
		EchoBoss boss = clericEcho();
		boss.getEchoHero().heroClass = HeroClass.WARRIOR;

		Assertions.assertThat(GuidingLightHit.isClericFreeHit(boss)).isFalse();
	}

	@Test
	@DisplayName("a cleric swinging a weapon over their STR loses the free hit")
	void overEncumberedClericIsNotAFreeHit() {
		EchoBoss boss = clericEcho();
		Hero kit = boss.getEchoHero();
		Greatsword heavy = new Greatsword();
		kit.belongings.weapon = heavy;
		kit.STR = heavy.STRReq() - 1;

		Assertions.assertThat(GuidingLightHit.isClericFreeHit(boss)).isFalse();

		kit.STR = heavy.STRReq();
		Assertions.assertThat(GuidingLightHit.isClericFreeHit(boss)).isTrue();
	}

	@Test
	@DisplayName("the player hero counts as the cleric when they are one")
	void playerClericIsAFreeHit() {
		Hero player = EchoTestSupport.warriorHero();
		Dungeon.hero = player;

		Assertions.assertThat(GuidingLightHit.isClericFreeHit(player)).isFalse();
		player.heroClass = HeroClass.CLERIC;
		Assertions.assertThat(GuidingLightHit.isClericFreeHit(player)).isTrue();
	}

	@Test
	@DisplayName("allies of a cleric player keep the base-game free hit")
	void alliesOfAClericPlayerKeepTheFreeHit() {
		EchoBoss boss = clericEcho();
		Hero player = Dungeon.hero;
		player.heroClass = HeroClass.CLERIC;

		Assertions.assertThat(GuidingLightHit.isClericAlly(new Rat())).isTrue();

		player.heroClass = HeroClass.WARRIOR;
		Assertions.assertThat(GuidingLightHit.isClericAlly(new Rat())).isFalse();
		// A cleric attacker is never "an ally" — it is the cleric.
		Assertions.assertThat(GuidingLightHit.isClericAlly(boss)).isFalse();
	}

	@Test
	@DisplayName("an Illuminated echo body is a guaranteed hit for the cleric player")
	void illuminatedEchoBodyCannotEvadeTheCleric() {
		EchoBoss boss = clericEcho();
		boss.getEchoHero().heroClass = HeroClass.WARRIOR;
		Hero player = Dungeon.hero;
		player.heroClass = HeroClass.CLERIC;

		Assertions.assertThat(boss.defenseSkill(player))
				.as("without Guiding Light the echo still evades")
				.isGreaterThan(0);

		Buff.affect(boss, GuidingLight.Illuminated.class);

		Assertions.assertThat(boss.defenseSkill(player))
				.as("Illuminated lands on the body, so the body must answer 0")
				.isZero();
	}

	@Test
	@DisplayName("null and non-hero attackers are handled")
	void nullAttackerIsNotAFreeHit() {
		Assertions.assertThat(GuidingLightHit.isClericFreeHit(null)).isFalse();
	}
}
