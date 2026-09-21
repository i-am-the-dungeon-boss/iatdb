package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.levels.VillageLevel;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class VillageStairsGuideTest {

	@BeforeEach
	void onGroundLevel() {
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
		Dungeon.depth = VillageLevel.VILLAGE_DEPTH;
		Dungeon.branch = 0;
	}

	@Test
	@DisplayName("The dungeon stairs float a start-here guide")
	void stairsSayStartHere() {
		Assertions.assertThat(VillageStairsGuide.title().text).isEqualTo("Start here");
	}

	@Test
	@DisplayName("The guide hangs over the dungeon stairs, not the arrival cell")
	void guideHangsOverTheStairs() {
		VillageLevel level = new VillageLevel();
		level.create();

		Assertions.assertThat(VillageStairsGuide.cell(level)).isEqualTo(level.dungeonEntrance());
		Assertions.assertThat(VillageStairsGuide.cell(level)).isNotEqualTo(level.arrivalCell());
	}
}
