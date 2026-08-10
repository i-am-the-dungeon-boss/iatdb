package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class ComboRiposteNullSpriteTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
	}

	@Test
	@DisplayName("RiposteTracker acts without NPE when combo target sprite is null")
	void riposteTrackerActsWhenTargetSpriteNull() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 1);

		Hero kit = boss.getEchoHero();
		kit.heroClass = HeroClass.WARRIOR;
		kit.subClass = HeroSubClass.GLADIATOR;
		kit.pos = boss.pos;
		Assertions.assertThat(kit.sprite).isNull();

		Combo combo = Buff.affect(kit, Combo.class);
		combo.hit(player);
		combo.hit(player);

		Combo.RiposteTracker riposte = Buff.append(kit, Combo.RiposteTracker.class);
		riposte.enemy = player;

		Assertions.assertThatCode(riposte::act).doesNotThrowAnyException();
		Assertions.assertThat(kit.buff(Combo.RiposteTracker.class)).isNull();
	}
}
