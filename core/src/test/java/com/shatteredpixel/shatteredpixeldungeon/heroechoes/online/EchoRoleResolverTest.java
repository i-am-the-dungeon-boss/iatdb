package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

class EchoRoleResolverTest {

	@Test
	@DisplayName("FIRST_LEGAL picks the first inventory-available item")
	void firstLegalPicksFirstAvailable() {
		JSONObject cap = new JSONObject()
				.put("pick", "FIRST_LEGAL")
				.put("items", new JSONArray()
						.put("PotionOfShielding")
						.put("PotionOfHealing"));

		String picked = EchoRoleResolver.resolveItemId(cap, set("PotionOfHealing", "MagesStaff"));

		Assertions.assertThat(picked).isEqualTo("PotionOfHealing");
	}

	@Test
	@DisplayName("virtual tags are always considered available")
	void virtualTagsAlwaysAvailable() {
		JSONObject cap = new JSONObject()
				.put("pick", "FIRST_LEGAL")
				.put("items", new JSONArray().put("*melee"));

		Assertions.assertThat(EchoRoleResolver.resolveItemId(cap, set())).isEqualTo("*melee");
	}

	@Test
	@DisplayName("MAX_DAMAGE prefers later listed items that are available")
	void maxDamagePrefersLastAvailable() {
		JSONObject cap = new JSONObject()
				.put("pick", "MAX_DAMAGE")
				.put("items", new JSONArray()
						.put("*melee")
						.put("WandOfFireblast")
						.put("MagesStaff"));

		String picked = EchoRoleResolver.resolveItemId(
				cap, set("WandOfFireblast", "MagesStaff"));

		Assertions.assertThat(picked).isEqualTo("MagesStaff");
	}

	private static Set<String> set(String... values) {
		return new LinkedHashSet<>(Arrays.asList(values));
	}
}
