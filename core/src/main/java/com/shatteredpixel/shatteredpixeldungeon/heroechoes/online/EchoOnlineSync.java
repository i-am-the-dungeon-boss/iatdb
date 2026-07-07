package com.shatteredpixel.shatteredpixeldungeon.heroechoes.online;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoFightResult;
import com.watabou.noosa.Game;

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
		if (!shouldSync() || echo == null) {
			return;
		}
		submit(() -> {
			try {
				client.uploadEcho(echo);
			} catch (Exception e) {
				Game.reportException(e);
			}
		});
	}

	public void postLeaderboardResultAsync(EchoFightResult result) {
		if (!shouldSync() || result == null) {
			return;
		}
		submit(() -> {
			try {
				client.postLeaderboardResult(result);
			} catch (Exception e) {
				Game.reportException(e);
			}
		});
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

	void awaitBackgroundTasksForTests() throws InterruptedException {
		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.SECONDS);
	}
}
