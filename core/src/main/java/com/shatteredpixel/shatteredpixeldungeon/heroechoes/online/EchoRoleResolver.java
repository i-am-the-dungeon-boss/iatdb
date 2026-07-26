package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Set;

/**
 * Picks an item id from a capability. Capability {@code items} are ordered
 * strongest-first; both {@code FIRST_LEGAL} and {@code MAX_DAMAGE} take the
 * first inventory-available entry (virtual {@code *} tags always count).
 */
public final class EchoRoleResolver {

	private EchoRoleResolver() {
	}

	public static String resolveItemId(JSONObject capability, Set<String> availableItemIds) {
		if (capability == null)
			return null;
		JSONArray items = capability.optJSONArray("items");
		if (items == null || items.length() == 0)
			return null;

		for (int i = 0; i < items.length(); i++) {
			String id = items.optString(i, "");
			if (isAvailable(id, availableItemIds)) {
				return id;
			}
		}
		return null;
	}

	public static boolean isAvailable(String itemId, Set<String> availableItemIds) {
		if (itemId == null || itemId.isEmpty())
			return false;
		if (itemId.startsWith("*"))
			return true;
		return availableItemIds != null && availableItemIds.contains(itemId);
	}

	public static boolean roleHasReadyItem(JSONObject capability, Set<String> availableItemIds) {
		return resolveItemId(capability, availableItemIds) != null;
	}
}
