package com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blizzard;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArcaneArmor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barkskin;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FireImbue;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FrostImbue;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Levitation;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Stamina;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ToxicImbue;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLevitation;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfMindVision;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfPurity;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.BlizzardBrew;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfArcaneArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfDragonsBlood;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfFeatherFall;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfHoneyedHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfIcyTouch;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfToxicEssence;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfEarthenArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShielding;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfStamina;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlast;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlink;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfFear;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfShock;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.PoisonDart;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicyChoice;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicyStatus;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoRoleExecutor;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

/**
 * Canvas-aligned Echo arsenal kinds: consumables (potions / exotic / brews /
 * elixirs / stones / bombs / tipped darts) from Hero Arsenal Combat.
 */
@ExtendWith(GdxTestExtension.class)
class EchoBossArsenalConsumableKindsTest {

	@BeforeEach
	void installUiStubs() {
		new TargetHealthIndicator();
	}

	@AfterEach
	void cleanup() {
		TargetHealthIndicator.instance = null;
	}

	private static Fight fight() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		return new Fight(player, boss);
	}

	private static final class Fight {
		final Hero player;
		final EchoBoss boss;

		Fight(Hero player, EchoBoss boss) {
			this.player = player;
			this.boss = boss;
		}

		Hero kit() {
			return boss.getEchoHero();
		}

		UseContext echo() {
			return UseContext.echo(boss);
		}
	}

	@Nested
	@DisplayName("Standard potions")
	class StandardPotions {

		@Test
		@DisplayName("Echo Liquid Flame throwAs seeds Fire on the Hero's cell")
		void liquidFlameThrowSeedsFireOnHero() {
			Fight f = fight();
			PotionOfLiquidFlame pot = new PotionOfLiquidFlame();
			pot.identify();
			pot.collect(f.kit().belongings.backpack);
			Arrays.fill(Dungeon.level.heroFOV, false);

			Assertions.assertThat(pot.throwAs(f.echo(), f.player.pos)).isTrue();
			Assertions.assertThat(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob
					.volumeAt(f.player.pos, Fire.class)).isGreaterThan(0);
			Assertions.assertThat(f.kit().belongings.getItem(PotionOfLiquidFlame.class)).isNull();
		}

		@Test
		@DisplayName("Echo Invisibility drinkAs buffs the boss body not the kit")
		void invisibilityDrinkBuffsBody() {
			Fight f = fight();
			PotionOfInvisibility pot = new PotionOfInvisibility();
			pot.identify();
			pot.collect(f.kit().belongings.backpack);

			Assertions.assertThat(pot.drinkAs(f.echo())).isTrue();
			Assertions.assertThat(f.boss.buff(Invisibility.class)).isNotNull();
			Assertions.assertThat(f.kit().buff(Invisibility.class)).isNull();
		}

		@Test
		@DisplayName("Echo Levitation drinkAs buffs the boss body")
		void levitationDrinkBuffsBody() {
			Fight f = fight();
			PotionOfLevitation pot = new PotionOfLevitation();
			pot.identify();
			pot.collect(f.kit().belongings.backpack);

			Assertions.assertThat(pot.drinkAs(f.echo())).isTrue();
			Assertions.assertThat(f.boss.buff(Levitation.class)).isNotNull();
			Assertions.assertThat(f.kit().buff(Levitation.class)).isNull();
		}

		@Test
		@DisplayName("Echo Purity drinkAs via CLEANSE role spends without phantom kit cooldown")
		void purityDrinkViaExecutor() {
			Fight f = fight();
			PotionOfPurity pot = new PotionOfPurity();
			pot.identify();
			pot.collect(f.kit().belongings.backpack);
			float kitBefore = f.kit().cooldown();

			EchoPolicy policy = EchoTestSupport.policyWithCapabilities(new JSONObject()
					.put("PURITY", EchoTestSupport.capability("PotionOfPurity")));
			boolean spent = EchoRoleExecutor.execute(
					f.boss, policy, status("PURITY"),
					new EchoPolicyChoice("PURITY", "default", null));

			Assertions.assertThat(spent).isTrue();
			Assertions.assertThat(f.kit().cooldown()).isEqualTo(kitBefore);
			Assertions.assertThat(f.kit().belongings.getItem(PotionOfPurity.class)).isNull();
		}

		@Test
		@DisplayName("Echo Mind Vision drink is refused by executor without consuming")
		void mindVisionHeroOnlyRefuse() {
			Fight f = fight();
			PotionOfMindVision pot = new PotionOfMindVision();
			pot.identify();
			pot.collect(f.kit().belongings.backpack);

			EchoPolicy policy = EchoTestSupport.policyWithCapabilities(new JSONObject()
					.put("HEAL", EchoTestSupport.capability("PotionOfMindVision")));
			boolean spent = EchoRoleExecutor.execute(
					f.boss, policy, status("HEAL"),
					new EchoPolicyChoice("HEAL", "default", null));

			Assertions.assertThat(spent).isFalse();
			Assertions.assertThat(f.kit().belongings.getItem(PotionOfMindVision.class)).isNotNull();
		}

		@Test
		@DisplayName("Echo Strength drink is refused by executor without consuming")
		void strengthHeroOnlyRefuse() {
			Fight f = fight();
			PotionOfStrength pot = new PotionOfStrength();
			pot.identify();
			pot.collect(f.kit().belongings.backpack);

			EchoPolicy policy = EchoTestSupport.policyWithCapabilities(new JSONObject()
					.put("HEAL", EchoTestSupport.capability("PotionOfStrength")));
			boolean spent = EchoRoleExecutor.execute(
					f.boss, policy, status("HEAL"),
					new EchoPolicyChoice("HEAL", "default", null));

			Assertions.assertThat(spent).isFalse();
			Assertions.assertThat(f.kit().belongings.getItem(PotionOfStrength.class)).isNotNull();
		}
	}

	@Nested
	@DisplayName("Exotic potions / brews / elixirs")
	class ExoticBrewsElixirs {

		@Test
		@DisplayName("Echo Stamina drinkAs applies Stamina on the boss body")
		void staminaDrinkBuffsBody() {
			Fight f = fight();
			PotionOfStamina pot = new PotionOfStamina();
			pot.identify();
			pot.collect(f.kit().belongings.backpack);

			Assertions.assertThat(pot.drinkAs(f.echo())).isTrue();
			Assertions.assertThat(f.boss.buff(Stamina.class)).isNotNull();
			Assertions.assertThat(f.kit().buff(Stamina.class)).isNull();
		}

		@Test
		@DisplayName("Echo Shielding drinkAs applies Barrier on the boss body")
		void shieldingDrinkBuffsBody() {
			Fight f = fight();
			PotionOfShielding pot = new PotionOfShielding();
			pot.identify();
			pot.collect(f.kit().belongings.backpack);

			Assertions.assertThat(pot.drinkAs(f.echo())).isTrue();
			Assertions.assertThat(f.boss.buff(Barrier.class)).isNotNull();
			Assertions.assertThat(f.kit().buff(Barrier.class)).isNull();
		}

		@Test
		@DisplayName("Echo Earthen Armor drinkAs applies Barkskin on the boss body")
		void earthenArmorDrinkBuffsBody() {
			Fight f = fight();
			PotionOfEarthenArmor pot = new PotionOfEarthenArmor();
			pot.identify();
			pot.collect(f.kit().belongings.backpack);

			Assertions.assertThat(pot.drinkAs(f.echo())).isTrue();
			Assertions.assertThat(f.boss.buff(Barkskin.class)).isNotNull();
			Assertions.assertThat(f.kit().buff(Barkskin.class)).isNull();
		}

		@Test
		@DisplayName("Echo Blizzard Brew throwAs seeds Blizzard on the Hero's cell")
		void blizzardBrewThrowSeedsBlobOnHero() {
			Fight f = fight();
			BlizzardBrew brew = new BlizzardBrew();
			brew.collect(f.kit().belongings.backpack);
			Arrays.fill(Dungeon.level.heroFOV, false);

			Assertions.assertThat(brew.throwAs(f.echo(), f.player.pos)).isTrue();
			Assertions.assertThat(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob
					.volumeAt(f.player.pos, Blizzard.class)).isGreaterThan(0);
		}

		@Test
		@DisplayName("Echo Arcane Armor elixir drinkAs buffs the boss body")
		void arcaneArmorElixirBuffsBody() {
			Fight f = fight();
			ElixirOfArcaneArmor elixir = new ElixirOfArcaneArmor();
			elixir.collect(f.kit().belongings.backpack);

			Assertions.assertThat(elixir.drinkAs(f.echo())).isTrue();
			Assertions.assertThat(f.boss.buff(ArcaneArmor.class)).isNotNull();
			Assertions.assertThat(f.kit().buff(ArcaneArmor.class)).isNull();
			Assertions.assertThat(f.player.buff(ArcaneArmor.class)).isNull();
		}

		@Test
		@DisplayName("Echo Dragon's Blood elixir drinkAs applies FireImbue on the boss body")
		void dragonsBloodElixirBuffsBody() {
			Fight f = fight();
			ElixirOfDragonsBlood elixir = new ElixirOfDragonsBlood();
			elixir.collect(f.kit().belongings.backpack);

			Assertions.assertThat(elixir.drinkAs(f.echo())).isTrue();
			Assertions.assertThat(f.boss.buff(FireImbue.class)).isNotNull();
			Assertions.assertThat(f.kit().buff(FireImbue.class)).isNull();
			Assertions.assertThat(f.player.buff(FireImbue.class)).isNull();
		}

		@Test
		@DisplayName("Echo Icy Touch elixir drinkAs applies FrostImbue on the boss body")
		void icyTouchElixirBuffsBody() {
			Fight f = fight();
			ElixirOfIcyTouch elixir = new ElixirOfIcyTouch();
			elixir.collect(f.kit().belongings.backpack);

			Assertions.assertThat(elixir.drinkAs(f.echo())).isTrue();
			Assertions.assertThat(f.boss.buff(FrostImbue.class)).isNotNull();
			Assertions.assertThat(f.kit().buff(FrostImbue.class)).isNull();
			Assertions.assertThat(f.player.buff(FrostImbue.class)).isNull();
		}

		@Test
		@DisplayName("Echo Toxic Essence elixir drinkAs applies ToxicImbue on the boss body")
		void toxicEssenceElixirBuffsBody() {
			Fight f = fight();
			ElixirOfToxicEssence elixir = new ElixirOfToxicEssence();
			elixir.collect(f.kit().belongings.backpack);

			Assertions.assertThat(elixir.drinkAs(f.echo())).isTrue();
			Assertions.assertThat(f.boss.buff(ToxicImbue.class)).isNotNull();
			Assertions.assertThat(f.kit().buff(ToxicImbue.class)).isNull();
			Assertions.assertThat(f.player.buff(ToxicImbue.class)).isNull();
		}

		@Test
		@DisplayName("Echo Honeyed Healing drinkAs heals the boss body not the player or kit")
		void honeyedHealingElixirBuffsBossNotPlayer() {
			Fight f = fight();
			ElixirOfHoneyedHealing elixir = new ElixirOfHoneyedHealing();
			elixir.collect(f.kit().belongings.backpack);

			Assertions.assertThat(elixir.drinkAs(f.echo())).isTrue();
			Assertions.assertThat(f.boss.buff(Healing.class)).isNotNull();
			Assertions.assertThat(f.kit().buff(Healing.class)).isNull();
			Assertions.assertThat(f.player.buff(Healing.class)).isNull();
		}

		@Test
		@DisplayName("Echo Aquatic Rejuvenation drinkAs buffs AquaHealing on the boss body")
		void aquaticElixirBuffsBossBody() {
			Fight f = fight();
			ElixirOfAquaticRejuvenation elixir = new ElixirOfAquaticRejuvenation();
			elixir.collect(f.kit().belongings.backpack);

			Assertions.assertThat(elixir.drinkAs(f.echo())).isTrue();
			Assertions.assertThat(f.boss.buff(ElixirOfAquaticRejuvenation.AquaHealing.class)).isNotNull();
			Assertions.assertThat(f.kit().buff(ElixirOfAquaticRejuvenation.AquaHealing.class)).isNull();
			Assertions.assertThat(f.player.buff(ElixirOfAquaticRejuvenation.AquaHealing.class)).isNull();
		}

		@Test
		@DisplayName("Echo Feather Fall drinkAs buffs the boss body not the player")
		void featherFallElixirBuffsBossBody() {
			Fight f = fight();
			ElixirOfFeatherFall elixir = new ElixirOfFeatherFall();
			elixir.collect(f.kit().belongings.backpack);

			Assertions.assertThat(elixir.drinkAs(f.echo())).isTrue();
			Assertions.assertThat(f.boss.buff(ElixirOfFeatherFall.FeatherBuff.class)).isNotNull();
			Assertions.assertThat(f.kit().buff(ElixirOfFeatherFall.FeatherBuff.class)).isNull();
			Assertions.assertThat(f.player.buff(ElixirOfFeatherFall.FeatherBuff.class)).isNull();
		}

		@Test
		@DisplayName("Hero Dragon's Blood drinkAs applies FireImbue on the hero not the echo")
		void heroDragonsBloodElixirBuffsHeroNotEcho() {
			Fight f = fight();
			ElixirOfDragonsBlood elixir = new ElixirOfDragonsBlood();
			elixir.collect(f.player.belongings.backpack);

			Assertions.assertThat(elixir.drinkAs(UseContext.hero(f.player))).isTrue();
			Assertions.assertThat(f.player.buff(FireImbue.class)).isNotNull();
			Assertions.assertThat(f.boss.buff(FireImbue.class)).isNull();
			Assertions.assertThat(f.kit().buff(FireImbue.class)).isNull();
		}

		@Test
		@DisplayName("Hero Arcane Armor drinkAs applies ArcaneArmor on the hero not the echo")
		void heroArcaneArmorElixirBuffsHeroNotEcho() {
			Fight f = fight();
			ElixirOfArcaneArmor elixir = new ElixirOfArcaneArmor();
			elixir.collect(f.player.belongings.backpack);

			Assertions.assertThat(elixir.drinkAs(UseContext.hero(f.player))).isTrue();
			Assertions.assertThat(f.player.buff(ArcaneArmor.class)).isNotNull();
			Assertions.assertThat(f.boss.buff(ArcaneArmor.class)).isNull();
			Assertions.assertThat(f.kit().buff(ArcaneArmor.class)).isNull();
		}

		@Test
		@DisplayName("Echo Arcane Armor drinkAs scales from echo kit level not dungeon depth")
		void echoArcaneArmorScalesFromKitLevel() {
			Fight f = fight();
			f.kit().lvl = 20;
			Dungeon.depth = 1;
			ElixirOfArcaneArmor elixir = new ElixirOfArcaneArmor();
			elixir.collect(f.kit().belongings.backpack);

			Assertions.assertThat(elixir.drinkAs(f.echo())).isTrue();
			Assertions.assertThat(f.boss.buff(ArcaneArmor.class).level()).isEqualTo(5 + 20 / 2);
		}

		@Test
		@DisplayName("Echo Might elixir is refused by executor without consuming")
		void mightElixirHeroOnlyRefuse() {
			Fight f = fight();
			ElixirOfMight elixir = new ElixirOfMight();
			elixir.collect(f.kit().belongings.backpack);

			EchoPolicy policy = EchoTestSupport.policyWithCapabilities(new JSONObject()
					.put("HEAL", EchoTestSupport.capability("ElixirOfMight")));
			boolean spent = EchoRoleExecutor.execute(
					f.boss, policy, status("HEAL"),
					new EchoPolicyChoice("HEAL", "default", null));

			Assertions.assertThat(spent).isFalse();
			Assertions.assertThat(f.kit().belongings.getItem(ElixirOfMight.class)).isNotNull();
		}
	}

	@Nested
	@DisplayName("Runestones / bombs / tipped darts")
	class StonesBombsDarts {

		@Test
		@DisplayName("Echo StoneOfBlink throwAs teleports the boss body to the land cell")
		void blinkStoneTeleportsBossBody() {
			Fight f = fight();
			StoneOfBlink stone = new StoneOfBlink();
			stone.collect(f.kit().belongings.backpack);
			int dest = emptyAdjacentAwayFromPlayer(f);
			int start = f.boss.pos;

			Assertions.assertThat(stone.throwAs(f.echo(), dest)).isTrue();
			Assertions.assertThat(f.boss.pos).isEqualTo(dest);
			Assertions.assertThat(f.boss.pos).isNotEqualTo(start);
			Assertions.assertThat(EchoTestSupport.stubSpritePlacedCell(f.boss)).isEqualTo(dest);
			Assertions.assertThat(f.kit().belongings.getItem(StoneOfBlink.class)).isNull();
		}

		@Test
		@DisplayName("Echo throwAs stays busy until missile callback (master-style gating)")
		void throwAsStaysBusyUntilMissileCallback() {
			Fight f = fight();
			EchoTestSupport.DeferredProjectileGroup fx = EchoTestSupport.attachDeferredProjectileParent(f.boss);
			StoneOfBlink stone = new StoneOfBlink();
			stone.collect(f.kit().belongings.backpack);
			int dest = emptyAdjacentAwayFromPlayer(f);
			float timeBefore = f.boss.cooldown();

			Assertions.assertThat(stone.throwAs(f.echo(), dest)).isTrue();
			Assertions.assertThat(fx.hasPending()).isTrue();
			Assertions.assertThat(f.boss.isBusy()).isTrue();
			Assertions.assertThat(f.kit().belongings.getItem(StoneOfBlink.class)).isNotNull();
			Assertions.assertThat(f.boss.pos).isNotEqualTo(dest);

			fx.complete();

			Assertions.assertThat(f.boss.isBusy()).isFalse();
			Assertions.assertThat(f.kit().belongings.getItem(StoneOfBlink.class)).isNull();
			Assertions.assertThat(f.boss.pos).isEqualTo(dest);
			Assertions.assertThat(f.boss.cooldown()).isGreaterThan(timeBefore);
		}

		@Test
		@DisplayName("Echo StoneOfBlink throwAs onto occupied cell teleports short of the occupant")
		void blinkStoneThrowOntoOccupiedCellDoesNotNpe() {
			Fight f = fight();
			StoneOfBlink stone = new StoneOfBlink();
			stone.collect(f.kit().belongings.backpack);
			int start = f.boss.pos;
			int occupied = f.player.pos;

			Assertions.assertThat(stone.throwAs(f.echo(), occupied)).isTrue();
			Assertions.assertThat(f.boss.pos).isNotEqualTo(start);
			Assertions.assertThat(f.boss.pos).isNotEqualTo(occupied);
			Assertions.assertThat(f.kit().belongings.getItem(StoneOfBlink.class)).isNull();
		}

		@Test
		@DisplayName("Echo StoneOfFear throwAs applies Terror on the Hero")
		void fearStoneTerrifiesHero() {
			Fight f = fight();
			StoneOfFear stone = new StoneOfFear();
			stone.collect(f.kit().belongings.backpack);
			EchoTestSupport.InstantProjectileGroup fx = EchoTestSupport.attachInstantProjectileParent(f.boss);

			Assertions.assertThat(f.player.sprite.ch).isSameAs(f.player);
			Assertions.assertThat(f.kit().sprite).isNull();
			Assertions.assertThat(stone.throwAs(f.echo(), f.player.pos)).isTrue();
			Assertions.assertThat(fx.missileSpriteRecycles).isGreaterThan(0);
			Assertions.assertThat(f.player.buff(Terror.class)).isNotNull();
			Assertions.assertThat(f.kit().belongings.getItem(StoneOfFear.class)).isNull();
		}

		@Test
		@DisplayName("Echo StoneOfBlast throwAs damages the Hero on a live fight stage")
		void blastStoneDamagesHero() {
			Fight f = fight();
			StoneOfBlast stone = new StoneOfBlast();
			stone.collect(f.kit().belongings.backpack);
			EchoTestSupport.InstantProjectileGroup fx = EchoTestSupport.attachInstantProjectileParent(f.boss);
			int hpBefore = f.player.HP;

			Assertions.assertThat(f.player.sprite.ch).isSameAs(f.player);
			Assertions.assertThat(f.boss.sprite.ch).isSameAs(f.boss);
			Assertions.assertThat(f.kit().sprite).isNull();
			Assertions.assertThat(stone.throwAs(f.echo(), f.player.pos)).isTrue();
			Assertions.assertThat(fx.missileSpriteRecycles).isGreaterThan(0);
			Assertions.assertThat(f.player.HP).isLessThan(hpBefore);
			Assertions.assertThat(f.kit().belongings.getItem(StoneOfBlast.class)).isNull();
		}

		@Test
		@DisplayName("Echo StoneOfShock throwAs paralyzes the Hero via lightning arcs")
		void shockStoneParalyzesHero() {
			Fight f = fight();
			StoneOfShock stone = new StoneOfShock();
			stone.collect(f.kit().belongings.backpack);
			EchoTestSupport.InstantProjectileGroup fx = EchoTestSupport.attachInstantProjectileParent(f.boss);

			Assertions.assertThat(f.player.sprite.ch).isSameAs(f.player);
			Assertions.assertThat(f.boss.sprite.ch).isSameAs(f.boss);
			Assertions.assertThat(f.kit().sprite).isNull();
			Assertions.assertThat(stone.throwAs(f.echo(), f.player.pos)).isTrue();
			Assertions.assertThat(fx.missileSpriteRecycles).isGreaterThan(0);
			Assertions.assertThat(f.player.buff(Paralysis.class)).isNotNull();
			Assertions.assertThat(f.kit().belongings.getItem(StoneOfShock.class)).isNull();
		}

		@Test
		@DisplayName("Echo PoisonDart throwAs damages and poisons the Hero on a live fight stage")
		void poisonDartDamagesHero() {
			Fight f = fight();
			PoisonDart dart = new PoisonDart();
			dart.collect(f.kit().belongings.backpack);
			EchoTestSupport.InstantProjectileGroup fx = EchoTestSupport.attachInstantProjectileParent(f.boss);
			// Tipped darts need STR 11+ for surprise accuracy; kit starts at 10.
			f.kit().STR = 16;
			f.kit().invisible = 1;

			int aimCell = emptyCellAwayFrom(f, f.player.pos, f.boss.pos);
			Assertions.assertThat(aimCell).isGreaterThanOrEqualTo(0);
			f.player.move(aimCell, false);
			Assertions.assertThat(f.player.pos).isEqualTo(aimCell);
			Assertions.assertThat(EchoTestSupport.stubSpritePlacedCell(f.player)).isEqualTo(aimCell);

			int hpBefore = f.player.HP;
			float kitBefore = f.kit().cooldown();

			Assertions.assertThat(f.player.sprite.ch).isSameAs(f.player);
			Assertions.assertThat(f.boss.sprite.ch).isSameAs(f.boss);
			Assertions.assertThat(f.kit().sprite).isNull();
			Assertions.assertThat(dart.throwAs(f.echo(), f.player.pos)).isTrue();
			Assertions.assertThat(fx.missileSpriteRecycles).isGreaterThan(0);
			Assertions.assertThat(f.kit().cooldown()).isEqualTo(kitBefore);
			Assertions.assertThat(f.player.HP).isLessThan(hpBefore);
			Assertions.assertThat(f.player.buff(Poison.class)).isNotNull();

			int afterCell = emptyCellAwayFrom(f, f.player.pos, f.boss.pos);
			Assertions.assertThat(afterCell).isGreaterThanOrEqualTo(0);
			f.player.move(afterCell, false);
			Assertions.assertThat(f.player.pos).isEqualTo(afterCell);
			Assertions.assertThat(EchoTestSupport.stubSpritePlacedCell(f.player)).isEqualTo(afterCell);
			Assertions.assertThat(f.player.buff(Poison.class)).isNotNull();
		}

	}

	/**
	 * Sanity: already-covered drink kinds still work through healing/haste
	 * fixtures.
	 */
	@Test
	@DisplayName("Echo Healing drinkAs still applies Healing on the boss body")
	void healingDrinkStillWorks() {
		Fight f = fight();
		f.boss.HP = Math.max(1, f.boss.HT / 4);
		PotionOfHealing pot = new PotionOfHealing();
		pot.identify();
		pot.collect(f.kit().belongings.backpack);

		Assertions.assertThat(pot.drinkAs(f.echo())).isTrue();
		Assertions.assertThat(f.boss.buff(
				com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing.class)).isNotNull();
	}

	@Test
	@DisplayName("Echo Haste drinkAs still applies Haste on the boss body")
	void hasteDrinkStillWorks() {
		Fight f = fight();
		PotionOfHaste pot = new PotionOfHaste();
		pot.identify();
		pot.collect(f.kit().belongings.backpack);

		Assertions.assertThat(pot.drinkAs(f.echo())).isTrue();
		Assertions.assertThat(f.boss.buff(
				com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste.class)).isNotNull();
	}

	private static EchoPolicyStatus status(String role) {
		return new EchoPolicyStatus.Builder().rolesReady(java.util.Set.of(role)).build();
	}

	private static int emptyAdjacentAwayFromPlayer(Fight f) {
		int[] neigh = com.watabou.utils.PathFinder.NEIGHBOURS8;
		for (int n : neigh) {
			int cell = f.boss.pos + n;
			if (cell == f.player.pos) {
				continue;
			}
			if (cell >= 0 && cell < Dungeon.level.length()
					&& Dungeon.level.passable[cell]
					&& com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(cell) == null) {
				return cell;
			}
		}
		return -1;
	}

	/** First empty passable cell that is not any of the excluded positions. */
	private static int emptyCellAwayFrom(Fight f, int... excluded) {
		java.util.HashSet<Integer> skip = new java.util.HashSet<>();
		for (int cell : excluded) {
			skip.add(cell);
		}
		for (int cell = 0; cell < Dungeon.level.length(); cell++) {
			if (skip.contains(cell)) {
				continue;
			}
			if (Dungeon.level.passable[cell]
					&& com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(cell) == null) {
				return cell;
			}
		}
		return -1;
	}
}
