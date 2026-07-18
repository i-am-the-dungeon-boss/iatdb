package com.shatteredpixel.shatteredpixeldungeon.heroechoes;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoFetchResult;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoLookupOutcome;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.levels.EchoReplacementDecider;
import com.watabou.utils.Bundle;

import com.watabou.noosa.Game;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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

    public static final int MAX_ECHOES_PER_DEPTH = 1;
    public static final String POLICY_BUNDLE_KEY = "echo_policy";
    private static final Pattern DEPTH_FILE = Pattern.compile("^depth-(\\d+)(?:-\\d+)?\\.dat$");

    public static File getEchoesDir() {
        File dir = new File(EchoPlayModePaths.echoesDir());
        if (!dir.exists()) {
            // noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
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
            getEchoesDir();
            if (echo.timestamp <= 0) {
                echo.timestamp = System.currentTimeMillis();
            }
            if (echo.echoId == null) {
                echo.echoId = echo.depth + "-" + echo.timestamp;
            }

            clearDepth(echo.depth);
            Bundle fileBundle = echo.toFileBundle();
            fileBundle.put(POLICY_BUNDLE_KEY, policy.toBundle());
            writeBundle(canonicalPath(echo.depth), fileBundle);
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

    /** Newest echo per boss depth, sorted newest first. */
    public List<EchoEntry> loadAll() {
        Map<Integer, EchoEntry> newestPerDepth = new HashMap<>();
        File dir = getEchoesDir();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".dat") && !name.startsWith("latest-"));
        if (files == null)
            return new ArrayList<>();

        for (File file : files) {
            int depth = parseDepth(file.getName());
            if (depth < 0)
                continue;
            try {
                Echo loaded = loadEchoFromFile(file);
                if (loaded == null)
                    continue;
                EchoEntry entry = new EchoEntry(file, loaded);
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
        File dir = getEchoesDir();
        File[] files = dir.listFiles((d, name) -> belongsToDepth(name, entry.echo.depth));
        if (files == null || files.length == 0) {
            return false;
        }
        boolean deleted = false;
        for (File file : files) {
            deleted |= file.delete();
        }
        return deleted;
    }

    private static String canonicalPath(int depth) {
        return String.format(Locale.ROOT, "%s/depth-%d.dat", EchoPlayModePaths.echoesDir(), depth);
    }

    private static void clearDepth(int depth) {
        File dir = getEchoesDir();
        File[] files = dir.listFiles((d, name) -> belongsToDepth(name, depth));
        if (files == null)
            return;
        for (File file : files) {
            // noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    private static boolean belongsToDepth(String name, int depth) {
        return name.equals("depth-" + depth + ".dat")
                || name.startsWith("depth-" + depth + "-")
                || name.equals("latest-depth-" + depth + ".dat");
    }

    private Optional<EchoFetchResult> loadResultForDepth(int depth, String currentGameVersion) {
        File canonical = new File(canonicalPath(depth));
        if (canonical.exists()) {
            try {
                return readResult(canonical, depth, currentGameVersion);
            } catch (Exception ignored) {
            }
            return Optional.empty();
        }

        File dir = getEchoesDir();
        File[] files = dir.listFiles((d, name) -> name.startsWith("depth-" + depth + "-") && name.endsWith(".dat"));
        if (files == null)
            return Optional.empty();

        EchoFetchResult newest = null;
        long newestTime = Long.MIN_VALUE;
        for (File file : files) {
            try {
                Optional<EchoFetchResult> loaded = readResult(file, depth, currentGameVersion);
                if (loaded.isEmpty())
                    continue;
                EchoFetchResult result = loaded.get();
                long sortTime = result.echo.timestamp > 0 ? result.echo.timestamp : file.lastModified();
                if (sortTime >= newestTime) {
                    newest = result;
                    newestTime = sortTime;
                }
            } catch (Exception ignored) {
            }
        }
        return Optional.ofNullable(newest);
    }

    private static Optional<EchoFetchResult> readResult(File file, int depth, String currentGameVersion)
            throws IOException {
        Bundle fileBundle = readBundle(file.getPath());
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

    private static Echo loadEchoFromFile(File file) throws IOException {
        Bundle fileBundle = readBundle(file.getPath());
        if (!fileBundle.contains(Echo.BUNDLE_KEY)) {
            return null;
        }
        return Echo.fromFileBundle(fileBundle);
    }

    private static void writeBundle(String path, Bundle bundle) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(path)) {
            Bundle.write(bundle, fos);
        }
    }

    private static Bundle readBundle(String path) throws IOException {
        try (FileInputStream fis = new FileInputStream(path)) {
            return Bundle.read(fis);
        }
    }
}
