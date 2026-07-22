package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(GdxTestExtension.class)
class GameSceneBossSlainSubtitleTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
	}

	@ParameterizedTest
	@CsvSource({
			"5, You are now the Sewer Boss (Depth 5)",
			"10, You are now the Prison Boss (Depth 10)",
			"15, You are now the Caves Boss (Depth 15)",
			"20, You are now the City Boss (Depth 20)",
			"25, You are now the Halls Boss (Depth 25)"
	})
	@DisplayName("boss slain subtitle names the claimed region boss")
	void bossSlainSubtitleNamesClaimedRegionBoss(int depth, String expected) {
		Assertions.assertThat(GameScene.floorBossClaimMessage(depth)).isEqualTo(expected);
	}

	@Test
	@DisplayName("boss slain subtitle is shown when echo capture applies")
	void bossSlainSubtitleShownWhenEchoCaptureApplies() {
		Assertions.assertThat(GameScene.showsFloorBossClaim(5, true)).isTrue();
	}

	@Test
	@DisplayName("boss slain subtitle is hidden when hero is dead")
	void bossSlainSubtitleHiddenWhenHeroDead() {
		Assertions.assertThat(GameScene.showsFloorBossClaim(5, false)).isFalse();
	}

	@Test
	@DisplayName("boss slain subtitle is hidden on non-boss depths")
	void bossSlainSubtitleHiddenOnNonBossDepths() {
		Assertions.assertThat(GameScene.showsFloorBossClaim(4, true)).isFalse();
	}

	@Test
	@DisplayName("bossSlain does not measure claim text on the actor thread")
	void bossSlainDoesNotMeasureClaimTextOnActorThread() {
		Hero hero = EchoTestSupport.warriorHero();
		Dungeon.depth = 10;
		Assertions.assertThat(GameScene.showsFloorBossClaim(Dungeon.depth, hero.isAlive())).isTrue();

		String priorName = Thread.currentThread().getName();
		Thread.currentThread().setName("SHPD Actor Thread");
		try {
			Assertions.assertThatCode(GameScene::bossSlain)
					.doesNotThrowAnyException();
		} finally {
			Thread.currentThread().setName(priorName);
		}
	}
}
