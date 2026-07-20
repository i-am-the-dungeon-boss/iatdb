package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.watabou.utils.Bundle;

import java.util.Locale;

public class Echo {

    public static final String BUNDLE_KEY = "echo";
    /** Prefix when {@link SPDSettings#playerName()} is unset (plus hero class). */
    public static final String DEFAULT_USER_NAME = "Anonymous";
    private static final String ECHO_DATA = "echo_data";

    public String echoId;
    public String gameVersion;
    public int depth;
    public String heroClass;
    public String userName = DEFAULT_USER_NAME;
    public int lvl;
    public int hp;
    public int ht;
    public long timestamp;
    public long gameSeed;
    public Bundle echoData;

    public boolean hasCombatData() {
        return echoData != null;
    }

    public static Echo create(int depth, String gameVersion, long gameSeed,
            String heroClass, int lvl, int hp, int ht,
            Bundle echoData) {
        if (heroClass == null || heroClass.isEmpty()) {
            throw new IllegalArgumentException("echo requires hero_class");
        }
        Echo s = new Echo();
        s.echoId = depth + "-" + System.currentTimeMillis();
        s.depth = depth;
        s.gameVersion = gameVersion;
        s.gameSeed = gameSeed;
        s.timestamp = System.currentTimeMillis();
        s.heroClass = heroClass;
        s.userName = defaultUserName(heroClass);
        s.lvl = lvl;
        s.hp = hp;
        s.ht = ht;
        s.echoData = echoData;
        return s;
    }

    public static Echo create(int depth, String gameVersion, long gameSeed,
            String heroClass, String userName, int lvl, int hp, int ht,
            Bundle echoData) {
        Echo echo = create(depth, gameVersion, gameSeed, heroClass, lvl, hp, ht, echoData);
        echo.userName = resolveUserName(userName, heroClass);
        return echo;
    }

    public static Echo fromHero(Hero hero, int depth, String gameVersion, long gameSeed) {
        if (hero == null) {
            throw new IllegalArgumentException("echo requires hero");
        }
        if (hero.heroClass == null) {
            throw new IllegalArgumentException("echo requires hero_class");
        }
        Bundle data = EchoHeroSnapshot.captureFromHero(hero);
        return create(
                depth,
                gameVersion,
                gameSeed,
                hero.heroClass.name(),
                SPDSettings.playerName(),
                hero.lvl,
                Math.max(0, hero.HP),
                Math.max(1, hero.HT),
                data);
    }

    /** e.g. {@code MAGE} → {@code Anonymous Mage}. */
    public static String defaultUserName(String heroClass) {
        if (heroClass == null || heroClass.isEmpty()) {
            return DEFAULT_USER_NAME;
        }
        String label = heroClass.substring(0, 1).toUpperCase(Locale.ROOT)
                + heroClass.substring(1).toLowerCase(Locale.ROOT);
        return DEFAULT_USER_NAME + " " + label;
    }

    public static String resolveUserName(String userName, String heroClass) {
        if (userName == null) {
            return defaultUserName(heroClass);
        }
        String trimmed = userName.trim();
        return trimmed.isEmpty() ? defaultUserName(heroClass) : trimmed;
    }

    public boolean isCompatibleWith(String currentVersion) {
        // Version gating disabled for now (keep update nudge on title scene only).
        // return majorVersion(gameVersion).equals(majorVersion(currentVersion));
        return true;
    }

    // private static String majorVersion(String version) {
    // if (version == null || version.isEmpty()) {
    // return "";
    // }
    // String base = version.split("-", 2)[0];
    // return base.split("\\.", 2)[0];
    // }

    public Bundle toBundle() {
        Bundle b = new Bundle();
        b.put("echo_id", echoId);
        b.put("game_version", gameVersion != null ? gameVersion : "");
        b.put("depth", depth);
        b.put("hero_class", heroClass);
        b.put("user_name", resolveUserName(userName, heroClass));
        b.put("lvl", lvl);
        b.put("hp", hp);
        b.put("ht", ht);
        b.put("timestamp", timestamp);
        b.put("game_seed", gameSeed);
        if (echoData != null) {
            b.put(ECHO_DATA, echoData);
        }
        return b;
    }

    public static Echo fromBundle(Bundle b) {
        Echo s = new Echo();
        s.echoId = b.getString("echo_id");
        if (b.contains("game_version")) {
            s.gameVersion = b.getString("game_version");
        }
        s.depth = b.getInt("depth");
        s.heroClass = b.getString("hero_class");
        s.userName = resolveUserName(
                b.contains("user_name") ? b.getString("user_name") : null,
                s.heroClass);
        s.lvl = b.getInt("lvl");
        s.hp = b.getInt("hp");
        s.ht = b.getInt("ht");
        s.timestamp = b.getLong("timestamp");
        s.gameSeed = b.getLong("game_seed");
        if (b.contains(ECHO_DATA)) {
            s.echoData = b.getBundle(ECHO_DATA);
        }
        return s;
    }

    public Bundle toFileBundle() {
        Bundle file = new Bundle();
        file.put(BUNDLE_KEY, toBundle());
        return file;
    }

    public static Echo fromFileBundle(Bundle file) {
        return fromBundle(file.getBundle(BUNDLE_KEY));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Echo))
            return false;
        Echo that = (Echo) o;
        return echoId != null && echoId.equals(that.echoId);
    }

    @Override
    public int hashCode() {
        return echoId != null ? echoId.hashCode() : 0;
    }
}
