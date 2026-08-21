package com.shatteredpixel.shatteredpixeldungeon.heroechoes.debug;

import com.shatteredpixel.shatteredpixeldungeon.DebugSettings;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.AuraOfProtection;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BlessSpell;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BodyForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Cleanse;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.DivineSense;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Flash;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HallowedGround;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyIntuition;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyLance;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Judgement;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.LayOnHands;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.MnemonicPrayer;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Radiance;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ShieldOfLight;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Smite;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Sunray;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Debug helper: turn an echo into a cleric that walks through every tome spell
 * the fork actually implements, one per turn.
 * <p>
 * Unlike {@link DebugEchoArsenal}, spells are not consumed, so a
 * {@code FIRST_LEGAL} default role would re-cast the same spell forever. The
 * cycle therefore comes from a <em>recipe</em>: {@code EchoPolicyMatcher}
 * advances a recipe step index only after a step actually executes, so one step
 * per spell walks the list exactly once and then falls through to melee.
 * <p>
 * Only spells listed in {@link #handledSpells()} are used, i.e. the set
 * {@code EchoClericHandlers.cast} dispatches. Anything else would fail and stall
 * the recipe on that step. That list is filtered again by
 * {@link HolyTome#canCast(Hero, ClericSpell)} against the prepared kit, which is
 * what drops the subclass-only spells and the Ascended-Form-only ones.
 */
public final class DebugClericSpells {

	/** Talents gating the spells in {@link #handledSpells()}. */
	private static final Talent[] SPELL_TALENTS = {
			Talent.LIGHT_READING, Talent.SUNRAY, Talent.HOLY_LANCE, Talent.HALLOWED_GROUND,
			Talent.LAY_ON_HANDS, Talent.BLESS, Talent.SHIELD_OF_LIGHT, Talent.MNEMONIC_PRAYER,
			Talent.CLEANSE, Talent.DIVINE_SENSE, Talent.AURA_OF_PROTECTION, Talent.HOLY_INTUITION,
			Talent.BODY_FORM,
	};

	private DebugClericSpells() {
	}

	/**
	 * Every spell {@code EchoClericHandlers.cast} dispatches, in the same order
	 * as that instanceof chain so the two stay easy to diff.
	 */
	public static List<ClericSpell> handledSpells() {
		List<ClericSpell> spells = new ArrayList<>();
		spells.add(GuidingLight.INSTANCE);
		spells.add(Sunray.INSTANCE);
		spells.add(Smite.INSTANCE);
		spells.add(HolyLance.INSTANCE);
		spells.add(HallowedGround.INSTANCE);
		spells.add(Flash.INSTANCE);
		spells.add(LayOnHands.INSTANCE);
		spells.add(BlessSpell.INSTANCE);
		spells.add(ShieldOfLight.INSTANCE);
		spells.add(MnemonicPrayer.INSTANCE);
		spells.add(HolyWeapon.INSTANCE);
		spells.add(HolyWard.INSTANCE);
		spells.add(Cleanse.INSTANCE);
		spells.add(Radiance.INSTANCE);
		spells.add(DivineSense.INSTANCE);
		spells.add(AuraOfProtection.INSTANCE);
		spells.add(Judgement.INSTANCE);
		spells.add(HolyIntuition.INSTANCE);
		spells.add(BodyForm.INSTANCE);
		return spells;
	}

	/**
	 * Radiance is Priest-only and Smite is Paladin-only, so no single kit can
	 * cast literally every handled spell. The debug button alternates instead:
	 * press once for the Priest set, again for the Paladin set.
	 */
	private static HeroSubClass nextSubClass = HeroSubClass.PRIEST;

	/** Subclass the next {@link #grantTomeSpellsAll()} will use. */
	public static HeroSubClass pendingSubClass() {
		return nextSubClass;
	}

	/**
	 * Arms every living echo boss, alternating Priest / Paladin between calls so
	 * both subclass-only spells are reachable. Debug builds only.
	 *
	 * @return number of echo bosses updated
	 */
	public static int grantTomeSpellsAll() {
		HeroSubClass use = nextSubClass;
		int updated = grantTomeSpellsAll(use);
		if (updated > 0) {
			nextSubClass = use == HeroSubClass.PRIEST ? HeroSubClass.PALADIN : HeroSubClass.PRIEST;
		}
		return updated;
	}

	/**
	 * Arms every living echo boss with the tome cycle. Debug builds only.
	 *
	 * @return number of echo bosses updated
	 */
	public static int grantTomeSpellsAll(HeroSubClass subClass) {
		if (!DebugSettings.isDebugBuild() || Dungeon.level == null) {
			return 0;
		}
		int updated = 0;
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (mob instanceof EchoBoss && mob.isAlive()) {
				grantTomeSpells((EchoBoss) mob, subClass);
				updated++;
			}
		}
		return updated;
	}

	/**
	 * Equips a fully-charged, identified {@link HolyTome} on the kit, grants the
	 * talents and subclass its spells need, and installs the cycling policy.
	 */
	public static void grantTomeSpells(EchoBoss boss, HeroSubClass subClass) {
		if (boss == null || boss.getEchoHero() == null) {
			throw new IllegalArgumentException("echo boss requires a kit hero");
		}
		Hero kit = boss.getEchoHero();
		kit.subClass = subClass;
		grantSpellTalents(kit);
		HolyTome tome = equipTome(kit);

		List<ClericSpell> castable = new ArrayList<>();
		for (ClericSpell spell : handledSpells()) {
			if (tome.canCast(kit, spell)) {
				castable.add(spell);
			}
		}
		if (castable.isEmpty()) {
			throw new IllegalStateException("no tome spell is castable for the prepared kit");
		}

		boss.replacePolicy(cyclePolicy(castable));
		boss.scheduleEchoKitBuffs();
		boss.state = boss.HUNTING;
		if (Dungeon.hero != null) {
			boss.aggro(Dungeon.hero);
		}
	}

	/** One capability plus one recipe step per spell, in {@code castable} order. */
	public static EchoPolicy cyclePolicy(List<ClericSpell> castable) {
		JSONObject caps = new JSONObject();
		JSONArray steps = new JSONArray();
		for (ClericSpell spell : castable) {
			String name = spell.getClass().getSimpleName();
			String role = "TOME_" + name;
			caps.put(role, new JSONObject()
					.put("pick", "FIRST_LEGAL")
					.put("items", new JSONArray().put("HolyTome"))
					.put("spell", name));
			steps.put(new JSONObject().put("do", new JSONObject().put("use_role", role)));
		}

		JSONArray recipes = new JSONArray().put(new JSONObject()
				.put("id", "debug_tome_cycle")
				.put("priority", 100)
				.put("steps", steps));

		JSONObject root = new JSONObject();
		root.put("policy_schema_version", EchoPolicy.supportedSchemaVersion());
		root.put("capabilities", caps);
		root.put("reactions", new JSONArray());
		root.put("recipes", recipes);
		root.put("positioning", new JSONObject());
		root.put("matchups", new JSONObject());
		root.put("selection", new JSONObject()
				.put("order", new JSONArray().put("recipes").put("default"))
				.put("default_roles", new JSONArray().put("MELEE")));
		root.put("tuning", new JSONObject());
		return new EchoPolicy(root);
	}

	/** Max points in every talent the handled spells gate on. */
	static void grantSpellTalents(Hero kit) {
		LinkedHashMap<Talent, Integer> tier = new LinkedHashMap<>();
		for (Talent talent : SPELL_TALENTS) {
			tier.put(talent, talent.maxPoints());
		}
		kit.talents.add(tier);
	}

	/** Identified, fully-charged tome in the artifact slot; drops any prior copy. */
	static HolyTome equipTome(Hero kit) {
		kit.belongings.artifact = null;
		Iterator<Item> it = kit.belongings.backpack.items.iterator();
		while (it.hasNext()) {
			if (it.next() instanceof HolyTome) {
				it.remove();
			}
		}

		HolyTome tome = new HolyTome();
		tome.identify();
		// chargeCap is protected, so raise it the only public way (upgrade), then
		// fill through the public charge hook rather than touching the field.
		for (int i = 0; i < 7; i++) {
			tome.upgrade();
		}
		kit.belongings.artifact = tome;
		tome.activate(kit);
		tome.charge(kit, 100f);
		Item.updateQuickslot();
		return tome;
	}
}
