package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss.armor;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpectralBlades;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBossTurnAssert;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.HuntressArmor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

/**
 * Echo {@link SpectralBlades} via
 * {@link com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility#activateAs}.
 */
@ExtendWith(GdxTestExtension.class)
class EchoArmorAbilitySpectralBladesTest {

	private static Fight fight() {
		Hero player = huntressHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		boss.fieldOfView = new boolean[Dungeon.level.length()];
		Arrays.fill(boss.fieldOfView, true);
		Dungeon.level.heroFOV = boss.fieldOfView;
		return new Fight(player, boss);
	}

	private static Hero huntressHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.HUNTRESS.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}

	private static final class Fight {
		final Hero player;
		final EchoBoss boss;

		Fight(Hero player, EchoBoss boss) {
			this.player = player;
			this.boss = boss;
		}

		UseContext echo() {
			return UseContext.echo(boss);
		}
	}

	@Test
	@DisplayName("Echo SpectralBlades activateAs damages the player from the boss body")
	void damagesPlayer() {
		Fight f = fight();
		EchoTestSupport.InstantProjectileGroup fx = EchoTestSupport.attachInstantProjectileParent(f.boss);
		HuntressArmor armor = new HuntressArmor();
		armor.charge = 100;
		f.player.belongings.armor = null;
		int hpBefore = f.player.HP;
		f.boss.getEchoHero().invisible = 1;
		f.boss.getEchoHero().STR = 20;

		boolean ok = new SpectralBlades().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(fx.missileSpriteRecycles).isGreaterThan(0);
		Assertions.assertThat(f.player.HP).isLessThan(hpBefore);
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo SpectralBlades activateAs spends ClassArmor charge from the kit")
	void spendsCharge() {
		Fight f = fight();
		EchoTestSupport.attachInstantProjectileParent(f.boss);
		HuntressArmor armor = new HuntressArmor();
		armor.charge = 100;
		float chargeBefore = armor.charge;
		f.boss.getEchoHero().invisible = 1;
		f.boss.getEchoHero().STR = 20;

		boolean ok = new SpectralBlades().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isLessThan(chargeBefore);
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo SpectralBlades activateAs clears busy so the boss turn can resume")
	void clearsBusy() {
		Fight f = fight();
		EchoTestSupport.attachInstantProjectileParent(f.boss);
		HuntressArmor armor = new HuntressArmor();
		armor.charge = 100;
		f.player.belongings.armor = null;
		f.boss.getEchoHero().invisible = 1;
		f.boss.getEchoHero().STR = 20;

		boolean ok = new SpectralBlades().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo SpectralBlades self-target refuse clears busy for next turn")
	void refuseSelfTargetClearsBusy() {
		Fight f = fight();
		HuntressArmor armor = new HuntressArmor();
		armor.charge = 100;
		float chargeBefore = armor.charge;

		boolean ok = new SpectralBlades().activateAs(f.echo(), armor, f.boss.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(armor.charge).isEqualTo(chargeBefore);
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo SpectralBlades leaves phantom kit sprite null after activateAs")
	void leavesKitSpriteNull() {
		Fight f = fight();
		EchoTestSupport.attachInstantProjectileParent(f.boss);
		HuntressArmor armor = new HuntressArmor();
		armor.charge = 100;
		f.player.belongings.armor = null;
		f.boss.getEchoHero().invisible = 1;
		f.boss.getEchoHero().STR = 20;
		Assertions.assertThat(f.boss.getEchoHero().sprite).isNull();

		boolean ok = new SpectralBlades().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(f.boss.getEchoHero().sprite).isNull();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}
}
