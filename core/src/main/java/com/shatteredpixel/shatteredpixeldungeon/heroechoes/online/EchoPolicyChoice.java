package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

/** Result of walking selection.order for one turn. */
public final class EchoPolicyChoice {

	public final String useRole;
	public final String layer;
	/** Recipe id when layer is recipes; otherwise null. */
	public final String recipeId;
	/** Optional inventory id override (e.g. door-break pick); null = resolve normally. */
	public final String itemId;

	public EchoPolicyChoice(String useRole, String layer, String recipeId) {
		this(useRole, layer, recipeId, null);
	}

	public EchoPolicyChoice(String useRole, String layer, String recipeId, String itemId) {
		if (useRole == null || useRole.isEmpty()) {
			throw new IllegalArgumentException("use_role is required");
		}
		this.useRole = useRole;
		this.layer = layer;
		this.recipeId = recipeId;
		this.itemId = itemId;
	}
}
