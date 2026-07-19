package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.CompositeEchoLookup;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EchoBossSpawnerTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("shouldSpawn is false when no pending echo is resolved")
	void shouldSpawnFalseWithoutPendingEcho() {
		Assertions.assertThat(EchoBossSpawner.shouldSpawn()).isFalse();
	}

	@Test
	@DisplayName("shouldSpawn is true after echo boss is prepared for depth")
	void shouldSpawnTrueWhenEchoBossPrepared() {
		EchoStorage storage = new EchoStorage();
		storage.save(EchoTestSupport.warriorEchoWithData(10));
		CompositeEchoLookup.setEchoLookupForTests(storage);

		Dungeon.depth = 10;
		Dungeon.prefetchEchoBossForDepth(10);

		Assertions.assertThat(EchoBossSpawner.shouldSpawn()).isTrue();
	}

	@Test
	@DisplayName("createRegionalBoss returns echo boss when spawn is pending")
	void createRegionalBossReturnsEchoBoss() {
		Echo echo = EchoTestSupport.warriorEchoWithData(10);
		EchoStorage storage = new EchoStorage();
		storage.save(echo);
		CompositeEchoLookup.setEchoLookupForTests(storage);
		Dungeon.depth = 10;
		Dungeon.prefetchEchoBossForDepth(10);

		Mob result = EchoBossSpawner.createRegionalBoss(new Goo());

		Assertions.assertThat(result).isInstanceOf(EchoBoss.class);
		Assertions.assertThat(((EchoBoss) result).getEcho().echoId).isEqualTo(echo.echoId);
		Assertions.assertThat(result.state).isEqualTo(result.SLEEPING);
	}

	@Test
	@DisplayName("shouldSpawn is false when pending echo lacks combat data")
	void shouldSpawnFalseWithoutCombatData() {
		EchoStorage storage = new EchoStorage();
		storage.save(EchoTestSupport.warriorEcho(10));
		CompositeEchoLookup.setEchoLookupForTests(storage);

		Dungeon.depth = 10;
		Dungeon.prefetchEchoBossForDepth(10);

		Assertions.assertThat(EchoBossSpawner.shouldSpawn()).isFalse();
	}

	@Test
	@DisplayName("createRegionalBoss returns default boss when spawn is not pending")
	void createRegionalBossReturnsDefaultBoss() {
		Goo defaultBoss = new Goo();

		Mob result = EchoBossSpawner.createRegionalBoss(defaultBoss);

		Assertions.assertThat(result).isSameAs(defaultBoss);
	}
}
