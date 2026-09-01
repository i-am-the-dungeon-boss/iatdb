package com.shatteredpixel.shatteredpixeldungeon.village;

import static org.assertj.core.api.Assertions.assertThat;

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greataxe;
import com.shatteredpixel.shatteredpixeldungeon.levels.VillageLevel;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldNet;
import com.watabou.utils.Base64Codec;
import com.watabou.utils.Bundle;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

/**
 * Walking past the figures in town is meeting other players, not playing the
 * game. Their gear is theirs; none of it is the local player's to be endorsed
 * for.
 */
@ExtendWith(GdxTestExtension.class)
@DisplayName("Village figures and the local player's badges")
class VillageBadgeIsolationTest {

	@BeforeEach
	void inTown() {
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
		Dungeon.depth = VillageLevel.VILLAGE_DEPTH;
		Dungeon.branch = 0;
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
	@DisplayName("A figure's bundle arriving endorses none of the player's badges")
	void deliveryEndorsesNoBadges() throws Exception {
		String theirs = wellEquippedBundle();

		Dungeon.hero = new VillageHero(HeroClass.WARRIOR);
		VillageEcho body = standing("5-1");
		Badges.reset();
		Statistics.itemTypesDiscovered.clear();

		VillageEchoBundles.deliver("5-1", theirs);

		assertThat(body.getEchoHero()).isNotNull();
		assertThat(Badges.totalUnlocked(false)).isZero();
		assertThat(Statistics.itemTypesDiscovered).isEmpty();
	}

	private static VillageFigure figure(String echoId) {
		VillageFigure body = new VillageFigure();
		body.post = VillageFigure.Post.DEPTH;
		body.depth = 5;
		body.echoId = echoId;
		body.userName = "Ann";
		body.heroClass = HeroClass.WARRIOR.name();
		body.lvl = 6;
		body.hp = 20;
		body.ht = 25;
		return body;
	}

	private static VillageEcho standing(String echoId) {
		VillageLevel level = new VillageLevel();
		level.create();
		Dungeon.level = level;
		ArrayList<VillageFigure> figures = new ArrayList<>();
		figures.add(figure(echoId));
		VillageFigures.apply(level, figures);
		return VillageFigures.standing().get(0);
	}

	/** Another player's hero, carrying enough to trip every badge in the path. */
	private static String wellEquippedBundle() throws Exception {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.WARRIOR.initHero(hero);
		hero.lvl = 6;
		Greataxe axe = new Greataxe();
		axe.upgrade(6);
		axe.levelKnown = true;
		axe.cursedKnown = true;
		axe.collect(hero.belongings.backpack);

		Echo echo = Echo.fromHero(hero, 5, "1.0.0", 1L);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Bundle.write(echo.toFileBundle(), out);
		Dungeon.hero = null;
		return Base64Codec.encode(out.toByteArray());
	}
}
