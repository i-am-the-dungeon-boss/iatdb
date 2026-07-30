package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.Ratmogrify;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Challenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Feint;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.NaturesPower;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpectralBlades;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.ElementalBlast;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WarpBeacon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WildMagic;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.DeathMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.ShadowClone;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.SmokeBomb;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Shockwave;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClericArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.DuelistArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.HuntressArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.MageArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.RogueArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.WarriorArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.PathFinder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Refuse and success paths for Echo armor abilities must leave
 * {@link EchoBoss#isBusy()} false so the next actor turn is not stalled.
 */
@ExtendWith(GdxTestExtension.class)
class EchoArmorAbilityNextTurnTest {

	static Stream<Arguments> successfulAbilities() {
		return Stream.of(
				Arguments.of("Endure", new Endure(), new WarriorArmor(), (Targeting) f -> null),
				Arguments.of("NaturesPower", new NaturesPower(), new HuntressArmor(), (Targeting) f -> null),
				Arguments.of("AscendedForm", new AscendedForm(), new ClericArmor(), (Targeting) f -> null),
				Arguments.of("SpiritHawk", new SpiritHawk(), new HuntressArmor(), (Targeting) f -> null),
				Arguments.of("ShadowClone", new ShadowClone(), new RogueArmor(), (Targeting) f -> null),
				Arguments.of("Shockwave", new Shockwave(), new WarriorArmor(), (Targeting) f -> f.player.pos),
				Arguments.of("DeathMark", new DeathMark(), new RogueArmor(), (Targeting) f -> f.player.pos),
				Arguments.of("Challenge", new Challenge(), new DuelistArmor(), (Targeting) f -> f.player.pos),
				Arguments.of("ElementalStrike", new ElementalStrike(), new DuelistArmor(),
						(Targeting) f -> {
							f.boss.getEchoHero().belongings.weapon = new WornShortsword();
							return f.player.pos;
						}),
				Arguments.of("WildMagic", new WildMagic(), new MageArmor(), (Targeting) f -> {
					WandOfMagicMissile wand = new WandOfMagicMissile();
					wand.cursed = false;
					wand.curCharges = 5;
					wand.collect(f.boss.getEchoHero().belongings.backpack);
					return f.player.pos;
				}),
				Arguments.of("ElementalBlast", new ElementalBlast(), new MageArmor(), (Targeting) f -> {
					f.boss.getEchoHero().belongings.weapon = new MagesStaff(new WandOfMagicMissile());
					return f.player.pos;
				}),
				Arguments.of("SpectralBlades", new SpectralBlades(), new HuntressArmor(), (Targeting) f -> {
					f.boss.fieldOfView = Dungeon.level.heroFOV;
					f.boss.getEchoHero().invisible = 1;
					return f.player.pos;
				}),
				Arguments.of("SmokeBomb", new SmokeBomb(), new RogueArmor(),
						(Targeting) f -> emptyAdjacent(f.boss.pos)),
				Arguments.of("HeroicLeap", new HeroicLeap(), new WarriorArmor(),
						(Targeting) f -> emptyAdjacent(f.boss.pos)),
				Arguments.of("Feint", new Feint(), new DuelistArmor(),
						(Targeting) f -> emptyAdjacent(f.boss.pos)),
				Arguments.of("PowerOfMany", new PowerOfMany(), new ClericArmor(),
						(Targeting) f -> emptyAdjacent(f.boss.pos)),
				Arguments.of("WarpBeacon", new WarpBeacon(), new MageArmor(), (Targeting) f -> {
					int cell = f.boss.pos;
					Dungeon.level.visited[cell] = true;
					Dungeon.level.mapped[cell] = true;
					return cell;
				}),
				Arguments.of("Trinity", new Trinity(), new ClericArmor(), (Targeting) f -> {
					((Trinity) f.ability).imbueBodyForm(new Blazing());
					return null;
				}),
				Arguments.of("Ratmogrify", new Ratmogrify(), new RogueArmor(), (Targeting) f -> {
					int cell = emptyAdjacent(f.boss.pos);
					Snake snake = new Snake();
					snake.pos = cell;
					EchoTestSupport.linkStubSprite(snake);
					Dungeon.level.mobs.add(snake);
					Actor.add(snake);
					return cell;
				}));
	}

	static Stream<Arguments> refusedAbilities() {
		return Stream.of(
				Arguments.of("Shockwave self-target", new Shockwave(), new WarriorArmor(),
						(Targeting) f -> f.boss.pos),
				Arguments.of("WildMagic self-target", new WildMagic(), new MageArmor(), (Targeting) f -> {
					WandOfMagicMissile wand = new WandOfMagicMissile();
					wand.cursed = false;
					wand.curCharges = 5;
					wand.collect(f.boss.getEchoHero().belongings.backpack);
					return f.boss.pos;
				}),
				Arguments.of("DeathMark ally cell", new DeathMark(), new RogueArmor(),
						(Targeting) f -> f.boss.pos),
				Arguments.of("DeathMark empty cell", new DeathMark(), new RogueArmor(),
						(Targeting) f -> emptyAdjacent(f.boss.pos)),
				Arguments.of("PowerOfMany hero cell", new PowerOfMany(), new ClericArmor(),
						(Targeting) f -> f.player.pos),
				Arguments.of("Ratmogrify EchoBoss too strong", new Ratmogrify(), new RogueArmor(),
						(Targeting) f -> f.player.pos),
				Arguments.of("ElementalBlast no staff", new ElementalBlast(), new MageArmor(),
						(Targeting) f -> f.player.pos),
				Arguments.of("SmokeBomb occupied cell", new SmokeBomb(), new RogueArmor(),
						(Targeting) f -> f.player.pos),
				Arguments.of("Feint non-adjacent", new Feint(), new DuelistArmor(),
						(Targeting) f -> f.player.pos),
				Arguments.of("SpectralBlades self-target", new SpectralBlades(), new HuntressArmor(),
						(Targeting) f -> f.boss.pos),
				Arguments.of("WarpBeacon unmapped cell", new WarpBeacon(), new MageArmor(),
						(Targeting) f -> {
							int cell = emptyAdjacent(f.boss.pos);
							Dungeon.level.visited[cell] = false;
							Dungeon.level.mapped[cell] = false;
							return cell;
						}));
	}

	@ParameterizedTest(name = "Echo {0} success leaves boss able to act next turn")
	@MethodSource("successfulAbilities")
	void successAllowsNextTurn(String name, ArmorAbility ability, ClassArmor armor, Targeting targeting) {
		Fight f = fight(ability);
		armor.charge = 100;
		Integer target = targeting.cell(f);
		fillFov(f.boss);

		ability.activateAs(f.echo(), armor, target);

		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@ParameterizedTest(name = "Echo {0} refuse leaves boss able to act next turn")
	@MethodSource("refusedAbilities")
	void refuseAllowsNextTurn(String name, ArmorAbility ability, ClassArmor armor, Targeting targeting) {
		Fight f = fight(ability);
		armor.charge = 100;
		Integer target = targeting.cell(f);
		fillFov(f.boss);

		ability.activateAs(f.echo(), armor, target);

		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	@Test
	@DisplayName("Echo armor refuse before busy (low charge) still allows next turn")
	void lowChargeRefuseAllowsNextTurn() {
		Fight f = fight(new Shockwave());
		WarriorArmor armor = new WarriorArmor();
		armor.charge = 0;

		boolean ok = new Shockwave().activateAs(f.echo(), armor, f.player.pos);

		Assertions.assertThat(ok).isFalse();
		EchoBossTurnAssert.assertCanTakeNextTurn(f.boss);
	}

	private static Fight fight(ArmorAbility ability) {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		fillFov(boss);
		Dungeon.level.heroFOV = boss.fieldOfView;
		return new Fight(player, boss, ability);
	}

	private static void fillFov(EchoBoss boss) {
		boss.fieldOfView = new boolean[Dungeon.level.length()];
		Arrays.fill(boss.fieldOfView, true);
		Dungeon.level.heroFOV = boss.fieldOfView;
	}

	private static int emptyAdjacent(int from) {
		for (int i : PathFinder.NEIGHBOURS8) {
			int cell = from + i;
			if (cell >= 0 && cell < Dungeon.level.length()
					&& Dungeon.level.map[cell] == Terrain.EMPTY
					&& Dungeon.hero.pos != cell
					&& Actor.findChar(cell) == null) {
				return cell;
			}
		}
		return -1;
	}

	@FunctionalInterface
	private interface Targeting {
		Integer cell(Fight f);
	}

	private static final class Fight {
		final Hero player;
		final EchoBoss boss;
		final ArmorAbility ability;

		Fight(Hero player, EchoBoss boss, ArmorAbility ability) {
			this.player = player;
			this.boss = boss;
			this.ability = ability;
		}

		UseContext echo() {
			return UseContext.echo(boss);
		}
	}
}
