/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * I am the Dungeon Boss
 * Copyright (C) 2026 Dungeon Boss
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
import com.shatteredpixel.shatteredpixeldungeon.DebugSettings;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.debug.DebugArenaItems;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.debug.DebugClericSpells;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.debug.DebugEchoArsenal;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.debug.EchoSnapshotDebug;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoStorage;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.SentryCrashReporting;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoOnlineSync;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.IntegrityReport;
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
import com.shatteredpixel.shatteredpixeldungeon.utils.SaveIntegrity;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;

public class WndGame extends Window {

	private static final int WIDTH = 120;
	private static final int BTN_HEIGHT = 20;
	private static final int GAP = 2;

	private static final int REPORT_MAX_LENGTH = 500;

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

		// Report an error — available in a run and in release builds.
		addButton(curBtn = new RedButton(Messages.get(this, "report_error")) {
			@Override
			protected void onClick() {
				hide();
				GameScene.show(new WndTextInput(
						Messages.get(WndGame.class, "report_error_title"),
						Messages.get(WndGame.class, "report_error_body"),
						"",
						REPORT_MAX_LENGTH,
						true,
						Messages.get(WndGame.class, "report_error_send"),
						Messages.get(WndGame.class, "report_error_cancel")) {
					@Override
					public void onSelect(boolean positive, String text) {
						if (positive) {
							SentryCrashReporting.reportUserMessage(text);
							GLog.p(Messages.get(WndGame.class, "report_error_sent"));
						}
					}
				});
			}
		});
		curBtn.icon(Icons.get(Icons.WARNING));

		if (showsEchoDebugTools()) {
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

			addButton(curBtn = new RedButton(markSaveStateLabel()) {
				@Override
				protected void onClick() {
					boolean marked = markSaveState();
					hide();
					if (marked) {
						GLog.p(Messages.get(WndGame.class, "save_state_marked"));
					} else {
						GLog.w(Messages.get(WndGame.class, "save_state_mark_failed"));
					}
				}
			});
			curBtn.icon(Icons.get(Icons.WARNING));
			if (SaveIntegrity.isModified()) {
				curBtn.textColor(Window.TITLE_COLOR);
			}

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

			addButton(curBtn = new RedButton(Messages.get(this, "stop_echo_hunting")) {
				@Override
				protected void onClick() {
					int stopped = EchoBoss.stopAllHunting();
					hide();
					if (stopped > 0) {
						GLog.p(Messages.get(WndGame.class, "echo_hunting_stopped"));
					} else {
						GLog.w(Messages.get(WndGame.class, "echo_hunting_none"));
					}
				}
			});
			curBtn.icon(Icons.get(Icons.SKULL));

			addButton(curBtn = new RedButton(Messages.get(this, "restock_ground_items")) {
				@Override
				protected void onClick() {
					int dropped = DebugArenaItems.restockGround();
					hide();
					if (dropped > 0) {
						GLog.p(Messages.get(WndGame.class, "ground_items_restocked", dropped));
					} else {
						GLog.w(Messages.get(WndGame.class, "ground_items_restock_failed"));
					}
				}
			});
			curBtn.icon(Icons.get(Icons.MAGNIFY));

			addButton(curBtn = new RedButton(Messages.get(this, "give_echo_arsenal")) {
				@Override
				protected void onClick() {
					int updated = DebugEchoArsenal.grantAndCycleAll();
					hide();
					if (updated > 0) {
						GLog.p(Messages.get(WndGame.class, "echo_arsenal_granted", updated));
					} else {
						GLog.w(Messages.get(WndGame.class, "echo_arsenal_none"));
					}
				}
			});
			curBtn.icon(Icons.get(Icons.TALENT));

			addButton(curBtn = new RedButton(Messages.get(this, "give_echo_tome_spells")) {
				@Override
				protected void onClick() {
					HeroSubClass used = DebugClericSpells.pendingSubClass();
					int updated = DebugClericSpells.grantTomeSpellsAll();
					hide();
					if (updated > 0) {
						GLog.p(Messages.get(WndGame.class, "echo_tome_spells_granted", used.title(), updated));
					} else {
						GLog.w(Messages.get(WndGame.class, "echo_tome_spells_none"));
					}
				}
			});
			curBtn.icon(Icons.get(Icons.TALENT));

			addButton(curBtn = new RedButton(Messages.get(this, "give_echo_armor_ability")) {
				@Override
				protected void onClick() {
					int updated = DebugEchoArsenal.grantArmorAbilityAll();
					hide();
					if (updated > 0 || (Dungeon.hero != null && Dungeon.hero.isAlive()
							&& Dungeon.hero.belongings.armor instanceof com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor)) {
						GLog.p(Messages.get(WndGame.class, "echo_armor_ability_granted", updated));
					} else {
						GLog.w(Messages.get(WndGame.class, "echo_armor_ability_none"));
					}
				}
			});
			curBtn.icon(Icons.get(Icons.TALENT));
		}

		addButton(curBtn = new RedButton(Messages.get(this, "menu")) {
			@Override
			protected void onClick() {
				try {
					Dungeon.saveAll();
				} catch (IOException e) {
					ShatteredPixelDungeon.reportException(e);
				}
				Dungeon.echoPlayMode = EchoPlayMode.sanitize(GamesInProgress.selectedEchoPlayMode);
				Game.switchScene(TitleScene.class);
			}
		});
		curBtn.icon(Icons.get(Icons.DISPLAY));
		if (SPDSettings.intro())
			curBtn.enable(false);

		resize(WIDTH, pos);
	}

	/**
	 * Button text for the debug save-marking action, doubling as the only place in the
	 * game that tells you whether the current run is marked. Package-visible for tests.
	 */
	static String markSaveStateLabel() {
		return Messages.get(WndGame.class,
				SaveIntegrity.isModified() ? "save_state_is_marked" : "mark_save_state");
	}

	/**
	 * Puts the current run into the state a modified save would leave it in, and writes
	 * that state to disk so it survives a reload. Debug builds only; package-visible for
	 * tests. Returns false when there is no run, so the caller can say so rather than
	 * claim a success that did not happen.
	 */
	static boolean markSaveState() {
		if (!Dungeon.markSaveModified(IntegrityReport.REASON_DEBUG)) {
			return false;
		}
		try {
			Dungeon.saveAll();
		} catch (IOException e) {
			ShatteredPixelDungeon.reportException(e);
			return false;
		}
		return true;
	}

	/** Save echo / view echoes / leaderboard — debug builds only. */
	public static boolean showsEchoDebugTools() {
		return DebugSettings.isDebugBuild();
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
