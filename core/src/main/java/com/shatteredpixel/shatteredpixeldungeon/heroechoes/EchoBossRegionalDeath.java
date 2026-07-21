package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Degrade;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KingsCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.TengusMask;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.LloydsBeacon;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.WornKey;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.GooBlob;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.MetalShard;
import com.shatteredpixel.shatteredpixeldungeon.journal.Bestiary;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.PrisonBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.function.Supplier;

/**
 * Applies the rewards and progression of the regional floor boss when an echo
 * boss is defeated.
 * Regional boss badges stay with the real bosses (Goo, Tengu, etc.), not echo
 * victories.
 */
public final class EchoBossRegionalDeath {

	private EchoBossRegionalDeath() {
	}

	public static void apply(EchoBoss boss, Object cause) {
		if (boss == null) {
			return;
		}

		switch (Dungeon.depth) {
			case 5:
				applySewerBoss(boss);
				break;
			case 10:
				applyPrisonBoss(boss);
				break;
			case 15:
				applyCavesBoss(boss);
				break;
			case 20:
				applyCityBoss(boss, cause);
				break;
			case 25:
				applyHallsBoss(boss, cause);
				break;
			default:
				if (Dungeon.level != null) {
					GameScene.bossSlain();
					Dungeon.level.unseal();
				}
				break;
		}

		EchoCaptureTrigger.onBossDefeated();
	}

	private static void applySewerBoss(EchoBoss boss) {
		if (Dungeon.level != null) {
			Dungeon.level.unseal();
			GameScene.bossSlain();
			dropWornKey(boss.pos);
			dropChanceLoot(boss.pos, GooBlob::new);
		}

		recordRegionalBossVictory(0, 1000);
		GLog.n(Messages.get(Goo.class, "defeated"));
	}

	private static void applyPrisonBoss(EchoBoss boss) {
		if (Dungeon.level != null) {
			if (Dungeon.hero != null && Dungeon.hero.subClass == HeroSubClass.NONE) {
				Dungeon.level.drop(new TengusMask(), boss.pos).sprite.drop();
			}

			GameScene.bossSlain();

			if (Dungeon.level instanceof PrisonBossLevel) {
				((PrisonBossLevel) Dungeon.level).completeEchoBossVictory();
			}
		}

		recordRegionalBossVictory(1, 2000);
		upgradeLloydsBeacon();
		GLog.n(Messages.get(Tengu.class, "defeated"));
	}

	private static void applyCavesBoss(EchoBoss boss) {
		if (Dungeon.level != null) {
			GameScene.bossSlain();
			Dungeon.level.unseal();
			dropChanceLoot(boss.pos, MetalShard::new);
		}

		recordRegionalBossVictory(2, 3000);
		upgradeLloydsBeacon();
		GLog.n(Messages.get(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300.class, "defeated"));
	}

	private static void applyCityBoss(EchoBoss boss, Object cause) {
		if (Dungeon.level != null) {
			GameScene.bossSlain();

			Heap throneHeap = Dungeon.level.heaps.get(CityBossLevel.throne);
			if (throneHeap != null) {
				for (Item item : throneHeap.items) {
					Dungeon.level.drop(item, CityBossLevel.throne + Dungeon.level.width());
				}
				throneHeap.destroy();
			}

			if (boss.pos == CityBossLevel.throne) {
				Dungeon.level.drop(new KingsCrown(), boss.pos + Dungeon.level.width()).sprite.drop(boss.pos);
			} else {
				Dungeon.level.drop(new KingsCrown(), boss.pos).sprite.drop();
			}

			Dungeon.level.unseal();

			Bestiary.skipCountingEncounters = true;
			for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
				if (mob.alignment == boss.alignment && mob != boss && mob.properties()
						.contains(com.shatteredpixel.shatteredpixeldungeon.actors.Char.Property.BOSS_MINION)) {
					mob.die(null);
				}
			}
			Bestiary.skipCountingEncounters = false;
		}

		recordRegionalBossVictory(3, 4000);
		upgradeLloydsBeacon();

		if (Dungeon.hero != null && Dungeon.hero.buff(Degrade.class) != null) {
			Dungeon.hero.buff(Degrade.class).detach();
		}

		GLog.n(Messages.get(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing.class, "defeated"));
	}

	@SuppressWarnings("unchecked")
	private static void applyHallsBoss(EchoBoss boss, Object cause) {
		if (Dungeon.level != null) {
			Bestiary.skipCountingEncounters = true;
			for (Mob mob : (Iterable<Mob>) Dungeon.level.mobs.clone()) {
				if (mob instanceof YogDzewa.Larva
						|| mob instanceof YogDzewa.YogRipper
						|| mob instanceof YogDzewa.YogEye
						|| mob instanceof YogDzewa.YogScorpio) {
					mob.die(cause);
				}
			}
			Bestiary.skipCountingEncounters = false;

			new YogDzewa().updateVisibility(Dungeon.level);

			GameScene.bossSlain();

			Dungeon.level.unseal();
		}

		Statistics.qualifiedForBossChallengeBadge = false;
		Statistics.bossScores[4] += 5000 + 1250 * Statistics.spawnersAlive;

		GLog.n(Messages.get(YogDzewa.class, "defeated"));
	}

	private static void recordRegionalBossVictory(int regionIndex, int scorePoints) {
		Statistics.qualifiedForBossChallengeBadge = false;
		Statistics.bossScores[regionIndex] += scorePoints;
	}

	private static void dropChanceLoot(int pos, Supplier<Item> itemFactory) {
		int drops = Random.chances(new float[] { 0, 0, 6, 3, 1 });
		for (int i = 0; i < drops; i++) {
			int ofs;
			do {
				ofs = PathFinder.NEIGHBOURS8[Random.Int(8)];
			} while (!Dungeon.level.passable[pos + ofs]);
			Dungeon.level.drop(itemFactory.get(), pos + ofs).sprite.drop(pos);
		}
	}

	private static void dropWornKey(int pos) {
		Dungeon.level.drop(new WornKey(Dungeon.depth), pos).sprite.drop();
	}

	private static void upgradeLloydsBeacon() {
		if (Dungeon.hero == null) {
			return;
		}
		LloydsBeacon beacon = Dungeon.hero.belongings.getItem(LloydsBeacon.class);
		if (beacon != null) {
			beacon.upgrade();
		}
	}
}
