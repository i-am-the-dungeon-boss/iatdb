/*
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

package com.shatteredpixel.shatteredpixeldungeon.worldnet.ui;

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.SPDAction;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldChatMessage;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldNet;
import com.watabou.input.KeyBindings;
import com.watabou.input.KeyEvent;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.TextInput;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.Signal;

/**
 * The always-visible chat line, sitting in a translucent box above the hero
 * info panel.
 *
 * <p>Two states, and the split is the whole point. Idle, there is no
 * {@link TextInput} at all — only a nine-patch and a prompt. A {@code TextInput}
 * owns a libGDX {@code Stage} plus an input processor and claims keyboard focus
 * <em>from its constructor</em>, so keeping one alive permanently would pop the
 * soft keyboard the moment a level loaded and leak a stage on every scene
 * change. The field is therefore built on tap and destroyed on send.
 *
 * <p>While it is alive, {@link TextInput#isEditing()} makes
 * {@code CellSelector} ignore key bindings, so typing does not also drive the
 * hero.
 */
public class WorldChatBar extends Component {

	public static final int HEIGHT = 14;

	private static final int TEXT_SIZE = 6;
	private static final int PROMPT_COLOUR = 0x888888;
	private static final float PADDING = 2f;

	private NinePatch bg;
	private RenderedTextBlock prompt;
	private PointerArea hotspot;
	private PointerArea historySpot;
	private RenderedTextBlock historyMark;
	private TextInput input;
	/** True for the frame in which the field was dismissed. */
	private boolean justClosed;

	/**
	 * Enter opens the field and Enter sends it, on desktop only.
	 *
	 * <p>Lives here rather than in {@code CellSelector} because PD's key
	 * listeners fire even while a libGDX text field holds focus — that is the
	 * same quirk {@link TextInput#isEditing()} guards against — so a single
	 * toggle in one place is the only way to avoid a press being handled twice.
	 */
	private final Signal.Listener<KeyEvent> keyListener = new Signal.Listener<KeyEvent>() {
		@Override
		public boolean onSignal(KeyEvent event) {
			if (!event.pressed || !DeviceCompat.isDesktop() || !WorldNet.isConnected()) {
				return false;
			}
			// Opening only. Once the field is live its libGDX stage consumes the
			// key and this listener never sees it, so sending is handled by
			// enterPressed() instead. justClosed covers the frame in which the
			// field went away, so the press that sent cannot also reopen.
			if (input != null || justClosed) {
				return false;
			}
			if (KeyBindings.getActionForKey(event) != SPDAction.CHAT) {
				return false;
			}
			beginComposing();
			return true;
		}
	};

	public WorldChatBar() {
		super();
		// Registered here rather than in createChildren(): Component's
		// constructor calls createChildren() before this class's field
		// initialisers have run, so keyListener is still null at that point and
		// Signal would happily store the null and then NPE on every key press.
		KeyEvent.addKeyListener(keyListener);
	}

	@Override
	protected void createChildren() {
		// TOAST_TR is the translucent chrome; the map stays readable behind it.
		bg = Chrome.get(Chrome.Type.TOAST_TR);
		add(bg);

		prompt = PixelScene.renderTextBlock(Messages.get(this, "prompt"), TEXT_SIZE);
		prompt.hardlight(PROMPT_COLOUR);
		add(prompt);

		hotspot = new PointerArea(0, 0, 0, 0) {
			@Override
			protected void onClick(PointerEvent event) {
				beginComposing();
			}
		};
		add(hotspot);

		// Right-edge strip opens the scrollback, which is also where mute and
		// report live — the bar itself has no room for per-message actions.
		historySpot = new PointerArea(0, 0, 0, 0) {
			@Override
			protected void onClick(PointerEvent event) {
				GameScene.show(new WndWorldChat());
			}
		};
		add(historySpot);

		historyMark = PixelScene.renderTextBlock(Messages.get(this, "history"), TEXT_SIZE);
		historyMark.hardlight(PROMPT_COLOUR);
		add(historyMark);
	}

	@Override
	protected void layout() {
		bg.x = x;
		bg.y = y;
		bg.size(width, height);

		prompt.setPos(x + PADDING, y + (height - prompt.height()) / 2f);
		PixelScene.align(prompt);

		float historyWidth = historyMark.width() + PADDING * 2;
		historyMark.setPos(
				x + width - historyWidth + PADDING,
				y + (height - historyMark.height()) / 2f);
		PixelScene.align(historyMark);

		historySpot.x = x + width - historyWidth;
		historySpot.y = y;
		historySpot.width = historyWidth;
		historySpot.height = height;

		// Compose covers everything left of the history strip.
		hotspot.x = x;
		hotspot.y = y;
		hotspot.width = width - historyWidth;
		hotspot.height = height;

		if (input != null) {
			// Full height and the same left inset as the prompt, so the caret
			// starts exactly where "Say something..." did. Stops short of the
			// history strip so typing never runs underneath it.
			input.setRect(x + PADDING, y, width - historyWidth - PADDING, height);
		}
	}

	public boolean isComposing() {
		return input != null;
	}

	/**
	 * Desktop types in place; touch opens a modal.
	 *
	 * <p>The split is forced by the soft keyboard. {@code AndroidManifest.xml}
	 * sets no {@code windowSoftInputMode} and the GL surface never resizes, so
	 * the IME simply overlays the bottom of the screen — which is exactly where
	 * this bar sits. An inline field there would be typed into blind. A
	 * centered {@link WndTextInput} stays above the keyboard and is already the
	 * proven path on both mobile targets.
	 *
	 * <p>It also sidesteps {@code PointerEvent.clearKeyboardThisPress}, which
	 * defaults to true on every press and would otherwise close the IME the
	 * instant it opened. Desktop has no soft keyboard, so the inline field
	 * never meets either problem.
	 */
	private void beginComposing() {
		if (input != null || !WorldNet.isConnected()) {
			return;
		}
		// Refuse rather than let the player type a message the server will
		// reject with a 403 they would never see.
		if (WorldNet.serverMuted()) {
			GLog.w(Messages.get(this, "server_muted"));
			return;
		}
		if (!DeviceCompat.isDesktop()) {
			showComposerWindow();
			return;
		}

		prompt.visible = false;
		WorldNet.setChatFocused(true);

		// The field's stage renders in screen pixels while the prompt renders in
		// camera units, so the requested size has to be scaled by the camera
		// zoom to come out the same height — the convention WndTextInput uses.
		int fieldSize = (int) PixelScene.uiCamera.zoom * TEXT_SIZE;

		// No nine-patch of its own — the bar already draws one, and a second
		// would read as a focus border boxed inside the first.
		input = new TextInput(null, false, fieldSize) {
			@Override
			public void enterPressed() {
				send();
			}
		};
		input.setMaxLength(WorldChatMessage.MAX_LENGTH);
		input.alignLeft();
		// The skin hard-codes black, which is invisible on the translucent bar.
		input.setFontColor(PROMPT_COLOUR);
		add(input);
		layout();
		input.claimKeyboardFocus();
	}

	private void showComposerWindow() {
		// The desktop field announces this when it opens; the window is the same
		// act on a touch device, so the village should read it the same way.
		WorldNet.setChatFocused(true);
		GameScene.show(new WndTextInput(
				Messages.get(this, "prompt"),
				null,
				"",
				WorldChatMessage.MAX_LENGTH,
				false,
				Messages.get(this, "send"),
				Messages.get(this, "cancel")) {
			@Override
			public void onSelect(boolean positive, String text) {
				if (positive && text != null && !text.trim().isEmpty()) {
					WorldNet.sendChat(text.trim());
				}
			}

			@Override
			public void hide() {
				// Overridden rather than clearing in onSelect: the window also
				// closes to a tap outside it and to the back button, and an
				// ellipsis left hanging by either would never come down.
				WorldNet.setChatFocused(false);
				super.hide();
			}
		});
	}

	private void send() {
		if (input == null) {
			return;
		}
		String text = input.getText();
		if (text != null && !text.trim().isEmpty()) {
			WorldNet.sendChat(text.trim());
		}
		endComposing();
	}

	/**
	 * Enter closes the field as well as sending.
	 *
	 * <p>Staying open would read better as chat, but it also silently keeps
	 * movement keys disabled — a player who walks away from the keyboard
	 * mid-sentence would find WASD dead with no visible reason why.
	 */
	private void endComposing() {
		if (input == null) {
			return;
		}
		remove(input);
		input.destroy();
		input = null;
		justClosed = true;
		prompt.visible = true;
		WorldNet.setChatFocused(false);
	}

	@Override
	public void update() {
		super.update();
		// Cleared a frame after closing, so the Enter that sent a message is
		// long past before the bar will accept an Enter to reopen.
		justClosed = false;
	}

	@Override
	public synchronized void destroy() {
		// Never leak the stage, the input processor, or the key listener on a
		// scene change.
		KeyEvent.removeKeyListener(keyListener);
		endComposing();
		super.destroy();
	}
}
