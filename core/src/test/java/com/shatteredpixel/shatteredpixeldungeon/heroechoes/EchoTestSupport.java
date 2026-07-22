package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.DebugSettings;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoFetchResult;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.CompositeEchoLookup;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoLookupOutcome;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoOnlineSettings;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPlayerSession;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoWireCodec;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Visual;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.FileUtils;
import com.watabou.utils.PointF;
import com.watabou.utils.SparseArray;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/** Shared fixtures and cleanup for hero-echoes workflow tests. */
public final class EchoTestSupport {

	public static final String ECHOES_DIR = "echoes";
	public static final String LEADERBOARD_FILE = "leaderboard.json";
	public static final String TEST_GAME_VERSION = "0.0.1";

	private EchoTestSupport() {
	}

	public static void resetWorkflowState() {
		FileUtils.deleteDir("echoes");
		FileUtils.deleteDir("echoes-solo");
		FileUtils.deleteDir("echoes-solo-easy");
		FileUtils.deleteDir("echoes-ranked");
		FileUtils.deleteDir("echoes-ranked-easy");
		FileUtils.deleteDir("echoes-debug");
		FileUtils.deleteFile("leaderboard.json");
		FileUtils.deleteFile("leaderboard-solo.json");
		FileUtils.deleteFile("leaderboard-solo-easy.json");
		FileUtils.deleteFile("leaderboard-ranked.json");
		FileUtils.deleteFile("leaderboard-ranked-easy.json");
		deleteRecursively(new File("echoes"));
		deleteRecursively(new File("echoes-solo"));
		deleteRecursively(new File("echoes-solo-easy"));
		deleteRecursively(new File("echoes-ranked"));
		deleteRecursively(new File("echoes-ranked-easy"));
		deleteRecursively(new File("echoes-debug"));
		new File("leaderboard.json").delete();
		new File("leaderboard-solo.json").delete();
		new File("leaderboard-solo-easy.json").delete();
		new File("leaderboard-ranked.json").delete();
		new File("leaderboard-ranked-easy.json").delete();
		Actor.clear();
		Dungeon.hero = null;
		Dungeon.level = null;
		Dungeon.easyMode = false;
		Dungeon.resetEchoStateForTests();
		GamesInProgress.clearSlotCache();
		GamesInProgress.selectedEchoPlayMode = EchoPlayMode.NONE;
		DebugSettings.resetForTests();
		SPDSettings.playerName("");
		SPDSettings.easyMode(false);
		EchoOnlineSettings.resetForTests();
		EchoPlayerSession.resetForTests();
		CompositeEchoLookup.resetForTests();
	}

	public static Echo warriorEcho(int depth) {
		return Echo.create(
				depth,
				TEST_GAME_VERSION,
				12345L,
				"WARRIOR",
				6,
				28,
				30,
				null);
	}

	/** Live warrior hero fixture (also sets {@link Dungeon#hero}). */
	public static Hero warriorHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.WARRIOR.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}

	/** Echo with bundled hero data — required for echo boss spawn and combat. */
	public static Echo warriorEchoWithData(int depth) {
		Hero hero = warriorHero();
		return Echo.create(
				depth,
				TEST_GAME_VERSION,
				12345L,
				"WARRIOR",
				6,
				28,
				30,
				bundleHero(hero));
	}

	/** Prefetch pending echo+policy then construct boss from pending. */
	public static EchoBoss createBossWithPolicy(Hero hero, EchoPolicy policy, int depth) {
		Echo echo = Echo.create(
				depth, TEST_GAME_VERSION, 1L,
				hero.heroClass.name(), hero.lvl, hero.HP, hero.HT, bundleHero(hero));
		CompositeEchoLookup.setEchoLookupForTests(d -> outcomeWithPolicy(echo, policy));
		Dungeon.depth = depth;
		Dungeon.prefetchEchoBossForDepth(depth);
		return new EchoBoss(echo, depth);
	}

	/**
	 * Bare 7×7 empty level that still mirrors a live fight enough for combat:
	 * linked sprites on hero + boss, FOV, and both registered with {@link Actor}.
	 * (Phantom {@link EchoBoss#getEchoHero()} stays sprite-less, as in production.)
	 */
	public static void installEchoBossLevel(Hero hero, Mob boss, int bossOffset) {
		Level level = new Level() {
			@Override
			public String tilesTex() {
				return null;
			}

			@Override
			public String waterTex() {
				return null;
			}

			@Override
			protected boolean build() {
				return true;
			}

			@Override
			protected void createMobs() {
			}

			@Override
			protected void createItems() {
			}
		};
		level.setSize(7, 7);
		Arrays.fill(level.map, Terrain.EMPTY);
		level.mobs = new HashSet<>();
		level.heaps = new SparseArray<>();
		level.blobs = new HashMap<>();
		level.plants = new SparseArray<>();
		level.traps = new SparseArray<>();
		level.buildFlagMaps();
		level.discoverable = new boolean[level.length()];
		Arrays.fill(level.discoverable, true);
		Arrays.fill(level.heroFOV, true);

		int center = 3 * level.width() + 3;
		hero.pos = center;
		boss.pos = center + bossOffset;
		// Unit tests have no GameScene / CellSelector.
		hero.damageInterrupt = false;
		linkStubSprite(hero);
		linkStubSprite(boss);

		Dungeon.level = level;
		level.mobs.add(boss);

		Actor.clear();
		Actor.add(hero);
		Actor.add(boss);
	}

	/**
	 * Links a stub sprite the way {@link CharSprite#link} does, without camera/VFX.
	 */
	public static void linkStubSprite(Char ch) {
		StubCharSprite sprite = new StubCharSprite();
		sprite.ch = ch;
		ch.sprite = sprite;
	}

	/** Last cell passed to {@link CharSprite#place} on a stub sprite, or -1. */
	public static int stubSpritePlacedCell(Char ch) {
		if (ch == null || !(ch.sprite instanceof StubCharSprite)) {
			return -1;
		}
		return ((StubCharSprite) ch.sprite).lastPlacedCell;
	}

	public static int stubSpriteAttackCalls(Char ch) {
		return stubSprite(ch) == null ? 0 : stubSprite(ch).attackCalls;
	}

	public static int stubSpriteJumpCalls(Char ch) {
		return stubSprite(ch) == null ? 0 : stubSprite(ch).jumpCalls;
	}

	public static int stubSpriteOperateCalls(Char ch) {
		return stubSprite(ch) == null ? 0 : stubSprite(ch).operateCalls;
	}

	public static int stubSpriteZapCalls(Char ch) {
		return stubSprite(ch) == null ? 0 : stubSprite(ch).zapCalls;
	}

	private static StubCharSprite stubSprite(Char ch) {
		if (ch == null || !(ch.sprite instanceof StubCharSprite)) {
			return null;
		}
		return (StubCharSprite) ch.sprite;
	}

	/**
	 * Attaches a headless scene parent that recycles MagicMissile / MissileSprite
	 * and invokes their arrive callbacks immediately — so Echo FX paths can be
	 * tested without GameScene.
	 */
	public static InstantProjectileGroup attachInstantProjectileParent(Char ch) {
		InstantProjectileGroup group = new InstantProjectileGroup();
		if (ch != null && ch.sprite != null) {
			ch.sprite.parent = group;
		}
		return group;
	}

	public static final class InstantProjectileGroup extends Group {
		public int magicMissileRecycles;
		public int missileSpriteRecycles;

		@Override
		public synchronized Gizmo recycle(Class<? extends Gizmo> c) {
			if (c == MagicMissile.class) {
				magicMissileRecycles++;
				return add(new InstantMagicMissile());
			}
			if (c == MissileSprite.class) {
				missileSpriteRecycles++;
				return add(new InstantMissileSprite());
			}
			return super.recycle(c);
		}
	}

	private static final class InstantMagicMissile extends MagicMissile {
		@Override
		public void reset(int type, PointF from, PointF to, Callback callback) {
			if (callback != null) {
				callback.call();
			}
		}
	}

	private static final class InstantMissileSprite extends MissileSprite {
		@Override
		public void reset(int from, int to, Item item, Callback listener) {
			invoke(listener);
		}

		@Override
		public void reset(Visual from, int to, Item item, Callback listener) {
			invoke(listener);
		}

		@Override
		public void reset(int from, Visual to, Item item, Callback listener) {
			invoke(listener);
		}

		@Override
		public void reset(Visual from, Visual to, Item item, Callback listener) {
			invoke(listener);
		}

		@Override
		public void reset(PointF from, PointF to, Item item, Callback listener) {
			invoke(listener);
		}

		private static void invoke(Callback listener) {
			if (listener != null) {
				listener.call();
			}
		}
	}

	/** FIRST_LEGAL capability with a single item id (or virtual tag). */
	public static JSONObject capability(String itemId) {
		return new JSONObject()
				.put("pick", "FIRST_LEGAL")
				.put("items", new JSONArray().put(itemId));
	}

	/** Minimal supported policy with the given role → item capability map. */
	public static EchoPolicy policyWithCapabilities(JSONObject capabilities) {
		return EchoPolicy.fromJson(new JSONObject()
				.put("policy_schema_version", TEST_GAME_VERSION)
				.put("capabilities", capabilities)
				.put("reactions", new JSONArray())
				.put("recipes", new JSONArray())
				.put("positioning", new JSONObject())
				.put("matchups", new JSONObject())
				.put("selection", new JSONObject()
						.put("order", new JSONArray().put("default"))
						.put("default_roles", new JSONArray().put("MELEE")))
				.put("tuning", new JSONObject()));
	}

	public static EchoPolicy healCapabilityPolicy() {
		return policyWithCapabilities(new JSONObject()
				.put("HEAL", capability("PotionOfHealing"))
				.put("MELEE", capability("*melee")));
	}

	/**
	 * CharSprite stand-in: real {@code ch} link, no GameScene/Splash/Camera side
	 * effects.
	 */
	private static final class StubCharSprite extends CharSprite {
		int lastPlacedCell = -1;
		int attackCalls;
		int jumpCalls;
		int operateCalls;
		int zapCalls;

		@Override
		public void place(int cell) {
			lastPlacedCell = cell;
			// skip worldToCamera — headless tests have no Camera.main
		}

		@Override
		public void move(int from, int to) {
			turnTo(from, to);
			place(to);
		}

		@Override
		public void jump(int from, int to, float height, float duration, com.watabou.utils.Callback callback) {
			jumpCalls++;
			place(to);
			if (callback != null) {
				callback.call();
			}
		}

		@Override
		public synchronized void attack(int cell, com.watabou.utils.Callback callback) {
			attackCalls++;
			if (ch != null) {
				turnTo(ch.pos, cell);
			}
			if (callback != null) {
				callback.call();
			}
		}

		@Override
		public synchronized void operate(int cell, com.watabou.utils.Callback callback) {
			operateCalls++;
			if (ch != null) {
				turnTo(ch.pos, cell);
			}
			if (callback != null) {
				callback.call();
			}
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
			// production hits this; Splash needs a live scene
		}

		@Override
		public void flash() {
		}

		@Override
		public void showStatus(int color, String text, Object... args) {
		}

		@Override
		public synchronized void zap(int cell, Callback callback) {
			zapCalls++;
			if (ch != null) {
				turnTo(ch.pos, cell);
			}
			if (callback != null) {
				callback.call();
			}
		}
	}

	public static Echo echoWithVersion(int depth, String gameVersion) {
		Echo snap = warriorEcho(depth);
		snap.gameVersion = gameVersion;
		return snap;
	}

	public static int countEchoFiles() {
		String dir = EchoPlayModePaths.echoesDir();
		if (!FileUtils.dirExists(dir)) {
			return 0;
		}
		return FileUtils.filesInDir(dir).size();
	}

	public static void deleteRecursively(File file) {
		if (file == null || !file.exists())
			return;
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children != null) {
				for (File child : children)
					deleteRecursively(child);
			}
		}
		// noinspection ResultOfMethodCallIgnored
		file.delete();
	}

	public static boolean bundleHasHeroData(Bundle bundle) {
		return bundle != null && bundle.contains("echo_data");
	}

	public static Bundle bundleHero(Hero hero) {
		return EchoHeroSnapshot.captureFromHero(hero);
	}

	public static EchoFetchResult withPolicy(Echo echo) {
		return new EchoFetchResult(echo, EchoPolicy.fallback());
	}

	public static EchoFetchResult withPolicy(Echo echo, EchoPolicy policy) {
		return new EchoFetchResult(echo, policy);
	}

	/** JSON body for GET /v1/echoes/{depth} with real combat data + policy. */
	public static String fetchResponseJson(Echo echo, EchoPolicy policy) throws Exception {
		org.json.JSONObject json = new org.json.JSONObject(
				EchoWireCodec.encodeEchoUpload(echo, "test-client"));
		json.put("echo_policy", policy.root());
		json.put("user_name", Echo.resolveUserName(echo.userName, echo.heroClass));
		json.put("kill_count", Math.max(0, echo.killCount));
		return json.toString();
	}

	public static EchoLookupOutcome outcomeWithPolicy(Echo echo) {
		return EchoLookupOutcome.found(withPolicy(echo));
	}

	public static EchoLookupOutcome outcomeWithPolicy(Echo echo, EchoPolicy policy) {
		return EchoLookupOutcome.found(withPolicy(echo, policy));
	}

	/** Builds an EchoBoss with a supported minimal policy (no pending prefetch). */
	public static EchoBoss createBoss(Echo echo, int depth) {
		return new EchoBoss(echo, depth, EchoPolicy.fallback());
	}

	/**
	 * Role-based merged policy fixture (capabilities / reactions / selection /
	 * tuning).
	 */
	public static EchoPolicy roleBasedPolicy() {
		return EchoPolicy.fromJson("{"
				+ "\"policy_schema_version\":\"" + TEST_GAME_VERSION + "\","
				+ "\"capabilities\":{"
				+ "\"RANGED\":{\"pick\":\"MAX_DAMAGE\",\"items\":[\"MagesStaff\"]},"
				+ "\"MELEE\":{\"pick\":\"FIRST_LEGAL\",\"items\":[\"*melee\"]},"
				+ "\"FINISHER\":{\"pick\":\"MAX_DAMAGE\",\"items\":[\"*melee\",\"MagesStaff\"]}"
				+ "},"
				+ "\"reactions\":[{"
				+ "\"id\":\"finish_him\","
				+ "\"priority\":110,"
				+ "\"when\":{\"enemy_hp_below\":0.05},"
				+ "\"do\":{\"use_role\":\"FINISHER\"}"
				+ "}],"
				+ "\"recipes\":[],"
				+ "\"positioning\":{\"MAGE\":{\"ideal_distance\":3,\"if_closer\":\"KEEP_DISTANCE\"}},"
				+ "\"matchups\":{},"
				+ "\"selection\":{\"order\":[\"reactions\",\"default\"],\"default_roles\":[\"RANGED\",\"MELEE\"]},"
				+ "\"tuning\":{\"aggression\":0.55,\"finish_hp\":0.05}"
				+ "}");
	}
}
