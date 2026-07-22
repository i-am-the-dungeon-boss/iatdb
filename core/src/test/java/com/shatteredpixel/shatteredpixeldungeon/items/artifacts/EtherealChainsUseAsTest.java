package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.UseContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class EtherealChainsUseAsTest {

	@AfterEach
	void cleanup() {
		Dungeon.level = null;
		EchoTestSupport.resetWorkflowState();
	}

	@Test
	@DisplayName("Echo useAs pulls a visible enemy toward the boss body")
	void echoPullsEnemyTowardBoss() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		EtherealChains chains = new EtherealChains();
		chains.charge = 10;
		kit.belongings.artifact = chains;
		chains.activate(kit);

		Rat rat = new Rat();
		rat.pos = boss.pos + 3;
		EchoTestSupport.linkStubSprite(rat);
		Dungeon.level.mobs.add(rat);
		Actor.add(rat);
		Dungeon.level.visited[rat.pos] = true;
		Dungeon.level.mapped[rat.pos] = true;
		int ratBefore = rat.pos;

		boolean ok = chains.useAs(UseContext.echo(boss), rat.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(rat.pos).isNotEqualTo(ratBefore);
		Assertions.assertThat(Dungeon.level.distance(boss.pos, rat.pos))
				.isLessThan(Dungeon.level.distance(boss.pos, ratBefore));
		Assertions.assertThat(chains.charge).isLessThan(10);
	}

	@Test
	@DisplayName("Echo useAs pull places the enemy sprite at the pulled cell")
	void echoPullPlacesEnemySprite() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		EtherealChains chains = new EtherealChains();
		chains.charge = 10;
		kit.belongings.artifact = chains;
		chains.activate(kit);

		Rat rat = new Rat();
		rat.pos = boss.pos + 3;
		EchoTestSupport.linkStubSprite(rat);
		Dungeon.level.mobs.add(rat);
		Actor.add(rat);
		Dungeon.level.visited[rat.pos] = true;
		Dungeon.level.mapped[rat.pos] = true;

		boolean ok = chains.useAs(UseContext.echo(boss), rat.pos);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(EchoTestSupport.stubSpritePlacedCell(rat)).isEqualTo(rat.pos);
	}

	@Test
	@DisplayName("Echo useAs self-pull places the boss sprite at the destination")
	void echoSelfPullPlacesBossSprite() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);

		Hero kit = boss.getEchoHero();
		EtherealChains chains = new EtherealChains();
		chains.charge = 10;
		kit.belongings.artifact = chains;
		chains.activate(kit);

		// Solid neighbour so chainLocationAs can grab
		int dest = -1;
		for (int n : com.watabou.utils.PathFinder.NEIGHBOURS8) {
			int cell = boss.pos + n;
			if (cell < 0 || cell >= Dungeon.level.length()) {
				continue;
			}
			if (!Dungeon.level.passable[cell] && !Dungeon.level.avoid[cell]) {
				continue;
			}
			if (com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(cell) != null) {
				continue;
			}
			boolean solidNear = false;
			for (int m : com.watabou.utils.PathFinder.NEIGHBOURS8) {
				int adj = cell + m;
				if (adj >= 0 && adj < Dungeon.level.length() && Dungeon.level.solid[adj]) {
					solidNear = true;
					break;
				}
			}
			if (solidNear) {
				dest = cell;
				break;
			}
		}
		Assertions.assertThat(dest).isGreaterThanOrEqualTo(0);
		Dungeon.level.visited[dest] = true;
		Dungeon.level.mapped[dest] = true;

		boolean ok = chains.useAs(UseContext.echo(boss), dest);

		Assertions.assertThat(ok).isTrue();
		Assertions.assertThat(boss.pos).isEqualTo(dest);
		Assertions.assertThat(EchoTestSupport.stubSpritePlacedCell(boss)).isEqualTo(dest);
	}
}
