package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.windows.EchoBackpackFormatter;
import com.shatteredpixel.shatteredpixeldungeon.windows.EchoDetailsFormatter;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndEchoDetail;
import com.watabou.utils.Bundle;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

@ExtendWith(GdxTestExtension.class)
class EchoViewerTest {

	@AfterEach
	void cleanup() {
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("loadAll returns empty list when echoes folder is empty")
	void loadAllEmptyWhenNoSnapshots() {
		Assertions.assertThat(new EchoStorage().loadAll()).isEmpty();
	}

	@Test
	@DisplayName("loadAll stores one echo per boss depth")
	void loadAllStoresOneEchoPerDepth() {
		EchoStorage storage = new EchoStorage();
		storage.save(EchoTestSupport.warriorEcho(5));

		List<EchoStorage.EchoEntry> entries = storage.loadAll();

		Assertions.assertThat(entries).hasSize(1);
		Assertions.assertThat(entries.get(0).filename()).isEqualTo("depth-5.dat");
	}

	@Test
	@DisplayName("loadAll keeps only the newest echo for a depth")
	void loadAllKeepsNewestEchoPerDepth() {
		EchoStorage storage = new EchoStorage();

		Echo older = EchoTestSupport.warriorEcho(5);
		older.echoId = "older";
		older.timestamp = 1_000L;
		storage.save(older);

		Echo newer = EchoTestSupport.warriorEcho(5);
		newer.echoId = "newer";
		newer.timestamp = 9_000L;
		storage.save(newer);

		List<EchoStorage.EchoEntry> entries = storage.loadAll();

		Assertions.assertThat(entries).hasSize(1);
		Assertions.assertThat(entries.get(0).echo.echoId).isEqualTo("newer");
	}

	@Test
	@DisplayName("listLabel includes depth, class, and level for boss snapshots")
	void listLabelIncludesBossMetadata() {
		String label = EchoDetailsFormatter.listLabel(EchoTestSupport.warriorEcho(5));

		Assertions.assertThat(label).contains("5");
		Assertions.assertThat(label.toLowerCase()).contains("warrior");
		Assertions.assertThat(label).contains("6");
	}

	@Test
	@DisplayName("formatDetails includes core snapshot metadata")
	void formatDetailsIncludesMetadata() {
		EchoStorage storage = new EchoStorage();
		Echo snap = EchoTestSupport.warriorEcho(5);
		snap.echoId = "test-snap-1";
		storage.save(snap);

		EchoStorage.EchoEntry entry = storage.loadAll().get(0);
		String details = EchoDetailsFormatter.formatDetails(
				entry, EchoTestSupport.TEST_GAME_VERSION);

		Assertions.assertThat(details).contains("test-snap-1");
		Assertions.assertThat(details).contains(entry.filename());
		Assertions.assertThat(details.toLowerCase()).contains("warrior");
		Assertions.assertThat(details).contains("Depth: 5");
		Assertions.assertThat(details).contains("Level: 6");
		Assertions.assertThat(details).contains("28 / 30");
	}

	@Test
	@DisplayName("backpack formatter lists items with stack quantities")
	void backpackFormatterListsItems() {
		Hero hero = new Hero();
		hero.live();
		Dungeon.hero = hero;

		PotionOfHealing potion = new PotionOfHealing();
		potion.quantity(2);
		potion.identify();
		hero.belongings.backpack.items.add(potion);

		ScrollOfIdentify scroll = new ScrollOfIdentify();
		scroll.identify();
		hero.belongings.backpack.items.add(scroll);

		StringBuilder sb = new StringBuilder();
		EchoBackpackFormatter.appendTo(sb, hero.belongings.backpack);
		String backpack = sb.toString();

		Assertions.assertThat(backpack).contains("Backpack:");
		Assertions.assertThat(backpack.toLowerCase()).contains("potion of healing");
		Assertions.assertThat(backpack).contains("x2");
		Assertions.assertThat(backpack.toLowerCase()).contains("scroll of identify");
	}

	@Test
	@DisplayName("backpack formatter shows empty state")
	void backpackFormatterShowsEmpty() {
		Hero hero = new Hero();
		hero.live();

		StringBuilder sb = new StringBuilder();
		EchoBackpackFormatter.appendTo(sb, hero.belongings.backpack);

		Assertions.assertThat(sb.toString()).contains("Backpack:");
		Assertions.assertThat(sb.toString()).contains("(empty)");
	}

	@Test
	@DisplayName("formatDetails includes backpack when hero bundle is present")
	void formatDetailsIncludesBackpackFromHeroBundle() {
		Hero hero = new Hero();
		hero.live();
		hero.heroClass = HeroClass.WARRIOR;
		hero.lvl = 6;
		Talent.initClassTalents(hero);
		Dungeon.hero = hero;

		PotionOfHealing potion = new PotionOfHealing();
		potion.quantity(2);
		potion.identify();
		hero.belongings.backpack.items.add(potion);

		Bundle echoData = EchoTestSupport.bundleHero(hero);
		Echo snap = Echo.create(
				5, EchoTestSupport.TEST_GAME_VERSION, 12345L,
				"WARRIOR", 6, hero.HP, hero.HT, echoData);
		snap.echoId = "backpack-test";

		EchoStorage.EchoEntry entry = new EchoStorage.EchoEntry(
				EchoStorage.getEchoesDir(), snap);
		String details = EchoDetailsFormatter.formatDetails(
				entry, EchoTestSupport.TEST_GAME_VERSION);

		Assertions.assertThat(details).contains("Full echo data: Present");
		Assertions.assertThat(details.toLowerCase()).contains("potion of healing");
		Assertions.assertThat(details).contains("x2");
	}

	@Test
	@DisplayName("inventory layout shrinks slots to fit full backpack in snapshot detail window")
	void inventoryLayoutFitsFullBackpackInDetailWindow() {
		int[] layout = WndEchoDetail.fittingInventoryLayout(25);

		Assertions.assertThat(layout[1]).isLessThan(28);
		Assertions.assertThat(layout[1]).isGreaterThanOrEqualTo(16);
	}

	@Test
	@DisplayName("inventory layout fits snapshot detail window bounds")
	void inventoryLayoutFitsDetailWindow() {
		for (int slots : new int[] { 4, 21, 25, 26 }) {
			int[] layout = WndEchoDetail.fittingInventoryLayout(slots);
			int cols = layout[0];
			int size = layout[1];
			int rows = (int) Math.ceil(slots / (float) cols);
			int width = size * cols + (cols - 1);
			int height = 14 + size * rows + (rows - 1);

			Assertions.assertThat(width).isLessThanOrEqualTo(120);
			Assertions.assertThat(height).isLessThanOrEqualTo(120);
		}
	}

	@Test
	@DisplayName("inventory layout always uses five columns like the main bag")
	void inventoryLayoutUsesFiveColumns() {
		for (int slots : new int[] { 21, 25, 26 }) {
			Assertions.assertThat(WndEchoDetail.fittingInventoryLayout(slots)[0]).isEqualTo(5);
		}
	}

	@Test
	@DisplayName("deleteEntry removes echo file for depth")
	void deleteEntryRemovesEchoFile() {
		EchoStorage storage = new EchoStorage();
		storage.save(EchoTestSupport.warriorEcho(5));

		EchoStorage.EchoEntry entry = storage.loadAll().get(0);
		Assertions.assertThat(entry.file.exists()).isTrue();

		Assertions.assertThat(storage.deleteEntry(entry)).isTrue();
		Assertions.assertThat(entry.file.exists()).isFalse();
		Assertions.assertThat(storage.loadAll()).isEmpty();
	}

	@Test
	@DisplayName("deleteEntry returns false when file is missing")
	void deleteEntryReturnsFalseWhenMissing() {
		EchoStorage storage = new EchoStorage();
		Echo echo = EchoTestSupport.warriorEcho(5);
		EchoStorage.EchoEntry entry = new EchoStorage.EchoEntry(
				new File(EchoStorage.getEchoesDir(), "depth-99.dat"), echo);

		Assertions.assertThat(storage.deleteEntry(entry)).isFalse();
	}

	@Test
	@DisplayName("loadAll loads snapshots saved through EchoStorage")
	void loadAllRoundTripsSavedSnapshots() {
		EchoStorage storage = new EchoStorage();
		storage.save(EchoTestSupport.warriorEcho(5));
		storage.save(EchoTestSupport.warriorEcho(10));

		List<Integer> depths = storage.loadAll().stream()
				.map(e -> e.echo.depth)
				.collect(Collectors.toList());

		Assertions.assertThat(depths).contains(5, 10);
	}
}
