package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicyChoice;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicyStatus;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoRoleExecutor;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.EchoBossSprite;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.MovieClip;
import com.watabou.utils.PointF;
import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;

@ExtendWith(GdxTestExtension.class)
class EchoBossMovementTest {

	@AfterEach
	void cleanup() {
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("CLOSE_IN steps one cell closer with correct sprite place and facing")
	void closeInStepsCloserWithSpriteUpdate() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, movePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		fillFov(boss);
		TrackingSprite sprite = linkTrackingSprite(boss);
		int start = boss.pos;
		int distBefore = Dungeon.level.distance(start, hero.pos);

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				ready("CLOSE_IN"),
				new EchoPolicyChoice("CLOSE_IN", "positioning", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(Dungeon.level.distance(boss.pos, hero.pos)).isEqualTo(distBefore - 1);
		Assertions.assertThat(Dungeon.level.adjacent(start, boss.pos)).isTrue();
		Assertions.assertThat(sprite.lastPlace).isEqualTo(boss.pos);
		Assertions.assertThat(sprite.lastTurnFrom).isEqualTo(start);
		Assertions.assertThat(sprite.lastTurnTo).isEqualTo(boss.pos);
		// Hero is left of boss (center vs center+2); step closer faces left.
		Assertions.assertThat(sprite.flipHorizontal).isTrue();
	}

	@Test
	@DisplayName("KEEP_DISTANCE steps one cell further with correct sprite place and facing")
	void keepDistanceStepsFurtherWithSpriteUpdate() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, movePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);
		fillFov(boss);
		TrackingSprite sprite = linkTrackingSprite(boss);
		int start = boss.pos;
		int distBefore = Dungeon.level.distance(start, hero.pos);

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				ready("KEEP_DISTANCE"),
				new EchoPolicyChoice("KEEP_DISTANCE", "positioning", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(Dungeon.level.distance(boss.pos, hero.pos)).isGreaterThan(distBefore);
		Assertions.assertThat(Dungeon.level.adjacent(start, boss.pos)).isTrue();
		Assertions.assertThat(sprite.lastPlace).isEqualTo(boss.pos);
		Assertions.assertThat(sprite.lastTurnFrom).isEqualTo(start);
		Assertions.assertThat(sprite.lastTurnTo).isEqualTo(boss.pos);
	}

	@Test
	@DisplayName("MOVE_TO_WATER steps toward the sensed water cell with sprite place")
	void moveToWaterStepsTowardWaterCell() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, movePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		fillFov(boss);
		TrackingSprite sprite = linkTrackingSprite(boss);

		int waterCell = boss.pos + Dungeon.level.width(); // one row below boss
		Dungeon.level.map[waterCell] = Terrain.WATER;
		Dungeon.level.buildFlagMaps();
		Arrays.fill(Dungeon.level.heroFOV, true);

		int distBefore = Dungeon.level.distance(boss.pos, waterCell);
		int start = boss.pos;

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder()
						.rolesReady(Set.of("MOVE_TO_WATER"))
						.terrainNearCell("water", waterCell)
						.build(),
				new EchoPolicyChoice("MOVE_TO_WATER", "reactions", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(Dungeon.level.distance(boss.pos, waterCell)).isLessThan(distBefore);
		Assertions.assertThat(Dungeon.level.adjacent(start, boss.pos)).isTrue();
		Assertions.assertThat(sprite.lastPlace).isEqualTo(boss.pos);
	}

	@Test
	@DisplayName("MOVE_TO_GRASS steps toward the sensed grass cell with sprite place")
	void moveToGrassStepsTowardGrassCell() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, movePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		fillFov(boss);
		TrackingSprite sprite = linkTrackingSprite(boss);

		int grassCell = boss.pos - 1;
		Dungeon.level.map[grassCell] = Terrain.GRASS;
		Dungeon.level.buildFlagMaps();
		Arrays.fill(Dungeon.level.heroFOV, true);

		int distBefore = Dungeon.level.distance(boss.pos, grassCell);
		int start = boss.pos;

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				new EchoPolicyStatus.Builder()
						.rolesReady(Set.of("MOVE_TO_GRASS"))
						.terrainNearCell("grass", grassCell)
						.build(),
				new EchoPolicyChoice("MOVE_TO_GRASS", "reactions", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(Dungeon.level.distance(boss.pos, grassCell)).isLessThan(distBefore);
		Assertions.assertThat(boss.pos).isEqualTo(grassCell);
		Assertions.assertThat(Dungeon.level.adjacent(start, boss.pos)).isTrue();
		Assertions.assertThat(sprite.lastPlace).isEqualTo(boss.pos);
	}

	@Test
	@DisplayName("EchoBoss speed matches echo hero combatSpeed including Haste")
	void speedMatchesEchoHeroCombatSpeedWithHaste() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, movePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);

		Assertions.assertThat(boss.speed()).isEqualTo(boss.getEchoHero().combatSpeed());
		Assertions.assertThat(boss.speed()).isEqualTo(1f);

		Buff.affect(boss.getEchoHero(), Haste.class, Haste.DURATION);

		Assertions.assertThat(boss.speed()).isEqualTo(boss.getEchoHero().combatSpeed());
		Assertions.assertThat(boss.speed()).isEqualTo(3f);
	}

	@Test
	@DisplayName("Echo drink haste on body triples boss speed like Hero")
	void echoDrinkHasteOnBodyTriplesBossSpeed() {
		Hero player = EchoTestSupport.warriorHero();
		PotionOfHaste haste = new PotionOfHaste();
		haste.identify();
		haste.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(player, movePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Potion potion = kit.belongings.getItem(PotionOfHaste.class);
		Assertions.assertThat(potion).isNotNull();
		Assertions.assertThat(boss.speed()).isEqualTo(1f);

		potion.drinkAs(UseContext.echo(boss));

		Assertions.assertThat(boss.buff(Haste.class))
				.as("drinkAs applies self-buffs to the boss body")
				.isNotNull();
		Assertions.assertThat(boss.speed())
				.as("body Haste must affect EchoBoss.speed like Hero.speed")
				.isEqualTo(3f);
	}

	@Test
	@DisplayName("Echo kit Ring of Haste multiplies boss speed like Hero")
	void echoKitRingOfHasteMultipliesBossSpeed() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(player, movePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		RingOfHaste ring = new RingOfHaste();
		kit.belongings.ring = ring;
		ring.activate(kit);

		float expected = kit.combatSpeed();
		Assertions.assertThat(expected).isGreaterThan(1f);
		Assertions.assertThat(boss.speed()).isEqualTo(expected);
	}

	@Test
	@DisplayName("Hunting CLOSE_IN spends 1/speed and updates position and sprite")
	void huntingCloseInSpendsInverseSpeedAndUpdatesSprite() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = closeInDefaultPolicy();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		boss.state = boss.HUNTING;
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		TrackingSprite sprite = linkTrackingSprite(boss);
		Buff.affect(boss.getEchoHero(), Haste.class, Haste.DURATION);

		int start = boss.pos;
		boss.timeToNow();
		float expectedSpend = 1f / boss.speed();

		boss.act();

		Assertions.assertThat(boss.pos).isNotEqualTo(start);
		Assertions.assertThat(Dungeon.level.distance(boss.pos, hero.pos))
				.isLessThan(Dungeon.level.distance(start, hero.pos));
		Assertions.assertThat(sprite.lastPlace).isEqualTo(boss.pos);
		Assertions.assertThat(boss.cooldown()).isEqualTo(expectedSpend);
	}

	@Test
	@DisplayName("Hunting KEEP_DISTANCE at base speed spends one turn tick")
	void huntingKeepDistanceAtBaseSpeedSpendsOneTick() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoPolicy policy = keepDistanceDefaultPolicy();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, policy, 5);
		boss.state = boss.HUNTING;
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);
		TrackingSprite sprite = linkTrackingSprite(boss);

		int start = boss.pos;
		boss.timeToNow();

		boss.act();

		Assertions.assertThat(boss.pos).isNotEqualTo(start);
		Assertions.assertThat(sprite.lastPlace).isEqualTo(boss.pos);
		Assertions.assertThat(boss.speed()).isEqualTo(1f);
		Assertions.assertThat(boss.cooldown()).isEqualTo(1f);
	}

	@Test
	@DisplayName("Kiting when closer than ideal steps away with sprite and 1/speed spend")
	void kitingWhenTooCloseStepsAway() {
		Hero hero = huntressHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, kitePolicy(), 5);
		boss.state = boss.HUNTING;
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);
		placeOnRow(hero, boss, 1, 2); // distance 1, ideal 3
		TrackingSprite sprite = linkTrackingSprite(boss);

		int start = boss.pos;
		int distBefore = Dungeon.level.distance(start, hero.pos);
		boss.timeToNow();

		boss.act();

		Assertions.assertThat(Dungeon.level.distance(boss.pos, hero.pos)).isGreaterThan(distBefore);
		Assertions.assertThat(Dungeon.level.adjacent(start, boss.pos)).isTrue();
		Assertions.assertThat(sprite.lastPlace).isEqualTo(boss.pos);
		Assertions.assertThat(boss.cooldown()).isEqualTo(1f / boss.speed());
	}

	@Test
	@DisplayName("Kiting continues stepping away until ideal distance")
	void kitingContinuesUntilIdealDistance() {
		Hero hero = huntressHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, kitePolicy(), 5);
		boss.state = boss.HUNTING;
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);
		placeOnRow(hero, boss, 1, 2); // start at distance 1

		for (int i = 0; i < 3; i++) {
			int distBefore = Dungeon.level.distance(boss.pos, hero.pos);
			if (distBefore >= 3) {
				break;
			}
			linkTrackingSprite(boss);
			boss.timeToNow();
			boss.act();
			Assertions.assertThat(Dungeon.level.distance(boss.pos, hero.pos))
					.as("kite step %s should increase distance", i + 1)
					.isGreaterThan(distBefore);
		}

		Assertions.assertThat(Dungeon.level.distance(boss.pos, hero.pos)).isEqualTo(3);
	}

	@Test
	@DisplayName("Kiting at ideal distance uses RANGED without moving")
	void kitingAtIdealUsesRangedWithoutMoving() {
		Hero hero = huntressHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, kitePolicy(), 5);
		boss.state = boss.HUNTING;
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);
		placeOnRow(hero, boss, 1, 4); // distance 3
		TrackingSprite sprite = linkTrackingSprite(boss);
		boss.getEchoHero().invisible = 1;

		int start = boss.pos;
		int hpBefore = hero.HP;
		boss.timeToNow();

		boss.act();

		Assertions.assertThat(boss.pos).isEqualTo(start);
		Assertions.assertThat(sprite.lastPlace).isEqualTo(-1);
		Assertions.assertThat(hero.HP).isLessThan(hpBefore);
		Assertions.assertThat(boss.cooldown()).isEqualTo(Actor.TICK);
	}

	@Test
	@DisplayName("Kiting when farther than ideal steps closer with sprite update")
	void kitingWhenTooFarStepsCloser() {
		Hero hero = huntressHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, kitePolicy(), 5);
		boss.state = boss.HUNTING;
		EchoTestSupport.installEchoBossLevel(hero, boss, 1);
		placeOnRow(hero, boss, 1, 5); // distance 4, ideal 3
		TrackingSprite sprite = linkTrackingSprite(boss);

		int start = boss.pos;
		int distBefore = Dungeon.level.distance(start, hero.pos);
		boss.timeToNow();

		boss.act();

		Assertions.assertThat(Dungeon.level.distance(boss.pos, hero.pos)).isLessThan(distBefore);
		Assertions.assertThat(Dungeon.level.adjacent(start, boss.pos)).isTrue();
		Assertions.assertThat(sprite.lastPlace).isEqualTo(boss.pos);
		Assertions.assertThat(boss.cooldown()).isEqualTo(1f / boss.speed());
	}

	@Test
	@DisplayName("rooted EchoBoss cannot step closer")
	void rootedCannotStepCloser() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, movePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		fillFov(boss);
		linkTrackingSprite(boss);
		boss.rooted = true;
		int start = boss.pos;

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				ready("CLOSE_IN"),
				new EchoPolicyChoice("CLOSE_IN", "positioning", null));

		Assertions.assertThat(spent).isFalse();
		Assertions.assertThat(boss.pos).isEqualTo(start);
	}

	@Test
	@DisplayName("WAIT does not move and does not update movement sprite place")
	void waitDoesNotMoveOrUpdateSpritePlace() {
		Hero hero = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(hero, movePolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, boss, 2);
		TrackingSprite sprite = linkTrackingSprite(boss);
		int start = boss.pos;

		boolean spent = EchoRoleExecutor.execute(
				boss,
				boss.getEchoPolicy(),
				ready("WAIT"),
				new EchoPolicyChoice("WAIT", "default", null));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(boss.pos).isEqualTo(start);
		Assertions.assertThat(sprite.lastPlace).isEqualTo(-1);
	}

	@Test
	@DisplayName("EchoBossSprite setup uses echo hero class sheet, armor tier, and run frames")
	void echoBossSpriteSetupUsesHeroClassTierAndRun() throws Exception {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.HUNTRESS.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;

		Echo echo = Echo.create(
				5, EchoTestSupport.TEST_GAME_VERSION, 1L,
				"HUNTRESS", 6, 30, 30, EchoTestSupport.bundleHero(hero));
		EchoBoss boss = EchoTestSupport.createBoss(echo, 5);

		int tier = EchoBossSprite.armorTierFor(boss.getEchoHero(), echo);
		EchoBossSprite sprite = new EchoBossSprite();
		sprite.setup(boss.getEchoHero().heroClass, tier);

		Assertions.assertThat(boss.spriteClass).isEqualTo(EchoBossSprite.class);
		Assertions.assertThat(sprite.texture)
				.isSameAs(TextureCache.get(HeroClass.HUNTRESS.spritesheet()));
		Assertions.assertThat(tier).isEqualTo(boss.getEchoHero().tier());

		Field run = CharSprite.class.getDeclaredField("run");
		run.setAccessible(true);
		MovieClip.Animation runAnim = (MovieClip.Animation) run.get(sprite);
		Assertions.assertThat(runAnim).isNotNull();
		Assertions.assertThat(runAnim.delay).isEqualTo(1f / 20f);
	}

	private static EchoPolicyStatus ready(String role) {
		return new EchoPolicyStatus.Builder().rolesReady(Set.of(role)).build();
	}

	private static void fillFov(EchoBoss boss) {
		boss.fieldOfView = new boolean[Dungeon.level.length()];
		Arrays.fill(boss.fieldOfView, true);
	}

	private static TrackingSprite linkTrackingSprite(EchoBoss boss) {
		TrackingSprite sprite = new TrackingSprite();
		sprite.ch = boss;
		boss.sprite = sprite;
		return sprite;
	}

	private static EchoPolicy movePolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("CLOSE_IN", EchoTestSupport.capability("*move_closer"))
				.put("KEEP_DISTANCE", EchoTestSupport.capability("*move_further"))
				.put("MOVE_TO_WATER", EchoTestSupport.capability("*move_to_terrain:water"))
				.put("MOVE_TO_GRASS", EchoTestSupport.capability("*move_to_terrain:grass"))
				.put("WAIT", EchoTestSupport.capability("*wait"))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static EchoPolicy closeInDefaultPolicy() {
		JSONObject root = new JSONObject(movePolicy().root().toString());
		root.put("selection", new JSONObject()
				.put("order", new JSONArray().put("default"))
				.put("default_roles", new JSONArray().put("CLOSE_IN")));
		return EchoPolicy.fromJson(root);
	}

	private static EchoPolicy keepDistanceDefaultPolicy() {
		JSONObject root = new JSONObject(movePolicy().root().toString());
		root.put("selection", new JSONObject()
				.put("order", new JSONArray().put("default"))
				.put("default_roles", new JSONArray().put("KEEP_DISTANCE")));
		return EchoPolicy.fromJson(root);
	}

	/**
	 * Huntress-style kite: ideal 3, step out when closer, close in when farther,
	 * shoot at ideal.
	 */
	private static EchoPolicy kitePolicy() {
		return EchoPolicy.fromJson(new JSONObject()
				.put("policy_schema_version", EchoTestSupport.TEST_GAME_VERSION)
				.put("capabilities", new JSONObject()
						.put("KEEP_DISTANCE", EchoTestSupport.capability("*move_further"))
						.put("CLOSE_IN", EchoTestSupport.capability("*move_closer"))
						.put("RANGED", new JSONObject()
								.put("pick", "MAX_DAMAGE")
								.put("items", new JSONArray().put("SpiritBow")))
						.put("MELEE", EchoTestSupport.capability("*melee"))
						.put("WAIT", EchoTestSupport.capability("*wait")))
				.put("reactions", new JSONArray())
				.put("recipes", new JSONArray())
				.put("positioning", new JSONObject()
						.put("HUNTRESS", new JSONObject()
								.put("ideal_distance", 3)
								.put("if_closer", "KEEP_DISTANCE")
								.put("if_farther", "CLOSE_IN")))
				.put("matchups", new JSONObject())
				.put("selection", new JSONObject()
						.put("order", new JSONArray()
								.put("reactions").put("recipes").put("positioning")
								.put("matchups").put("default"))
						.put("default_roles", new JSONArray().put("RANGED").put("MELEE").put("WAIT")))
				.put("tuning", new JSONObject()));
	}

	private static Hero huntressHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.HUNTRESS.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}

	/** Places hero/boss on row y=3 at the given x cells (7×7 fixture). */
	private static void placeOnRow(Hero hero, EchoBoss boss, int heroX, int bossX) {
		int y = 3;
		hero.pos = y * Dungeon.level.width() + heroX;
		boss.pos = y * Dungeon.level.width() + bossX;
	}

	/** Records place/turn/move without GameScene or Camera. */
	private static final class TrackingSprite extends CharSprite {
		int lastPlace = -1;
		int lastTurnFrom = -1;
		int lastTurnTo = -1;

		@Override
		public void place(int cell) {
			lastPlace = cell;
		}

		@Override
		public void turnTo(int from, int to) {
			lastTurnFrom = from;
			lastTurnTo = to;
			super.turnTo(from, to);
		}

		@Override
		public void move(int from, int to) {
			turnTo(from, to);
			place(to);
		}

		@Override
		public void showAlert() {
		}

		@Override
		public void hideAlert() {
		}

		@Override
		public void hideLost() {
		}

		@Override
		public void hideInvestigate() {
		}

		@Override
		public void bloodBurstA(PointF from, int damage) {
		}

		@Override
		public void flash() {
		}

		@Override
		public void showStatus(int color, String text, Object... args) {
		}
	}
}
