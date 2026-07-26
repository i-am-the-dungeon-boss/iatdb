package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Builds per-turn {@link EchoPolicyStatus} from the live fight (canvas §5). */
public final class EchoPolicyStatusBuilder {

	private EchoPolicyStatusBuilder() {
	}

	public static EchoPolicyStatus build(EchoBoss boss, EchoPolicy policy) {
		Hero enemy = Dungeon.hero;
		Level level = Dungeon.level;
		Hero echoHero = boss.getEchoHero();

		float selfHp = boss.HT > 0 ? (float) boss.HP / boss.HT : 1f;
		float enemyHp = enemy != null && enemy.HT > 0 ? (float) enemy.HP / enemy.HT : 1f;
		float enemyShield = enemy != null && enemy.HT > 0
				? (float) enemy.shielding() / enemy.HT
				: 0f;
		int distance = enemy != null && level != null
				? level.distance(boss.pos, enemy.pos)
				: 99;
		// Match Mob hunting: FOV alone is not enough while the hero is invisible.
		boolean inLos = enemy != null
				&& enemy.invisible <= 0
				&& boss.fieldOfView != null
				&& enemy.pos >= 0
				&& enemy.pos < boss.fieldOfView.length
				&& boss.fieldOfView[enemy.pos];
		// Door-stall reactions still need a remembered door focus.
		if (inLos) {
			boss.noteEnemySeenAt(enemy.pos);
		}

		int nearTiles = 3;
		JSONObject tuning = policy.root().optJSONObject("tuning");
		if (tuning != null) {
			nearTiles = tuning.optInt("terrain_near_tiles", 3);
		}

		Map<String, Integer> nearDist = new HashMap<>();
		Map<String, Integer> nearCell = new HashMap<>();
		if (level != null) {
			recordTerrain(level, boss.pos, Terrain.WATER, "water", nearTiles, nearDist, nearCell);
			recordTerrain(level, boss.pos, Terrain.GRASS, "grass", nearTiles, nearDist, nearCell);
			recordTerrain(level, boss.pos, Terrain.HIGH_GRASS, "grass", nearTiles, nearDist, nearCell);
		}

		boolean onWater = "water".equals(onTerrainName(level, boss.pos));
		boolean waterNear = nearDist.containsKey("water");
		boolean clearOfBlast = enemy == null || level == null
				|| level.distance(boss.pos, enemy.pos) >= 2;

		Set<String> available = EchoInventory.availableIds(echoHero);
		Set<String> rolesReady = new HashSet<>();
		Set<String> safe = new HashSet<>();
		Set<String> unsafe = new HashSet<>();
		Set<String> selfStatuses = statusNames(boss);
		if (EchoAoeDots.isAoeDotAt(boss, boss.pos)) {
			selfStatuses.add(EchoAoeDots.STATUS);
		}

		JSONObject caps = policy.root().optJSONObject("capabilities");
		if (caps != null) {
			Iterator<String> keys = caps.keys();
			while (keys.hasNext()) {
				String role = keys.next();
				JSONObject cap = caps.optJSONObject(role);
				if (cap == null)
					continue;
				if (!EchoRoleResolver.roleHasReadyItem(cap, available))
					continue;
				if (!virtualRoleFeasible(role, boss, enemy, level))
					continue;
				if (!respectsPotionReserve(role, cap, tuning, echoHero))
					continue;
				rolesReady.add(role);

				String hazard = cap.optString("hazard", "");
				if (hazard.isEmpty()) {
					safe.add(role);
					continue;
				}
				boolean mitigated = clearOfBlast
						|| (EchoPolicyHazards.FIRE_AOE.equals(hazard) && (onWater || waterNear));
				if (mitigated) {
					safe.add(role);
					safe.add(hazard);
				} else {
					unsafe.add(role);
					unsafe.add(hazard);
				}
			}
		}

		EchoPolicyStatus.Builder b = new EchoPolicyStatus.Builder()
				.selfHpRatio(selfHp)
				.enemyHpRatio(enemyHp)
				.enemyShieldRatio(enemyShield)
				.distance(distance)
				.enemyInLos(inLos)
				.doorStalling(boss.isDoorStalling())
				.selfClass(boss.getEcho().heroClass)
				.enemyClass(enemy != null && enemy.heroClass != null ? enemy.heroClass.name() : "")
				.onTerrain(onTerrainName(level, boss.pos))
				.selfStatuses(selfStatuses)
				.enemyStatuses(enemy != null ? statusNames(enemy) : new HashSet<>())
				.terrainNearTiles(nearTiles)
				.rolesReady(rolesReady)
				.safeHazards(safe)
				.unsafeHazards(unsafe);

		for (Map.Entry<String, Integer> e : nearDist.entrySet()) {
			b.terrainNear(e.getKey(), e.getValue());
		}
		for (Map.Entry<String, Integer> e : nearCell.entrySet()) {
			b.terrainNearCell(e.getKey(), e.getValue());
		}
		return b.build();
	}

	private static boolean respectsPotionReserve(
			String role, JSONObject cap, JSONObject tuning, Hero echoHero) {
		if (tuning == null)
			return true;
		JSONObject reserve = tuning.optJSONObject("potion_reserve");
		if (reserve == null || !reserve.has(role))
			return true;
		int keep = reserve.optInt(role, 0);
		if (keep <= 0)
			return true;
		return EchoInventory.countMatching(echoHero, cap.optJSONArray("items")) > keep;
	}

	private static boolean virtualRoleFeasible(String role, EchoBoss boss, Hero enemy, Level level) {
		switch (role) {
			case "MOVE_TO_WATER":
				return level != null
						&& nearestTerrainCell(level, boss.pos, Terrain.WATER, Integer.MAX_VALUE) != null;
			case "MOVE_TO_GRASS":
				return level != null
						&& (nearestTerrainCell(level, boss.pos, Terrain.GRASS, Integer.MAX_VALUE) != null
								|| nearestTerrainCell(level, boss.pos, Terrain.HIGH_GRASS, Integer.MAX_VALUE) != null);
			case "LEAVE_AOE":
				return EchoAoeDots.canLeave(boss);
			case "BLINK":
				return EchoTargetPicker.pickBlinkAway(boss) >= 0;
			default:
				return true;
		}
	}

	private static void recordTerrain(
			Level level, int from, int terrain, String name, int maxDist,
			Map<String, Integer> nearDist, Map<String, Integer> nearCell) {
		int[] found = nearestTerrainCell(level, from, terrain, maxDist);
		if (found == null)
			return;
		Integer prev = nearDist.get(name);
		if (prev == null || found[1] < prev) {
			nearDist.put(name, found[1]);
			nearCell.put(name, found[0]);
		}
	}

	/** @return int[]{cell, distance} or null */
	static int[] nearestTerrainCell(Level level, int from, int terrain, int maxDist) {
		if (level == null)
			return null;
		int bestCell = -1;
		int bestDist = Integer.MAX_VALUE;
		for (int i = 0; i < level.length(); i++) {
			if (level.map[i] != terrain)
				continue;
			int d = level.distance(from, i);
			if (d <= maxDist && d < bestDist) {
				bestDist = d;
				bestCell = i;
			}
		}
		return bestCell >= 0 ? new int[] { bestCell, bestDist } : null;
	}

	private static String onTerrainName(Level level, int pos) {
		if (level == null || pos < 0 || pos >= level.length())
			return "empty";
		int t = level.map[pos];
		if (t == Terrain.WATER)
			return "water";
		if (t == Terrain.GRASS || t == Terrain.HIGH_GRASS || t == Terrain.FURROWED_GRASS) {
			return "grass";
		}
		return "empty";
	}

	private static Set<String> statusNames(Char ch) {
		Set<String> names = new HashSet<>();
		if (ch == null)
			return names;
		if (ch.invisible > 0)
			names.add("invisible");
		if (ch.buff(Burning.class) != null)
			names.add("burning");
		if (ch.buff(Paralysis.class) != null)
			names.add("paralysed");
		if (ch.buff(Frost.class) != null)
			names.add("frozen");
		for (Buff buff : ch.buffs()) {
			names.add(buff.getClass().getSimpleName().toLowerCase(Locale.ROOT));
		}
		return names;
	}
}
