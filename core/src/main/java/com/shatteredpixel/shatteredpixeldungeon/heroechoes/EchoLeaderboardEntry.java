package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

public final class EchoLeaderboardEntry {

	public final int rank;
	public final String echoId;
	public final boolean bossWin;
	public final int depth;
	public final String playerClass;
	public final int damageDealt;
	public final int damageTaken;
	public final int turns;
	public final float winRateProxy;

	public EchoLeaderboardEntry(
			int rank,
			String echoId,
			boolean bossWin,
			int depth,
			String playerClass,
			int damageDealt,
			int damageTaken,
			int turns,
			float winRateProxy
	) {
		this.rank = rank;
		this.echoId = echoId;
		this.bossWin = bossWin;
		this.depth = depth;
		this.playerClass = playerClass;
		this.damageDealt = damageDealt;
		this.damageTaken = damageTaken;
		this.turns = turns;
		this.winRateProxy = winRateProxy;
	}

	public static EchoLeaderboardEntry fromFightResult(EchoFightResult result, int rank) {
		return new EchoLeaderboardEntry(
				rank,
				result.echoId,
				result.bossWin,
				result.depth,
				result.playerClass,
				result.damageDealt,
				result.damageTaken,
				result.turns,
				result.bossWin ? 1f : 0f
		);
	}
}
