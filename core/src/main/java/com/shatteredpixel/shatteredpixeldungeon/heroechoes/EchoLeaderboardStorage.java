package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.badlogic.gdx.files.FileHandle;
import com.watabou.utils.FileUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EchoLeaderboardStorage {

    private static final int CAP = 200;

    private final String file;

    public EchoLeaderboardStorage() {
        this(EchoPlayModePaths.leaderboardFile());
    }

    EchoLeaderboardStorage(String file) {
        this.file = file;
    }

    public void append(EchoFightResult rec) {
        List<EchoFightResult> all = readAll();
        all.add(rec);
        if (all.size() > CAP) {
            Collections.sort(all, Comparator.comparingLong(r -> r.timestamp));
            all = new ArrayList<>(all.subList(all.size() - CAP, all.size()));
        }
        writeAll(all);
    }

    public List<EchoFightResult> loadTop(int limit) {
        List<EchoFightResult> all = readAll();
        all.sort((a, b) -> {
            if (a.bossWin != b.bossWin)
                return a.bossWin ? -1 : 1;
            int cmp = Integer.compare(b.damageDealt, a.damageDealt);
            if (cmp != 0)
                return cmp;
            return Long.compare(b.timestamp, a.timestamp);
        });
        if (all.isEmpty())
            return all;
        return all.subList(0, Math.min(limit, all.size()));
    }

    private List<EchoFightResult> readAll() {
        try {
            FileHandle handle = FileUtils.getFileHandle(file);
            if (handle == null || !handle.exists() || handle.isDirectory() || handle.length() == 0) {
                return new ArrayList<>();
            }
            String content = handle.readString("UTF-8").trim();
            if (content.isEmpty())
                return new ArrayList<>();
            List<EchoFightResult> list = new ArrayList<>();
            String[] rows = content.split("\n");
            for (String r : rows) {
                String[] parts = r.split(",", -1);
                // Full format only; legacy 6-column rows omitted player_class and are skipped.
                if (parts.length < 9 || parts[5].isEmpty()) {
                    continue;
                }
                list.add(new EchoFightResult(
                        emptyToNull(parts[0]),
                        Boolean.parseBoolean(parts[1]),
                        Integer.parseInt(parts[2]),
                        Long.parseLong(parts[3]),
                        parts[4],
                        parts[5],
                        Integer.parseInt(parts[6]),
                        Integer.parseInt(parts[7]),
                        Integer.parseInt(parts[8])));
            }
            return list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private void writeAll(List<EchoFightResult> list) {
        try {
            StringBuilder sb = new StringBuilder();
            for (EchoFightResult r : list) {
                if (r.playerClass == null || r.playerClass.isEmpty()) {
                    continue;
                }
                sb.append(r.echoId != null ? r.echoId : "").append(',')
                        .append(r.bossWin).append(',')
                        .append(r.depth).append(',')
                        .append(r.timestamp).append(',')
                        .append(r.gameVersion != null ? r.gameVersion : "").append(',')
                        .append(r.playerClass).append(',')
                        .append(r.damageDealt).append(',')
                        .append(r.damageTaken).append(',')
                        .append(r.turns).append('\n');
            }
            FileUtils.getFileHandle(file).writeBytes(sb.toString().getBytes(StandardCharsets.UTF_8), false);
        } catch (Exception ignored) {
        }
    }
}
