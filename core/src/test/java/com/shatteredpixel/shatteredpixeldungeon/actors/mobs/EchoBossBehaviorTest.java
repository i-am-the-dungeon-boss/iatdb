package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combo;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(GdxTestExtension.class)
class EchoBossBehaviorTest {

	@AfterEach
	void cleanup() {
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("notice does not reseal when the floor is already locked")
	void noticeDoesNotResealWhenAlreadyLocked() {
		AtomicInteger sealCalls = new AtomicInteger();
		Level level = countingSealLevel(sealCalls);
		level.locked = true;
		Dungeon.level = level;

		EchoBoss boss = EchoTestSupport.createBoss(EchoTestSupport.warriorEchoWithData(15), 15);
		level.mobs.add(boss);
		EchoTestSupport.linkStubSprite(boss);

		boss.notice();

		Assertions.assertThat(sealCalls.get()).isZero();
	}

	@Test
	@DisplayName("notice seals unlocked floors for sewer-style echo fights")
	void noticeSealsWhenFloorUnlocked() {
		AtomicInteger sealCalls = new AtomicInteger();
		Level level = countingSealLevel(sealCalls);
		level.locked = false;
		Dungeon.level = level;

		EchoBoss boss = EchoTestSupport.createBoss(EchoTestSupport.warriorEchoWithData(5), 5);
		level.mobs.add(boss);
		EchoTestSupport.linkStubSprite(boss);

		boss.notice();

		Assertions.assertThat(sealCalls.get()).isEqualTo(1);
		Assertions.assertThat(level.locked).isTrue();
	}

	private static Level countingSealLevel(AtomicInteger sealCalls) {
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

			@Override
			public void seal() {
				sealCalls.incrementAndGet();
				super.seal();
			}
		};
		level.setSize(7, 7);
		Arrays.fill(level.map, Terrain.EMPTY);
		level.mobs = new HashSet<>();
		level.heaps = new com.watabou.utils.SparseArray<>();
		level.blobs = new HashMap<>();
		level.plants = new com.watabou.utils.SparseArray<>();
		level.traps = new com.watabou.utils.SparseArray<>();
		return level;
	}

	@Test
	@DisplayName("EchoBoss hunting act does not apply armor ability side effects")
	void huntingActDoesNotApplyArmorAbilitySideEffects() {
		Hero hero = EchoTestSupport.warriorHero();
		hero.armorAbility = new HeroicLeap();
		EchoPolicy waitPolicy = EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("WAIT", EchoTestSupport.capability("*wait")));
		// Override default_roles to WAIT so policy spends the turn.
		JSONObject root = new JSONObject(waitPolicy.root().toString());
		root.put("selection", new JSONObject()
				.put("order", new org.json.JSONArray().put("default"))
				.put("default_roles", new org.json.JSONArray().put("WAIT")));
		waitPolicy = EchoPolicy.fromJson(root);

		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, waitPolicy, 5);
		boss.state = boss.HUNTING;
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);
		Assertions.assertThat(boss.getEchoHero().armorAbility).isNotNull();

		boss.act();

		Assertions.assertThat(boss.buff(Combo.class)).isNull();
	}

	@Test
	@DisplayName("EchoBoss does not use custom AI while sleeping")
	void doesNotUseCustomAiWhileSleeping() {
		Hero hero = EchoTestSupport.warriorHero();
		PotionOfHealing potion = new PotionOfHealing();
		potion.identify();
		potion.collect(hero.belongings.backpack);
		Echo echo = Echo.create(
				5, EchoTestSupport.TEST_GAME_VERSION, 1L,
				"WARRIOR", 6, 8, 30, EchoTestSupport.bundleHero(hero));

		EchoBoss boss = EchoTestSupport.createBoss(echo, 5);
		boss.state = boss.SLEEPING;
		boss.HP = 8;
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);

		boss.act();

		Assertions.assertThat(boss.getEchoHero().belongings.getItem(PotionOfHealing.class)).isNotNull();
	}
}
