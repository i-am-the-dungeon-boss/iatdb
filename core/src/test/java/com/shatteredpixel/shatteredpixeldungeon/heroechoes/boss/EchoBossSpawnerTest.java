package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoStorage;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.CompositeEchoLookup;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.HashSet;

@ExtendWith(GdxTestExtension.class)
class EchoBossSpawnerTest {

	@AfterEach
	void cleanup() {
		BossHealthBar.assignBoss(null);
	}

	@Test
	@DisplayName("shouldSpawn is false when no pending echo is resolved")
	void shouldSpawnFalseWithoutPendingEcho() {
		Assertions.assertThat(EchoBossSpawner.shouldSpawn()).isFalse();
	}

	@Test
	@DisplayName("shouldSpawnDecision explains false when no pending echo")
	void shouldSpawnDecisionExplainsNoPendingEcho() {
		Dungeon.depth = 10;

		Assertions.assertThat(EchoBossSpawner.shouldSpawnDecision())
				.isEqualTo("shouldSpawn=false depth=10 reason=no_pending_echo");
	}

	@Test
	@DisplayName("shouldSpawnDecision explains false when pending depth mismatches")
	void shouldSpawnDecisionExplainsDepthMismatch() {
		Dungeon.armPendingEchoBoss(
				EchoTestSupport.warriorEchoWithData(10),
				EchoTestSupport.roleBasedPolicy());
		Dungeon.depth = 15;

		Assertions.assertThat(EchoBossSpawner.shouldSpawnDecision())
				.isEqualTo("shouldSpawn=false depth=15 reason=pending_depth_mismatch pending=10");
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
		Assertions.assertThat(EchoBossSpawner.shouldSpawnDecision())
				.isEqualTo("shouldSpawn=true depth=10");
	}

	@Test
	@DisplayName("create returns echo boss when spawn is pending")
	void createReturnsEchoBossWhenPending() {
		Echo echo = EchoTestSupport.warriorEchoWithData(10);
		EchoStorage storage = new EchoStorage();
		storage.save(echo);
		CompositeEchoLookup.setEchoLookupForTests(storage);
		Dungeon.depth = 10;
		Dungeon.prefetchEchoBossForDepth(10);

		EchoBoss result = EchoBossSpawner.create(Dungeon.depth);

		Assertions.assertThat(result.getEcho().echoId).isEqualTo(echo.echoId);
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
	@DisplayName("present assigns boss bar when sprite is missing")
	void presentAssignsBossBarWhenSpriteMissing() {
		Level level = stubLevel();
		Dungeon.level = level;
		Goo boss = new Goo();
		boss.pos = 1;
		Assertions.assertThat(boss.sprite).isNull();

		EchoBossSpawner.present(boss);

		Assertions.assertThat(level.mobs).contains(boss);
		Assertions.assertThat(BossHealthBar.isAssigned()).isTrue();
	}

	@Test
	@DisplayName("present notices when sprite is present")
	void presentNoticesWhenSpritePresent() {
		Level level = stubLevel();
		level.locked = true;
		Dungeon.level = level;
		EchoBoss boss = EchoTestSupport.createBoss(EchoTestSupport.warriorEchoWithData(10), 10);
		boss.pos = 1;
		level.mobs.add(boss);
		EchoTestSupport.linkStubSprite(boss);
		level.mobs.remove(boss);

		EchoBossSpawner.present(boss);

		Assertions.assertThat(level.mobs).contains(boss);
		Assertions.assertThat(BossHealthBar.isAssigned()).isTrue();
	}

	@Test
	@DisplayName("present without notice does not assign bar when sprite is present")
	void presentWithoutNoticeSkipsNoticeWhenSpritePresent() {
		Level level = stubLevel();
		level.locked = true;
		Dungeon.level = level;
		EchoBoss boss = EchoTestSupport.createBoss(EchoTestSupport.warriorEchoWithData(10), 10);
		boss.pos = 1;
		level.mobs.add(boss);
		EchoTestSupport.linkStubSprite(boss);
		level.mobs.remove(boss);

		EchoBossSpawner.present(boss, false);

		Assertions.assertThat(level.mobs).contains(boss);
		Assertions.assertThat(BossHealthBar.isAssigned()).isFalse();
	}

	@Test
	@DisplayName("present without notice still assigns bar when headless")
	void presentWithoutNoticeAssignsBarWhenHeadless() {
		Level level = stubLevel();
		Dungeon.level = level;
		Goo boss = new Goo();
		boss.pos = 1;

		EchoBossSpawner.present(boss, false);

		Assertions.assertThat(level.mobs).contains(boss);
		Assertions.assertThat(BossHealthBar.isAssigned()).isTrue();
	}

	private static Level stubLevel() {
		Level level = new Level() {
			@Override
			public String tilesTex() {
				return null;
			}

			@Override
			public String waterTex() {
				return null;
			}

			@Override
			protected boolean build() {
				return true;
			}

			@Override
			protected void createMobs() {
			}

			@Override
			protected void createItems() {
			}
		};
		level.setSize(7, 7);
		java.util.Arrays.fill(level.map, Terrain.EMPTY);
		level.mobs = new HashSet<>();
		level.heaps = new com.watabou.utils.SparseArray<>();
		level.blobs = new HashMap<>();
		level.plants = new com.watabou.utils.SparseArray<>();
		level.traps = new com.watabou.utils.SparseArray<>();
		return level;
	}
}
