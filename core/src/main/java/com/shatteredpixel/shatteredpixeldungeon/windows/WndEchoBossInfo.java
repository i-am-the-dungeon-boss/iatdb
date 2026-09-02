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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoInspectable;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.EchoKitSlot;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.TalentsPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Group;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;

/**
 * Tabbed inspect for any {@link EchoInspectable} body — the boss the player
 * fights and the figures standing in the village alike: mob info and stats,
 * talents, equipped kit.
 *
 * <p>Does not swap {@link com.shatteredpixel.shatteredpixeldungeon.Dungeon#hero},
 * which is the whole reason it exists rather than reusing the hero windows.
 */
public class WndEchoBossInfo extends WndTabbed {

	private static final int WIDTH_MIN = 120;
	private static final int WIDTH_MAX = 220;
	private static final int GAP = 2;
	private static final int STAT_GAP = 6;
	private static final int COLS = 5;
	private static final int SLOT_MARGIN = 1;
	private static final int TALENTS_HEIGHT = 120;

	private static final String HERO_STAT = "windows.wndhero$statstab.";

	private final Hero echoHero;

	public static boolean hasInspectableKit(Mob mob) {
		return mob instanceof EchoInspectable && ((EchoInspectable) mob).getEchoHero() != null;
	}

	public static Window windowFor(Mob mob) {
		if (hasInspectableKit(mob)) {
			return new WndEchoBossInfo(mob);
		}
		return new WndInfoMob(mob);
	}

	public static Item[] equippedKit(Hero echoHero) {
		Belongings stuff = echoHero.belongings;
		ArrayList<Item> kit = new ArrayList<>();
		kit.add(stuff.weapon != null ? stuff.weapon : placeholder(ItemSpriteSheet.WEAPON_HOLDER));
		kit.add(stuff.armor != null ? stuff.armor : placeholder(ItemSpriteSheet.ARMOR_HOLDER));
		kit.add(stuff.artifact != null ? stuff.artifact : placeholder(ItemSpriteSheet.ARTIFACT_HOLDER));
		kit.add(stuff.misc != null ? stuff.misc : placeholder(ItemSpriteSheet.SOMETHING));
		kit.add(stuff.ring != null ? stuff.ring : placeholder(ItemSpriteSheet.RING_HOLDER));
		if (stuff.secondWep != null) {
			kit.add(stuff.secondWep);
		}
		return kit.toArray(new Item[0]);
	}

	public static String[][] statRows(Mob body) {
		Hero echoHero = ((EchoInspectable) body).getEchoHero();
		ArrayList<String[]> rows = new ArrayList<>();

		int strBonus = echoHero.STR() - echoHero.STR;
		String strValue;
		if (strBonus > 0) {
			strValue = echoHero.STR + " + " + strBonus;
		} else if (strBonus < 0) {
			strValue = echoHero.STR + " - " + -strBonus;
		} else {
			strValue = Integer.toString(echoHero.STR());
		}
		rows.add(new String[] { Messages.get(HERO_STAT + "str"), strValue });

		String health;
		if (body.shielding() > 0) {
			health = body.HP + "+" + body.shielding() + "/" + body.HT;
		} else {
			health = body.HP + "/" + body.HT;
		}
		rows.add(new String[] { Messages.get(HERO_STAT + "health"), health });
		rows.add(new String[] {
				Messages.get(HERO_STAT + "exp"),
				echoHero.exp + "/" + echoHero.maxExp()
		});

		if (echoHero.subClass != HeroSubClass.NONE) {
			rows.add(new String[] {
					Messages.get(WndEchoBossInfo.class, "subclass"),
					echoHero.subClass.title()
			});
		}

		Echo echo = ((EchoInspectable) body).getEcho();
		if (echo != null && echo.depth > 0) {
			rows.add(new String[] {
					Messages.get(WndEchoBossInfo.class, "depth"),
					Integer.toString(echo.depth)
			});
		}

		return rows.toArray(new String[0][]);
	}

	public static int talentTiersToShow(Hero echoHero) {
		int tiers = 1;
		while (tiers < Talent.MAX_TALENT_TIERS
				&& echoHero.lvl + 1 >= Talent.tierLevelThresholds[tiers + 1]) {
			tiers++;
		}
		if (tiers > 2 && echoHero.subClass == HeroSubClass.NONE) {
			tiers = 2;
		} else if (tiers > 3 && echoHero.armorAbility == null) {
			tiers = 3;
		}
		return Math.min(tiers, echoHero.talents.size());
	}

	private static Item placeholder(int image) {
		return new WndBag.Placeholder(image);
	}

	public WndEchoBossInfo(Mob body) {
		super();

		echoHero = ((EchoInspectable) body).getEchoHero();

		int width = WIDTH_MIN;

		WndInfoMob.MobTitle title = new WndInfoMob.MobTitle(body);
		title.setRect(0, 0, width, 0);

		RenderedTextBlock text = WndInfoMob.infoBlock(body);
		text.maxWidth(width);
		text.setPos(title.left(), title.bottom() + 2 * GAP);

		while (PixelScene.landscape()
				&& text.bottom() > PixelScene.MIN_HEIGHT_L - 10
				&& width < WIDTH_MAX) {
			width += 20;
			title.setRect(0, 0, width, 0);
			text.setPos(title.left(), title.bottom() + 2 * GAP);
			text.maxWidth(width);
		}

		Item[] kit = equippedKit(echoHero);
		int slotSize = WndEchoDetail.fittingInventorySlotSize(kit.length);
		int rows = (kit.length + COLS - 1) / COLS;
		int kitHeight = slotSize * rows + SLOT_MARGIN * Math.max(0, rows - 1);

		final InfoTab infoTab = new InfoTab(title, text, statRows(body), width);
		int infoHeight = (int) infoTab.bottom();
		int height = Math.max(infoHeight, Math.max(kitHeight, TALENTS_HEIGHT));

		resize(width, height);

		add(infoTab);

		final TalentsTab talentsTab = new TalentsTab();
		add(talentsTab);
		talentsTab.setRect(0, 0, width, height);

		final KitTab kitTab = new KitTab(kit, slotSize);
		add(kitTab);

		add(new IconTab(Icons.get(Icons.INFO)) {
			@Override
			protected void select(boolean value) {
				super.select(value);
				infoTab.visible = infoTab.active = selected;
			}
		});
		add(new IconTab(Icons.get(Icons.TALENT)) {
			@Override
			protected void select(boolean value) {
				super.select(value);
				talentsTab.visible = talentsTab.active = selected;
			}
		});
		add(new IconTab(Icons.get(Icons.BACKPACK)) {
			@Override
			protected void select(boolean value) {
				super.select(value);
				kitTab.visible = kitTab.active = selected;
			}
		});

		layoutTabs();
		select(0);
	}

	private static class InfoTab extends Group {
		private final float bottom;

		InfoTab(WndInfoMob.MobTitle title, RenderedTextBlock desc, String[][] rows, int width) {
			title.setRect(0, 0, width, 0);
			add(title);

			float pos = title.bottom() + 2 * GAP;
			for (int i = 0; i < rows.length; i++) {
				RenderedTextBlock label = PixelScene.renderTextBlock(rows[i][0], 8);
				label.setPos(0, pos);
				add(label);
				RenderedTextBlock value = PixelScene.renderTextBlock(rows[i][1], 8);
				value.setPos(width * 0.55f, pos);
				PixelScene.align(value);
				add(value);
				pos += STAT_GAP + value.height();
			}

			pos += GAP;
			desc.maxWidth(width);
			desc.setPos(title.left(), pos);
			add(desc);
			bringToFront(title);
			bottom = desc.bottom() + 2;
		}

		float bottom() {
			return bottom;
		}
	}

	private class TalentsTab extends Component {
		private final TalentsPane pane;

		TalentsTab() {
			pane = TalentsPane.preview(echoHero.talents, talentTiersToShow(echoHero));
			add(pane);
		}

		@Override
		protected void layout() {
			super.layout();
			pane.setRect(x, y, width, height);
		}
	}

	private class KitTab extends Group {
		KitTab(Item[] kit, int slotSize) {
			int col = 0;
			int row = 0;
			for (int i = 0; i < kit.length; i++) {
				Item item = kit[i];
				boolean equipped = !(item instanceof WndBag.Placeholder);
				EchoKitSlot slot = new EchoKitSlot(item, echoHero, equipped);
				int x = col * (slotSize + SLOT_MARGIN);
				int y = row * (slotSize + SLOT_MARGIN);
				slot.setRect(x, y, slotSize, slotSize);
				add(slot);

				col++;
				if (col >= COLS) {
					col = 0;
					row++;
				}
			}
		}
	}
}
