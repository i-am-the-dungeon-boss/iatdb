package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Drowsy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfLullaby;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

/** Canvas-aligned Echo scroll kinds not already covered by ScrollReadAsTest. */
@ExtendWith(GdxTestExtension.class)
class EchoBossArsenalScrollKindsTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	private static Fight fight() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		boss.fieldOfView = new boolean[Dungeon.level.length()];
		Arrays.fill(boss.fieldOfView, true);
		Arrays.fill(Dungeon.level.heroFOV, true);
		return new Fight(player, boss);
	}

	private static final class Fight {
		final Hero player;
		final EchoBoss boss;

		Fight(Hero player, EchoBoss boss) {
			this.player = player;
			this.boss = boss;
		}

		Hero kit() {
			return boss.getEchoHero();
		}

		UseContext echo() {
			return UseContext.echo(boss);
		}
	}

	@Test
	@DisplayName("Echo Lullaby readAs applies Drowsy on the Hero and boss body")
	void lullabyAppliesDrowsyOnHero() {
		Fight f = fight();
		ScrollOfLullaby scroll = new ScrollOfLullaby();
		scroll.identify();
		scroll.collect(f.kit().belongings.backpack);

		Assertions.assertThat(scroll.readAs(f.echo())).isTrue();
		Assertions.assertThat(f.player.buff(Drowsy.class)).isNotNull();
		Assertions.assertThat(f.boss.buff(Drowsy.class)).isNotNull();
		Assertions.assertThat(f.kit().belongings.getItem(ScrollOfLullaby.class)).isNull();
	}

	@Test
	@DisplayName("Echo Retribution readAs damages and blinds the Hero")
	void retributionDamagesHero() {
		Fight f = fight();
		f.boss.HP = Math.max(1, f.boss.HT / 4);
		ScrollOfRetribution scroll = new ScrollOfRetribution();
		scroll.identify();
		scroll.collect(f.kit().belongings.backpack);
		f.player.invisible = 1;
		int hpBefore = f.player.HP;

		Assertions.assertThat(scroll.readAs(f.echo())).isTrue();
		Assertions.assertThat(f.player.HP).isLessThan(hpBefore);
		Assertions.assertThat(f.player.buff(Blindness.class)).isNotNull();
		Assertions.assertThat(f.boss.buff(Weakness.class)).isNotNull();
		Assertions.assertThat(f.kit().belongings.getItem(ScrollOfRetribution.class)).isNull();
	}

	@Test
	@DisplayName("Echo Mapping readAs spends the scroll Char-safe")
	void mappingSpendsScroll() {
		Fight f = fight();
		ScrollOfMagicMapping scroll = new ScrollOfMagicMapping();
		scroll.identify();
		scroll.collect(f.kit().belongings.backpack);

		Assertions.assertThat(scroll.readAs(f.echo())).isTrue();
		Assertions.assertThat(f.kit().belongings.getItem(ScrollOfMagicMapping.class)).isNull();
	}

	@Test
	@DisplayName("Echo Psionic Blast readAs damages and blinds the Hero")
	void psionicBlastDamagesHero() {
		Fight f = fight();
		ScrollOfPsionicBlast scroll = new ScrollOfPsionicBlast();
		scroll.identify();
		scroll.collect(f.kit().belongings.backpack);
		f.player.invisible = 1;
		int hpBefore = f.player.HP;

		Assertions.assertThat(scroll.readAs(f.echo())).isTrue();
		Assertions.assertThat(f.player.HP).isLessThan(hpBefore);
		Assertions.assertThat(f.player.buff(Blindness.class)).isNotNull();
		Assertions.assertThat(f.boss.buff(Weakness.class)).isNotNull();
		Assertions.assertThat(f.kit().belongings.getItem(ScrollOfPsionicBlast.class)).isNull();
	}
}
