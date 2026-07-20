package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.watabou.noosa.Game;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Merged echo policy blob from the backend (capabilities / reactions / …).
 * Persisted as received;
 * {@link com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss}
 * matches and executes roles each hunting turn.
 */
public final class EchoPolicy {

	public final String schemaVersion;
	private final JSONObject root;

	public EchoPolicy(JSONObject root) {
		JSONObject copy = root != null ? new JSONObject(root.toString()) : new JSONObject();
		this.root = copy;
		this.schemaVersion = readSchemaVersion(copy);
	}

	/** Label written into fallback / local policies (not enforced). */
	public static String supportedSchemaVersion() {
		return Game.version != null ? Game.version : "";
	}

	public JSONObject root() {
		return root;
	}

	public static EchoPolicy fromJson(String json) {
		return fromJson(new JSONObject(json));
	}

	public static EchoPolicy fromJson(JSONObject root) {
		return new EchoPolicy(root);
	}

	public static EchoPolicy fallback() {
		JSONObject root = new JSONObject();
		root.put("policy_schema_version", supportedSchemaVersion());
		JSONObject capabilities = new JSONObject();
		capabilities.put("MELEE", new JSONObject()
				.put("pick", "FIRST_LEGAL")
				.put("items", new JSONArray().put("*melee")));
		root.put("capabilities", capabilities);
		root.put("reactions", new JSONArray());
		root.put("recipes", new JSONArray());
		root.put("positioning", new JSONObject());
		root.put("matchups", new JSONObject());
		root.put("selection", new JSONObject()
				.put("order", new JSONArray().put("default"))
				.put("default_roles", new JSONArray().put("MELEE").put("WAIT")));
		root.put("tuning", new JSONObject());
		return new EchoPolicy(root);
	}

	/** Role-based policy with capabilities. Schema version is not checked for now. */
	public boolean isSupported() {
		return root.has("capabilities");
	}

	public com.watabou.utils.Bundle toBundle() {
		com.watabou.utils.Bundle bundle = new com.watabou.utils.Bundle();
		bundle.put("policy_schema_version", schemaVersion);
		bundle.put("policy_json", root.toString());
		return bundle;
	}

	public static EchoPolicy fromBundle(com.watabou.utils.Bundle bundle) {
		if (bundle == null || !bundle.contains("policy_json")) {
			throw new IllegalArgumentException("echo_policy is required");
		}
		return fromJson(bundle.getString("policy_json"));
	}

	private static String readSchemaVersion(JSONObject root) {
		Object raw = root.opt("policy_schema_version");
		return raw instanceof String ? (String) raw : "";
	}
}
