package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.levels.HouseLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.VillageLevel;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndDungeonMode;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class VillageGroundLevelTest {

	@AfterEach
	void restorePlayMode() {
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
	}

	@Test
	@DisplayName("Depth 0 routes to the village, and its branch 1 to the house")
	void depthZeroRoutesToGroundLevel() {
		Assertions.assertThat(Dungeon.levelClassForDepth(0, 0)).isEqualTo(VillageLevel.class);
		Assertions.assertThat(Dungeon.levelClassForDepth(0, VillageLevel.HOUSE_BRANCH))
				.isEqualTo(HouseLevel.class);
	}

	@Test
	@DisplayName("Adding the ground level leaves dungeon depth routing untouched")
	void dungeonRoutingUnchanged() {
		Assertions.assertThat(Dungeon.levelClassForDepth(1, 0)).isEqualTo(SewerLevel.class);
		Assertions.assertThat(Dungeon.levelClassForDepth(4, 0)).isEqualTo(SewerLevel.class);
	}

	@Test
	@DisplayName("The debug arena never replaces the village, so the hub stays reachable")
	void debugModeDoesNotReplaceVillage() {
		Dungeon.echoPlayMode = EchoPlayMode.DEBUG;
		Assertions.assertThat(Dungeon.levelClassForDepth(0, 0)).isEqualTo(VillageLevel.class);
	}

	@Test
	@DisplayName("Only depth 0 counts as the ground level")
	void groundLevelPredicate() {
		Assertions.assertThat(VillageLevel.isGroundLevel(0)).isTrue();
		Assertions.assertThat(VillageLevel.isGroundLevel(1)).isFalse();
		Assertions.assertThat(VillageLevel.isVillage(0, 0)).isTrue();
		Assertions.assertThat(VillageLevel.isVillage(0, VillageLevel.HOUSE_BRANCH)).isFalse();
	}

	@Test
	@DisplayName("The village save slot sits past every run slot, so it can never collide")
	void villageSlotCannotCollideWithRunSlots() {
		Assertions.assertThat(VillageGateway.VILLAGE_SLOT).isGreaterThan(GamesInProgress.MAX_SLOTS);
		Assertions.assertThat(VillageGateway.runSlot())
				.isNotEqualTo(VillageGateway.VILLAGE_SLOT)
				.isBetween(1, GamesInProgress.MAX_SLOTS);
	}

	@Test
	@DisplayName("The village is always stored in the solo namespace")
	void villageIsStoredSolo() {
		Assertions.assertThat(VillageGateway.villageStorageMode()).isEqualTo(EchoPlayMode.SOLO);
	}

	@Test
	@DisplayName("The dungeon prompt maps its options to play modes, and cancel to none")
	void promptOptionsMapToModes() {
		Assertions.assertThat(WndDungeonMode.modeForOption(WndDungeonMode.SOLO))
				.isEqualTo(EchoPlayMode.SOLO);
		Assertions.assertThat(WndDungeonMode.modeForOption(WndDungeonMode.RANKED))
				.isEqualTo(EchoPlayMode.RANKED);
		Assertions.assertThat(WndDungeonMode.modeForOption(WndDungeonMode.CANCEL)).isNull();
	}

	@Test
	@DisplayName("The ground level is always rebuilt, never resumed from disk")
	void groundLevelIsNeverRestored() {
		// A saved Level restores its own width and height from its bundle, so a
		// village written by an older build would keep that build's map forever
		// — which is exactly how a hero ended up standing off the edge of the
		// house map. The ground level holds no progress, so it is never resumed.
		VillageGateway.prepareVillageEntry();

		Assertions.assertThat(InterlevelScene.mode)
				.isEqualTo(InterlevelScene.Mode.VILLAGE)
				.isNotEqualTo(InterlevelScene.Mode.CONTINUE);
		Assertions.assertThat(VillageGateway.villageExists()).isFalse();
	}

	@Test
	@DisplayName("The town avatar falls back to a real hero class when none was picked")
	void villageHeroClassAlwaysResolves() {
		GamesInProgress.selectedClass = null;
		Assertions.assertThat(VillageGateway.villageHeroClass()).isNotNull();
	}
}
