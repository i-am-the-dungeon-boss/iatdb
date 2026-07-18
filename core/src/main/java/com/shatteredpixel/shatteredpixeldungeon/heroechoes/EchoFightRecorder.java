package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoOnlineSync;

/**
 * Records hero-boss fight outcomes locally and/or to the online service.
 */
public final class EchoFightRecorder {

	private final EchoLeaderboardStorage storage;
	private int damageDealt;
	private int damageTaken;
	private int turns;

	public EchoFightRecorder(EchoLeaderboardStorage storage) {
		this.storage = storage;
	}

	public void trackDamageDealt(int amount) {
		if (amount > 0) damageDealt += amount;
	}

	public void trackDamageTaken(int amount) {
		if (amount > 0) damageTaken += amount;
	}

	public void trackTurn() {
		turns++;
	}

	public void recordBossVictory(Echo bossEcho, int depth, HeroClass playerClass, String gameVersion) {
		persist(new EchoFightResult(
				bossEcho != null ? bossEcho.echoId : null,
				true,
				depth,
				System.currentTimeMillis(),
				gameVersion,
				playerClass != null ? playerClass.name() : "UNKNOWN",
				damageDealt,
				damageTaken,
				turns
		));
	}

	private void persist(EchoFightResult result) {
		if (EchoPlayModePaths.persistsLeaderboardLocally()) {
			storage.append(result);
		}
		EchoOnlineSync.instance().postLeaderboardResultAsync(result);
	}

	public void recordBossDefeat(Echo bossEcho, int depth, HeroClass playerClass, String gameVersion) {
		persist(new EchoFightResult(
				bossEcho != null ? bossEcho.echoId : null,
				false,
				depth,
				System.currentTimeMillis(),
				gameVersion,
				playerClass != null ? playerClass.name() : "UNKNOWN",
				damageDealt,
				damageTaken,
				turns
		));
	}

	public int damageDealt() { return damageDealt; }
	public int damageTaken() { return damageTaken; }
	public int turns() { return turns; }
}
