package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.watabou.utils.Bundle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
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

    public void append(EchoFightResult rec){
        List<EchoFightResult> all = readAll();
        all.add(rec);
        if (all.size() > CAP){
            Collections.sort(all, Comparator.comparingLong(r -> r.timestamp));
            all = new ArrayList<>(all.subList(all.size()-CAP, all.size()));
        }
        writeAll(all);
    }

    public List<EchoFightResult> loadTop(int limit){
        List<EchoFightResult> all = readAll();
        all.sort((a,b) -> {
            if (a.bossWin != b.bossWin) return a.bossWin ? -1 : 1;
            int cmp = Integer.compare(b.damageDealt, a.damageDealt);
            if (cmp != 0) return cmp;
            return Long.compare(b.timestamp, a.timestamp);
        });
        if (all.isEmpty()) return all;
        return all.subList(0, Math.min(limit, all.size()));
    }

    private List<EchoFightResult> readAll(){
        try {
            File f = new File(file);
            if (!f.exists()) return new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))){
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append('\n');
                String content = sb.toString().trim();
                if (content.isEmpty()) return new ArrayList<>();
                List<EchoFightResult> list = new ArrayList<>();
                String[] rows = content.split("\n");
                for (String r : rows){
                    String[] parts = r.split(",", -1);
                    if (parts.length < 6) continue;
                    if (parts.length >= 9) {
                        list.add(new EchoFightResult(
                                emptyToNull(parts[0]),
                                Boolean.parseBoolean(parts[1]),
                                Integer.parseInt(parts[2]),
                                Long.parseLong(parts[3]),
                                parts[4],
                                parts[5],
                                Integer.parseInt(parts[6]),
                                Integer.parseInt(parts[7]),
                                Integer.parseInt(parts[8])
                        ));
                    } else {
                        list.add(new EchoFightResult(
                                Boolean.parseBoolean(parts[0]),
                                Integer.parseInt(parts[1]),
                                Long.parseLong(parts[2]),
                                Integer.parseInt(parts[3]),
                                Integer.parseInt(parts[4]),
                                Integer.parseInt(parts[5])
                        ));
                    }
                }
                return list;
            }
        } catch (Exception e){
            return new ArrayList<>();
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private void writeAll(List<EchoFightResult> list){
        try {
            File f = new File(file);
            try (FileOutputStream fos = new FileOutputStream(f, false)){
                StringBuilder sb = new StringBuilder();
                for (EchoFightResult r : list){
                    sb.append(r.echoId != null ? r.echoId : "").append(',')
                      .append(r.bossWin).append(',')
                      .append(r.depth).append(',')
                      .append(r.timestamp).append(',')
                      .append(r.gameVersion).append(',')
                      .append(r.playerClass != null ? r.playerClass : "UNKNOWN").append(',')
                      .append(r.damageDealt).append(',')
                      .append(r.damageTaken).append(',')
                      .append(r.turns).append('\n');
                }
                fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored){
        }
    }
}
