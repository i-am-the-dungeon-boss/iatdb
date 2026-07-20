package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.CompositeEchoLookup;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor;
import com.watabou.utils.Bundle;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoBossBundlePersistenceTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("EchoBoss level save restores the same echo after pending echo is cleared")
	void levelSaveRestoresSameEchoWhenPendingCleared() {
		Echo saved = mageEchoWithPlate(5);
		EchoBoss original = EchoTestSupport.createBoss(saved, 5);
		original.HP = 40;

		Bundle bundle = new Bundle();
		original.storeInBundle(bundle);

		EchoTestSupport.resetWorkflowState();
		Dungeon.depth = 5;

		EchoBoss restored = new EchoBoss();
		restored.restoreFromBundle(bundle);

		Assertions.assertThat(restored.getEcho()).isNotNull();
		Assertions.assertThat(restored.getEcho().echoId).isEqualTo(saved.echoId);
		Assertions.assertThat(restored.getEchoHero().heroClass).isEqualTo(HeroClass.MAGE);
		Assertions.assertThat(restored.getEchoHero().belongings.armor()).isInstanceOf(PlateArmor.class);
		Assertions.assertThat(restored.HP).isEqualTo(40);
	}

	@Test
	@DisplayName("EchoBoss level save prefers stored echo over a mismatched pending echo")
	void levelSavePrefersStoredEchoOverMismatchedPending() {
		Echo saved = mageEchoWithPlate(5);
		EchoBoss original = EchoTestSupport.createBoss(saved, 5);

		Bundle bundle = new Bundle();
		original.storeInBundle(bundle);

		Echo wrongPending = EchoTestSupport.warriorEchoWithData(5);
		wrongPending.echoId = "wrong-pending-warrior";
		CompositeEchoLookup.setEchoLookupForTests(depth -> EchoTestSupport.outcomeWithPolicy(wrongPending));
		Dungeon.prefetchEchoBossForDepth(5);
		Assertions.assertThat(Dungeon.getPendingEcho().echoId).isEqualTo(wrongPending.echoId);

		EchoBoss restored = new EchoBoss();
		restored.restoreFromBundle(bundle);

		Assertions.assertThat(restored.getEcho().echoId).isEqualTo(saved.echoId);
		Assertions.assertThat(restored.getEchoHero().heroClass).isEqualTo(HeroClass.MAGE);
		Assertions.assertThat(restored.getEcho().echoId).isNotEqualTo(wrongPending.echoId);
	}

	@Test
	@DisplayName("EchoBoss level save restores pending policy with the echo")
	void levelSaveRestoresEchoPolicy() {
		Echo saved = mageEchoWithPlate(5);
		EchoPolicy policy = EchoTestSupport.roleBasedPolicy();
		CompositeEchoLookup.setEchoLookupForTests(
				depth -> EchoTestSupport.outcomeWithPolicy(saved, policy));
		Dungeon.prefetchEchoBossForDepth(5);
		Assertions.assertThat(Dungeon.getPendingEchoPolicy().root().toString())
				.isEqualTo(policy.root().toString());

		EchoBoss original = new EchoBoss(saved, 5);
		Bundle bundle = new Bundle();
		original.storeInBundle(bundle);

		EchoTestSupport.resetWorkflowState();
		Dungeon.depth = 5;

		EchoBoss restored = new EchoBoss();
		restored.restoreFromBundle(bundle);

		Assertions.assertThat(restored.getEcho().echoId).isEqualTo(saved.echoId);
		Assertions.assertThat(restored.getEchoPolicy()).isNotNull();
		Assertions.assertThat(restored.getEchoPolicy().isSupported()).isTrue();
		Assertions.assertThat(restored.getEchoPolicy().root().toString())
				.isEqualTo(policy.root().toString());
		Assertions.assertThat(restored.getEchoPolicy().root().getJSONObject("capabilities")
				.getJSONObject("RANGED").getJSONArray("items").getString(0))
				.isEqualTo("MagesStaff");
	}

	private static Echo mageEchoWithPlate(int depth) {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.MAGE.initHero(hero);
		hero.lvl = 14;
		hero.HP = 65;
		hero.HT = 70;
		PlateArmor armor = new PlateArmor();
		armor.identify();
		hero.belongings.armor = armor;
		Echo echo = Echo.fromHero(hero, depth, EchoTestSupport.TEST_GAME_VERSION, 1L);
		echo.echoId = "mage-plate-" + depth;
		return echo;
	}
}
