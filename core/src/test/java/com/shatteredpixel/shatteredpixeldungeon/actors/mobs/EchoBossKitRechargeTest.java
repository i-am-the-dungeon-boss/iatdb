package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Field;
import java.util.HashSet;

@ExtendWith(GdxTestExtension.class)
class EchoBossKitRechargeTest {

	@AfterEach
	void cleanup() {
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("Echo kit wand Charger is scheduled as an Actor when the boss is added")
	void echoKitWandChargerIsScheduledAsActor() throws Exception {
		Hero player = mageHero();
		WandOfMagicMissile seed = new WandOfMagicMissile();
		seed.identify();
		seed.curCharges = 0;
		seed.partialCharge = 0f;
		seed.collect(player.belongings.backpack);

		EchoBoss boss = EchoTestSupport.createBossWithPolicy(player, wandPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Wand wand = kit.belongings.getItem(WandOfMagicMissile.class);
		Assertions.assertThat(wand).isNotNull();
		wand.curCharges = 0;
		wand.partialCharge = 0f;

		Wand.Charger charger = kitCharger(kit);
		Assertions.assertThat(charger)
				.as("restore must attach Wand.Charger to the phantom kit")
				.isNotNull();

		Assertions.assertThat(actorAllContains(charger))
				.as("phantom kit recharge buffs must run in Actor time like the Hero's")
				.isTrue();
	}

	@Test
	@DisplayName("Echo kit wand regains charge when its Charger acts")
	void echoKitWandRegainsChargeWhenChargerActs() {
		Hero player = mageHero();
		WandOfMagicMissile seed = new WandOfMagicMissile();
		seed.identify();
		seed.collect(player.belongings.backpack);

		EchoBoss boss = EchoTestSupport.createBossWithPolicy(player, wandPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Wand wand = kit.belongings.getItem(WandOfMagicMissile.class);
		Assertions.assertThat(wand).isNotNull();
		wand.curCharges = 0;
		wand.partialCharge = 0f;

		Wand.Charger charger = kitCharger(kit);
		Assertions.assertThat(charger).isNotNull();

		for (int i = 0; i < 60; i++) {
			charger.act();
		}

		Assertions.assertThat(wand.curCharges + wand.partialCharge)
				.as("natural wand recharge must tick for Echo kit the same as Hero")
				.isGreaterThan(0f);
	}

	private static Wand.Charger kitCharger(Hero kit) {
		for (Buff b : kit.buffs()) {
			if (b instanceof Wand.Charger) {
				return (Wand.Charger) b;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static boolean actorAllContains(Actor actor) throws Exception {
		Field all = Actor.class.getDeclaredField("all");
		all.setAccessible(true);
		return ((HashSet<Actor>) all.get(null)).contains(actor);
	}

	private static Hero mageHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.MAGE.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		MagesStaff staff = hero.belongings.getItem(MagesStaff.class);
		if (staff != null && staff.wand() != null) {
			staff.wand().curCharges = staff.wand().maxCharges;
		}
		return hero;
	}

	private static EchoPolicy wandPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("RANGED", new JSONObject()
						.put("pick", "FIRST_LEGAL")
						.put("items", new JSONArray().put("WandOfMagicMissile")))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}
}
