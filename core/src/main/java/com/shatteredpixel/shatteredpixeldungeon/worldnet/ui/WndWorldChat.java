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

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollingListPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.MuteList;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldChatMessage;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldNet;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldNetEngine;
import com.shatteredpixel.shatteredpixeldungeon.worldnet.WorldPresence;

import java.util.List;

/**
 * World chat scrollback and composer.
 *
 * <p>The composer is a modal {@link WndTextInput} rather than an always-present
 * input bar. A live {@code TextInput} owns a libGDX {@code Stage} and an input
 * processor, would have to arbitrate {@code claimKeyboardFocus()} against every
 * other window, and — worst — would let WASD keep reaching
 * {@code CellSelector} and walk the hero around while the player types. A modal
 * sidesteps all three, because windows already block hero input.
 */
public class WndWorldChat extends Window implements WorldNet.Observer {

	/** Must match the server's {@code REPORT_REASONS} enum, in menu order. */
	private static final String[] REPORT_REASONS = { "spam", "harassment", "hate", "other" };

	private static final int WIDTH = 135;
	private static final int HEIGHT = 145;
	private static final int BUTTON_HEIGHT = 16;
	private static final int GAP = 2;

	private final ScrollingListPane list;

	public WndWorldChat() {
		resize(WIDTH, HEIGHT);

		RenderedTextBlock title = PixelScene.renderTextBlock(Messages.get(this, "title"), 9);
		title.hardlight(TITLE_COLOR);
		title.setPos((WIDTH - title.width()) / 2f, GAP);
		PixelScene.align(title);
		add(title);

		float top = title.bottom() + GAP;

		list = new ScrollingListPane();
		add(list);
		list.setRect(0, top, WIDTH, HEIGHT - top - BUTTON_HEIGHT - GAP);

		RedButton say = new RedButton(Messages.get(this, "say")) {
			@Override
			protected void onClick() {
				compose();
			}
		};
		say.setRect(0, HEIGHT - BUTTON_HEIGHT, WIDTH, BUTTON_HEIGHT);
		add(say);

		refresh();
		WorldNet.observe(this);
		// Reading the scrollback counts as caring; the engine decides what to
		// do with that.
		WorldNet.setChatFocused(true);
	}

	private void refresh() {
		list.clear();
		List<WorldChatMessage> history = WorldNet.history();
		if (history.isEmpty()) {
			list.addItem(null, null, Messages.get(this, "empty"));
			return;
		}
		for (WorldChatMessage message : history) {
			list.addItem(new MessageItem(message));
		}
		scrollToNewest();
	}

	/** A chat line that offers mute and report when tapped. */
	private class MessageItem extends ScrollingListPane.ListItem {

		/** Breathing room above and below the text, and either side of it. */
		private static final int PAD = 2;
		/** Keeps a one-word message from becoming an unreadably thin strip. */
		private static final int MIN_HEIGHT = 11;

		private final WorldChatMessage message;

		MessageItem(WorldChatMessage message) {
			super(null, null, Messages.get(WndWorldChat.class, "line", message.name, message.text));
			this.message = message;
		}

		/**
		 * Sizes the row to its wrapped text instead of a fixed height.
		 *
		 * <p>{@link ScrollingListPane} stacks items by the height it reads back
		 * after {@code setRect}, and the inherited layout leaves that at a fixed
		 * 18px while vertically centring a single line. A chat message is the one
		 * kind of list content whose length is not under our control, so anything
		 * that wraps past one line draws straight over the row beneath it.
		 *
		 * <p>The 16px icon gutter is reclaimed too: chat lines have no icon, and
		 * the extra width means fewer messages wrap at all.
		 */
		@Override
		protected void layout() {
			line.size(width, 1);
			line.x = x;
			line.y = y;

			label.maxWidth((int) (width - PAD * 2));
			label.setPos(x + PAD, y + PAD);
			PixelScene.align(label);

			height = Math.max(MIN_HEIGHT, label.height() + PAD * 2);
		}

		@Override
		public boolean onClick(float x, float y) {
			if (!inside(x, y)) {
				return false;
			}
			showActions(message);
			return true;
		}
	}

	private void showActions(final WorldChatMessage message) {
		final boolean alreadyMuted = MuteList.isMuted(message.name);
		GameScene.show(new WndOptions(
				Messages.get(this, "actions_title", message.name),
				Messages.get(this, "actions_body"),
				alreadyMuted ? Messages.get(this, "unmute") : Messages.get(this, "mute"),
				Messages.get(this, "report"),
				Messages.get(this, "cancel")) {
			@Override
			protected void onSelect(int index) {
				if (index == 0) {
					if (alreadyMuted) {
						MuteList.unmute(message.name);
						GLog.p(Messages.get(WndWorldChat.class, "unmuted", message.name));
					} else {
						MuteList.mute(message.name);
						GLog.p(Messages.get(WndWorldChat.class, "muted", message.name));
					}
					refresh();
				} else if (index == 1) {
					showReportReasons(message);
				}
			}
		});
	}

	/**
	 * A reason is asked for rather than assumed — an unlabelled report tells a
	 * moderator nothing, and the queue is only as useful as what lands in it.
	 */
	private void showReportReasons(final WorldChatMessage message) {
		GameScene.show(new WndOptions(
				Messages.get(this, "report_title"),
				Messages.get(this, "report_body", message.name),
				Messages.get(this, "reason_spam"),
				Messages.get(this, "reason_harassment"),
				Messages.get(this, "reason_hate"),
				Messages.get(this, "reason_other"),
				Messages.get(this, "cancel")) {
			@Override
			protected void onSelect(int index) {
				if (index < 0 || index >= REPORT_REASONS.length) {
					return;
				}
				WorldNet.report(message.id, REPORT_REASONS[index]);
				GLog.p(Messages.get(WndWorldChat.class, "reported", message.name));
			}
		});
	}

	/** Chat reads oldest-first, so the useful end is the bottom. */
	private void scrollToNewest() {
		float overflow = list.content().height() - list.height();
		if (overflow > 0) {
			list.scrollTo(0, overflow);
		}
	}

	private void compose() {
		GameScene.show(new WndTextInput(
				Messages.get(this, "say"),
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
		});
	}

	@Override
	public void onWorldChat(WorldChatMessage message) {
		refresh();
	}

	@Override
	public void onLocalChat(String text) {
		// The scrollback shows the server's copy when it comes back around, so
		// an optimistic line here would only double up.
	}

	@Override
	public void onWorldRoster(List<WorldPresence> occupants) {
	}

	@Override
	public void onWorldStatus(WorldNetEngine.Status status) {
	}

	@Override
	public void onServerMute(boolean muted) {
		// The scrollback is a read-only view; WorldChannel does the announcing.
	}

	@Override
	public void destroy() {
		WorldNet.unobserve(this);
		WorldNet.setChatFocused(false);
		super.destroy();
	}
}
