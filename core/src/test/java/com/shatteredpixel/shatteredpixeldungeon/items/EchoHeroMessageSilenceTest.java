package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorruption;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corruption;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hex;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLogCapture;
import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Echo item use must not emit first-person Hero GLog lines ("your wand
 * fizzles",
 * "it is…", etc.).
 */
@ExtendWith(GdxTestExtension.class)
class EchoHeroMessageSilenceTest {

	private final GLogCapture log = new GLogCapture();

	@BeforeEach
	void setUp() {
		new TargetHealthIndicator();
		log.start();
	}

	@AfterEach
	void tearDown() {
		log.stop();
		TargetHealthIndicator.instance = null;
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("echo wand fizzle does not GLog")
	void echoWandFizzleDoesNotGlog() {
		Hero player = mageHero();
		WandOfMagicMissile seed = new WandOfMagicMissile();
		seed.curCharges = 0;
		seed.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(player, wandPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		WandOfMagicMissile wand = boss.getEchoHero().belongings.getItem(WandOfMagicMissile.class);
		Assertions.assertThat(wand).isNotNull();
		wand.curCharges = 0;

		boolean ok = wand.zapAs(UseContext.echo(boss), player.pos);

		Assertions.assertThat(ok).isFalse();
		Assertions.assertThat(log.lines())
				.as("echo empty wand must not print hero 'fizzles' feedback")
				.noneMatch(line -> line.contains(Messages.get(wand, "fizzles")));
	}

	@Test
	@DisplayName("hero wand fizzle still GLogs")
	void heroWandFizzleStillGlogs() {
		Hero hero = mageHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		MagesStaff staff = hero.belongings.getItem(MagesStaff.class);
		Assertions.assertThat(staff).isNotNull();
		Wand wand = staff.wand();
		Assertions.assertThat(wand).isNotNull();
		wand.curCharges = 0;

		boolean ok = wand.zapAs(UseContext.hero(hero), target.pos);

		Assertions.assertThat(ok).isFalse();
		Assertions.assertThat(log.lines())
				.anyMatch(line -> line.contains(Messages.get(wand, "fizzles")));
	}

	@Test
	@DisplayName("echo identify does not GLog it_is")
	void echoIdentifyDoesNotGlog() {
		Hero player = EchoTestSupport.warriorHero();
		ScrollOfIdentify scroll = new ScrollOfIdentify();
		scroll.identify();
		scroll.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		WornShortsword sword = new WornShortsword();
		kit.belongings.weapon = sword;

		ScrollOfIdentify item = kit.belongings.getItem(ScrollOfIdentify.class);
		Assertions.assertThat(item.readAs(UseContext.echo(boss))).isTrue();
		Assertions.assertThat(sword.isIdentified()).isTrue();
		Assertions.assertThat(log.isEmpty())
				.as("echo identify must not print 'it is…' to the player log")
				.isTrue();
	}

	@Test
	@DisplayName("echo remove curse does not GLog cleansed")
	void echoRemoveCurseDoesNotGlog() {
		Hero player = EchoTestSupport.warriorHero();
		ScrollOfRemoveCurse scroll = new ScrollOfRemoveCurse();
		scroll.identify();
		scroll.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		WornShortsword sword = new WornShortsword();
		sword.cursed = true;
		sword.cursedKnown = true;
		kit.belongings.weapon = sword;

		Assertions.assertThat(kit.belongings.getItem(ScrollOfRemoveCurse.class)
				.readAs(UseContext.echo(boss))).isTrue();
		Assertions.assertThat(sword.cursed).isFalse();
		Assertions.assertThat(log.lines())
				.as("echo remove-curse must not print 'cleansed' to the player log")
				.noneMatch(line -> line.contains(Messages.get(ScrollOfRemoveCurse.class, "cleansed")));
	}

	@Test
	@DisplayName("echo kit potion apply does not GLog invisible")
	void echoKitPotionApplyDoesNotGlog() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		new PotionOfInvisibility().apply(kit);

		Assertions.assertThat(log.lines())
				.as("echo kit must not print hero 'invisible' feedback")
				.noneMatch(line -> line.contains(
						Messages.get(PotionOfInvisibility.class, "invisible")));
	}

	@Test
	@DisplayName("echo corruption re-zap does not GLog already_corrupted")
	void echoCorruptionAlreadyCorruptedDoesNotGlog() {
		Hero player = mageHero();
		WandOfCorruption seed = new WandOfCorruption();
		seed.curCharges = 3;
		seed.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(player, corruptionPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Rat rat = new Rat();
		rat.pos = player.pos;
		Dungeon.level.mobs.add(rat);
		Actor.add(rat);
		// Already corrupted + every major flavor debuff forces corruptEnemy() →
		// already_corrupted.
		Buff.affect(rat, Corruption.class);
		Buff.affect(rat, Amok.class, 10f);
		Buff.affect(rat, Slow.class, 10f);
		Buff.affect(rat, Hex.class, 10f);
		Buff.affect(rat, Paralysis.class, 10f);

		WandOfCorruption wand = boss.getEchoHero().belongings.getItem(WandOfCorruption.class);
		Assertions.assertThat(wand).isNotNull();
		wand.curCharges = 3;
		wand.setCurrent(boss.getEchoHero());
		Ballistica bolt = new Ballistica(boss.pos, rat.pos, Ballistica.MAGIC_BOLT);
		wand.onZap(bolt);

		Assertions.assertThat(log.lines())
				.as("echo wand must not print hero 'already corrupted' feedback")
				.noneMatch(line -> line.contains(Messages.get(wand, "already_corrupted")));
	}

	@Test
	@DisplayName("echo missile throw does not GLog curse_discover")
	void echoMissileCurseDiscoverDoesNotGlog() {
		Hero player = EchoTestSupport.warriorHero();
		ThrowingStone seed = new ThrowingStone();
		seed.quantity(3);
		seed.cursed = true;
		seed.cursedKnown = false;
		seed.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		ThrowingStone stone = boss.getEchoHero().belongings.getItem(ThrowingStone.class);
		Assertions.assertThat(stone).isNotNull();
		stone.cursed = true;
		stone.cursedKnown = false;
		boss.getEchoHero().STR = 20;

		Assertions.assertThat(stone.throwAs(UseContext.echo(boss), player.pos)).isTrue();
		Assertions.assertThat(log.lines())
				.as("echo cursed missile must not print hero curse-discovery feedback")
				.noneMatch(line -> line.contains(Messages.get(stone, "curse_discover")));
	}

	@Test
	@DisplayName("echo teleport failure does not GLog no_tele")
	void echoTeleportFailureDoesNotGlog() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		// properties() returns a copy — mutate the live set via Char API for tests.
		EchoTestSupport.addPropertyForTests(boss, Char.Property.IMMOVABLE);

		boolean ok = ScrollOfTeleportation.teleportChar(boss);

		Assertions.assertThat(ok).isFalse();
		Assertions.assertThat(log.lines())
				.noneMatch(line -> line.contains(
						Messages.get(ScrollOfTeleportation.class, "no_tele")));
	}

	private static Hero mageHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.MAGE.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}

	private static com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicy wandPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("RANGED", new JSONObject()
						.put("pick", "FIRST_LEGAL")
						.put("items", new JSONArray().put("WandOfMagicMissile")))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}

	private static com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicy corruptionPolicy() {
		return EchoTestSupport.policyWithCapabilities(new JSONObject()
				.put("RANGED", new JSONObject()
						.put("pick", "FIRST_LEGAL")
						.put("items", new JSONArray().put("WandOfCorruption")))
				.put("MELEE", EchoTestSupport.capability("*melee")));
	}
}
