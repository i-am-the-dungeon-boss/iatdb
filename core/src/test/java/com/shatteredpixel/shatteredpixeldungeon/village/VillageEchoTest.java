package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.VillageEchoSprite;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndEchoBossInfo;
import com.watabou.gltextures.TextureCache;
import com.watabou.utils.Bundle;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class VillageEchoTest {

	private static VillageFigure warriorFigure() {
		VillageFigure figure = new VillageFigure();
		figure.post = VillageFigure.Post.DEPTH;
		figure.depth = 5;
		figure.echoId = "5-1771000000000";
		figure.userName = "Somebody";
		figure.heroClass = HeroClass.WARRIOR.name();
		figure.armorTier = 3;
		figure.lvl = 14;
		figure.hp = 30;
		figure.ht = 40;
		figure.killCount = 7;
		figure.timestamp = 1771000000000L;
		figure.badges.add(new VillageFigure.Badge("first-depth-5", VillageFigure.Badge.NO_COUNT));
		return figure;
	}

	@Test
	@DisplayName("A figure stands as itself: the player's name, at the echo's full health")
	void carriesTheFiguresIdentity() {
		VillageEcho echo = new VillageEcho(warriorFigure());

		Assertions.assertThat(echo.name()).isEqualTo("Somebody");
		Assertions.assertThat(echo.HP).isEqualTo(40);
		Assertions.assertThat(echo.HT).isEqualTo(40);
		Assertions.assertThat(echo.heroClass()).isEqualTo(HeroClass.WARRIOR);
	}

	@Test
	@DisplayName("Nothing in town can hurt, move, buff or kill a standing figure")
	void isScenery() {
		VillageEcho echo = new VillageEcho(warriorFigure());
		int hp = echo.HP;

		echo.damage(999, this);

		Assertions.assertThat(echo.HP).isEqualTo(hp);
		Assertions.assertThat(echo.isAlive()).isTrue();
		Assertions.assertThat(echo.add(new Burning())).isFalse();
		Assertions.assertThat(echo.properties()).contains(Mob.Property.IMMOVABLE);
		Assertions.assertThat(echo.defenseSkill(null)).isEqualTo(Mob.INFINITE_EVASION);
	}

	@Test
	@DisplayName("The description names every badge the figure carries")
	void descriptionListsBadges() {
		VillageFigure figure = warriorFigure();
		figure.badges.add(new VillageFigure.Badge("hero-slayer", 42));
		VillageEcho echo = new VillageEcho(figure);

		String description = echo.description();

		Assertions.assertThat(description).isNotBlank();
		Assertions.assertThat(description).doesNotContain("!!!");
		Assertions.assertThat(description).contains("42");
	}

	@Test
	@DisplayName("The description explains the title the figure is shouting, not only its badges")
	void descriptionExplainsTheDepthTitle() {
		VillageFigure figure = warriorFigure();
		figure.badges.clear();
		VillageEcho echo = new VillageEcho(figure);

		String description = echo.description();

		Assertions.assertThat(description).doesNotContain("!!!");
		Assertions.assertThat(description)
				.contains(VillageFigureTitle.of(figure).description);
	}

	@Test
	@DisplayName("An unknown badge kind is skipped rather than printing a missing-key marker")
	void unknownBadgeKindIsSkipped() {
		VillageFigure figure = warriorFigure();
		figure.badges.clear();
		figure.badges.add(new VillageFigure.Badge("no-such-kind", VillageFigure.Badge.NO_COUNT));
		VillageEcho echo = new VillageEcho(figure);

		Assertions.assertThat(echo.description()).doesNotContain("!!!");
	}

	@Test
	@DisplayName("The echo hero is absent until its bundle arrives, so inspect stays closed until then")
	void echoHeroIsLazy() {
		VillageEcho echo = new VillageEcho(warriorFigure());

		Assertions.assertThat(echo.getEchoHero()).isNull();
		Assertions.assertThat(WndEchoBossInfo.hasInspectableKit(echo)).isFalse();

		Hero arrived = new Hero();
		echo.setEchoHero(arrived);

		Assertions.assertThat(echo.getEchoHero()).isSameAs(arrived);
		Assertions.assertThat(WndEchoBossInfo.hasInspectableKit(echo)).isTrue();
	}

	@Test
	@DisplayName("An empty depth post has no echo to request and never becomes inspectable")
	void emptyPostHasNothingToFetch() {
		VillageFigure figure = new VillageFigure();
		figure.post = VillageFigure.Post.DEPTH;
		figure.depth = 10;

		VillageEcho echo = new VillageEcho(figure);

		Assertions.assertThat(figure.isEmptyPost()).isTrue();
		Assertions.assertThat(echo.getEchoHero()).isNull();
	}

	@Test
	@DisplayName("A filled figure is drawn from its hero class spritesheet")
	void filledFigureUsesHeroClassSheet() {
		VillageEcho echo = new VillageEcho(warriorFigure());

		CharSprite sprite = echo.sprite();

		Assertions.assertThat(sprite).isInstanceOf(VillageEchoSprite.class);
		Assertions.assertThat(sprite.texture)
				.isSameAs(TextureCache.get(HeroClass.WARRIOR.spritesheet()));
	}

	@Test
	@DisplayName("An empty depth post is drawn as the unknown warrior placeholder")
	void emptyPostUsesUnknownWarriorSheet() {
		VillageFigure figure = new VillageFigure();
		figure.post = VillageFigure.Post.DEPTH;
		figure.depth = 10;

		CharSprite sprite = new VillageEcho(figure).sprite();

		Assertions.assertThat(sprite).isInstanceOf(VillageEchoSprite.class);
		Assertions.assertThat(sprite.texture)
				.isSameAs(TextureCache.get(Assets.Sprites.WARRIOR_UNKNOWN));
	}

	@Test
	@DisplayName("A figure survives the bundle a mid-session save writes")
	void bundleRoundTrip() {
		VillageEcho original = new VillageEcho(warriorFigure());
		Bundle bundle = new Bundle();
		original.storeInBundle(bundle);

		VillageEcho restored = new VillageEcho();
		restored.restoreFromBundle(bundle);

		Assertions.assertThat(restored.name()).isEqualTo("Somebody");
		Assertions.assertThat(restored.figure().echoId).isEqualTo("5-1771000000000");
		Assertions.assertThat(restored.figure().depth).isEqualTo(5);
		Assertions.assertThat(restored.figure().armorTier).isEqualTo(3);
		Assertions.assertThat(restored.figure().lvl).isEqualTo(14);
		Assertions.assertThat(restored.figure().killCount).isEqualTo(7);
		Assertions.assertThat(restored.figure().timestamp).isEqualTo(1771000000000L);
		Assertions.assertThat(restored.figure().badges).hasSize(1);
		Assertions.assertThat(restored.figure().badges.get(0).kind).isEqualTo("first-depth-5");
		Assertions.assertThat(restored.heroClass()).isEqualTo(HeroClass.WARRIOR);
	}

	@Test
	@DisplayName("A figure whose class the game does not know still stands, as a warrior")
	void unknownHeroClassFallsBack() {
		VillageFigure figure = warriorFigure();
		figure.heroClass = "ARCHAEOLOGIST";

		VillageEcho echo = new VillageEcho(figure);

		Assertions.assertThat(echo.heroClass()).isEqualTo(HeroClass.WARRIOR);
	}
}
