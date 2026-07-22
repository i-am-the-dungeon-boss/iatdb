package com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class ClericSpellCastAsTest {

	@AfterEach
	void cleanup() {
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("Echo Sunray castAs damages the player from boss body aim")
	void echoSunrayDamagesPlayer() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		HolyTome tome = clericKitTome(boss);
		Hero kit = boss.getEchoHero();
		kit.upgradeTalent(Talent.SUNRAY);

		boss.fieldOfView = new boolean[Dungeon.level.length()];
		java.util.Arrays.fill(boss.fieldOfView, true);
		player.invisible = 1;
		int hpBefore = player.HP;

		boolean ok = tome.castAs(UseContext.echo(boss), Sunray.INSTANCE, player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(player.HP).isLessThan(hpBefore);
	}

	@Test
	@DisplayName("Echo GuidingLight castAs damages the player from boss body aim")
	void echoGuidingLightDamagesPlayer() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		HolyTome tome = clericKitTome(boss);
		player.invisible = 1;
		int hpBefore = player.HP;

		boolean ok = tome.castAs(UseContext.echo(boss), GuidingLight.INSTANCE, player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(player.HP).isLessThan(hpBefore);
	}

	@Test
	@DisplayName("Echo GuidingLight castAs fires MagicMissile when the body sprite has a parent")
	void echoGuidingLightFiresMagicMissileWhenSpriteHasParent() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		EchoTestSupport.InstantProjectileGroup fx = EchoTestSupport.attachInstantProjectileParent(boss);

		HolyTome tome = clericKitTome(boss);
		player.invisible = 1;

		boolean ok = tome.castAs(UseContext.echo(boss), GuidingLight.INSTANCE, player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(fx.magicMissileRecycles).isGreaterThan(0);
	}

	@Test
	@DisplayName("Echo BodyForm castAs applies BodyFormBuff from kit weapon enchantment")
	void echoBodyFormAppliesEnchantBuff() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		HolyTome tome = clericKitTome(boss);
		Hero kit = boss.getEchoHero();
		kit.armorAbility = new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity();
		Talent.initArmorTalents(kit);
		kit.upgradeTalent(Talent.BODY_FORM);
		com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword sword = new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword();
		sword.enchant(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing());
		kit.belongings.weapon = sword;
		String chargeBefore = tome.status();

		boolean ok = tome.castAs(UseContext.echo(boss), BodyForm.INSTANCE, null);

		Assertions.assertThat(ok).isTrue();
		BodyForm.BodyFormBuff buff = boss.buff(BodyForm.BodyFormBuff.class);
		Assertions.assertThat(buff).isNotNull();
		Assertions.assertThat(buff.enchant()).isInstanceOf(
				com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing.class);
		Assertions.assertThat(kit.buff(BodyForm.BodyFormBuff.class)).isNull();
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
		tome.directCharge(10f);
		return tome;
	}
}
