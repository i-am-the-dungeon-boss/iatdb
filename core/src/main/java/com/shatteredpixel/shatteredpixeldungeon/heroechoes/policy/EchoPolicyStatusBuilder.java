package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BlobImmunity;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invulnerability;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Stamina;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.watabou.utils.PathFinder;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Builds per-turn {@link EchoPolicyStatus} from the live fight.
 * <p>
 * Design record: {@code hero-echoes/docs/features/echo-policy.md} § "One turn, end to end" →
 * "Phase 1 — Sense".
 */
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
		// Blind-defense aim still needs a remembered last-seen cell.
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
			recordTerrain(
					boss, level, boss.pos, Terrain.WATER, "water", nearTiles, nearDist, nearCell);
			recordTerrain(
					boss, level, boss.pos, Terrain.GRASS, "grass", nearTiles, nearDist, nearCell);
			recordTerrain(
					boss, level, boss.pos, Terrain.HIGH_GRASS, "grass", nearTiles, nearDist, nearCell);
		}

		boolean onWater = "water".equals(onTerrainName(level, boss.pos));
		boolean waterNear = nearDist.containsKey("water");
		boolean clearOfBlast = enemy == null || level == null
				|| level.distance(boss.pos, enemy.pos) >= 2;

		Set<String> available = EchoInventory.availableIds(echoHero);
		Set<String> safe = new HashSet<>();
		Set<String> unsafe = new HashSet<>();
		Set<String> selfStatuses = statusNames(boss);
		if (EchoAoeDots.isAoeDotAt(boss, boss.pos)) {
			selfStatuses.add(EchoAoeDots.STATUS);
		}
		// Re-decided below, once readiness is known. Reset first so the sense
		// itself runs against the normal "plants are walls" map.
		boss.setAvoidHarmfulPlants(true);
		int plantBlocker = sensePlantBlocked(boss, level, selfStatuses);
		int losBlocker = senseLosBlocked(boss, level, selfStatuses, inLos);
		sensePathBlocked(boss, level, selfStatuses);
		// Enemy statuses drive hard gates below, so they must be sensed first.
		Set<String> enemyStatuses = enemy != null ? statusNames(enemy) : new HashSet<String>();

		// The echo's own shield drives the SHIELD_SELF anti-stacking gate, so it
		// must be read from the same helper as the hero's before roles are sensed.
		int ownShield = EchoUntouchable.temporaryShielding(boss);
		boss.noteSelfShield(ownShield);
		if (ownShield > 0) {
			selfStatuses.add(EchoPolicyHazards.SELF_SHIELDED);
		}

		// Observe → pre-gate → stance → gate → hazards. The stance needs to know
		// which roles are ready before it can decide, and the gate then depends on
		// the stance, so readiness is computed in two passes over one sense.
		JSONObject caps = policy.root().optJSONObject("capabilities");
		Set<String> preGateReady = readyRolesBeforeStanceGate(
				caps, boss, enemy, level, echoHero, tuning, available, enemyStatuses,
				hasAimAtEnemy(boss, inLos, enemyStatuses), plantBlocker, losBlocker);

		boolean invulnerable = EchoUntouchable.isInvulnerable(enemy, boss.getClass());
		boolean bigShield = EchoUntouchable.hasBigTemporaryShield(enemy);
		EchoUntouchable.Stance stance = EchoUntouchable.stanceFor(
				invulnerable, bigShield, preGateReady,
				hasStepAwayFrom(boss, enemy, level), boss.disengageTurns(),
				ownShield > 0, selfHp);
		if (stance != EchoUntouchable.Stance.NONE) {
			// Aggregate "attacking this is pointless, but only for now". Derived from
			// the stance so a playbook can never see it while the echo is fighting on.
			enemyStatuses.add(EchoPolicyHazards.DAMAGE_IMMUNE);
		}
		if (stance == EchoUntouchable.Stance.PREP || stance == EchoUntouchable.Stance.FIGHT) {
			selfStatuses.add(EchoPolicyHazards.NO_ESCAPE);
		}

		Set<String> rolesReady = new HashSet<>();
		for (String role : preGateReady) {
			if (!EchoUntouchable.gatesRole(stance, role)) {
				rolesReady.add(role);
			}
		}
		classifyHazards(caps, rolesReady, clearOfBlast, onWater, waterNear, safe, unsafe);
		boss.setAvoidHarmfulPlants(
				!isPlantWalledIn(selfStatuses, rolesReady));

		EchoPolicyStatus.Builder b = new EchoPolicyStatus.Builder()
				.untouchableStance(stance)
				.selfHpRatio(selfHp)
				.enemyHpRatio(enemyHp)
				.enemyShieldRatio(enemyShield)
				.distance(distance)
				.enemyInLos(inLos)
				.selfClass(boss.getEcho().heroClass)
				.enemyClass(enemy != null && enemy.heroClass != null ? enemy.heroClass.name() : "")
				.onTerrain(onTerrainName(level, boss.pos))
				.selfStatuses(selfStatuses)
				.enemyStatuses(enemyStatuses)
				.terrainNearTiles(nearTiles)
				.rolesReady(rolesReady)
				.safeHazards(safe)
				.unsafeHazards(unsafe)
				.selfSpeedGtEnemy(enemy != null && boss.speed() > enemy.combatSpeed());

		for (Map.Entry<String, Integer> e : nearDist.entrySet()) {
			b.terrainNear(e.getKey(), e.getValue());
		}
		for (Map.Entry<String, Integer> e : nearCell.entrySet()) {
			b.terrainNearCell(e.getKey(), e.getValue());
		}
		recordRoleTargets(b, rolesReady, plantBlocker, losBlocker);
		return b.build();
	}

	/**
	 * Hands every role whose aim this phase resolved its cell, so the plan the
	 * matcher builds carries the target instead of leaving
	 * {@link EchoRoleExecutor} to guess at one it cannot reconstruct. Roles
	 * absent here aim normally via {@link EchoTargetPicker}.
	 */
	private static void recordRoleTargets(
			EchoPolicyStatus.Builder b, Set<String> rolesReady,
			int plantBlocker, int losBlocker) {
		if (rolesReady.contains(EchoPolicyHazards.CLEAR_PLANT)) {
			b.roleTargetCell(EchoPolicyHazards.CLEAR_PLANT, plantBlocker);
		}
		if (rolesReady.contains(EchoRole.CLEAR_LOS.id())) {
			b.roleTargetCell(EchoRole.CLEAR_LOS.id(), losBlocker);
		}
	}

	/**
	 * The four capability gates that do not depend on the untouchable stance:
	 * a ready item, virtual feasibility, the potion reserve, and legality
	 * against the current hero.
	 */
	private static Set<String> readyRolesBeforeStanceGate(
			JSONObject caps, EchoBoss boss, Hero enemy, Level level, Hero echoHero,
			JSONObject tuning, Set<String> available, Set<String> enemyStatuses,
			boolean aimAvailable, int plantBlocker, int losBlocker) {
		Set<String> ready = new HashSet<>();
		if (caps == null) {
			return ready;
		}
		Iterator<String> keys = caps.keys();
		while (keys.hasNext()) {
			String role = keys.next();
			JSONObject cap = caps.optJSONObject(role);
			if (cap == null)
				continue;
			if (!EchoRoleResolver.roleHasReadyItem(cap, available))
				continue;
			if (!virtualRoleFeasible(role, boss, enemy, level, plantBlocker, losBlocker))
				continue;
			if (!respectsPotionReserve(role, cap, tuning, echoHero))
				continue;
			if (!allowedAgainstEnemy(role, cap, available, enemyStatuses))
				continue;
			if (!aimAvailable && needsAimAtEnemy(role))
				continue;
			ready.add(role);
		}
		return ready;
	}

	/**
	 * A harmful plant blocks the only sensible route, and the echo can neither
	 * burn it nor fight past it from where it stands. Walking through is the
	 * only remaining way to have a fight at all, so movement stops treating
	 * plants as walls for this turn.
	 */
	private static boolean isPlantWalledIn(Set<String> selfStatuses, Set<String> rolesReady) {
		if (!selfStatuses.contains(EchoPolicyHazards.PLANT_BLOCKED)) {
			return false;
		}
		if (rolesReady.contains(EchoPolicyHazards.CLEAR_PLANT)) {
			return false;
		}
		for (String role : rolesReady) {
			if (needsAimAtEnemy(role)) {
				return false;
			}
		}
		return true;
	}

	private static boolean needsAimAtEnemy(String role) {
		EchoRole known = EchoRole.byId(role);
		// An unrecognised role has no aim requirement this build can assert.
		return known != null && known.needsAimAtEnemy();
	}

	/**
	 * Whether {@link EchoTargetPicker} would find any cell to aim at this turn:
	 * the hero is visible, or cloaked with blind-defence shots still budgeted
	 * against the last cell it was seen in. Mirrors the picker's own opening
	 * gate so readiness and execute cannot disagree.
	 */
	private static boolean hasAimAtEnemy(EchoBoss boss, boolean inLos, Set<String> enemyStatuses) {
		if (inLos) {
			return true;
		}
		return enemyStatuses.contains("invisible")
				&& boss.blindDefenseShotsLeft() > 0
				&& boss.lastSeenEnemyPos() >= 0;
	}

	/**
	 * Splits the <em>final</em> ready roles into safe / unsafe by their declared
	 * hazard, so a stance-gated role lands in neither.
	 */
	private static void classifyHazards(
			JSONObject caps, Set<String> rolesReady, boolean clearOfBlast,
			boolean onWater, boolean waterNear, Set<String> safe, Set<String> unsafe) {
		if (caps == null) {
			return;
		}
		for (String role : rolesReady) {
			JSONObject cap = caps.optJSONObject(role);
			String hazard = cap != null ? cap.optString("hazard", "") : "";
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

	/**
	 * Fail-closed legality against the current hero, independent of what the
	 * generated policy asks for. A custom or stale playbook must not be able to
	 * waste kit on a target that cannot be affected by it.
	 * <p>
	 * Note the immunity gate is <em>not</em> here: whether damage roles are
	 * unready while the hero is untouchable is stance-dependent and lives in
	 * {@link EchoUntouchable#gatesRole}, because the echo deliberately keeps
	 * swinging once it has shielded up or run out of disengage budget.
	 */
	private static boolean allowedAgainstEnemy(
			String role, JSONObject cap, Set<String> available, Set<String> enemyStatuses) {
		// Potion of Purity: every blob-based setup / payoff is wasted.
		if (enemyStatuses.contains(EchoPolicyHazards.PURITY)
				&& EchoPolicyHazards.isBlobRole(role)) {
			return false;
		}
		// Shared stun lockout invalidates SETUP_CC stun items, not the role.
		if (enemyStatuses.contains(EchoPolicyHazards.PARALYSIS_IMMUNITY)
				&& EchoPolicyHazards.SETUP_CC.equals(role)
				&& !EchoRoleResolver.roleHasReadyItem(
						EchoPolicySafety.withoutParalyticGas(cap), available)) {
			return false;
		}
		return true;
	}

	private static boolean virtualRoleFeasible(
			String role, EchoBoss boss, Hero enemy, Level level,
			int plantBlocker, int losBlocker) {
		EchoRole known = EchoRole.byId(role);
		if (known == null) {
			// A role this build does not recognise has no precondition to check.
			return true;
		}
		switch (known) {
			case SHIELD_SELF:
				// Anti-stacking: never burn a second shielding item while the one the
				// echo already spent is still mostly intact. Gated here rather than in
				// the stance so reactions and recipes obey it too.
				return !boss.hasMostlyIntactShield();
			case HASTE:
				// Same anti-stack idea: Haste and Stamina are the HASTE role's drinks.
				// A second bottle while either is still up wastes the kit.
				return boss.buff(Haste.class) == null && boss.buff(Stamina.class) == null;
			case MOVE_TO_WATER:
				return level != null
						&& nearestTerrainCell(
								level, boss, boss.pos, Terrain.WATER, Integer.MAX_VALUE) != null;
			case MOVE_TO_GRASS:
				return level != null
						&& (nearestTerrainCell(
								level, boss, boss.pos, Terrain.GRASS, Integer.MAX_VALUE) != null
								|| nearestTerrainCell(
										level, boss, boss.pos, Terrain.HIGH_GRASS,
										Integer.MAX_VALUE) != null);
			case LEAVE_AOE:
				return EchoAoeDots.canLeave(boss);
			case BLINK:
				return EchoTargetPicker.pickBlinkAway(boss) >= 0;
			case KEEP_DISTANCE:
				return hasStepAwayFrom(boss, enemy, level);
			case CLOSE_IN:
				// Same reasoning as KEEP_DISTANCE above: a virtual movement role
				// is only ready when the board actually offers the step. Nothing
				// to close on leaves it usable, as with hasStepAwayFrom.
				return enemy == null || boss.hasStepCloser(enemy.pos);
			case CLEAR_PLANT:
				return isBlockerAimable(boss, plantBlocker);
			case CLEAR_LOS:
				// Falling through to the default reported the role permanently
				// ready, so a playbook naming it burned kit on an aimless shot
				// at the hero.
				return isBlockerAimable(boss, losBlocker);
			default:
				return true;
		}
	}

	/**
	 * True when a blocked-route sense identified a cell and a straight throw/zap
	 * from the echo actually reaches it (no wall or another blocker short of
	 * it). A blocker the echo cannot hit is not one it can clear.
	 */
	private static boolean isBlockerAimable(EchoBoss boss, int cell) {
		if (cell < 0) {
			return false;
		}
		Ballistica path = new Ballistica(boss.pos, cell, Ballistica.PROJECTILE);
		return path.collisionPos == cell;
	}

	/**
	 * A harmful plant sits on the only reasonably short route to
	 * {@link EchoBoss#policyFocusCell()}: the path that must avoid it is null or
	 * much longer than one that may cross it. Marks self status
	 * {@link EchoPolicyHazards#PLANT_BLOCKED} and returns the first harmful
	 * plant on the short route.
	 * <p>
	 * Both searches share {@link EchoAoeDots#isAoeDotAt} for current fire/gas —
	 * the only difference between them is the plant exclusion, so any gap in
	 * path length can only be attributed to a plant. Neither uses the
	 * predicted-growth ring: that only matters for movement about to happen,
	 * not for judging whether the general route is open.
	 */
	private static int sensePlantBlocked(EchoBoss boss, Level level, Set<String> selfStatuses) {
		if (level == null) {
			return -1;
		}
		int focus = boss.policyFocusCell();
		if (focus < 0 || focus >= level.length() || focus == boss.pos) {
			return -1;
		}

		boolean[] avoidingPlants = level.passable.clone();
		boss.modifyPassable(avoidingPlants);
		PathFinder.Path directPath = PathFinder.find(boss.pos, focus, avoidingPlants);

		boolean[] crossingPlants = level.passable.clone();
		for (int i = 0; i < crossingPlants.length; i++) {
			if (crossingPlants[i] && i != boss.pos && EchoAoeDots.isAoeDotAt(boss, i)) {
				crossingPlants[i] = false;
			}
		}
		PathFinder.Path shortPath = PathFinder.find(boss.pos, focus, crossingPlants);
		if (shortPath == null) {
			return -1;
		}

		boolean detour = directPath == null || directPath.size() > 2 * shortPath.size();
		if (!detour) {
			return -1;
		}

		for (int cell : shortPath) {
			if (EchoAoeDots.isHarmfulPlantAt(cell)) {
				selfStatuses.add(EchoPolicyHazards.PLANT_BLOCKED);
				return cell;
			}
		}
		return -1;
	}

	/**
	 * Burnable grass, not a wall, is what is eating the sightline to the hero:
	 * the one blocked sightline the echo can do something about. Marks self
	 * status {@link EchoPolicyHazards#LOS_BLOCKED} and returns the grass cell so
	 * {@code CLEAR_LOS} aims at the grass rather than at a hero it cannot see.
	 * <p>
	 * The line is traced with {@link Ballistica#STOP_TARGET} alone so it runs
	 * the whole way to the focus, and the <em>first</em> sight-blocking cell on
	 * it decides: grass behind a wall is not the reason the hero is hidden, and
	 * burning it opens nothing.
	 */
	private static int senseLosBlocked(
			EchoBoss boss, Level level, Set<String> selfStatuses, boolean inLos) {
		if (level == null || inLos) {
			return -1;
		}
		int focus = boss.policyFocusCell();
		if (focus < 0 || focus >= level.length() || focus == boss.pos) {
			return -1;
		}
		Ballistica line = new Ballistica(boss.pos, focus, Ballistica.STOP_TARGET);
		if (line.collisionPos == null) {
			return -1;
		}
		// Only as far as the focus: the raw path runs on past it to the map edge.
		for (int cell : line.subPath(1, line.path.indexOf(line.collisionPos))) {
			if (cell < 0 || cell >= level.length() || !level.losBlocking[cell]) {
				continue;
			}
			if (!isBurnableGrass(level, cell)) {
				// A wall: nothing the echo carries opens this sightline.
				return -1;
			}
			selfStatuses.add(EchoPolicyHazards.LOS_BLOCKED);
			return cell;
		}
		return -1;
	}

	private static boolean isBurnableGrass(Level level, int cell) {
		int terrain = level.map[cell];
		return terrain == Terrain.HIGH_GRASS || terrain == Terrain.FURROWED_GRASS;
	}

	/**
	 * A character that is not the hero stands in the shot, so an ordinary
	 * ranged role would spend the turn hitting a sheep. Marks self status
	 * {@link EchoPolicyHazards#PATH_BLOCKED}, which the playbook answers with
	 * {@code PATH_THROUGH} — a role aimed at the hero's own cell precisely to
	 * punch through the blocker, so it needs no target cell of its own.
	 */
	private static void sensePathBlocked(EchoBoss boss, Level level, Set<String> selfStatuses) {
		if (level == null || Dungeon.hero == null) {
			return;
		}
		int focus = Dungeon.hero.pos;
		if (focus < 0 || focus >= level.length() || focus == boss.pos) {
			return;
		}
		Ballistica shot = new Ballistica(boss.pos, focus, Ballistica.PROJECTILE);
		if (shot.collisionPos == null || shot.collisionPos == focus) {
			return;
		}
		Char blocker = Actor.findChar(shot.collisionPos);
		if (blocker != null && blocker != boss && blocker != Dungeon.hero) {
			selfStatuses.add(EchoPolicyHazards.PATH_BLOCKED);
		}
	}

	/**
	 * True when some adjacent cell is legal to stand on and strictly farther from
	 * the hero.
	 * <p>
	 * Without this, {@code KEEP_DISTANCE} is ready even with the echo backed into
	 * a corner: the matcher picks it, the step fails, and the turn is thrown away.
	 * Reactions that back off also need to know the difference, so that being
	 * cornered can fall through to something useful instead.
	 */
	private static boolean hasStepAwayFrom(EchoBoss boss, Hero enemy, Level level) {
		if (enemy == null || level == null || !level.insideMap(enemy.pos)) {
			// Nothing to back away from; leave the role usable.
			return true;
		}
		int current = level.distance(boss.pos, enemy.pos);
		for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
			int cell = boss.pos + PathFinder.NEIGHBOURS8[i];
			if (!level.insideMap(cell) || !boss.policyCellPathable(cell)) {
				continue;
			}
			if (level.distance(cell, enemy.pos) > current) {
				return true;
			}
		}
		return false;
	}

	private static void recordTerrain(
			Char ch, Level level, int from, int terrain, String name, int maxDist,
			Map<String, Integer> nearDist, Map<String, Integer> nearCell) {
		int[] found = nearestTerrainCell(level, ch, from, terrain, maxDist);
		if (found == null)
			return;
		Integer prev = nearDist.get(name);
		if (prev == null || found[1] < prev) {
			nearDist.put(name, found[1]);
			nearCell.put(name, found[0]);
		}
	}

	/** @return int[]{cell, distance} or null; no hazard filtering. */
	static int[] nearestTerrainCell(Level level, int from, int terrain, int maxDist) {
		return nearestTerrainCell(level, null, from, terrain, maxDist);
	}

	/**
	 * Nearest {@code terrain} cell that is somewhere {@code ch} could actually
	 * stand.
	 * <p>
	 * A terrain destination is only worth walking to if the tile is clear of
	 * harm: water under a fire or a gas cloud puts the burning echo back in a
	 * DoT for the sake of leaving one, and grass under a blaze is not cover.
	 * Hazardous tiles are therefore skipped entirely rather than reported and
	 * refused later — that keeps {@code terrain_near} honest, so a playbook that
	 * asks for {@code terrain_near_none: water} before falling back to
	 * {@code CLEANSE_BURN} takes the fallback instead of stalling on water it
	 * must not use.
	 * <p>
	 * Two passes, mirroring {@link EchoAoeDots#bestExit}: a tile that is only
	 * unsafe because gas is predicted to spread onto it is used when no strictly
	 * clear tile of that terrain exists. Tiles harmful <em>now</em>, and harmful
	 * plants, are never accepted.
	 *
	 * @param ch character the hazard is judged for; {@code null} disables
	 *           filtering
	 * @return int[]{cell, distance} or null
	 */
	static int[] nearestTerrainCell(Level level, Char ch, int from, int terrain, int maxDist) {
		if (level == null)
			return null;
		int bestCell = -1;
		int bestDist = Integer.MAX_VALUE;
		int fallbackCell = -1;
		int fallbackDist = Integer.MAX_VALUE;
		for (int i = 0; i < level.length(); i++) {
			if (level.map[i] != terrain)
				continue;
			int d = level.distance(from, i);
			if (d > maxDist)
				continue;
			if (ch != null && i != from && EchoAoeDots.isAoeHazardForPath(ch, i, false)) {
				continue;
			}
			boolean strict = ch == null || i == from
					|| !EchoAoeDots.isAoeHazardForPath(ch, i, true);
			if (strict) {
				if (d < bestDist) {
					bestDist = d;
					bestCell = i;
				}
			} else if (d < fallbackDist) {
				fallbackDist = d;
				fallbackCell = i;
			}
		}
		if (bestCell >= 0) {
			return new int[] { bestCell, bestDist };
		}
		return fallbackCell >= 0 ? new int[] { fallbackCell, fallbackDist } : null;
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
		// Explicit aliases for specific buffs (backward-compatible with auto-lowercase).
		// Paralysis.Immunity lowercases to a bare "immunity" that reads as purity —
		// policy must key on these aliases, never on the simpleName.
		if (ch.buff(BlobImmunity.class) != null)
			names.add(EchoPolicyHazards.PURITY);
		if (ch.buff(Paralysis.Immunity.class) != null)
			names.add(EchoPolicyHazards.PARALYSIS_IMMUNITY);
		if (ch.buff(Invulnerability.class) != null)
			names.add(EchoPolicyHazards.INVULNERABLE);
		// damage_immune is deliberately not derived here: it is stance-dependent
		// and added for the enemy only, in build().
		if (EchoUntouchable.hasTemporaryShield(ch))
			names.add(EchoPolicyHazards.TEMP_SHIELD);
		for (Buff buff : ch.buffs()) {
			names.add(buff.getClass().getSimpleName().toLowerCase(Locale.ROOT));
		}
		return names;
	}
}
