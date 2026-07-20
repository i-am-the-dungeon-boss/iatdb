package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

public class EchoFightResult {
    public final String echoId;
    public final boolean bossWin;
    public final int depth;
    public final long timestamp;
    public final String gameVersion;
    public final String playerClass;
    public final int damageDealt;
    public final int damageTaken;
    public final int turns;

    public EchoFightResult(String echoId, boolean bossWin, int depth, long timestamp,
            String gameVersion, String playerClass,
            int damageDealt, int damageTaken, int turns) {
        if (playerClass == null || playerClass.isEmpty()) {
            throw new IllegalArgumentException("fight result requires player_class");
        }
        this.echoId = echoId;
        this.bossWin = bossWin;
        this.depth = depth;
        this.timestamp = timestamp;
        this.gameVersion = gameVersion;
        this.playerClass = playerClass;
        this.damageDealt = damageDealt;
        this.damageTaken = damageTaken;
        this.turns = turns;
    }
}
