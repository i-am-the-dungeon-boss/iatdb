package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.sprites.EchoBossSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class CloakOfShadowsUseAsTest {

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
	@DisplayName("Echo useAs applies cloak stealth on the boss body not the kit")
	void echoUseAsAppliesStealthToBossBody() {
		Hero player = rogueHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		CloakOfShadows cloak = kit.belongings.getItem(CloakOfShadows.class);
		Assertions.assertThat(cloak).isNotNull();
		cloak.charge = 5;
		float kitBefore = kit.cooldown();

		boolean ok = cloak.useAs(UseContext.echo(boss));

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(boss.buff(CloakOfShadows.cloakStealth.class)).isNotNull();
		Assertions.assertThat(kit.buff(CloakOfShadows.cloakStealth.class)).isNull();
		Assertions.assertThat(boss.invisible).isGreaterThan(0);
		Assertions.assertThat(kit.invisible).isEqualTo(0);
		Assertions.assertThat(kit.cooldown()).isEqualTo(kitBefore);
		Assertions.assertThat(cloak.charge).isEqualTo(5);
	}

	@Test
	@DisplayName("Echo cloak-on plays operate VFX when the body sprite has a parent")
	void echoCloakOnPlaysOperateWhenSpriteHasParent() {
		Hero player = rogueHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		EchoTestSupport.attachInstantProjectileParent(boss);

		CloakOfShadows cloak = boss.getEchoHero().belongings.getItem(CloakOfShadows.class);
		Assertions.assertThat(cloak).isNotNull();
		cloak.charge = 5;
		int operatesBefore = EchoTestSupport.stubSpriteOperateCalls(boss);

		boolean ok = cloak.useAs(UseContext.echo(boss));

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(EchoTestSupport.stubSpriteOperateCalls(boss))
				.as("cloak activate must operate like Hero, not only on deactivate")
				.isGreaterThan(operatesBefore);
	}

	@Test
	@DisplayName("Echo useAs refuses when cloak has no charge")
	void echoUseAsRefusesWhenNoCharge() {
		Hero player = rogueHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		CloakOfShadows cloak = boss.getEchoHero().belongings.getItem(CloakOfShadows.class);
		Assertions.assertThat(cloak).isNotNull();
		cloak.charge = 0;

		boolean ok = cloak.useAs(UseContext.echo(boss));

		Assertions.assertThat(ok).isFalse();
		Assertions.assertThat(boss.buff(CloakOfShadows.cloakStealth.class)).isNull();
		Assertions.assertThat(boss.invisible).isEqualTo(0);
	}

	@Test
	@DisplayName("Echo kit cloak recharges while equipped without player Light Cloak")
	void echoKitCloakRechargesWhileEquippedOnKit() {
		Hero player = rogueHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		CloakOfShadows cloak = kit.belongings.getItem(CloakOfShadows.class);
		Assertions.assertThat(cloak).isNotNull();
		cloak.charge = 0;
		cloak.partialCharge = 0f;

		CloakOfShadows.cloakRecharge recharge = null;
		for (Buff b : kit.buffs()) {
			if (b instanceof CloakOfShadows.cloakRecharge) {
				recharge = (CloakOfShadows.cloakRecharge) b;
				break;
			}
		}
		Assertions.assertThat(recharge).isNotNull();

		for (int i = 0; i < 50; i++) {
			recharge.act();
		}

		Assertions.assertThat(cloak.charge + cloak.partialCharge)
				.as("equipped echo cloak must recharge using kit wearer, not Dungeon.hero talents")
				.isGreaterThan(0f);
	}

	@Test
	@DisplayName("canActivateStealth ignores dangling activeBuff restored without a target")
	void canActivateStealthIgnoresDanglingRestoredBuff() {
		Hero hero = rogueHero();
		CloakOfShadows live = hero.belongings.getItem(CloakOfShadows.class);
		Assertions.assertThat(live).isNotNull();
		live.charge = 5;
		Assertions.assertThat(live.useAs(UseContext.hero(hero))).isTrue();

		Bundle bundle = new Bundle();
		live.storeInBundle(bundle);

		CloakOfShadows restored = new CloakOfShadows();
		restored.restoreFromBundle(bundle);

		Assertions.assertThat(restored.canActivateStealth())
				.as("restored mid-stealth cloak has activeBuff with no target")
				.isTrue();
	}

	@Test
	@DisplayName("Echo useAs activates when cloak was restored with a dangling activeBuff")
	void echoUseAsActivatesAfterDanglingRestoredBuff() {
		Hero player = rogueHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		CloakOfShadows kitCloak = kit.belongings.getItem(CloakOfShadows.class);
		Assertions.assertThat(kitCloak).isNotNull();
		kitCloak.charge = 5;
		Assertions.assertThat(kitCloak.useAs(UseContext.echo(boss))).isTrue();

		Bundle bundle = new Bundle();
		kitCloak.storeInBundle(bundle);
		kitCloak.useAs(UseContext.echo(boss)); // toggle off so boss is visible

		CloakOfShadows restored = new CloakOfShadows();
		restored.restoreFromBundle(bundle);
		kit.belongings.artifact = restored;
		restored.activate(kit);

		Assertions.assertThat(restored.useAs(UseContext.echo(boss))).isTrue();
		Assertions.assertThat(boss.buff(CloakOfShadows.cloakStealth.class)).isNotNull();
		Assertions.assertThat(boss.invisible).isGreaterThan(0);
	}

	@Test
	@DisplayName("wand zap damage clears EchoBoss cloak stealth on the body")
	void wandZapClearsEchoBossCloakStealth() {
		// Rogue echo has the cloak; mage hero provides a wand to zap with.
		Hero player = rogueHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		CloakOfShadows cloak = boss.getEchoHero().belongings.getItem(CloakOfShadows.class);
		Assertions.assertThat(cloak).isNotNull();
		cloak.charge = 5;
		Assertions.assertThat(cloak.useAs(UseContext.echo(boss))).isTrue();
		Assertions.assertThat(boss.buff(CloakOfShadows.cloakStealth.class)).isNotNull();
		Assertions.assertThat(boss.invisible).isGreaterThan(0);

		WandOfMagicMissile wand = new WandOfMagicMissile();
		wand.identify();
		wand.curCharges = wand.maxCharges;
		Assertions.assertThat(wand.collect(player.belongings.backpack)).isTrue();

		boolean ok = wand.zapAs(UseContext.hero(player), boss.pos);
		Assertions.assertThat(ok).isTrue();

		Assertions.assertThat(boss.buff(CloakOfShadows.cloakStealth.class))
				.as("cloak stealth on EchoBoss body must clear when wand damage lands")
				.isNull();
		Assertions.assertThat(boss.invisible).isEqualTo(0);
	}

	@Test
	@DisplayName("simulates cloak → wand hit → EchoBoss reappears on stage")
	void simulatesCloakThenWandHitRevealsEchoBossSprite() {
		Hero player = rogueHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		boss.state = boss.HUNTING;

		// Real EchoBossSprite on a scene parent (stub sprites hide the reveal bug).
		EchoBossSprite sprite = new EchoBossSprite() {
			@Override
			public PointF worldToCamera(int cell) {
				return new PointF(cell, 0);
			}
		};
		Group stage = new Group();
		stage.add(sprite);
		sprite.ch = boss;
		boss.sprite = sprite;
		sprite.visible = true;
		sprite.alpha(1f);

		CloakOfShadows cloak = boss.getEchoHero().belongings.getItem(CloakOfShadows.class);
		Assertions.assertThat(cloak).isNotNull();
		cloak.charge = 5;
		Assertions.assertThat(cloak.useAs(UseContext.echo(boss))).isTrue();

		// Parent-hosted AlphaTweener fades over time; advance like a game frame.
		Game.elapsed = 1f;
		sprite.update();
		stage.update();
		Assertions.assertThat(boss.buff(CloakOfShadows.cloakStealth.class)).isNotNull();
		Assertions.assertThat(boss.invisible).isGreaterThan(0);
		Assertions.assertThat(sprite.visible)
				.as("cloaked Echo must be fully hidden from the hero")
				.isFalse();
		Assertions.assertThat(sprite.alpha()).isZero();

		WandOfMagicMissile wand = new WandOfMagicMissile();
		wand.identify();
		wand.curCharges = wand.maxCharges;
		Assertions.assertThat(wand.collect(player.belongings.backpack)).isTrue();

		int hpBefore = boss.HP;
		boolean ok = wand.zapAs(UseContext.hero(player), boss.pos);
		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(boss.HP)
				.as("simulative hit must actually deal wand damage")
				.isLessThan(hpBefore);

		// Buff fx queues state removal; update sprite + stage like the game loop.
		Game.elapsed = 1f;
		sprite.update();
		stage.update();

		Assertions.assertThat(boss.buff(CloakOfShadows.cloakStealth.class)).isNull();
		Assertions.assertThat(boss.invisible).isEqualTo(0);
		Assertions.assertThat(sprite.alpha())
				.as("revealed Echo must not stay at 0 alpha after the invis tweener")
				.isEqualTo(1f);
		Assertions.assertThat(sprite.visible)
				.as("revealed Echo in FOV must render again after the wand hit")
				.isTrue();
	}

	@Test
	@DisplayName("Hero useAs spends turn and applies cloak stealth on the hero")
	void heroUseAsSpendsTurnAndAppliesStealth() {
		Hero hero = rogueHero();
		EchoBoss target = EchoTestSupport.createBossWithPolicy(
				hero, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(hero, target, 2);

		CloakOfShadows cloak = hero.belongings.getItem(CloakOfShadows.class);
		Assertions.assertThat(cloak).isNotNull();
		cloak.charge = 5;
		float before = hero.cooldown();

		boolean ok = cloak.useAs(UseContext.hero(hero));

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(hero.buff(CloakOfShadows.cloakStealth.class)).isNotNull();
		Assertions.assertThat(hero.invisible).isGreaterThan(0);
		Assertions.assertThat(hero.cooldown()).isGreaterThan(before);
	}

	private static Hero rogueHero() {
		Hero hero = new Hero();
		Dungeon.hero = hero;
		HeroClass.ROGUE.initHero(hero);
		hero.lvl = 6;
		hero.HP = hero.HT = 30;
		return hero;
	}

}
