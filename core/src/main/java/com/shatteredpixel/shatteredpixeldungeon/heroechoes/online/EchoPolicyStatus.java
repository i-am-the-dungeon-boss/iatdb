package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Per-turn sense snapshot for policy matching (canvas §5). */
public final class EchoPolicyStatus {

	public final float selfHpRatio;
	public final float enemyHpRatio;
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
	public final Set<String> safeHazards;
	public final Set<String> unsafeHazards;
	public final int terrainNearTiles;

	private EchoPolicyStatus(Builder b) {
		this.selfHpRatio = b.selfHpRatio;
		this.enemyHpRatio = b.enemyHpRatio;
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
		this.safeHazards = Collections.unmodifiableSet(new HashSet<>(b.safeHazards));
		this.unsafeHazards = Collections.unmodifiableSet(new HashSet<>(b.unsafeHazards));
		this.terrainNearTiles = b.terrainNearTiles;
	}

	public boolean isTerrainNear(String type) {
		Integer d = terrainNearDistance.get(type);
		return d != null && d <= terrainNearTiles;
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
		private Set<String> safeHazards = new HashSet<>();
		private Set<String> unsafeHazards = new HashSet<>();
		private int terrainNearTiles = 3;

		public Builder selfHpRatio(float v) {
			selfHpRatio = v;
			return this;
		}

		public Builder enemyHpRatio(float v) {
			enemyHpRatio = v;
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

		public EchoPolicyStatus build() {
			return new EchoPolicyStatus(this);
		}
	}
}
