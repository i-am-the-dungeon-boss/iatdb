package com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy;

/**
 * The one turn the echo has decided to take: a resolved plan, not just a role
 * name.
 * <p>
 * Before this type existed the match phase handed execute a role string and
 * nothing else, so execute re-derived every aim from scratch and the sense
 * phase's geometry reached it only through mutable fields on
 * {@code EchoBoss}. That cost real behaviour — {@code CLEAR_PLANT} fired at the
 * hero instead of at the plant blocking its route, and every other targeted
 * role needed a bespoke {@code if} in the executor to find its cell.
 * <p>
 * So the plan carries what only the phase that resolved it can know:
 * {@link #role} as a typed {@link EchoRole} rather than a wire string, and
 * {@link #targetCell} as the cell the sense phase picked for that role. See
 * {@code docs/hero-echoes/echo-boss-code-patterns.md} § 5.1. Resolving the
 * {@code Item} and the turn cost into the plan as well is § 5.2 / § 5.5 and is
 * still open.
 */
public final class EchoPlan {

	/** The wire id, as the playbook named it. Never null or empty. */
	public final String useRole;
	/**
	 * The same role typed, or {@code null} when the playbook named a role this
	 * build does not know. Callers that must handle unknown roles keep reading
	 * {@link #useRole}; everything else should switch on this.
	 */
	public final EchoRole role;
	public final String layer;
	/** Recipe id when layer is recipes; otherwise null. */
	public final String recipeId;
	/** Optional inventory id override (e.g. a recipe's named item); null = resolve normally. */
	public final String itemId;
	/** Cell resolved by the sense phase; -1 = none, aim normally. */
	public final int targetCell;

	public EchoPlan(String useRole, String layer, String recipeId) {
		this(useRole, layer, recipeId, null, -1);
	}

	public EchoPlan(String useRole, String layer, String recipeId, String itemId) {
		this(useRole, layer, recipeId, itemId, -1);
	}

	public EchoPlan(String useRole, String layer, String recipeId, String itemId, int targetCell) {
		if (useRole == null || useRole.isEmpty()) {
			throw new IllegalArgumentException("use_role is required");
		}
		this.useRole = useRole;
		this.role = EchoRole.byId(useRole);
		this.layer = layer;
		this.recipeId = recipeId;
		this.itemId = itemId;
		this.targetCell = targetCell;
	}

	/**
	 * Builds the plan for a role picked out of {@code status}, carrying whatever
	 * cell the sense phase resolved for that role. Every producer of a plan
	 * should come through here so no layer can silently drop the target.
	 */
	public static EchoPlan resolve(
			String useRole, String layer, String recipeId, EchoPolicyStatus status) {
		return new EchoPlan(
				useRole, layer, recipeId, null,
				status != null ? status.targetCellFor(useRole) : -1);
	}

	/** True when this plan is for {@code candidate}. Null-safe on unknown roles. */
	public boolean is(EchoRole candidate) {
		return role != null && role == candidate;
	}

	@Override
	public String toString() {
		return "EchoPlan[" + useRole + " layer=" + layer
				+ (recipeId != null ? " recipe=" + recipeId : "")
				+ (itemId != null ? " item=" + itemId : "")
				+ (targetCell >= 0 ? " target=" + targetCell : "")
				+ "]";
	}
}
