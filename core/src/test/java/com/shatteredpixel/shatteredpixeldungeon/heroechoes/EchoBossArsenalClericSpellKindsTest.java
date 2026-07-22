package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bless;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.AuraOfProtection;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BlessSpell;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Cleanse;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.DivineSense;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Flash;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HallowedGround;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Judgement;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.LayOnHands;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.MnemonicPrayer;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Radiance;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ShieldOfLight;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Smite;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.PathFinder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

/**
 * Canvas-aligned Echo cleric spell kinds not already covered by
 * ClericSpellCastAsTest.
 */
@ExtendWith(GdxTestExtension.class)
class EchoBossArsenalClericSpellKindsTest {

	@AfterEach
	void cleanup() {
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	private static Fight fight() {
		return fight(1, HeroSubClass.PALADIN);
	}

	private static Fight fightPriest(int bossOffset) {
		return fight(bossOffset, HeroSubClass.PRIEST);
	}

	private static Fight fight(int bossOffset, HeroSubClass subClass) {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, bossOffset);
		HolyTome tome = clericKitTome(boss, subClass);
		boss.fieldOfView = new boolean[Dungeon.level.length()];
		Arrays.fill(boss.fieldOfView, true);
		return new Fight(player, boss, tome);
	}

	private static HolyTome clericKitTome(EchoBoss boss, HeroSubClass subClass) {
		Hero previous = Dungeon.hero;
		Hero kit = boss.getEchoHero();
		Dungeon.hero = kit;
		HeroClass.CLERIC.initHero(kit);
		kit.lvl = 20;
		kit.subClass = subClass;
		Talent.initSubclassTalents(kit);
		kit.armorAbility = new AscendedForm();
		Talent.initArmorTalents(kit);
		Dungeon.hero = previous;
		HolyTome tome = (HolyTome) kit.belongings.artifact;
		tome.directCharge(30f);
		return tome;
	}

	private static void takeTalent(Hero kit, Talent talent, int points) {
		for (int i = 0; i < points; i++) {
			kit.upgradeTalent(talent);
		}
	}

	private static final class Fight {
		final Hero player;
		final EchoBoss boss;
		final HolyTome tome;

		Fight(Hero player, EchoBoss boss, HolyTome tome) {
			this.player = player;
			this.boss = boss;
			this.tome = tome;
		}

		Hero kit() {
			return boss.getEchoHero();
		}

		UseContext echo() {
			return UseContext.echo(boss);
		}
	}

	@Test
	@DisplayName("Echo Smite castAs damages the player from boss body aim")
	void smiteDamagesPlayer() {
		Fight f = fight();
		f.player.invisible = 1;
		int hpBefore = f.player.HP;

		Assertions.assertThat(f.tome.castAs(f.echo(), Smite.INSTANCE, f.player.pos)).isTrue();
		Assertions.assertThat(f.player.HP).isLessThan(hpBefore);
	}

	@Test
	@DisplayName("Echo Radiance castAs illuminates and paralyzes the Hero")
	void radianceHitsHero() {
		Fight f = fight();
		f.kit().subClass = HeroSubClass.PRIEST;

		Assertions.assertThat(f.tome.castAs(f.echo(), Radiance.INSTANCE, null)).isTrue();
		Assertions.assertThat(f.player.buff(
				com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight.Illuminated.class))
				.isNotNull();
		Assertions.assertThat(f.player.buff(
				com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis.class)).isNotNull();
	}

	@Test
	@DisplayName("Echo Lay On Hands castAs heals the boss body")
	void layOnHandsHealsBody() {
		Fight f = fight();
		takeTalent(f.kit(), Talent.LAY_ON_HANDS, 1);
		f.boss.HP = Math.max(1, f.boss.HT / 4);
		int hpBefore = f.boss.HP;

		Assertions.assertThat(f.tome.castAs(f.echo(), LayOnHands.INSTANCE, f.boss.pos)).isTrue();
		int shield = f.boss.buff(Barrier.class) != null ? f.boss.buff(Barrier.class).shielding() : 0;
		Assertions.assertThat(f.boss.HP + shield).isGreaterThan(hpBefore);
	}

	@Test
	@DisplayName("Echo Bless castAs applies Bless on the boss body")
	void blessBuffsBody() {
		Fight f = fight();
		takeTalent(f.kit(), Talent.BLESS, 1);

		Assertions.assertThat(f.tome.castAs(f.echo(), BlessSpell.INSTANCE, f.boss.pos)).isTrue();
		Assertions.assertThat(f.boss.buff(Bless.class)).isNotNull();
		Assertions.assertThat(f.kit().buff(Bless.class)).isNull();
	}

	@Test
	@DisplayName("Echo Cleanse castAs strips a debuff from the boss body")
	void cleanseStripsBodyDebuff() {
		Fight f = fight();
		takeTalent(f.kit(), Talent.CLEANSE, 1);
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(f.boss, Burning.class);

		Assertions.assertThat(f.tome.castAs(f.echo(), Cleanse.INSTANCE, null)).isTrue();
		Assertions.assertThat(f.boss.buff(Burning.class)).isNull();
	}

	@Test
	@DisplayName("Echo Divine Sense castAs spends tome charge on the kit")
	void divineSenseSpendsCharge() {
		Fight f = fight();
		takeTalent(f.kit(), Talent.DIVINE_SENSE, 1);
		String before = f.tome.status();

		Assertions.assertThat(f.tome.castAs(f.echo(), DivineSense.INSTANCE, null)).isTrue();
		Assertions.assertThat(f.tome.status()).isNotEqualTo(before);
	}

	@Test
	@DisplayName("Echo Shield of Light castAs marks the player from boss body")
	void shieldOfLightMarksPlayer() {
		Fight f = fight();
		takeTalent(f.kit(), Talent.SHIELD_OF_LIGHT, 1);

		Assertions.assertThat(f.tome.castAs(f.echo(), ShieldOfLight.INSTANCE, f.player.pos)).isTrue();
		Assertions.assertThat(f.boss.buff(ShieldOfLight.ShieldOfLightTracker.class)).isNotNull();
	}

	@Test
	@DisplayName("Echo Hallowed Ground castAs spends charge aiming near the player")
	void hallowedGroundSpendsCharge() {
		Fight f = fightPriest(1);
		takeTalent(f.kit(), Talent.HALLOWED_GROUND, 1);
		String before = f.tome.status();

		Assertions.assertThat(f.tome.castAs(f.echo(), HallowedGround.INSTANCE, f.player.pos)).isTrue();
		Assertions.assertThat(f.tome.status()).isNotEqualTo(before);
	}

	@Test
	@DisplayName("Echo Mnemonic Prayer castAs illuminates an enemy body from boss aim")
	void mnemonicPrayerIlluminatesEnemy() {
		Fight f = fightPriest(1);
		takeTalent(f.kit(), Talent.MNEMONIC_PRAYER, 1);

		Assertions.assertThat(f.tome.castAs(f.echo(), MnemonicPrayer.INSTANCE, f.player.pos)).isTrue();
		Assertions.assertThat(f.player.buff(
				com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight.Illuminated.class))
				.isNotNull();
	}

	@Test
	@DisplayName("Echo Aura of Protection castAs applies aura buff on the boss body")
	void auraOfProtectionBuffsBody() {
		Fight f = fight();
		takeTalent(f.kit(), Talent.AURA_OF_PROTECTION, 1);

		Assertions.assertThat(f.tome.castAs(f.echo(), AuraOfProtection.INSTANCE, null)).isTrue();
		Assertions.assertThat(f.boss.buff(AuraOfProtection.AuraBuff.class)).isNotNull();
	}

	@Test
	@DisplayName("Echo Judgement castAs damages the player while Ascended")
	void judgementDamagesPlayer() {
		Fight f = fight();
		takeTalent(f.kit(), Talent.JUDGEMENT, 1);
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(f.kit(), AscendedForm.AscendBuff.class);
		f.player.invisible = 1;
		int hpBefore = f.player.HP;

		Assertions.assertThat(f.tome.castAs(f.echo(), Judgement.INSTANCE, null)).isTrue();
		Assertions.assertThat(f.player.HP).isLessThan(hpBefore);
	}

	@Test
	@DisplayName("Echo Flash castAs teleports the boss body to a mapped empty cell")
	void flashTeleportsBossBody() {
		Fight f = fight();
		takeTalent(f.kit(), Talent.FLASH, 1);
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(f.kit(), AscendedForm.AscendBuff.class);
		int dest = -1;
		for (int n : PathFinder.NEIGHBOURS8) {
			int cell = f.boss.pos + n;
			if (cell >= 0 && cell < Dungeon.level.length()
					&& Dungeon.level.passable[cell]
					&& cell != f.player.pos
					&& com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(cell) == null) {
				dest = cell;
				break;
			}
		}
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);
		Dungeon.level.mapped[dest] = true;
		Dungeon.level.visited[dest] = true;
		Dungeon.level.map[dest] = Terrain.EMPTY;
		int start = f.boss.pos;

		Assertions.assertThat(f.tome.castAs(f.echo(), Flash.INSTANCE, dest)).isTrue();
		Assertions.assertThat(f.boss.pos).isEqualTo(dest);
		Assertions.assertThat(f.boss.pos).isNotEqualTo(start);
	}
}
