package com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Snake;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoClericAdapter;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoClericUnsupportedRefuseTest {

	@Test
	@DisplayName("Echo cleric dispatcher rejects unsupported Wall of Light without spending")
	void rejectsWallOfLightWithoutSpending() {
		Fight f = fight();
		grantTalent(f.kit, Talent.WALL_OF_LIGHT, 1);
		String chargeBefore = f.tome.status();

		boolean ok = EchoClericAdapter.cast(f.boss, f.tome, WallOfLight.INSTANCE, f.player.pos);

		Assertions.assertThat(ok).isFalse();
		Assertions.assertThat(f.tome.status()).isEqualTo(chargeBefore);
	}

	@Test
	@DisplayName("Echo cleric dispatcher rejects unsupported Stasis without spending")
	void rejectsStasisWithoutSpending() {
		Fight f = fight();
		grantTalent(f.kit, Talent.STASIS, 1);
		Buff.affect(f.kit, Stasis.StasisBuff.class);
		String chargeBefore = f.tome.status();

		boolean ok = EchoClericAdapter.cast(f.boss, f.tome, Stasis.INSTANCE, null);

		Assertions.assertThat(ok).isFalse();
		Assertions.assertThat(f.tome.status()).isEqualTo(chargeBefore);
	}

	@Test
	@DisplayName("Echo cleric dispatcher rejects unsupported Beaming Ray without spending")
	void rejectsBeamingRayWithoutSpending() {
		Fight f = fight();
		grantTalent(f.kit, Talent.BEAMING_RAY, 1);
		Snake ally = new Snake();
		ally.pos = f.boss.pos + 1;
		EchoTestSupport.linkStubSprite(ally);
		Dungeon.level.mobs.add(ally);
		com.shatteredpixel.shatteredpixeldungeon.actors.Actor.add(ally);
		Buff.affect(ally, PowerOfMany.PowerBuff.class);
		String chargeBefore = f.tome.status();

		boolean ok = EchoClericAdapter.cast(f.boss, f.tome, BeamingRay.INSTANCE, f.player.pos);

		Assertions.assertThat(ok).isFalse();
		Assertions.assertThat(f.tome.status()).isEqualTo(chargeBefore);
	}

	@Test
	@DisplayName("Echo cleric dispatcher rejects a missing target before charging the Tome")
	void rejectsMissingTargetBeforeChargingTome() {
		Fight f = fight();
		String chargeBefore = f.tome.status();

		boolean ok = EchoClericAdapter.cast(f.boss, f.tome, GuidingLight.INSTANCE, null);

		Assertions.assertThat(ok).isFalse();
		Assertions.assertThat(f.tome.status()).isEqualTo(chargeBefore);
	}

	private static Fight fight() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		HolyTome tome = clericKitTome(boss);
		return new Fight(player, boss, boss.getEchoHero(), tome);
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

	private static void grantTalent(Hero hero, Talent talent, int points) {
		for (int i = 0; i < points; i++) {
			hero.upgradeTalent(talent);
		}
	}

	private static final class Fight {
		final Hero player;
		final EchoBoss boss;
		final Hero kit;
		final HolyTome tome;

		Fight(Hero player, EchoBoss boss, Hero kit, HolyTome tome) {
			this.player = player;
			this.boss = boss;
			this.kit = kit;
			this.tome = tome;
		}
	}
}
