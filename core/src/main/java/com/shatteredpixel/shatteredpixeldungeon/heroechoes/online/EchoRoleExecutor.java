package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.EtherealChains;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfExperience;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfMindVision;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDragonsBreath;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfMagicalSight;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.InventoryStone;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.watabou.utils.DeviceCompat;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Executes a resolved role via SPD item/movement APIs (canvas §9).
 * Inventory from {@code echoHero}; effects/VFX/turn on {@link EchoBoss}
 * via shared {@link UseContext} paths ({@code drinkAs}/{@code throwAs}/
 * {@code zapAs}/{@code activateAs}/{@code readAs}).
 *
 * @return true if the turn was spent; false to let the boss fall through (e.g.
 *         melee).
 */
public final class EchoRoleExecutor {

	private EchoRoleExecutor() {
	}

	public static boolean execute(
			EchoBoss boss,
			EchoPolicy policy,
			EchoPolicyStatus status,
			EchoPolicyChoice choice) {
		JSONObject caps = policy.root().optJSONObject("capabilities");
		JSONObject cap = caps != null ? caps.optJSONObject(choice.useRole) : null;
		java.util.Set<String> available = EchoInventory.availableIds(boss.getEchoHero());
		String itemId = EchoRoleResolver.resolveItemId(cap, available);
		if (itemId == null) {
			debugExec("resolve miss role=" + choice.useRole + " available=" + available);
			return false;
		}
		debugExec("resolve role=" + choice.useRole + " → item=" + itemId);

		if (itemId.startsWith("*")) {
			boolean ok = executeVirtual(boss, status, itemId);
			debugExec("virtual " + itemId + " → " + (ok ? "spent" : "fallthrough"));
			return ok;
		}

		Item item = EchoInventory.find(boss.getEchoHero(), itemId);
		if (item == null) {
			debugExec("inventory miss item=" + itemId);
			return false;
		}

		boolean aoe = cap != null && cap.has("hazard") && !cap.optString("hazard").isEmpty();
		int cell = EchoTargetPicker.pick(boss, status, itemId, aoe);

		if (item instanceof Potion) {
			boolean ok = executePotion(boss, (Potion) item, choice.useRole, cell);
			debugExec("potion " + itemId + " cell=" + cell + " → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof Scroll) {
			boolean ok = ((Scroll) item).readAs(UseContext.echo(boss));
			debugExec("scroll " + itemId + " → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof ClassArmor) {
			boolean ok = executeArmorAbility(boss, (ClassArmor) item, cell);
			debugExec("armor ability " + itemId + " cell=" + cell + " → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof Wand) {
			int aim = cell >= 0 ? cell : (Dungeon.hero != null ? Dungeon.hero.pos : -1);
			boolean ok = aim >= 0 && Dungeon.level != null
					&& ((Wand) item).zapAs(UseContext.echo(boss), aim);
			debugExec("wand " + itemId + " cell=" + aim + " charges=" + ((Wand) item).curCharges
					+ " → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof SpiritBow) {
			int aim = cell >= 0 ? cell : (Dungeon.hero != null ? Dungeon.hero.pos : -1);
			boolean ok = aim >= 0 && Dungeon.level != null
					&& ((SpiritBow) item).knockArrow().throwAs(UseContext.echo(boss), aim);
			debugExec("spirit bow cell=" + aim + " → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof MagesStaff) {
			int aim = cell >= 0 ? cell : (Dungeon.hero != null ? Dungeon.hero.pos : -1);
			boolean ok = aim >= 0 && Dungeon.level != null
					&& ((MagesStaff) item).zapAs(UseContext.echo(boss), aim);
			debugExec("staff zap cell=" + aim + " → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof MeleeWeapon && !(item instanceof MagesStaff)) {
			Hero kit = boss.getEchoHero();
			if (kit != null && kit.heroClass == HeroClass.DUELIST) {
				MeleeWeapon weapon = (MeleeWeapon) item;
				Integer target = weapon.targetingPrompt() != null
						? (cell >= 0 ? cell : (Dungeon.hero != null ? Dungeon.hero.pos : null))
						: null;
				if (weapon.targetingPrompt() != null && target == null) {
					debugExec("no aim cell for melee ability " + itemId);
					return false;
				}
				boolean ok = weapon.abilityAs(UseContext.echo(boss), target);
				debugExec("melee ability " + itemId + " cell=" + target + " → " + (ok ? "spent" : "fail"));
				return ok;
			}
		}
		if (item instanceof MissileWeapon || item instanceof Bomb || isThrowableRunestone(item)) {
			int aim = cell >= 0 ? cell : (Dungeon.hero != null ? Dungeon.hero.pos : -1);
			if (aim < 0 || Dungeon.level == null) {
				debugExec("no aim cell for " + itemId);
				return false;
			}
			boolean ok = item.throwAs(UseContext.echo(boss), aim);
			debugExec("throwable " + itemId + " cell=" + aim + " → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof InventoryStone) {
			boolean ok = ((InventoryStone) item).useAs(UseContext.echo(boss));
			debugExec("inventory stone " + itemId + " → " + (ok ? "spent" : "fail"));
			return ok;
		}
		// Artifacts — HolyTome cleric spells via castAs (merge: keep sibling branches)
		if (item instanceof HolyTome) {
			boolean ok = executeHolyTome(boss, (HolyTome) item, cap, cell);
			debugExec("holy tome cell=" + cell + " → " + (ok ? "spent" : "fail"));
			return ok;
		}
		// Artifacts — CloakOfShadows stealth via shared useAs (merge: keep sibling
		// branches)
		if (item instanceof CloakOfShadows) {
			boolean ok = ((CloakOfShadows) item).useAs(UseContext.echo(boss));
			debugExec("artifact CloakOfShadows → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof HornOfPlenty) {
			boolean ok = ((HornOfPlenty) item).useAs(UseContext.echo(boss));
			debugExec("artifact HornOfPlenty → " + (ok ? "spent" : "fail"));
			return ok;
		}
		if (item instanceof EtherealChains) {
			int aim = cell >= 0 ? cell : (Dungeon.hero != null ? Dungeon.hero.pos : -1);
			if (aim < 0) {
				debugExec("artifact EtherealChains no aim");
				return false;
			}
			boolean ok = ((EtherealChains) item).useAs(UseContext.echo(boss), aim);
			debugExec("artifact EtherealChains cell=" + aim + " → " + (ok ? "spent" : "fail"));
			return ok;
		}
		debugExec("unsupported item class=" + item.getClass().getSimpleName());
		return false;
	}

	/** Inventory stones need a bag UI; throwable runestones activate on land. */
	private static boolean isThrowableRunestone(Item item) {
		return item instanceof Runestone && !(item instanceof InventoryStone);
	}

	private static void debugExec(String message) {
		if (DeviceCompat.isDebug()) {
			DeviceCompat.log("EchoBoss", "exec " + message);
		}
	}

	/**
	 * Self-drink when the role is an explicit drink role (dual-mode / must-throw
	 * exceptions like CLEANSE_BURN+Frost), or the potion's default action is
	 * {@link Potion#AC_DRINK} (not must-throw / choose).
	 */
	private static boolean shouldSelfDrink(Potion potion, String role) {
		if (isSelfDrinkRole(role)) {
			return true;
		}
		return Potion.AC_DRINK.equals(potion.defaultAction());
	}

	/**
	 * Dual-mode ({@code AC_CHOOSE}) and must-throw potions that policy still
	 * drinks via role (e.g. Purity, Cleansing, Frost cleanse).
	 */
	private static boolean isSelfDrinkRole(String role) {
		return "HEAL".equals(role)
				|| "CLEANSE_BURN".equals(role)
				|| "CLEANSE".equals(role)
				|| "PURITY".equals(role)
				|| "HASTE".equals(role)
				|| "INVIS".equals(role)
				|| "LEVITATE".equals(role);
	}

	/**
	 * {@code apply(Char)} is a no-op on non-Hero — refuse without consuming.
	 */
	private static boolean isHeroOnlyDrink(Potion potion) {
		return potion instanceof PotionOfStrength
				|| potion instanceof PotionOfExperience
				|| potion instanceof ElixirOfMight
				|| potion instanceof PotionOfMindVision
				|| potion instanceof PotionOfMagicalSight;
	}

	private static boolean executeVirtual(EchoBoss boss, EchoPolicyStatus status, String tag) {
		Hero enemy = Dungeon.hero;
		if ("*wait".equals(tag)) {
			return true;
		}
		if ("*melee".equals(tag)) {
			return false;
		}
		if ("*move_further".equals(tag)) {
			return enemy != null && boss.policyStepFurther(enemy.pos);
		}
		if ("*move_closer".equals(tag)) {
			return enemy != null && boss.policyStepCloser(enemy.pos);
		}
		if (tag.startsWith("*move_to_terrain:")) {
			String terrain = tag.substring("*move_to_terrain:".length());
			Integer cell = status.terrainNearCell.get(terrain);
			return cell != null && boss.policyStepCloser(cell);
		}
		return false;
	}

	/**
	 * Potion execute: self-drink via {@link Potion#drinkAs}, throw via
	 * {@link Item#throwAs}.
	 */
	private static boolean executePotion(EchoBoss boss, Potion potion, String role, int cell) {
		UseContext ctx = UseContext.echo(boss);
		// Targeted cone — not self-drink / shatter
		if (potion instanceof PotionOfDragonsBreath) {
			if (cell < 0 || Dungeon.level == null) {
				return false;
			}
			return ((PotionOfDragonsBreath) potion).breatheAs(ctx, cell);
		}
		if (shouldSelfDrink(potion, role)) {
			if (isHeroOnlyDrink(potion)) {
				return false;
			}
			return potion.drinkAs(ctx);
		}
		if (cell < 0 || Dungeon.level == null) {
			return false;
		}
		return potion.throwAs(ctx, cell);
	}

	/** ClassArmor charge skill via {@link ArmorAbility#activateAs}. */
	private static boolean executeArmorAbility(EchoBoss boss, ClassArmor armor, int cell) {
		Hero kit = boss.getEchoHero();
		ArmorAbility ability = kit != null ? kit.armorAbility : null;
		if (ability == null) {
			return false;
		}
		Integer target = null;
		if (ability.useTargeting()) {
			int aim = cell >= 0 ? cell : (Dungeon.hero != null ? Dungeon.hero.pos : -1);
			if (aim < 0) {
				return false;
			}
			target = aim;
		}
		return ability.activateAs(UseContext.echo(boss), armor, target);
	}

	private static boolean executeHolyTome(EchoBoss boss, HolyTome tome, JSONObject cap, int cell) {
		ClericSpell spell = resolveClericSpell(cap);
		if (spell == null) {
			return false;
		}
		Integer target = null;
		if (spell.targetingFlags() != -1) {
			int aim = cell >= 0 ? cell : (Dungeon.hero != null ? Dungeon.hero.pos : -1);
			if (aim < 0 || Dungeon.level == null) {
				return false;
			}
			target = aim;
		}
		return tome.castAs(UseContext.echo(boss), spell, target);
	}

	/**
	 * Reads optional {@code spell} on capability; else first items entry that maps
	 * to a spell.
	 */
	static ClericSpell resolveClericSpell(JSONObject cap) {
		if (cap == null) {
			return null;
		}
		String spellName = cap.optString("spell", "");
		if (!spellName.isEmpty()) {
			ClericSpell spell = ClericSpell.bySimpleName(spellName);
			if (spell != null) {
				return spell;
			}
		}
		JSONArray items = cap.optJSONArray("items");
		if (items != null) {
			for (int i = 0; i < items.length(); i++) {
				String id = items.optString(i, "");
				if ("HolyTome".equals(id)) {
					continue;
				}
				ClericSpell spell = ClericSpell.bySimpleName(id);
				if (spell != null) {
					return spell;
				}
			}
		}
		return null;
	}

}
