package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.DebugSettings;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoFightResult;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoLeaderboardEntry;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoLeaderboardStorage;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayModePaths;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoClient;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoOnlineSettings;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollingListPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Game;

import java.util.ArrayList;
import java.util.List;

public class WndLeaderboard extends Window {

	private static final int WIDTH = 130;
	private static final int HEIGHT = 140;

	private final OnlineFetchLifecycle onlineFetch = new OnlineFetchLifecycle();

	public static void show() {
		if (!DebugSettings.isDebugBuild()) {
			return;
		}
		GameScene.show(new WndLeaderboard(Dungeon.depth));
	}

	private WndLeaderboard(int depth) {
		super();

		ScrollingListPane list = new ScrollingListPane();
		add(list);

		list.addTitle(Messages.get(this, "title", depth));

		List<EchoLeaderboardEntry> entries = loadLocalEntries(depth);
		populate(list, entries);

		if (EchoOnlineSettings.canSyncOnline()) {
			new Thread(() -> {
				try {
					List<EchoLeaderboardEntry> online = EchoClient.createDefault()
							.fetchLeaderboard(depth, 25);
					if (!online.isEmpty() && onlineFetch.isActive()) {
						Game.runOnRenderThread(
								() -> applyOnlineLeaderboard(onlineFetch, list, depth, online, WndLeaderboard.this));
					}
				} catch (Exception ignored) {
				}
			}, "echo-leaderboard-fetch").start();
		}

		resize(WIDTH, HEIGHT);
		list.setRect(0, 0, width, height);
	}

	@Override
	public void destroy() {
		onlineFetch.cancel();
		super.destroy();
	}

	static void applyOnlineLeaderboard(
			OnlineFetchLifecycle lifecycle,
			ScrollingListPane list,
			int depth,
			List<EchoLeaderboardEntry> online,
			WndLeaderboard window) {
		if (!lifecycle.isActive()) {
			return;
		}
		list.clear();
		list.addTitle(Messages.get(window, "title_online", depth));
		window.populate(list, online);
	}

	static final class OnlineFetchLifecycle {
		private volatile boolean active = true;

		void cancel() {
			active = false;
		}

		boolean isActive() {
			return active;
		}
	}

	private static List<EchoLeaderboardEntry> loadLocalEntries(int depth) {
		if (!EchoPlayModePaths.persistsLeaderboardLocally()) {
			return new ArrayList<>();
		}
		List<EchoLeaderboardEntry> entries = new ArrayList<>();
		List<EchoFightResult> local = new EchoLeaderboardStorage().loadTop(25);
		int rank = 1;
		for (EchoFightResult result : local) {
			if (result.depth == depth) {
				entries.add(EchoLeaderboardEntry.fromFightResult(result, rank++));
			}
		}
		return entries;
	}

	private void populate(ScrollingListPane list, List<EchoLeaderboardEntry> entries) {
		if (entries.isEmpty()) {
			list.addItem(new ScrollingListPane.ListItem(null, null, Messages.get(this, "empty")));
			return;
		}
		for (EchoLeaderboardEntry entry : entries) {
			String label = Messages.get(
					this,
					"entry",
					entry.rank,
					entry.playerClass,
					entry.bossWin ? Messages.get(this, "win") : Messages.get(this, "loss"),
					entry.damageDealt,
					entry.turns);
			list.addItem(new ScrollingListPane.ListItem(null, null, label));
		}
	}
}
