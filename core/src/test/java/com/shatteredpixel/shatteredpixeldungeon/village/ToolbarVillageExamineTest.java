package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.ui.Toolbar;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class ToolbarVillageExamineTest {

	@Test
	@DisplayName("Examine is the one tool the village keeps")
	void examineAvailableInVillage() {
		Dungeon.hero = new VillageHero(HeroClass.WARRIOR);

		Assertions.assertThat(Toolbar.examineAllowed()).isTrue();
	}

	@Test
	@DisplayName("Searching is off in town, because the village map is fully revealed")
	void searchNotAllowedInVillage() {
		Dungeon.hero = new VillageHero(HeroClass.WARRIOR);

		Assertions.assertThat(Toolbar.searchAllowed()).isFalse();
	}

	@Test
	@DisplayName("Both halves of the button stay on in the dungeon")
	void bothAllowedInDungeon() {
		Dungeon.hero = new Hero();

		Assertions.assertThat(Toolbar.examineAllowed()).isTrue();
		Assertions.assertThat(Toolbar.searchAllowed()).isTrue();
	}

	@Test
	@DisplayName("Neither half runs without a hero to run it")
	void neitherAllowedWithoutHero() {
		Dungeon.hero = null;

		Assertions.assertThat(Toolbar.examineAllowed()).isFalse();
		Assertions.assertThat(Toolbar.searchAllowed()).isFalse();
	}

	@Test
	@DisplayName("The town avatar's search stays a no-op, so the guard is belt and braces")
	void villageHeroSearchIsStillANoOp() {
		VillageHero hero = new VillageHero(HeroClass.WARRIOR);

		Assertions.assertThat(hero.search(true)).isFalse();
	}
}
