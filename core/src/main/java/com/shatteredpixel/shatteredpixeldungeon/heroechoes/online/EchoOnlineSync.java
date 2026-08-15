package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss.EchoFightResult;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.SentryCrashReporting;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class EchoOnlineSync {

	private static EchoOnlineSync defaultInstance;

	private final EchoClient client;
	private final ExecutorService executor;
	private final List<Runnable> testTasks = new ArrayList<>();

	public EchoOnlineSync(EchoClient client) {
		this(client, Executors.newSingleThreadExecutor(r -> {
			Thread thread = new Thread(r, "echo-online-sync");
			thread.setDaemon(true);
			return thread;
		}));
	}

	EchoOnlineSync(EchoClient client, ExecutorService executor) {
		this.client = client;
		this.executor = executor;
	}

	public static EchoOnlineSync instance() {
		if (defaultInstance == null) {
			defaultInstance = new EchoOnlineSync(EchoClient.createDefault());
		}
		return defaultInstance;
	}

	public static void setDefaultForTests(EchoOnlineSync sync) {
		defaultInstance = sync;
	}

	public void uploadEchoAsync(Echo echo) {
		if (!shouldSync() || echo == null || Dungeon.currentRunModified()) {
			return;
		}
		submit(() -> {
			try {
				if (!ensurePlayerSession()) {
					return;
				}
				// Display name is set server-side from the player JWT.
				client.uploadEcho(echo);
			} catch (Exception e) {
				SentryCrashReporting.report(e);
			}
		});
	}

	public void postLeaderboardResultAsync(EchoFightResult result) {
		if (!shouldSync() || result == null || Dungeon.currentRunModified()) {
			return;
		}
		submit(() -> {
			try {
				if (!ensurePlayerSession()) {
					return;
				}
				client.postLeaderboardResult(result);
			} catch (Exception e) {
				SentryCrashReporting.report(e);
			}
		});
	}

	/**
	 * Delivers anything the queue is still holding. Unlike the upload paths this runs
	 * for modified saves — it is the only thing a modified run ever sends — and stays
	 * silent on every failure so a detected player sees nothing.
	 */
	public void flushIntegrityReportsAsync() {
		// deliberately not gated on shouldSync(): that requires RANKED, and a modified
		// save is worth knowing about whichever mode it was loaded in. A configured
		// backend and an existing session are enough.
		if (!EchoOnlineSettings.isConfigured() || IntegrityReportQueue.pending().isEmpty()) {
			return;
		}
		submit(() -> {
			if (!ensurePlayerSession()) {
				return;
			}
			for (IntegrityReport report : IntegrityReportQueue.pending()) {
				try {
					client.postIntegrityReport(report);
					IntegrityReportQueue.acknowledge(report.reportId);
				} catch (Exception ignored) {
					// keep it queued and try again next time; never reported anywhere
					return;
				}
			}
		});
	}

	private boolean ensurePlayerSession() {
		// Never create accounts from background sync — that recreates deleted players
		// from SPDSettings.playerName. Auth gate must establish the session first.
		return EchoPlayerSession.hasSession();
	}

	private boolean shouldSync() {
		return EchoOnlineSettings.canSyncOnline();
	}

	private void submit(Runnable task) {
		if (Thread.currentThread().getName().equals("echo-online-sync")) {
			task.run();
			return;
		}
		executor.execute(task);
	}

	public void awaitBackgroundTasksForTests() throws InterruptedException {
		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.SECONDS);
	}
}
