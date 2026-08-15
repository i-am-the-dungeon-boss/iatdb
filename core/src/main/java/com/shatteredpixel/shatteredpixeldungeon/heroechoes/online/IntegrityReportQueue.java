package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Holds detections until the backend acknowledges them, so a player who is offline
 * (or who stays offline) is still reported once they connect.
 * <p>
 * Every failure here is swallowed. Nothing in this class may log, report an
 * exception, or surface a window: the whole mechanism depends on the player seeing
 * an ordinary game.
 */
public final class IntegrityReportQueue {

	static final String QUEUE_FILE = "echoes-online/integrity-reports.dat";

	private static final String REPORTS = "reports";

	// a client stuck offline should not grow this without bound
	private static final int MAX_ENTRIES = 20;

	private IntegrityReportQueue() {
	}

	/** Captures the run currently in memory. Safe to call before a hero exists. */
	public static void recordCurrentRun(String reason) {
		IntegrityReport report = new IntegrityReport();
		report.reportId = UUID.randomUUID().toString();
		report.detectedAt = System.currentTimeMillis();
		report.reason = reason;
		report.heroClass = Dungeon.hero != null && Dungeon.hero.heroClass != null
				? Dungeon.hero.heroClass.name()
				: "";
		report.heroLevel = Dungeon.hero != null ? Dungeon.hero.lvl : 0;
		report.depth = Dungeon.depth;
		report.seed = Dungeon.seed;
		report.gameVersion = Game.version != null ? Game.version : "";
		report.playMode = Dungeon.echoPlayMode != null ? Dungeon.echoPlayMode.name() : "";
		report.easyMode = Dungeon.easyMode;
		report.saveSlot = GamesInProgress.curSlot;

		record(report);
	}

	public static synchronized void record(IntegrityReport report) {
		if (report == null) {
			return;
		}
		List<IntegrityReport> pending = pending();
		pending.add(report);
		while (pending.size() > MAX_ENTRIES) {
			pending.remove(0);
		}
		persist(pending);
	}

	public static synchronized List<IntegrityReport> pending() {
		List<IntegrityReport> reports = new ArrayList<>();
		if (!FileUtils.fileExists(QUEUE_FILE)) {
			return reports;
		}
		try {
			Bundle bundle = FileUtils.bundleFromFile(QUEUE_FILE);
			for (Bundlable entry : bundle.getCollection(REPORTS)) {
				if (entry instanceof IntegrityReport) {
					reports.add((IntegrityReport) entry);
				}
			}
		} catch (IOException ignored) {
			// an unreadable queue is not worth surfacing; it will be rewritten on the next record
		}
		return reports;
	}

	/** Drops one acknowledged entry. Unknown ids are ignored. */
	public static synchronized void acknowledge(String reportId) {
		if (reportId == null || reportId.isEmpty()) {
			return;
		}
		List<IntegrityReport> pending = pending();
		boolean removed = false;
		for (int i = pending.size() - 1; i >= 0; i--) {
			if (reportId.equals(pending.get(i).reportId)) {
				pending.remove(i);
				removed = true;
			}
		}
		if (removed) {
			persist(pending);
		}
	}

	public static synchronized void clear() {
		FileUtils.deleteFile(QUEUE_FILE);
	}

	private static void persist(List<IntegrityReport> reports) {
		try {
			if (reports.isEmpty()) {
				FileUtils.deleteFile(QUEUE_FILE);
				return;
			}
			Bundle bundle = new Bundle();
			bundle.put(REPORTS, reports);
			FileUtils.bundleToFile(QUEUE_FILE, bundle);
		} catch (IOException ignored) {
			// nothing to do: the detection is already reflected in the save's own checksum
		}
	}
}
