package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoHeroSnapshot;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Structured kit card for remote policy generation (items, talents, class).
 * Separate from the opaque combat restore blob ({@code echo_data_base64}).
 */
public final class EchoPolicyInput {

	public final String heroClass;
	public final String subclass;
	public final String armorAbility;
	public final int lvl;
	public final List<String> items;
	public final Map<String, Integer> talents;

	public EchoPolicyInput(
			String heroClass,
			String subclass,
			String armorAbility,
			int lvl,
			List<String> items,
			Map<String, Integer> talents) {
		if (heroClass == null || heroClass.isEmpty()) {
			throw new IllegalArgumentException("policy input requires hero_class");
		}
		this.heroClass = heroClass;
		this.subclass = subclass != null && !subclass.isEmpty() ? subclass : HeroSubClass.NONE.name();
		this.armorAbility = armorAbility;
		this.lvl = Math.max(0, lvl);
		this.items = Collections.unmodifiableList(new ArrayList<>(items != null ? items : List.of()));
		this.talents = Collections.unmodifiableMap(
				new LinkedHashMap<>(talents != null ? talents : Map.of()));
	}

	public static EchoPolicyInput fromEcho(Echo echo) {
		if (echo == null) {
			throw new IllegalArgumentException("policy input requires echo");
		}
		Hero hero = EchoHeroSnapshot.restoreHero(echo);
		if (hero == null) {
			throw new IllegalArgumentException("policy input requires restorable echo hero");
		}
		return fromHero(hero);
	}

	public static EchoPolicyInput fromHero(Hero hero) {
		if (hero == null) {
			throw new IllegalArgumentException("policy input requires hero");
		}
		if (hero.heroClass == null) {
			throw new IllegalArgumentException("policy input requires hero_class");
		}
		String subclass = hero.subClass != null ? hero.subClass.name() : HeroSubClass.NONE.name();
		String armorAbility = hero.armorAbility != null
				? hero.armorAbility.getClass().getSimpleName()
				: null;
		return new EchoPolicyInput(
				hero.heroClass.name(),
				subclass,
				armorAbility,
				hero.lvl,
				collectItems(hero),
				collectTalents(hero));
	}

	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("hero_class", heroClass);
		json.put("subclass", subclass);
		if (armorAbility != null) {
			json.put("armor_ability", armorAbility);
		} else {
			json.put("armor_ability", JSONObject.NULL);
		}
		json.put("lvl", lvl);
		JSONArray itemArr = new JSONArray();
		for (String item : items) {
			itemArr.put(item);
		}
		json.put("items", itemArr);
		JSONObject talentObj = new JSONObject();
		for (Map.Entry<String, Integer> entry : talents.entrySet()) {
			talentObj.put(entry.getKey(), entry.getValue());
		}
		json.put("talents", talentObj);
		return json;
	}

	private static List<String> collectItems(Hero hero) {
		Set<String> ids = new LinkedHashSet<>();
		for (Item item : hero.belongings) {
			if (item != null) {
				ids.add(item.getClass().getSimpleName());
			}
		}
		return new ArrayList<>(ids);
	}

	private static Map<String, Integer> collectTalents(Hero hero) {
		Map<String, Integer> out = new LinkedHashMap<>();
		if (hero.talents == null) {
			return out;
		}
		for (LinkedHashMap<Talent, Integer> tier : hero.talents) {
			if (tier == null) {
				continue;
			}
			for (Map.Entry<Talent, Integer> entry : tier.entrySet()) {
				if (entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0) {
					continue;
				}
				out.put(entry.getKey().name(), entry.getValue());
			}
		}
		return out;
	}
}
