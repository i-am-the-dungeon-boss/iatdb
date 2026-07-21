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

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.services.billing.SupportBilling;
import com.shatteredpixel.shatteredpixeldungeon.services.billing.SupportBillingService;
import com.shatteredpixel.shatteredpixeldungeon.ui.ExitButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.SupportTipProducts;
import com.shatteredpixel.shatteredpixeldungeon.ui.TitleBackground;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.RectF;

import java.util.ArrayList;

public class SupporterScene extends PixelScene {

	private static final int BTN_HEIGHT = 22;
	private static final int GAP = 2;

	@Override
	public void create() {
		super.create();

		uiCamera.visible = false;

		int w = Camera.main.width;
		int h = Camera.main.height;
		RectF insets = getCommonInsets();

		int elementWidth = PixelScene.landscape() ? 202 : 120;

		TitleBackground BG = new TitleBackground(w, h);
		add(BG);

		w -= insets.right + insets.left;
		h -= insets.top + insets.bottom;

		ExitButton btnExit = new ExitButton();
		btnExit.setPos(insets.left + w - btnExit.width(), insets.top);
		add(btnExit);

		IconTitle title = new IconTitle(Icons.GOLD.get(), Messages.get(this, "title"));
		title.setSize(200, 0);
		title.setPos(
				insets.left + (w - title.reqWidth()) / 2f,
				insets.top + (20 - title.height()) / 2f);
		align(title);
		add(title);

		SupporterMessage msg = new SupporterMessage();
		msg.setSize(elementWidth, 0);
		add(msg);

		ArrayList<StyledButton> tipButtons = new ArrayList<>();
		for (String productId : SupportTipProducts.PRODUCT_IDS) {
			final String id = productId;
			StyledButton tip = new StyledButton(Chrome.Type.GREY_BUTTON_TR,
					Messages.get(this, "tip_amount", SupportTipProducts.displayAmountUsd(id))) {
				@Override
				protected void onClick() {
					super.onClick();
					SupportBilling.purchase(id, new SupportBillingService.PurchaseCallback() {
						@Override
						public void onSuccess() {
							ShatteredPixelDungeon.scene()
									.add(new WndMessage(Messages.get(SupporterScene.class, "thanks")));
						}

						@Override
						public void onCancelled() {
						}

						@Override
						public void onError(String message) {
							ShatteredPixelDungeon.scene().add(new WndMessage(
									Messages.get(SupporterScene.class, "billing_error", message)));
						}
					});
				}
			};
			tip.icon(Icons.get(Icons.GOLD));
			tip.textColor(Window.TITLE_COLOR);
			tip.setSize(elementWidth, BTN_HEIGHT);
			add(tip);
			tipButtons.add(tip);
		}

		float elementHeight = msg.height() + tipButtons.size() * (BTN_HEIGHT + GAP);

		float top = insets.top + 16 + Math.max(0, (h - 16 - elementHeight) / 2f);
		float left = insets.left + (w - elementWidth) / 2f;

		msg.setPos(left, top);
		align(msg);

		float y = msg.bottom() + GAP;
		for (StyledButton tip : tipButtons) {
			tip.setPos(left, y);
			align(tip);
			y = tip.bottom() + GAP;
		}
	}

	@Override
	protected void onBackPressed() {
		ShatteredPixelDungeon.switchNoFade(TitleScene.class);
	}

	private static class SupporterMessage extends Component {

		NinePatch bg;
		RenderedTextBlock text;
		Image icon;

		@Override
		protected void createChildren() {
			bg = Chrome.get(Chrome.Type.GREY_BUTTON_TR);
			add(bg);

			String message = Messages.get(SupporterScene.class, "intro");
			message += "\n\n" + Messages.get(SupporterScene.class, "play_msg");
			message += "\n\n- Dungeon Boss";

			text = PixelScene.renderTextBlock(message, 6);
			add(text);

			icon = Icons.get(Icons.IATDB);
			add(icon);
		}

		@Override
		protected void layout() {
			bg.x = x;
			bg.y = y;

			text.maxWidth((int) width - bg.marginHor());
			text.setPos(x + bg.marginLeft(), y + bg.marginTop() + 1);

			icon.y = text.bottom() - icon.height() + 4;
			icon.x = x + 25;

			height = (text.bottom() + 3) - y;
			height += bg.marginBottom();
			bg.size(width, height);
		}
	}
}
