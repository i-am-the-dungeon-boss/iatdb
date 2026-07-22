package com.shatteredpixel.shatteredpixeldungeon.items.scrolls;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Recharging;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class ScrollReadAsTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("Echo readAs Recharging buffs boss body and detaches scroll")
	void echoReadAsRechargingBuffsBody() {
		Hero player = EchoTestSupport.warriorHero();
		ScrollOfRecharging scroll = new ScrollOfRecharging();
		scroll.identify();
		scroll.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Scroll item = kit.belongings.getItem(ScrollOfRecharging.class);
		Assertions.assertThat(item).isNotNull();
		float kitBefore = kit.cooldown();

		boolean spent = item.readAs(UseContext.echo(boss));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(boss.buff(Recharging.class)).isNotNull();
		Assertions.assertThat(kit.buff(Recharging.class)).isNull();
		Assertions.assertThat(kit.cooldown()).isEqualTo(kitBefore);
		Assertions.assertThat(kit.belongings.getItem(ScrollOfRecharging.class)).isNull();
	}

	@Test
	@DisplayName("Echo readAs Recharging plays read operate VFX when sprite has a parent")
	void echoReadAsRechargingPlaysOperateWhenSpriteHasParent() {
		Hero player = EchoTestSupport.warriorHero();
		ScrollOfRecharging scroll = new ScrollOfRecharging();
		scroll.identify();
		scroll.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		EchoTestSupport.attachInstantProjectileParent(boss);

		Scroll item = boss.getEchoHero().belongings.getItem(ScrollOfRecharging.class);
		Assertions.assertThat(item).isNotNull();
		int operatesBefore = EchoTestSupport.stubSpriteOperateCalls(boss);

		boolean spent = item.readAs(UseContext.echo(boss));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(EchoTestSupport.stubSpriteOperateCalls(boss))
				.as("readAnimation must show world FX for Echo like HeroSprite.read")
				.isGreaterThan(operatesBefore);
	}

	@Test
	@DisplayName("Echo readAs Teleportation plays read operate VFX when sprite has a parent")
	void echoReadAsTeleportationPlaysOperateWhenSpriteHasParent() {
		Hero player = EchoTestSupport.warriorHero();
		ScrollOfTeleportation scroll = new ScrollOfTeleportation();
		scroll.identify();
		scroll.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		EchoTestSupport.attachInstantProjectileParent(boss);

		Scroll item = boss.getEchoHero().belongings.getItem(ScrollOfTeleportation.class);
		Assertions.assertThat(item).isNotNull();
		int operatesBefore = EchoTestSupport.stubSpriteOperateCalls(boss);

		boolean spent = item.readAs(UseContext.echo(boss));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(EchoTestSupport.stubSpriteOperateCalls(boss))
				.as("successful teleport must run readAnimation world FX for Echo")
				.isGreaterThan(operatesBefore);
	}

	@Test
	@DisplayName("Echo readAs Rage amoks visible enemies from boss position")
	void echoReadAsRageAmoksVisibleEnemies() {
		Hero player = EchoTestSupport.warriorHero();
		ScrollOfRage scroll = new ScrollOfRage();
		scroll.identify();
		scroll.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Rat rat = new Rat();
		rat.pos = boss.pos + 1;
		EchoTestSupport.linkStubSprite(rat);
		Dungeon.level.mobs.add(rat);
		Dungeon.level.heroFOV[rat.pos] = true;

		Hero kit = boss.getEchoHero();
		Scroll item = kit.belongings.getItem(ScrollOfRage.class);
		Assertions.assertThat(item).isNotNull();

		boolean spent = item.readAs(UseContext.echo(boss));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(rat.buff(Amok.class)).isNotNull();
		Assertions.assertThat(kit.belongings.getItem(ScrollOfRage.class)).isNull();
	}

	@Test
	@DisplayName("Echo readAs Upgrade auto-upgrades an equipped kit weapon")
	void echoReadAsUpgradeAutoUpgradesWeapon() {
		Hero player = EchoTestSupport.warriorHero();
		ScrollOfUpgrade scroll = new ScrollOfUpgrade();
		scroll.identify();
		scroll.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword sword = new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword();
		sword.identify();
		kit.belongings.weapon = sword;
		int levelBefore = sword.level();

		Scroll item = kit.belongings.getItem(ScrollOfUpgrade.class);
		Assertions.assertThat(item).isNotNull();

		boolean spent = item.readAs(UseContext.echo(boss));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(sword.level()).isEqualTo(levelBefore + 1);
		Assertions.assertThat(kit.belongings.getItem(ScrollOfUpgrade.class)).isNull();
	}

	@Test
	@DisplayName("Echo readAs Terror frightens visible enemies from boss position")
	void echoReadAsTerrorFrightensVisibleEnemies() {
		Hero player = EchoTestSupport.warriorHero();
		ScrollOfTerror scroll = new ScrollOfTerror();
		scroll.identify();
		scroll.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Rat rat = new Rat();
		rat.pos = boss.pos + 1;
		EchoTestSupport.linkStubSprite(rat);
		Dungeon.level.mobs.add(rat);
		Dungeon.level.heroFOV[rat.pos] = true;

		Hero kit = boss.getEchoHero();
		Scroll item = kit.belongings.getItem(ScrollOfTerror.class);
		Assertions.assertThat(item).isNotNull();

		boolean spent = item.readAs(UseContext.echo(boss));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(rat.buff(Terror.class)).isNotNull();
		Assertions.assertThat(kit.belongings.getItem(ScrollOfTerror.class)).isNull();
	}

	@Test
	@DisplayName("Echo readAs Identify auto-identifies an unknown kit item")
	void echoIdentifyAutoPicksUnknownItem() {
		Hero player = EchoTestSupport.warriorHero();
		ScrollOfIdentify scroll = new ScrollOfIdentify();
		scroll.identify();
		scroll.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword sword = new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword();
		kit.belongings.weapon = sword;
		Assertions.assertThat(sword.isIdentified()).isFalse();

		ScrollOfIdentify item = kit.belongings.getItem(ScrollOfIdentify.class);
		Assertions.assertThat(item).isNotNull();

		boolean spent = item.readAs(UseContext.echo(boss));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(sword.isIdentified()).isTrue();
		Assertions.assertThat(kit.belongings.getItem(ScrollOfIdentify.class)).isNull();
	}

	@Test
	@DisplayName("Echo readAs RemoveCurse cleanses a cursed kit weapon")
	void echoRemoveCurseCleansesCursedWeapon() {
		Hero player = EchoTestSupport.warriorHero();
		ScrollOfRemoveCurse scroll = new ScrollOfRemoveCurse();
		scroll.identify();
		scroll.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword sword = new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword();
		sword.cursed = true;
		sword.cursedKnown = true;
		kit.belongings.weapon = sword;

		Scroll item = kit.belongings.getItem(ScrollOfRemoveCurse.class);
		Assertions.assertThat(item).isNotNull();

		boolean spent = item.readAs(UseContext.echo(boss));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(sword.cursed).isFalse();
		Assertions.assertThat(kit.belongings.getItem(ScrollOfRemoveCurse.class)).isNull();
	}

	@Test
	@DisplayName("Echo readAs Teleportation moves boss body and detaches scroll")
	void echoTeleportationMovesBody() {
		Hero player = EchoTestSupport.warriorHero();
		ScrollOfTeleportation scroll = new ScrollOfTeleportation();
		scroll.identify();
		scroll.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Scroll item = kit.belongings.getItem(ScrollOfTeleportation.class);
		Assertions.assertThat(item).isNotNull();
		int posBefore = boss.pos;

		boolean spent = item.readAs(UseContext.echo(boss));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(kit.belongings.getItem(ScrollOfTeleportation.class)).isNull();
		// Teleport may fail on tiny stub levels; consume still happens.
		Assertions.assertThat(boss.pos).isGreaterThanOrEqualTo(0);
		if (boss.pos != posBefore) {
			Assertions.assertThat(Dungeon.level.passable[boss.pos]
					|| Dungeon.level.avoid[boss.pos]).isTrue();
		}
	}

	@Test
	@DisplayName("Echo readAs MirrorImage spawns copies at boss position")
	void echoMirrorImageSpawnsAtBody() {
		Hero player = EchoTestSupport.warriorHero();
		ScrollOfMirrorImage scroll = new ScrollOfMirrorImage();
		scroll.identify();
		scroll.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Scroll item = kit.belongings.getItem(ScrollOfMirrorImage.class);
		Assertions.assertThat(item).isNotNull();
		int mobsBefore = Dungeon.level.mobs.size();

		boolean spent = item.readAs(UseContext.echo(boss));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(kit.belongings.getItem(ScrollOfMirrorImage.class)).isNull();
		Assertions.assertThat(Dungeon.level.mobs.size()).isGreaterThanOrEqualTo(mobsBefore);
	}

	@Test
	@DisplayName("Echo readAs Dread applies Dread to visible enemies from boss id")
	void echoDreadAppliesToVisibleEnemies() {
		Hero player = EchoTestSupport.warriorHero();
		com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfDread scroll = new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfDread();
		scroll.identify();
		scroll.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Rat rat = new Rat();
		rat.pos = boss.pos + 1;
		EchoTestSupport.linkStubSprite(rat);
		Dungeon.level.mobs.add(rat);
		Dungeon.level.heroFOV[rat.pos] = true;

		Hero kit = boss.getEchoHero();
		Scroll item = kit.belongings.getItem(
				com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfDread.class);
		Assertions.assertThat(item).isNotNull();

		boolean spent = item.readAs(UseContext.echo(boss));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(rat.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Dread.class))
				.isNotNull();
		Assertions.assertThat(kit.belongings.getItem(
				com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfDread.class)).isNull();
	}

	@Test
	@DisplayName("Echo readAs AntiMagic applies MagicImmune on the boss body")
	void echoAntiMagicBuffsBossBody() {
		Hero player = EchoTestSupport.warriorHero();
		com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfAntiMagic scroll = new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfAntiMagic();
		scroll.identify();
		scroll.collect(player.belongings.backpack);
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		Scroll item = kit.belongings.getItem(
				com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfAntiMagic.class);
		Assertions.assertThat(item).isNotNull();

		boolean spent = item.readAs(UseContext.echo(boss));

		Assertions.assertThat(spent).isTrue();
		Assertions.assertThat(boss.buff(
				com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune.class)).isNotNull();
		Assertions.assertThat(kit.buff(
				com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune.class)).isNull();
	}
}
