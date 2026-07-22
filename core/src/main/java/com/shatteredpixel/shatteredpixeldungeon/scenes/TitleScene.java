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

package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.DebugSettings;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoBackendProbe;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPlayerAuthGate;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.effects.Fireball;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.services.news.News;
import com.shatteredpixel.shatteredpixeldungeon.services.updates.AvailableUpdateData;
import com.shatteredpixel.shatteredpixeldungeon.services.updates.Updates;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ExitButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.SupportPrompts;
import com.shatteredpixel.shatteredpixeldungeon.ui.TitleBackground;
import com.shatteredpixel.shatteredpixeldungeon.ui.TitleBrandBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.TitleFeedButtons;
import com.shatteredpixel.shatteredpixeldungeon.ui.TitleRankedIcon;
import com.shatteredpixel.shatteredpixeldungeon.ui.TitleSupportLayout;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndEchoConnectionFailed;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndSettings;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUpdateAvailable;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndVictoryCongrats;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.tweeners.Tweener;
import com.watabou.utils.ColorMath;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.GameMath;
import com.watabou.utils.RectF;

import java.util.Date;

public class TitleScene extends PixelScene {

	/** How far outside the brand block each side torch sits. */
	public static final float TORCH_OUTSET_LANDSCAPE = 16f;
	public static final float TORCH_OUTSET_PORTRAIT = 12f;
	/** Keeps tall landscape torches / floating “New Boss!” below the top edge. */
	public static final float BRAND_TOP_CLEARANCE_LANDSCAPE = 36f;
	public static final float BRAND_TOP_CLEARANCE_PORTRAIT = 24f;

	private TitleBrandBlock title;
	private Fireball leftFB;
	private Fireball rightFB;

	private StyledButton btnRanked;
	private StyledButton btnSolo;
	private StyledButton btnDebug;
	private StyledButton btnSupport;
	private StyledButton btnRankings;
	private StyledButton btnJournal;
	private StyledButton btnNews;
	private StyledButton btnChanges;
	private StyledButton btnSettings;
	private StyledButton btnAbout;

	private BitmapText version;
	private IconButton btnFade;
	private ExitButton btnExit;

	@Override
	public void create() {

		super.create();

		Music.INSTANCE.playTracks(
				new String[] { Assets.Music.THEME_1, Assets.Music.THEME_2 },
				new float[] { 1, 1 },
				false);

		uiCamera.visible = false;

		int w = Camera.main.width;
		int h = Camera.main.height;

		RectF insets = getCommonInsets();

		TitleBackground BG = new TitleBackground(w, h);
		add(BG);

		w -= insets.left + insets.right;
		h -= insets.top + insets.bottom;

		title = new TitleBrandBlock();
		add(title);
		title.setPos(0, 0); // measure preferred size via layout()

		float topRegion = Math.max(title.height() - 6, h * 0.45f);
		title.setPos(
				insets.left + (w - title.width()) / 2f,
				brandTitleY(insets.top, topRegion, title.height(), landscape()));
		align(title);

		if (landscape()) {
			leftFB = placeTorch(torchLeftX(title.left(), TORCH_OUTSET_LANDSCAPE), title.logoAnchorY());
			rightFB = placeTorch(torchRightX(title.right(), TORCH_OUTSET_LANDSCAPE), title.logoAnchorY());
		} else {
			leftFB = placeTorch(torchLeftX(title.left(), TORCH_OUTSET_PORTRAIT), title.logoAnchorY());
			rightFB = placeTorch(torchRightX(title.right(), TORCH_OUTSET_PORTRAIT), title.logoAnchorY());
		}

		final Chrome.Type GREY_TR = Chrome.Type.GREY_BUTTON_TR;

		btnRanked = new StyledButton(GREY_TR, Messages.get(this, "ranked")) {
			@Override
			protected void onClick() {
				beginEchoRun(EchoPlayMode.RANKED);
			}

			@Override
			protected boolean onLongClick() {
				if (DeviceCompat.isDebug()) {
					beginEchoRun(EchoPlayMode.RANKED);
					return true;
				}
				return super.onLongClick();
			}
		};
		btnRanked.icon(Icons.get(TitleRankedIcon.type()));
		add(btnRanked);

		btnSolo = new StyledButton(GREY_TR, Messages.get(this, "solo")) {
			@Override
			protected void onClick() {
				beginEchoRun(EchoPlayMode.SOLO);
			}

			@Override
			protected boolean onLongClick() {
				if (DeviceCompat.isDebug()) {
					beginEchoRun(EchoPlayMode.SOLO);
					return true;
				}
				return super.onLongClick();
			}
		};
		btnSolo.icon(Icons.get(Icons.ENTER));
		add(btnSolo);

		if (DebugSettings.isDebugBuild()) {
			btnDebug = new StyledButton(GREY_TR, Messages.get(this, "debug")) {
				@Override
				protected void onClick() {
					beginDebugRun();
				}
			};
			btnDebug.icon(Icons.get(Icons.MAGNIFY));
			add(btnDebug);
		}

		if (SupportPrompts.playBillingEnabled()) {
			btnSupport = new SupportButton(GREY_TR, Messages.get(this, "support"));
			add(btnSupport);
		}

		btnRankings = new StyledButton(GREY_TR, Messages.get(this, "rankings")) {
			@Override
			protected void onClick() {
				ShatteredPixelDungeon.switchNoFade(RankingsScene.class);
			}
		};
		btnRankings.icon(Icons.get(Icons.RANKINGS));
		add(btnRankings);
		Dungeon.daily = Dungeon.dailyReplay = false;

		btnJournal = new StyledButton(GREY_TR, Messages.get(this, "journal")) {
			@Override
			protected void onClick() {
				ShatteredPixelDungeon.switchNoFade(JournalScene.class);
			}
		};
		btnJournal.icon(Icons.get(Icons.JOURNAL));
		add(btnJournal);

		if (TitleFeedButtons.visible()) {
			btnNews = new NewsButton(GREY_TR, Messages.get(this, "news"));
			btnNews.icon(Icons.get(Icons.NEWS));
			add(btnNews);

			btnChanges = new ChangesButton(GREY_TR, Messages.get(this, "changes"));
			btnChanges.icon(Icons.get(Icons.CHANGES));
			add(btnChanges);
		}

		btnSettings = new SettingsButton(GREY_TR, Messages.get(this, "settings"));
		add(btnSettings);

		btnAbout = new StyledButton(GREY_TR, Messages.get(this, "about")) {
			@Override
			protected void onClick() {
				ShatteredPixelDungeon.switchScene(AboutScene.class);
			}
		};
		btnAbout.icon(Icons.get(Icons.IATDB));
		add(btnAbout);

		final int BTN_HEIGHT = 20;
		boolean feedVisible = TitleFeedButtons.visible();
		int buttonRows = TitleSupportLayout.buttonRows(landscape(), btnSupport != null, feedVisible);
		float reservedTop = menuRegionTop(insets.top, topRegion, title.bottom()) - insets.top;
		int GAP = (int) (h - reservedTop - buttonRows * BTN_HEIGHT) / 3;
		GAP /= landscape() ? 3 : 5;
		GAP = Math.max(GAP, 2);

		float buttonAreaWidth = landscape() ? PixelScene.MIN_WIDTH_L - 6 : PixelScene.MIN_WIDTH_P - 2;
		float btnAreaLeft = insets.left + (w - buttonAreaWidth) / 2f;
		if (landscape()) {
			layoutPlayModeButtons(btnAreaLeft, insets.top + reservedTop + GAP, buttonAreaWidth, BTN_HEIGHT);
			Float supportBottom = null;
			if (btnSupport != null) {
				btnSupport.setRect(btnSolo.left(), btnSolo.bottom() + GAP, buttonAreaWidth, BTN_HEIGHT);
				supportBottom = btnSupport.bottom();
			}
			float rankingsTop = TitleSupportLayout.rankingsY(btnSolo.bottom(), GAP, supportBottom);
			float midWidth = feedVisible
					? (float) (Math.floor(buttonAreaWidth / 3f) - 1)
					: (buttonAreaWidth / 2) - 1;
			btnRankings.setRect(btnSolo.left(), rankingsTop, midWidth, BTN_HEIGHT);
			btnJournal.setRect(btnRankings.right() + 2, btnRankings.top(), btnRankings.width(), BTN_HEIGHT);
			if (btnNews != null) {
				btnNews.setRect(btnJournal.right() + 2, btnJournal.top(), btnRankings.width(), BTN_HEIGHT);
			}
			btnSettings.setRect(btnRankings.left(), btnRankings.bottom() + GAP, btnRankings.width(), BTN_HEIGHT);
			if (btnChanges != null) {
				btnChanges.setRect(btnSettings.right() + 2, btnSettings.top(), btnRankings.width(), BTN_HEIGHT);
				btnAbout.setRect(btnChanges.right() + 2, btnSettings.top(), btnRankings.width(), BTN_HEIGHT);
			} else {
				btnAbout.setRect(btnSettings.right() + 2, btnSettings.top(), btnSettings.width(), BTN_HEIGHT);
			}
		} else {
			layoutPlayModeButtons(btnAreaLeft, insets.top + reservedTop + GAP, buttonAreaWidth, BTN_HEIGHT);
			Float supportBottom = null;
			if (btnSupport != null) {
				btnSupport.setRect(btnSolo.left(), btnSolo.bottom() + GAP, buttonAreaWidth, BTN_HEIGHT);
				supportBottom = btnSupport.bottom();
			}
			float rankingsTop = TitleSupportLayout.rankingsY(btnSolo.bottom(), GAP, supportBottom);
			btnRankings.setRect(btnSolo.left(), rankingsTop, (btnSolo.width()), BTN_HEIGHT);
			btnJournal.setRect(btnRankings.right() + 2, btnRankings.top(), btnRankings.width(), BTN_HEIGHT);
			Float newsBottom = null;
			if (btnNews != null && btnChanges != null) {
				btnNews.setRect(btnRankings.left(), btnRankings.bottom() + GAP, btnRankings.width(), BTN_HEIGHT);
				btnChanges.setRect(btnNews.right() + 2, btnNews.top(), btnNews.width(), BTN_HEIGHT);
				newsBottom = btnNews.bottom();
			}
			float settingsTop = TitleSupportLayout.settingsY(btnRankings.bottom(), GAP, newsBottom);
			btnSettings.setRect(btnRankings.left(), settingsTop, btnRankings.width(), BTN_HEIGHT);
			btnAbout.setRect(btnSettings.right() + 2, btnSettings.top(), btnSettings.width(), BTN_HEIGHT);
		}

		version = new BitmapText("v" + Game.version, pixelFont);
		version.measure();
		version.hardlight(0x888888);
		version.x = insets.left + w - version.width() - (DeviceCompat.isDesktop() ? 4 : 8);
		version.y = insets.top + h - version.height() - (DeviceCompat.isDesktop() ? 2 : 4);
		add(version);

		btnFade = new IconButton(Icons.CHEVRON.get()) {
			@Override
			protected void onClick() {
				enable(false);
				parent.add(new Tweener(parent, 0.5f) {
					@Override
					protected void updateValues(float progress) {
						if (!btnFade.active) {
							uiAlpha = 1 - progress;
							updateFade();
						}
					}
				});
			}
		};
		btnFade.icon().originToCenter();
		btnFade.icon().angle = 180f;
		btnFade.setRect(btnAreaLeft + (buttonAreaWidth - 16) / 2, camera.main.height - 16 - insets.bottom, 16, 16);
		add(btnFade);

		PointerArea fadeResetter = new PointerArea(0, 0, Camera.main.width, Camera.main.height) {
			@Override
			public boolean onSignal(PointerEvent event) {
				if (event != null && event.type == PointerEvent.Type.UP && uiAlpha == 0) {
					parent.add(new Tweener(parent, 0.5f) {
						@Override
						protected void updateValues(float progress) {
							uiAlpha = progress;
							updateFade();
							if (progress >= 1) {
								btnFade.enable(true);
							}
						}
					});
				}
				return false;
			}
		};
		add(fadeResetter);

		if (DeviceCompat.isDesktop()) {
			btnExit = new ExitButton();
			btnExit.setPos(w - btnExit.width(), 0);
			add(btnExit);
		}

		Badges.loadGlobal();
		if (Badges.isUnlocked(Badges.Badge.VICTORY) && !SPDSettings.victoryNagged()) {
			SPDSettings.victoryNagged(true);
			add(new WndVictoryCongrats());
		}

		fadeIn();

		uiAlpha = 1f;
		updateFade();
		requestUpdateCheckIfEnabled(SPDSettings.updates());
		EchoBackendProbe.probeAsync(this::onBackendProbeComplete);
	}

	private float uiAlpha;
	private boolean offlineErrorShown;
	private boolean updateNudgeShown;

	/** Starts an async game-version check when the updates preference is on. */
	static void requestUpdateCheckIfEnabled(boolean updatesEnabled) {
		if (updatesEnabled) {
			Updates.checkForUpdate();
		}
	}

	/** True when an update is ready and this title visit has not nudged yet. */
	static boolean shouldShowUpdateNudge(boolean updateAvailable, boolean alreadyShown) {
		return updateAvailable && !alreadyShown;
	}

	@Override
	public void update() {
		super.update();
		if (shouldShowUpdateNudge(Updates.updateAvailable(), updateNudgeShown)
				&& canApplyBackendProbeUi(this)) {
			updateNudgeShown = true;
			showUpdateAvailableDialog();
		}
	}

	private void showUpdateAvailableDialog() {
		AvailableUpdateData update = Updates.updateData();
		addToFront(new WndUpdateAvailable(update, () -> Updates.launchUpdate(Updates.updateData())));
	}

	private void onBackendProbeComplete() {
		// Resize/resetScene destroys this instance; ignore stale probe callbacks.
		if (!canApplyBackendProbeUi(this)) {
			return;
		}
		updateFade();
		if (!EchoBackendProbe.isOnlineReady() && !offlineErrorShown) {
			offlineErrorShown = true;
			showOfflineConnectionDialog();
		}
	}

	private void showOfflineConnectionDialog() {
		add(new WndEchoConnectionFailed(
				Messages.get(this, EchoBackendProbe.offlineMessageKey()),
				new WndEchoConnectionFailed.Listener() {
					@Override
					public void onRetry() {
						offlineErrorShown = false;
						EchoBackendProbe.probeAsync(() -> onBackendProbeComplete());
					}

					@Override
					public void onDismiss() {
					}
				}));
	}

	/**
	 * False after {@link #destroy()} nulls {@code members} (e.g. window resize).
	 */
	static boolean canApplyBackendProbeUi(TitleScene scene) {
		return scene != null && scene.members != null;
	}

	public void updateFade() {
		float alpha = GameMath.gate(0f, uiAlpha, 1f);
		boolean online = EchoBackendProbe.isOnlineReady();

		title.alpha(alpha);
		leftFB.am = alpha;
		rightFB.am = alpha;

		btnRanked.enable(alpha != 0 && online);
		btnSolo.enable(alpha != 0 && online);
		if (btnDebug != null) {
			btnDebug.enable(alpha != 0);
		}
		if (btnSupport != null)
			btnSupport.enable(alpha != 0);
		btnRankings.enable(alpha != 0);
		btnJournal.enable(alpha != 0);
		if (btnNews != null)
			btnNews.enable(alpha != 0);
		if (btnChanges != null)
			btnChanges.enable(alpha != 0);
		btnSettings.enable(alpha != 0);
		btnAbout.enable(alpha != 0);

		btnRanked.alpha(alpha);
		btnSolo.alpha(alpha);
		if (btnDebug != null) {
			btnDebug.alpha(alpha);
		}
		if (btnSupport != null)
			btnSupport.alpha(alpha);
		btnRankings.alpha(alpha);
		btnJournal.alpha(alpha);
		if (btnNews != null)
			btnNews.alpha(alpha);
		if (btnChanges != null)
			btnChanges.alpha(alpha);
		btnSettings.alpha(alpha);
		btnAbout.alpha(alpha);

		version.alpha(alpha);
		btnFade.icon().alpha(alpha);
		if (btnExit != null) {
			btnExit.enable(alpha != 0);
			btnExit.icon().alpha(alpha);
		}

	}

	private void layoutPlayModeButtons(float left, float top, float areaWidth, float height) {
		if (btnDebug != null) {
			float third = (areaWidth - 4) / 3f;
			btnSolo.setRect(left, top, third, height);
			align(btnSolo);
			btnRanked.setRect(btnSolo.right() + 2, top, third, height);
			btnDebug.setRect(btnRanked.right() + 2, top, third, height);
		} else {
			btnSolo.setRect(left, top, (areaWidth / 2) - 1, height);
			align(btnSolo);
			btnRanked.setRect(btnSolo.right() + 2, top, btnSolo.width(), height);
		}
	}

	private void beginEchoRun(EchoPlayMode mode) {
		if (!EchoBackendProbe.isOnlineReady()) {
			showOfflineConnectionDialog();
			return;
		}

		Runnable start = () -> {
			GamesInProgress.selectEchoPlayMode(mode);
			GamesInProgress.selectedClass = null;
			if (GamesInProgress.checkAll().size() == 0) {
				GamesInProgress.curSlot = 1;
				ShatteredPixelDungeon.switchScene(HeroSelectScene.class);
			} else {
				ShatteredPixelDungeon.switchNoFade(StartScene.class);
			}
		};

		EchoPlayerAuthGate.ensureReadyThen(start::run);
	}

	/** Debug arena: no backend / auth gate. Debug builds only. */
	private void beginDebugRun() {
		if (!DebugSettings.isDebugBuild()) {
			return;
		}
		GamesInProgress.selectEchoPlayMode(EchoPlayMode.DEBUG);
		GamesInProgress.selectedClass = null;
		if (GamesInProgress.checkAll().size() == 0) {
			GamesInProgress.curSlot = 1;
			ShatteredPixelDungeon.switchScene(HeroSelectScene.class);
		} else {
			ShatteredPixelDungeon.switchNoFade(StartScene.class);
		}
	}

	static float torchLeftX(float titleLeft, float outset) {
		return titleLeft - outset;
	}

	static float torchRightX(float titleRight, float outset) {
		return titleRight + outset;
	}

	static float brandTitleY(float insetsTop, float topRegion, float titleHeight, boolean landscape) {
		float centered = insetsTop + 2f + (topRegion - titleHeight) / 2f;
		float minTop = insetsTop + (landscape ? BRAND_TOP_CLEARANCE_LANDSCAPE : BRAND_TOP_CLEARANCE_PORTRAIT);
		return Math.max(centered, minTop);
	}

	/**
	 * Menu starts at topRegion, or lower if the brand was pushed down for
	 * clearance.
	 */
	static float menuRegionTop(float insetsTop, float topRegion, float titleBottom) {
		return Math.max(insetsTop + topRegion, titleBottom);
	}

	private Fireball placeTorch(float x, float y) {
		Fireball fb = new Fireball();
		fb.x = x - fb.width() / 2f;
		fb.y = y - fb.height();

		align(fb);
		add(fb);
		return fb;
	}

	private static class NewsButton extends StyledButton {

		public NewsButton(Chrome.Type type, String label) {
			super(type, label);
			if (SPDSettings.news())
				News.checkForNews();
		}

		int unreadCount = -1;

		@Override
		public void update() {
			super.update();

			if (unreadCount == -1 && News.articlesAvailable()) {
				long lastRead = SPDSettings.newsLastRead();
				if (lastRead == 0) {
					if (News.articles().get(0) != null) {
						SPDSettings.newsLastRead(News.articles().get(0).date.getTime());
					}
				} else {
					unreadCount = News.unreadArticles(new Date(SPDSettings.newsLastRead()));
					if (unreadCount > 0) {
						unreadCount = Math.min(unreadCount, 9);
						text(text() + "(" + unreadCount + ")");
					}
				}
			}

			if (unreadCount > 0) {
				textColor(ColorMath.interpolate(0xFFFFFF, Window.SHPX_COLOR,
						0.5f + (float) Math.sin(Game.timeTotal * 5) / 2f));
			}
		}

		@Override
		protected void onClick() {
			super.onClick();
			ShatteredPixelDungeon.switchNoFade(NewsScene.class);
		}
	}

	private static class ChangesButton extends StyledButton {

		public ChangesButton(Chrome.Type type, String label) {
			super(type, label);
			if (SPDSettings.updates())
				Updates.checkForUpdate();
		}

		boolean updateShown = false;

		@Override
		public void update() {
			super.update();

			if (!updateShown && Updates.updateAvailable()) {
				updateShown = true;
				text(Messages.get(TitleScene.class, "update"));
			}

			if (updateShown) {
				textColor(ColorMath.interpolate(0xFFFFFF, Window.SHPX_COLOR,
						0.5f + (float) Math.sin(Game.timeTotal * 5) / 2f));
			}
		}

		@Override
		protected void onClick() {
			if (Updates.updateAvailable()) {
				AvailableUpdateData update = Updates.updateData();

				ShatteredPixelDungeon.scene().addToFront(new WndOptions(
						Icons.get(Icons.CHANGES),
						update.versionName == null ? Messages.get(this, "title")
								: Messages.get(this, "versioned_title", update.versionName),
						update.desc == null ? Messages.get(this, "desc") : update.desc,
						Messages.get(this, "update"),
						Messages.get(this, "changes")) {
					@Override
					protected void onSelect(int index) {
						if (index == 0) {
							Updates.launchUpdate(Updates.updateData());
						} else if (index == 1) {
							ChangesScene.changesSelected = 0;
							ShatteredPixelDungeon.switchNoFade(ChangesScene.class);
						}
					}
				});

			} else {
				ChangesScene.changesSelected = 0;
				ShatteredPixelDungeon.switchNoFade(ChangesScene.class);
			}
		}

	}

	private static class SettingsButton extends StyledButton {

		public SettingsButton(Chrome.Type type, String label) {
			super(type, label);
			if (Messages.lang().status() == Languages.Status.X_UNFINISH) {
				icon(Icons.get(Icons.LANGS));
				icon.hardlight(1.5f, 0, 0);
			} else {
				icon(Icons.get(Icons.PREFS));
			}
		}

		@Override
		public void update() {
			super.update();

			if (Messages.lang().status() == Languages.Status.X_UNFINISH) {
				textColor(ColorMath.interpolate(0xFFFFFF, CharSprite.NEGATIVE,
						0.5f + (float) Math.sin(Game.timeTotal * 5) / 2f));
			}
		}

		@Override
		protected void onClick() {
			if (Messages.lang().status() == Languages.Status.X_UNFINISH) {
				WndSettings.last_index = 5;
			}
			ShatteredPixelDungeon.scene().add(new WndSettings());
		}
	}

	private static class SupportButton extends StyledButton {

		public SupportButton(Chrome.Type type, String label) {
			super(type, label);
			icon(Icons.get(Icons.GOLD));
			textColor(Window.TITLE_COLOR);
		}

		@Override
		protected void onClick() {
			ShatteredPixelDungeon.switchNoFade(SupporterScene.class);
		}
	}
}
