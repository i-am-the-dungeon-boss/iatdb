package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

import java.util.HashMap;
import java.util.Map;

/**
 * Every role the generated playbook can ask for, with the facts about it that
 * the rest of the package used to re-encode separately.
 * <p>
 * Before this existed, "is this a damage role", "is this a prep role", "does
 * this role drink or throw a potion" and "which virtual tag backs it" lived as
 * scattered string literals and hand-kept arrays across
 * {@link EchoPolicyHazards}, {@link EchoUntouchable},
 * {@link EchoPolicyStatusBuilder} and {@link EchoRoleExecutor} — so adding a
 * role meant editing six files and a typo in any of them failed silently. One
 * enum constant now carries all of it.
 * <p>
 * Roles still cross the wire as strings, so {@link #byId(String)} is the single
 * boundary: it returns {@code null} for anything outside this vocabulary rather
 * than throwing, and callers decide whether an unknown role is fatal.
 * <p>
 * Mirrored on the backend by {@code src/lib/echo/roles.ts}. Documented in
 * {@code hero-echoes/docs/features/echo-policy.md} § "Role catalogue".
 */
public enum EchoRole {

	// --- Damage --------------------------------------------------------------
	MELEE(Kind.DAMAGE, "*melee"),
	RANGED(Kind.DAMAGE, null),
	FINISHER(Kind.DAMAGE, null),
	WEAPON_ABILITY(Kind.DAMAGE, null),
	ARMOR_ABILITY(Kind.DAMAGE, null),
	/**
	 * Aimed at the hero cell specifically to pierce a soft blocker and still
	 * connect, so it is an attack rather than a bypass.
	 */
	PATH_THROUGH(Kind.DAMAGE, null),
	SURPRISE_SHOT(Kind.DAMAGE, null),
	/** Damage <em>and</em> a blob: seeds an area the hero has to leave. */
	PAYOFF_AOE(Kind.DAMAGE_BLOB, null),

	// --- Control -------------------------------------------------------------
	/** Freezing an untouchable hero turns the wasted window into free hits. */
	SETUP_CC(Kind.BLOB, null, 2),
	/** Leads the prep order: cornered, it buys the distance RUN could not. */
	KNOCKBACK(Kind.PLAIN, null, 1),
	BLIND(Kind.PLAIN, null, 3),
	FEAR(Kind.PLAIN, null, 4),

	// --- Recovery and self-buff ----------------------------------------------
	HEAL(Kind.SELF_DRINK, null, 5),
	CLEANSE(Kind.SELF_DRINK, null, 6),
	CLEANSE_BURN(Kind.SELF_DRINK, null, 7),
	/** The role; distinct from the lowercase {@code purity} enemy status. */
	PURITY(Kind.SELF_DRINK, null, 8),
	/** Grants armor, not shielding — see {@link #SHIELD_SELF}. */
	ARCANE_ARMOR(Kind.PLAIN, null, 9),
	HASTE(Kind.SELF_DRINK, null, 10),
	FIRE_IMBUE(Kind.PLAIN, null, 11),
	FROST_IMBUE(Kind.PLAIN, null, 12),
	TOXIC_IMBUE(Kind.PLAIN, null, 13),
	INVIS(Kind.SELF_DRINK, null, 14),
	STEALTH(Kind.PLAIN, null, 15),
	LEVITATE(Kind.SELF_DRINK, null, 16),
	/** Potion of Shielding is AC_CHOOSE; this role always means drink it. */
	SHIELD_SELF(Kind.SELF_DRINK, null),
	/** Generic "drink whatever this capability resolved to". */
	DRINK(Kind.SELF_DRINK, null),

	// --- Forced-throw roles ---------------------------------------------------
	THROW(Kind.THROW, null),
	THROW_POTION(Kind.THROW, null),
	GAS(Kind.THROW, null),

	// --- Movement and positioning --------------------------------------------
	CLOSE_IN(Kind.PLAIN, "*move_closer"),
	KEEP_DISTANCE(Kind.DISENGAGE, "*move_further"),
	BLINK(Kind.DISENGAGE, null),
	LEAVE_AOE(Kind.PLAIN, "*leave_aoe"),
	MOVE_TO_WATER(Kind.PLAIN, "*move_to_terrain:water"),
	MOVE_TO_GRASS(Kind.PLAIN, "*move_to_terrain:grass"),
	/** Strategy-only hold: legal solely via the matcher's {@code if_at_ideal}. */
	HOLD(Kind.PLAIN, "*wait"),

	// --- Obstacle clearing -----------------------------------------------------
	CLEAR_LOS(Kind.PLAIN, null),
	CLEAR_PLANT(Kind.PLAIN, null),
	DOOR_BREAK(Kind.PLAIN, null),

	// --- Cleric spells ---------------------------------------------------------
	GUIDING_LIGHT(Kind.DAMAGE, null),
	HOLY_WEAPON(Kind.PLAIN, null),
	HOLY_WARD(Kind.PLAIN, null),
	SMITE(Kind.DAMAGE, null),
	SUNRAY(Kind.PLAIN, null),
	LAY_ON_HANDS(Kind.PLAIN, null);

	/** How a role behaves; keeps the per-role flags from drifting apart. */
	private enum Kind {
		PLAIN, DAMAGE, BLOB, DAMAGE_BLOB, DISENGAGE, SELF_DRINK, THROW
	}

	/** Not a prep role. */
	private static final int NO_PREP = 0;

	private static final Map<String, EchoRole> BY_ID = buildIndex();

	private final Kind kind;
	private final String virtualTag;
	private final int prepRank;

	EchoRole(Kind kind, String virtualTag) {
		this(kind, virtualTag, NO_PREP);
	}

	EchoRole(Kind kind, String virtualTag, int prepRank) {
		this.kind = kind;
		this.virtualTag = virtualTag;
		this.prepRank = prepRank;
	}

	private static Map<String, EchoRole> buildIndex() {
		Map<String, EchoRole> index = new HashMap<>();
		EchoRole[] all = values();
		for (int i = 0; i < all.length; i++) {
			index.put(all[i].id(), all[i]);
		}
		return index;
	}

	/** The wire id. Identical to the enum name by construction. */
	public String id() {
		return name();
	}

	/**
	 * The role with this wire id, or {@code null} when the playbook names a role
	 * this build does not know.
	 */
	public static EchoRole byId(String id) {
		return id == null ? null : BY_ID.get(id);
	}

	public static boolean isKnown(String id) {
		return byId(id) != null;
	}

	/** Virtual item tag backing this role, or {@code null} for real kit. */
	public String virtualTag() {
		return virtualTag;
	}

	/** Roles that spend the turn trying to damage the hero. */
	public boolean isDamage() {
		return kind == Kind.DAMAGE || kind == Kind.DAMAGE_BLOB;
	}

	/** Roles whose whole point is seeding a blob on the hero. */
	public boolean isBlob() {
		return kind == Kind.BLOB || kind == Kind.DAMAGE_BLOB;
	}

	/** Roles whose whole point is putting distance between echo and hero. */
	public boolean isDisengage() {
		return kind == Kind.DISENGAGE;
	}

	/** Dual-mode / must-throw potions the policy nonetheless drinks by role. */
	public boolean drinksPotion() {
		return kind == Kind.SELF_DRINK;
	}

	/** Force shatter / throw regardless of the potion's default action. */
	public boolean throwsPotion() {
		return kind == Kind.THROW;
	}

	/** Roles the echo can usefully spend a wasted untouchable window on. */
	public boolean isPrep() {
		return prepRank != NO_PREP;
	}

	/** Lower spends first; only meaningful when {@link #isPrep()}. */
	public int prepRank() {
		return prepRank;
	}

	// Convenience forms for the wire strings that arrive from the playbook.

	public static boolean isDamageRole(String id) {
		EchoRole role = byId(id);
		return role != null && role.isDamage();
	}

	public static boolean isBlobRole(String id) {
		EchoRole role = byId(id);
		return role != null && role.isBlob();
	}

	public static boolean isDisengageRole(String id) {
		EchoRole role = byId(id);
		return role != null && role.isDisengage();
	}

	public static boolean isPrepRole(String id) {
		EchoRole role = byId(id);
		return role != null && role.isPrep();
	}
}
