/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * I am the Dungeon Boss
 * Copyright (C) 2014-2026 Marwan Elzainy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.windows;

import java.io.IOException;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoSnapshotDebug;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoStorage;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoOnlineSync;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.HeroSelectScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.RankingsScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;

public class WndGame extends Window {

	private static final int WIDTH = 120;
	private static final int BTN_HEIGHT = 20;
	private static final int GAP = 2;

	private int pos;

	public WndGame() {

		super();

		// settings
		RedButton curBtn;
		addButton(curBtn = new RedButton(Messages.get(this, "settings")) {
			@Override
			protected void onClick() {
				hide();
				GameScene.show(new WndSettings());
			}
		});
		curBtn.icon(Icons.get(Icons.PREFS));

		// Challenges window
		if (Dungeon.challenges > 0) {
			addButton(curBtn = new RedButton(Messages.get(this, "challenges")) {
				@Override
				protected void onClick() {
					hide();
					GameScene.show(new WndChallenges(Dungeon.challenges, false));
				}
			});
			curBtn.icon(Icons.get(Icons.CHALLENGE_COLOR));
		}

		// Restart
		if (Dungeon.hero == null || !Dungeon.hero.isAlive()) {

			addButton(curBtn = new RedButton(Messages.get(this, "start")) {
				@Override
				protected void onClick() {
					GamesInProgress.selectedClass = Dungeon.hero.heroClass;
					GamesInProgress.curSlot = GamesInProgress.firstEmpty();
					ShatteredPixelDungeon.switchScene(HeroSelectScene.class);
				}
			});
			curBtn.icon(Icons.get(Icons.ENTER));
			curBtn.textColor(Window.TITLE_COLOR);

			addButton(curBtn = new RedButton(Messages.get(this, "rankings")) {
				@Override
				protected void onClick() {
					InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
					Game.switchScene(RankingsScene.class);
				}
			});
			curBtn.icon(Icons.get(Icons.RANKINGS));
		}

		// Main menu
		RedButton saveEcho = new RedButton(Messages.get(this, "save_echo")) {
			@Override
			protected void onClick() {
				String timestamp = String.format("%d", System.currentTimeMillis());
				Echo echo = Echo.fromHero(Dungeon.hero, Dungeon.depth, Game.version, Dungeon.seed);
				EchoSnapshotDebug.applyIfEnabled(echo);
				echo.echoId = "manual-" + timestamp;
				if (Dungeon.echoPlayMode == EchoPlayMode.RANKED) {
					saveRankedEcho(echo);
				} else {
					saveLocalEcho(echo);
				}
				GLog.p(Messages.get(WndGame.class, "echo_saved"));
			}
		};
		addButton(saveEcho);
		saveEcho.icon(Icons.get(Icons.ENTER));

		addButton(curBtn = new RedButton(Messages.get(this, "view_echoes")) {
			@Override
			protected void onClick() {
				WndEchoes.show();
			}
		});
		curBtn.icon(Icons.get(Icons.ENTER));

		addButton(curBtn = new RedButton(Messages.get(this, "view_leaderboard")) {
			@Override
			protected void onClick() {
				WndLeaderboard.show();
			}
		});
		curBtn.icon(Icons.get(Icons.ENTER));

		addButton(curBtn = new RedButton(Messages.get(this, "menu")) {
			@Override
			protected void onClick() {
				try {
					Dungeon.saveAll();
				} catch (IOException e) {
					ShatteredPixelDungeon.reportException(e);
				}
				Dungeon.echoPlayMode = EchoPlayMode.NONE;
				Game.switchScene(TitleScene.class);
			}
		});
		curBtn.icon(Icons.get(Icons.DISPLAY));
		if (SPDSettings.intro())
			curBtn.enable(false);

		resize(WIDTH, pos);
	}

	/**
	 * Persists a manually captured echo for the active non-ranked play mode.
	 * Package-visible for tests.
	 */
	static void saveLocalEcho(Echo echo) {
		new EchoStorage().save(echo);
	}

	/**
	 * Uploads a manually captured echo for ranked mode (no local file).
	 * Package-visible for tests.
	 */
	static void saveRankedEcho(Echo echo) {
		EchoOnlineSync.instance().uploadEchoAsync(echo);
	}

	private void addButton(RedButton btn) {
		add(btn);
		btn.setRect(0, pos > 0 ? pos += GAP : 0, WIDTH, BTN_HEIGHT);
		pos += BTN_HEIGHT;
	}

	private void addButtons(RedButton btn1, RedButton btn2) {
		add(btn1);
		btn1.setRect(0, pos > 0 ? pos += GAP : 0, (WIDTH - GAP) / 2, BTN_HEIGHT);
		add(btn2);
		btn2.setRect(btn1.right() + GAP, btn1.top(), WIDTH - btn1.right() - GAP, BTN_HEIGHT);
		pos += BTN_HEIGHT;
	}
}
