package com.shatteredpixel.shatteredpixeldungeon.village;

import static org.assertj.core.api.Assertions.assertThat;

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.levels.VillageLevel;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldNet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;

/**
 * A figure leaving town is a leaderboard push, not a kill. Nothing about the
 * standings may reach the local player's badges or statistics — and that must
 * hold on its own, not because the figures happen to be neutral today.
 */
@ExtendWith(GdxTestExtension.class)
@DisplayName("Village figures leaving town")
class VillageFigureDestroyTest {

	@BeforeEach
	void inTown() {
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
		Dungeon.depth = VillageLevel.VILLAGE_DEPTH;
		Dungeon.branch = 0;
		Dungeon.hero = new VillageHero(HeroClass.WARRIOR);
		VillageFigures.clear();
		VillageEchoBundles.clear();
	}

	@AfterEach
	void leaveTown() {
		VillageFigures.clear();
		VillageEchoBundles.clear();
		WorldNet.reset();
		Dungeon.level = null;
		Dungeon.hero = null;
		Actor.clear();
	}

	@Test
	@DisplayName("A pushed-out figure counts as no kill, even if it were hostile")
	void pushedOutFigureIsNoKill() {
		VillageLevel level = new VillageLevel();
		level.create();
		Dungeon.level = level;
		VillageFigures.apply(level, figures("5-1"));
		VillageEcho body = VillageFigures.standing().get(0);
		// The safety must not rest on VillageEcho happening to be an NPC.
		body.alignment = Char.Alignment.ENEMY;

		Badges.reset();
		Statistics.reset();
		int xpBefore = Dungeon.hero.exp;

		VillageFigures.apply(level, new ArrayList<VillageFigure>());

		assertThat(VillageFigures.standing()).isEmpty();
		assertThat(Statistics.enemiesSlain).isZero();
		assertThat(Dungeon.hero.exp).isEqualTo(xpBefore);
		assertThat(Badges.totalUnlocked(false)).isZero();
	}

	private static ArrayList<VillageFigure> figures(String echoId) {
		VillageFigure body = new VillageFigure();
		body.post = VillageFigure.Post.DEPTH;
		body.depth = 5;
		body.echoId = echoId;
		body.userName = "Ann";
		body.heroClass = HeroClass.WARRIOR.name();
		body.lvl = 6;
		body.hp = 20;
		body.ht = 25;
		ArrayList<VillageFigure> all = new ArrayList<>();
		all.add(body);
		return all;
	}
}
