package com.shatteredpixel.shatteredpixeldungeon.village;

import static org.assertj.core.api.Assertions.assertThat;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.levels.VillageLevel;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldIdentity;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldNet;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldNetEngine;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldPresence;
import com.watabou.utils.Base64Codec;
import com.watabou.utils.Bundle;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(GdxTestExtension.class)
@DisplayName("Village echo inspect bundles")
class VillageEchoBundlesTest {

	/** Records only what this test asks about: which figures were requested. */
	private static final class RequestRecorder implements WorldNetEngine {
		final List<String> requested = new ArrayList<>();

		@Override
		public void connect(WorldIdentity identity, Listener listener) {
		}

		@Override
		public void disconnect() {
		}

		@Override
		public boolean isConnected() {
			return true;
		}

		@Override
		public void setPresence(WorldPresence local) {
		}

		@Override
		public void sendChat(String text) {
		}

		@Override
		public void report(String messageId, String reason) {
		}

		@Override
		public void requestEchoBundle(String echoId) {
			requested.add(echoId);
		}

		@Override
		public void setChatFocused(boolean focused) {
		}

		@Override
		public void tick(float elapsed) {
		}

		@Override
		public String name() {
			return "recorder";
		}

		@Override
		public boolean isServerMuted() {
			return false;
		}

		@Override
		public void applyServerMute(long mutedUntil) {
		}
	}

	private RequestRecorder engine;

	@BeforeEach
	void inTown() {
		Dungeon.echoPlayMode = EchoPlayMode.SOLO;
		Dungeon.depth = VillageLevel.VILLAGE_DEPTH;
		Dungeon.branch = 0;
		Dungeon.hero = new Hero();
		engine = new RequestRecorder();
		WorldNet.setEngineForTests(engine);
		VillageFigures.dropLabels();
		VillageEchoBundles.clear();
	}

	@AfterEach
	void leaveTown() {
		VillageFigures.dropLabels();
		VillageEchoBundles.clear();
		WorldNet.reset();
		Dungeon.level = null;
		Dungeon.hero = null;
		Actor.clear();
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

	private VillageEcho standing(String echoId) {
		VillageLevel level = new VillageLevel();
		level.create();
		Dungeon.level = level;
		ArrayList<VillageFigure> figures = new ArrayList<>();
		figures.add(figure(echoId));
		VillageFigures.apply(level, figures);
		return VillageFigures.standing().get(0);
	}

	private static String bundleFor(HeroClass heroClass) throws Exception {
		Hero hero = new Hero();
		hero.heroClass = heroClass;
		hero.lvl = 6;
		Echo echo = Echo.fromHero(hero, 5, "1.0.0", 1L);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Bundle.write(echo.toFileBundle(), out);
		return Base64Codec.encode(out.toByteArray());
	}

	@Test
	@DisplayName("gives the standing body its hero once the bundle arrives")
	void deliversTheHero() throws Exception {
		VillageEcho body = standing("5-1");

		VillageEchoBundles.deliver("5-1", bundleFor(HeroClass.WARRIOR));

		assertThat(body.getEchoHero()).isNotNull();
		assertThat(body.getEchoHero().heroClass).isEqualTo(HeroClass.WARRIOR);
	}

	@Test
	@DisplayName("ignores a bundle for a figure that is not standing here")
	void ignoresAnUnknownFigure() throws Exception {
		VillageEcho body = standing("5-1");

		VillageEchoBundles.deliver("25-9", bundleFor(HeroClass.MAGE));

		assertThat(body.getEchoHero()).isNull();
	}

	@Test
	@DisplayName("leaves the hero absent rather than standing a hollow one on a bad bundle")
	void refusesAMalformedBundle() {
		VillageEcho body = standing("5-1");

		VillageEchoBundles.deliver("5-1", "not-a-bundle");

		assertThat(body.getEchoHero()).isNull();
	}

	@Test
	@DisplayName("asks the channel for a figure the player is looking at")
	void requestsOnDemand() {
		VillageEcho body = standing("5-1");

		VillageEchoBundles.request(body);

		assertThat(engine.requested).containsExactly("5-1");
	}

	@Test
	@DisplayName("asks only once while a request is still outstanding")
	void doesNotAskTwice() {
		VillageEcho body = standing("5-1");

		VillageEchoBundles.request(body);
		VillageEchoBundles.request(body);

		assertThat(engine.requested).containsExactly("5-1");
	}

	@Test
	@DisplayName("asks for nothing once the hero is already here")
	void doesNotAskForWhatItHas() throws Exception {
		VillageEcho body = standing("5-1");
		VillageEchoBundles.deliver("5-1", bundleFor(HeroClass.WARRIOR));
		engine.requested.clear();

		VillageEchoBundles.request(body);

		assertThat(engine.requested).isEmpty();
	}

	@Test
	@DisplayName("asks again after a failed delivery, since nothing arrived to show")
	void retriesAfterAFailedDelivery() {
		VillageEcho body = standing("5-1");
		VillageEchoBundles.request(body);
		VillageEchoBundles.deliver("5-1", "not-a-bundle");
		engine.requested.clear();

		VillageEchoBundles.request(body);

		assertThat(engine.requested).containsExactly("5-1");
	}

	@Test
	@DisplayName("forgets outstanding requests when the village empties")
	void clearsWithTheVillage() {
		VillageEcho body = standing("5-1");
		VillageEchoBundles.request(body);

		VillageEchoBundles.clear();
		engine.requested.clear();
		VillageEchoBundles.request(body);

		assertThat(engine.requested).containsExactly("5-1");
	}

	@Test
	@DisplayName("asks as the player walks up, so the hero is there before the window opens")
	void prefetchesOnApproach() {
		VillageEcho body = standing("5-1");
		Dungeon.hero.pos = body.pos;

		body.act();

		assertThat(engine.requested).containsExactly("5-1");
	}

	@Test
	@DisplayName("stays quiet about a figure nobody is near")
	void doesNotPrefetchAcrossTheVillage() {
		VillageEcho body = standing("5-1");
		Dungeon.hero.pos = farFrom(body.pos);

		body.act();

		assertThat(engine.requested).isEmpty();
	}

	private int farFrom(int cell) {
		int width = Dungeon.level.width();
		int row = cell / width;
		int col = cell % width;
		int farRow = row > Dungeon.level.height() / 2 ? 1 : Dungeon.level.height() - 2;
		int farCol = col > width / 2 ? 1 : width - 2;
		return farRow * width + farCol;
	}
}
