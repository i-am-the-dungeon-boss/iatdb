package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Per-turn sense snapshot for policy matching.
 * <p>
 * Design record: {@code hero-echoes/docs/features/echo-policy.md} § "One turn, end to end".
 */
public final class EchoPolicyStatus {

	public final float selfHpRatio;
	public final float enemyHpRatio;
	/**
	 * Enemy
	 * {@link com.shatteredpixel.shatteredpixeldungeon.actors.Char#shielding()} /
	 * HT.
	 */
	public final float enemyShieldRatio;
	public final int distance;
	public final boolean enemyInLos;
	public final String selfClass;
	public final String enemyClass;
	public final String onTerrain;
	public final Set<String> selfStatuses;
	public final Set<String> enemyStatuses;
	public final Set<String> rolesReady;
	/**
	 * terrain type → distance in tiles (≤ tuning.terrain_near_tiles to count as
	 * near).
	 */
	public final Map<String, Integer> terrainNearDistance;
	/** terrain type → cell for MOVE_TO_*. */
	public final Map<String, Integer> terrainNearCell;
	/**
	 * role id → the cell the sense phase resolved for it. Only the sense phase
	 * knows the geometry behind a role like {@code CLEAR_PLANT} or
	 * {@code CLEAR_LOS}, so it hands the cell to the plan here rather than
	 * leaving execute to re-derive an aim it cannot reconstruct.
	 */
	private final Map<String, Integer> roleTargetCells;
	public final Set<String> safeHazards;
	public final Set<String> unsafeHazards;
	public final int terrainNearTiles;
	/**
	 * True when echo move speed strictly exceeds the hero's (rings, glyphs,
	 * potions, armor).
	 */
	public final boolean selfSpeedGtEnemy;
	/**
	 * What the echo is doing about a hero it cannot currently hurt. Its
	 * JSON-visible projections are {@code damage_immune} on the enemy and
	 * {@link EchoPolicyHazards#NO_ESCAPE} on self, derived in the same place so
	 * the three cannot drift.
	 */
	public final EchoUntouchable.Stance untouchableStance;

	private EchoPolicyStatus(Builder b) {
		this.selfHpRatio = b.selfHpRatio;
		this.enemyHpRatio = b.enemyHpRatio;
		this.enemyShieldRatio = b.enemyShieldRatio;
		this.distance = b.distance;
		this.enemyInLos = b.enemyInLos;
		this.selfClass = b.selfClass != null ? b.selfClass : "";
		this.enemyClass = b.enemyClass != null ? b.enemyClass : "";
		this.onTerrain = b.onTerrain != null ? b.onTerrain : "empty";
		this.selfStatuses = Collections.unmodifiableSet(new HashSet<>(b.selfStatuses));
		this.enemyStatuses = Collections.unmodifiableSet(new HashSet<>(b.enemyStatuses));
		this.rolesReady = Collections.unmodifiableSet(new HashSet<>(b.rolesReady));
		this.terrainNearDistance = Collections.unmodifiableMap(new HashMap<>(b.terrainNearDistance));
		this.terrainNearCell = Collections.unmodifiableMap(new HashMap<>(b.terrainNearCell));
		this.roleTargetCells = Collections.unmodifiableMap(new HashMap<>(b.roleTargetCells));
		this.safeHazards = Collections.unmodifiableSet(new HashSet<>(b.safeHazards));
		this.unsafeHazards = Collections.unmodifiableSet(new HashSet<>(b.unsafeHazards));
		this.terrainNearTiles = b.terrainNearTiles;
		this.selfSpeedGtEnemy = b.selfSpeedGtEnemy;
		this.untouchableStance = b.untouchableStance;
	}

	public boolean isTerrainNear(String type) {
		Integer d = terrainNearDistance.get(type);
		return d != null && d <= terrainNearTiles;
	}

	/**
	 * @return the cell the sense phase resolved for this role, or -1 when the
	 *         role has no geometry of its own and execute should aim normally.
	 */
	public int targetCellFor(String role) {
		Integer cell = roleTargetCells.get(role);
		return cell != null ? cell : -1;
	}

	public boolean isRoleReady(String role) {
		return rolesReady.contains(role);
	}

	public boolean isSafeFor(String roleOrHazard) {
		return safeHazards.contains(roleOrHazard);
	}

	public boolean isUnsafeFor(String roleOrHazard) {
		return unsafeHazards.contains(roleOrHazard);
	}

	public static final class Builder {
		private float selfHpRatio = 1f;
		private float enemyHpRatio = 1f;
		private float enemyShieldRatio = 0f;
		private int distance = 1;
		private boolean enemyInLos = true;
		private String selfClass = "";
		private String enemyClass = "";
		private String onTerrain = "empty";
		private Set<String> selfStatuses = new HashSet<>();
		private Set<String> enemyStatuses = new HashSet<>();
		private Set<String> rolesReady = new HashSet<>();
		private Map<String, Integer> terrainNearDistance = new HashMap<>();
		private Map<String, Integer> terrainNearCell = new HashMap<>();
		private Map<String, Integer> roleTargetCells = new HashMap<>();
		private Set<String> safeHazards = new HashSet<>();
		private Set<String> unsafeHazards = new HashSet<>();
		private int terrainNearTiles = 3;
		private boolean selfSpeedGtEnemy = false;
		private EchoUntouchable.Stance untouchableStance = EchoUntouchable.Stance.NONE;

		public Builder selfHpRatio(float v) {
			selfHpRatio = v;
			return this;
		}

		public Builder enemyHpRatio(float v) {
			enemyHpRatio = v;
			return this;
		}

		public Builder enemyShieldRatio(float v) {
			enemyShieldRatio = v;
			return this;
		}

		public Builder distance(int v) {
			distance = v;
			return this;
		}

		public Builder enemyInLos(boolean v) {
			enemyInLos = v;
			return this;
		}

		public Builder selfClass(String v) {
			selfClass = v;
			return this;
		}

		public Builder enemyClass(String v) {
			enemyClass = v;
			return this;
		}

		public Builder onTerrain(String v) {
			onTerrain = v;
			return this;
		}

		public Builder selfStatuses(Set<String> v) {
			selfStatuses = v;
			return this;
		}

		public Builder enemyStatuses(Set<String> v) {
			enemyStatuses = v;
			return this;
		}

		public Builder rolesReady(Set<String> v) {
			rolesReady = v;
			return this;
		}

		public Builder terrainNearTiles(int v) {
			terrainNearTiles = v;
			return this;
		}

		public Builder safeHazards(Set<String> v) {
			safeHazards = v;
			return this;
		}

		public Builder unsafeHazards(Set<String> v) {
			unsafeHazards = v;
			return this;
		}

		public Builder terrainNear(String type, int distance) {
			terrainNearDistance.put(type, distance);
			return this;
		}

		public Builder terrainNearCell(String type, int cell) {
			terrainNearCell.put(type, cell);
			return this;
		}

		/** Records the sensed aim cell for a role; -1 clears it. */
		public Builder roleTargetCell(String role, int cell) {
			if (role == null) {
				return this;
			}
			if (cell < 0) {
				roleTargetCells.remove(role);
			} else {
				roleTargetCells.put(role, cell);
			}
			return this;
		}

		public Builder selfSpeedGtEnemy(boolean v) {
			selfSpeedGtEnemy = v;
			return this;
		}

		public Builder untouchableStance(EchoUntouchable.Stance v) {
			untouchableStance = v != null ? v : EchoUntouchable.Stance.NONE;
			return this;
		}

		public EchoPolicyStatus build() {
			return new EchoPolicyStatus(this);
		}
	}
}
