package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.badlogic.gdx.files.FileHandle;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoFetchResult;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoLookupOutcome;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.levels.EchoReplacementDecider;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EchoStorage implements EchoReplacementDecider.EchoLookup {

    /** One persisted echo per boss depth within the active play mode. */
    public static final int MAX_ECHOES_PER_DEPTH = 1;
    public static final String POLICY_BUNDLE_KEY = "echo_policy";
    private static final Pattern DEPTH_FILE = Pattern.compile("^depth-(\\d+)(?:-\\d+)?\\.dat$");

    public static File getEchoesDir() {
        ensureEchoesDir();
        return FileUtils.getFileHandle(EchoPlayModePaths.echoesDir()).file();
    }

    public void save(Echo echo) {
        save(echo, EchoPolicy.fallback());
    }

    public void save(Echo echo, EchoPolicy policy) {
        if (echo == null)
            return;
        if (policy == null) {
            throw new IllegalArgumentException("echo_policy is required");
        }
        try {
            ensureEchoesDir();
            if (echo.timestamp <= 0) {
                echo.timestamp = System.currentTimeMillis();
            }
            if (echo.echoId == null) {
                echo.echoId = echo.depth + "-" + echo.timestamp;
            }

            clearDepth(echo.depth);
            Bundle fileBundle = echo.toFileBundle();
            fileBundle.put(POLICY_BUNDLE_KEY, policy.toBundle());
            FileUtils.bundleToFile(canonicalPath(echo.depth), fileBundle);
        } catch (IOException ignored) {
        }
    }

    public Optional<Echo> loadForDepth(int depth, String currentGameVersion) {
        return loadResultForDepth(depth, currentGameVersion).map(result -> result.echo);
    }

    @Override
    public EchoLookupOutcome findEchoForDepth(int depth) {
        return loadResultForDepth(depth, Game.version)
                .map(EchoLookupOutcome::found)
                .orElseGet(EchoLookupOutcome::notFound);
    }

    public static final class EchoEntry {
        public final File file;
        public final Echo echo;

        public EchoEntry(File file, Echo echo) {
            this.file = file;
            this.echo = echo;
        }

        public String filename() {
            return file.getName();
        }

        public long sortTime() {
            if (echo.timestamp > 0)
                return echo.timestamp;
            return file.lastModified();
        }
    }

    /** Newest echo per boss depth for the active play mode, sorted newest first. */
    public List<EchoEntry> loadAll() {
        Map<Integer, EchoEntry> newestPerDepth = new HashMap<>();
        ensureEchoesDir();
        String dirName = EchoPlayModePaths.echoesDir();
        for (String name : FileUtils.filesInDir(dirName)) {
            if (!name.endsWith(".dat") || name.startsWith("latest-")) {
                continue;
            }
            int depth = parseDepth(name);
            if (depth < 0) {
                continue;
            }
            try {
                String path = dirName + "/" + name;
                Echo loaded = loadEchoFromPath(path);
                if (loaded == null) {
                    continue;
                }
                EchoEntry entry = new EchoEntry(FileUtils.getFileHandle(path).file(), loaded);
                EchoEntry existing = newestPerDepth.get(depth);
                if (existing == null || entry.sortTime() > existing.sortTime()) {
                    newestPerDepth.put(depth, entry);
                }
            } catch (Exception ignored) {
            }
        }

        List<EchoEntry> entries = new ArrayList<>(newestPerDepth.values());
        entries.sort((a, b) -> Long.compare(b.sortTime(), a.sortTime()));
        return entries;
    }

    /** Deletes the echo file(s) for the entry's boss depth. */
    public boolean deleteEntry(EchoEntry entry) {
        if (entry == null || entry.echo == null) {
            return false;
        }
        ensureEchoesDir();
        String dirName = EchoPlayModePaths.echoesDir();
        boolean deleted = false;
        for (String name : FileUtils.filesInDir(dirName)) {
            if (belongsToDepth(name, entry.echo.depth)) {
                deleted |= FileUtils.deleteFile(dirName + "/" + name);
            }
        }
        return deleted;
    }

    private static void ensureEchoesDir() {
        String dirName = EchoPlayModePaths.echoesDir();
        if (!FileUtils.dirExists(dirName)) {
            FileHandle dir = FileUtils.getFileHandle(dirName);
            if (dir != null) {
                dir.mkdirs();
            }
        }
    }

    private static String canonicalPath(int depth) {
        return String.format(Locale.ROOT, "%s/depth-%d.dat", EchoPlayModePaths.echoesDir(), depth);
    }

    private static void clearDepth(int depth) {
        String dirName = EchoPlayModePaths.echoesDir();
        if (!FileUtils.dirExists(dirName)) {
            return;
        }
        for (String name : FileUtils.filesInDir(dirName)) {
            if (belongsToDepth(name, depth)) {
                FileUtils.deleteFile(dirName + "/" + name);
            }
        }
    }

    private static boolean belongsToDepth(String name, int depth) {
        return name.equals("depth-" + depth + ".dat")
                || name.startsWith("depth-" + depth + "-")
                || name.equals("latest-depth-" + depth + ".dat");
    }

    private Optional<EchoFetchResult> loadResultForDepth(int depth, String currentGameVersion) {
        String canonical = canonicalPath(depth);
        if (FileUtils.fileExists(canonical)) {
            try {
                return readResult(canonical, depth, currentGameVersion);
            } catch (Exception ignored) {
            }
            return Optional.empty();
        }

        String dirName = EchoPlayModePaths.echoesDir();
        if (!FileUtils.dirExists(dirName)) {
            return Optional.empty();
        }

        EchoFetchResult newest = null;
        long newestTime = Long.MIN_VALUE;
        for (String name : FileUtils.filesInDir(dirName)) {
            if (!name.startsWith("depth-" + depth + "-") || !name.endsWith(".dat")) {
                continue;
            }
            try {
                String path = dirName + "/" + name;
                Optional<EchoFetchResult> loaded = readResult(path, depth, currentGameVersion);
                if (loaded.isEmpty()) {
                    continue;
                }
                EchoFetchResult result = loaded.get();
                long sortTime = result.echo.timestamp > 0
                        ? result.echo.timestamp
                        : FileUtils.getFileHandle(path).lastModified();
                if (sortTime >= newestTime) {
                    newest = result;
                    newestTime = sortTime;
                }
            } catch (Exception ignored) {
            }
        }
        return Optional.ofNullable(newest);
    }

    private static Optional<EchoFetchResult> readResult(String path, int depth, String currentGameVersion)
            throws IOException {
        Bundle fileBundle = FileUtils.bundleFromFile(path);
        if (!fileBundle.contains(Echo.BUNDLE_KEY) || !fileBundle.contains(POLICY_BUNDLE_KEY)) {
            return Optional.empty();
        }
        Echo loaded = Echo.fromFileBundle(fileBundle);
        if (loaded.depth != depth || !loaded.isCompatibleWith(currentGameVersion)) {
            return Optional.empty();
        }
        EchoPolicy policy = EchoPolicy.fromBundle(fileBundle.getBundle(POLICY_BUNDLE_KEY));
        return Optional.of(new EchoFetchResult(loaded, policy));
    }

    private static int parseDepth(String filename) {
        Matcher matcher = DEPTH_FILE.matcher(filename);
        if (!matcher.matches())
            return -1;
        return Integer.parseInt(matcher.group(1));
    }

    private static Echo loadEchoFromPath(String path) throws IOException {
        Bundle fileBundle = FileUtils.bundleFromFile(path);
        if (!fileBundle.contains(Echo.BUNDLE_KEY)) {
            return null;
        }
        return Echo.fromFileBundle(fileBundle);
    }
}
