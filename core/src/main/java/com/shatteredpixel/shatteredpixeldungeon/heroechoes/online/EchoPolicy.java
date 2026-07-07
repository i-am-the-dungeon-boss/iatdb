package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class EchoPolicy {

	public static final int SUPPORTED_SCHEMA_VERSION = 1;

	public final int schemaVersion;
	public final List<Rule> rules;
	public final JSONObject tuning;

	public EchoPolicy(int schemaVersion, List<Rule> rules, JSONObject tuning) {
		this.schemaVersion = schemaVersion;
		this.rules = rules != null ? rules : Collections.emptyList();
		this.tuning = tuning;
	}

	public static EchoPolicy fromJson(String json) {
		JSONObject root = new JSONObject(json);
		return fromJson(root);
	}

	public static EchoPolicy fromJson(JSONObject root) {
		int schemaVersion = root.optInt("policy_schema_version", 0);
		JSONArray rulesArray = root.optJSONArray("rules");
		List<Rule> rules = new ArrayList<>();
		if (rulesArray != null) {
			for (int i = 0; i < rulesArray.length(); i++) {
				rules.add(Rule.fromJson(rulesArray.getJSONObject(i)));
			}
		}
		return new EchoPolicy(schemaVersion, rules, root.optJSONObject("tuning"));
	}

	public static EchoPolicy fallback() {
		List<Rule> rules = new ArrayList<>();
		rules.add(new Rule(
				new JSONObject(),
				new EchoPolicyAction(EchoPolicyAction.Type.MELEE_CHASE, null, 0),
				0
		));
		return new EchoPolicy(SUPPORTED_SCHEMA_VERSION, rules, null);
	}

	public boolean isSupported() {
		return schemaVersion == SUPPORTED_SCHEMA_VERSION && !rules.isEmpty();
	}

	public com.watabou.utils.Bundle toBundle() {
		com.watabou.utils.Bundle bundle = new com.watabou.utils.Bundle();
		bundle.put("policy_schema_version", schemaVersion);
		org.json.JSONArray rulesArray = new org.json.JSONArray();
		for (Rule rule : rules) {
			org.json.JSONObject ruleJson = new org.json.JSONObject();
			ruleJson.put("when", rule.when);
			org.json.JSONObject doJson = new org.json.JSONObject();
			doJson.put("action", rule.action.type.name());
			if (rule.action.item != null) {
				doJson.put("item", rule.action.item);
			}
			if (rule.action.range > 0) {
				doJson.put("range", rule.action.range);
			}
			ruleJson.put("do", doJson);
			ruleJson.put("priority", rule.priority);
			rulesArray.put(ruleJson);
		}
		bundle.put("rules_json", rulesArray.toString());
		if (tuning != null) {
			bundle.put("tuning_json", tuning.toString());
		}
		return bundle;
	}

	public static EchoPolicy fromBundle(com.watabou.utils.Bundle bundle) {
		if (bundle == null) {
			return fallback();
		}
		org.json.JSONObject root = new org.json.JSONObject();
		root.put("policy_schema_version", bundle.getInt("policy_schema_version"));
		if (bundle.contains("rules_json")) {
			root.put("rules", new org.json.JSONArray(bundle.getString("rules_json")));
		}
		if (bundle.contains("tuning_json")) {
			root.put("tuning", new org.json.JSONObject(bundle.getString("tuning_json")));
		}
		EchoPolicy policy = fromJson(root);
		return policy.isSupported() ? policy : fallback();
	}

	public static final class Rule {
		public final JSONObject when;
		public final EchoPolicyAction action;
		public final int priority;

		public Rule(JSONObject when, EchoPolicyAction action, int priority) {
			this.when = when != null ? when : new JSONObject();
			this.action = action;
			this.priority = priority;
		}

		static Rule fromJson(JSONObject json) {
			JSONObject when = json.optJSONObject("when");
			JSONObject doObj = json.optJSONObject("do");
			EchoPolicyAction action = EchoPolicyAction.fromJson(doObj);
			return new Rule(when, action, json.optInt("priority", 0));
		}
	}
}
