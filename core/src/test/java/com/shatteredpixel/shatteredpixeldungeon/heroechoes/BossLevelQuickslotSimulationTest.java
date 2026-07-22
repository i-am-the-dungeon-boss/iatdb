package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.DebugSettings;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.QuickSlot;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.CompositeEchoLookup;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoLookupOutcome;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicyInput;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.levels.CavesBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.HallsBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.PrisonBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Simulates boss-floor start with regional boss and with EchoBoss. Echo restore
 * can
 * mutate shared {@link Dungeon#quickslot} via item bundle {@code quickslotpos}.
 */
@ExtendWith(GdxTestExtension.class)
class BossLevelQuickslotSimulationTest {

	@BeforeEach
	@AfterEach
	void cleanup() {
		Dungeon.quickslot.reset();
		QuickSlotButton.reset();
		ActionIndicator.clearAction();
		Statistics.qualifiedForBossChallengeBadge = false;
		EchoTestSupport.resetWorkflowState();
		Assertions.assertThat(DebugSettings.isDebugBuild()).isFalse();
		Assertions.assertThat(DebugSettings.weakEchoSnapshots()).isFalse();
		Assertions.assertThat(DebugSettings.debugStart()).isFalse();
	}

	@Test
	@DisplayName("city regional DwarfKing seal keeps the player's quickslots")
	void cityRegionalBossPreservesPlayerQuickslots() {
		PlayerBar bar = installPlayerWithQuickslots();
		armNoEcho(20);

		CityBossLevel level = createCityBossLevel();
		level.seal();

		Assertions.assertThat(findMob(level, DwarfKing.class)).isNotNull();
		Assertions.assertThat(findMob(level, EchoBoss.class)).isNull();
		assertPlayerQuickslotsUnchanged(bar);
	}

	@Test
	@DisplayName("city EchoBoss seal keeps the player's quickslots")
	void cityEchoBossPreservesPlayerQuickslots() {
		Echo echo = echoHeroWithQuickslottedKit(20);
		PlayerBar bar = installPlayerWithQuickslots();
		armPendingEcho(echo, 20);

		CityBossLevel level = createCityBossLevel();
		level.seal();

		Assertions.assertThat(findMob(level, EchoBoss.class)).isNotNull();
		Assertions.assertThat(findMob(level, DwarfKing.class)).isNull();
		assertPlayerQuickslotsUnchanged(bar);
	}

	@Test
	@DisplayName("caves regional DM300 seal keeps the player's quickslots")
	void cavesRegionalBossPreservesPlayerQuickslots() {
		PlayerBar bar = installPlayerWithQuickslots();
		armNoEcho(15);

		CavesBossLevel level = createCavesBossLevel();
		level.seal();

		Assertions.assertThat(findMob(level, DM300.class)).isNotNull();
		Assertions.assertThat(findMob(level, EchoBoss.class)).isNull();
		assertPlayerQuickslotsUnchanged(bar);
	}

	@Test
	@DisplayName("caves EchoBoss seal keeps the player's quickslots")
	void cavesEchoBossPreservesPlayerQuickslots() {
		Echo echo = echoHeroWithQuickslottedKit(15);
		PlayerBar bar = installPlayerWithQuickslots();
		armPendingEcho(echo, 15);

		CavesBossLevel level = createCavesBossLevel();
		level.seal();

		Assertions.assertThat(findMob(level, EchoBoss.class)).isNotNull();
		Assertions.assertThat(findMob(level, DM300.class)).isNull();
		assertPlayerQuickslotsUnchanged(bar);
	}

	@Test
	@DisplayName("sewer regional Goo create keeps the player's quickslots")
	void sewerRegionalBossPreservesPlayerQuickslots() {
		PlayerBar bar = installPlayerWithQuickslots();
		armNoEcho(5);

		SewerBossLevel level = new SewerBossLevel();
		level.create();
		Dungeon.level = level;

		Assertions.assertThat(findMob(level, Goo.class)).isNotNull();
		Assertions.assertThat(findMob(level, EchoBoss.class)).isNull();
		assertPlayerQuickslotsUnchanged(bar);
	}

	@Test
	@DisplayName("sewer EchoBoss create keeps the player's quickslots")
	void sewerEchoBossPreservesPlayerQuickslots() {
		Echo echo = echoHeroWithQuickslottedKit(5);
		PlayerBar bar = installPlayerWithQuickslots();
		armPendingEcho(echo, 5);

		SewerBossLevel level = new SewerBossLevel();
		level.create();
		Dungeon.level = level;

		Assertions.assertThat(findMob(level, EchoBoss.class)).isNotNull();
		Assertions.assertThat(findMob(level, Goo.class)).isNull();
		assertPlayerQuickslotsUnchanged(bar);
	}

	@Test
	@DisplayName("prison regional Tengu start keeps the player's quickslots")
	void prisonRegionalBossPreservesPlayerQuickslots() {
		PlayerBar bar = installPlayerWithQuickslots();
		armNoEcho(10);

		PrisonBossLevel level = createPrisonBossLevel();
		level.progress();

		Assertions.assertThat(findMob(level, Tengu.class)).isNotNull();
		Assertions.assertThat(findMob(level, EchoBoss.class)).isNull();
		assertPlayerQuickslotsUnchanged(bar);
	}

	@Test
	@DisplayName("prison EchoBoss start keeps the player's quickslots")
	void prisonEchoBossPreservesPlayerQuickslots() {
		Echo echo = echoHeroWithQuickslottedKit(10);
		PlayerBar bar = installPlayerWithQuickslots();
		armPendingEcho(echo, 10);

		PrisonBossLevel level = createPrisonBossLevel();
		level.progress();

		Assertions.assertThat(findMob(level, EchoBoss.class)).isNotNull();
		Assertions.assertThat(findMob(level, Tengu.class)).isNull();
		assertPlayerQuickslotsUnchanged(bar);
	}

	@Test
	@DisplayName("halls regional Yog seal keeps the player's quickslots")
	void hallsRegionalBossPreservesPlayerQuickslots() {
		PlayerBar bar = installPlayerWithQuickslots();
		armNoEcho(25);

		HallsBossLevel level = createHallsBossLevel();
		level.seal();

		Assertions.assertThat(findMob(level, YogDzewa.class)).isNotNull();
		Assertions.assertThat(findMob(level, EchoBoss.class)).isNull();
		assertPlayerQuickslotsUnchanged(bar);
	}

	@Test
	@DisplayName("halls EchoBoss seal keeps the player's quickslots")
	void hallsEchoBossPreservesPlayerQuickslots() {
		Echo echo = echoHeroWithQuickslottedKit(25);
		PlayerBar bar = installPlayerWithQuickslots();
		armPendingEcho(echo, 25);

		HallsBossLevel level = createHallsBossLevel();
		level.seal();

		Assertions.assertThat(findMob(level, EchoBoss.class)).isNotNull();
		Assertions.assertThat(findMob(level, YogDzewa.class)).isNull();
		assertPlayerQuickslotsUnchanged(bar);
	}

	@Test
	@DisplayName("policy input fromEcho keeps the player's quickslots")
	void policyInputFromEchoPreservesPlayerQuickslots() {
		Echo echo = echoHeroWithQuickslottedKit(10);
		PlayerBar bar = installPlayerWithQuickslots();

		EchoPolicyInput.fromEcho(echo);

		assertPlayerQuickslotsUnchanged(bar);
	}

	@Test
	@DisplayName("weak-snapshot debug weaken keeps the player's quickslots")
	void weakSnapshotWeakenPreservesPlayerQuickslots() {
		Echo echo = echoHeroWithQuickslottedKit(5);
		PlayerBar bar = installPlayerWithQuickslots();

		EchoSnapshotDebug.weaken(echo);

		assertPlayerQuickslotsUnchanged(bar);
	}

	private static CityBossLevel createCityBossLevel() {
		CityBossLevel level = new CityBossLevel();
		level.create();
		Dungeon.level = level;
		Dungeon.hero.pos = CityBossLevel.throne;
		level.heroFOV = new boolean[level.length()];
		return level;
	}

	private static CavesBossLevel createCavesBossLevel() {
		CavesBossLevel level = new CavesBossLevel();
		level.create();
		Dungeon.level = level;
		Dungeon.hero.pos = level.pointToCell(CavesBossLevel.mainArena.center());
		return level;
	}

	private static PrisonBossLevel createPrisonBossLevel() {
		PrisonBossLevel level = new PrisonBossLevel();
		level.create();
		Dungeon.level = level;
		Dungeon.hero.pos = level.entrance();
		level.heroFOV = new boolean[level.length()];
		return level;
	}

	private static HallsBossLevel createHallsBossLevel() {
		HallsBossLevel level = new HallsBossLevel();
		level.create();
		Dungeon.level = level;
		Dungeon.hero.pos = level.entrance();
		level.heroFOV = new boolean[level.length()];
		return level;
	}

	private static PlayerBar installPlayerWithQuickslots() {
		Hero player = EchoTestSupport.warriorHero();
		PotionOfHealing potion = new PotionOfHealing();
		potion.identify();
		ScrollOfMagicMapping scroll = new ScrollOfMagicMapping();
		scroll.identify();
		player.belongings.backpack.items.add(potion);
		player.belongings.backpack.items.add(scroll);
		Dungeon.quickslot.reset();
		Dungeon.quickslot.setSlot(0, potion);
		Dungeon.quickslot.setSlot(1, player.belongings.weapon);
		Dungeon.quickslot.setSlot(2, scroll);
		return snapshotBar();
	}

	private static void armPendingEcho(Echo echo, int depth) {
		CompositeEchoLookup.setEchoLookupForTests(d -> EchoTestSupport.outcomeWithPolicy(echo));
		Dungeon.depth = depth;
		Dungeon.seed = 1L;
		Assertions.assertThat(Dungeon.prefetchEchoBossForDepth(depth)).isTrue();
		Assertions.assertThat(EchoBossSpawner.shouldSpawn()).isTrue();
	}

	private static void armNoEcho(int depth) {
		CompositeEchoLookup.setEchoLookupForTests(d -> EchoLookupOutcome.notFound());
		Dungeon.depth = depth;
		Dungeon.seed = 1L;
		Assertions.assertThat(Dungeon.prefetchEchoBossForDepth(depth)).isFalse();
		Assertions.assertThat(EchoBossSpawner.shouldSpawn()).isFalse();
	}

	/**
	 * Builds an echo whose kit was quickslotted when captured — restore will try to
	 * write those positions into {@link Dungeon#quickslot}.
	 */
	private static Echo echoHeroWithQuickslottedKit(int depth) {
		Hero previousHero = Dungeon.hero;
		Item[] previousSlots = new Item[QuickSlot.SIZE];
		for (int i = 0; i < QuickSlot.SIZE; i++) {
			previousSlots[i] = Dungeon.quickslot.getItem(i);
		}

		Hero echoSource = new Hero();
		Dungeon.hero = echoSource;
		HeroClass.MAGE.initHero(echoSource);
		echoSource.lvl = 12;
		echoSource.HP = echoSource.HT = 60;

		PotionOfStrength echoPotion = new PotionOfStrength();
		echoPotion.identify();
		echoSource.belongings.backpack.items.add(echoPotion);
		WornShortsword echoSword = new WornShortsword();
		echoSword.identify();
		echoSource.belongings.weapon = echoSword;

		Dungeon.quickslot.reset();
		Dungeon.quickslot.setSlot(0, echoPotion);
		Dungeon.quickslot.setSlot(1, echoSword);
		Dungeon.quickslot.setSlot(3, echoSource.belongings.armor);

		Echo echo = Echo.fromHero(echoSource, depth, EchoTestSupport.TEST_GAME_VERSION, 99L);

		Dungeon.hero = previousHero;
		Dungeon.quickslot.reset();
		for (int i = 0; i < QuickSlot.SIZE; i++) {
			if (previousSlots[i] != null) {
				Dungeon.quickslot.setSlot(i, previousSlots[i]);
			}
		}
		return echo;
	}

	private static PlayerBar snapshotBar() {
		Item[] slots = new Item[QuickSlot.SIZE];
		for (int i = 0; i < QuickSlot.SIZE; i++) {
			slots[i] = Dungeon.quickslot.getItem(i);
		}
		return new PlayerBar(slots, Dungeon.hero);
	}

	private static void assertPlayerQuickslotsUnchanged(PlayerBar bar) {
		Assertions.assertThat(Dungeon.hero).isSameAs(bar.player);
		for (int i = 0; i < QuickSlot.SIZE; i++) {
			Assertions.assertThat(Dungeon.quickslot.getItem(i))
					.as("quickslot %d", i)
					.isSameAs(bar.slots[i]);
		}
	}

	private static <T extends Mob> T findMob(Level level, Class<T> type) {
		for (Mob mob : level.mobs) {
			if (type.isInstance(mob)) {
				return type.cast(mob);
			}
		}
		return null;
	}

	private static final class PlayerBar {
		final Item[] slots;
		final Hero player;

		PlayerBar(Item[] slots, Hero player) {
			this.slots = slots;
			this.player = player;
		}
	}
}
