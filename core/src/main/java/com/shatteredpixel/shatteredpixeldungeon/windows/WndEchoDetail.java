package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoStorage;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.InventorySlot;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.DungeonSeed;
import com.watabou.noosa.Group;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WndEchoDetail extends WndTabbed {

	private static final int WIDTH = 120;
	private static final int HEIGHT = 120;
	private static final int DELETE_BTN_HEIGHT = 16;

	private static final int DEFAULT_SLOT_SIZE = 28;
	private static final int MIN_SLOT_SIZE = 16;
	private static final int SLOT_MARGIN = 1;
	private static final int COLS = 5;
	private static final int TITLE_HEIGHT = 14;

	public static int[] fittingInventoryLayout(int slotCount) {
		return new int[] { COLS, fittingInventorySlotSize(slotCount) };
	}

	public static int fittingInventorySlotSize(int slotCount) {
		int rows = (int) Math.ceil(slotCount / (float) COLS);
		int size = DEFAULT_SLOT_SIZE;
		while (size >= MIN_SLOT_SIZE) {
			int gridWidth = size * COLS + SLOT_MARGIN * (COLS - 1);
			int gridHeight = TITLE_HEIGHT + size * rows + SLOT_MARGIN * (rows - 1);
			if (gridWidth <= WIDTH && gridHeight <= HEIGHT) {
				return size;
			}
			size--;
		}
		return MIN_SLOT_SIZE;
	}

	private final EchoStorage.EchoEntry entry;
	private final Echo echo;
	private final Hero viewHero;
	private final Hero previousHero;

	private StatsTab stats;
	private InventoryTab inventory;

	public WndEchoDetail(EchoStorage.EchoEntry entry) {
		super();

		this.entry = entry;
		this.echo = entry.echo;
		this.previousHero = Dungeon.hero;
		this.viewHero = EchoHeroLoader.load(echo);
		if (viewHero != null) {
			Dungeon.hero = viewHero;
		}

		resize(WIDTH, HEIGHT);

		stats = new StatsTab(entry);
		add(stats);

		inventory = new InventoryTab();
		add(inventory);

		add(new IconTab(Icons.get(Icons.RANKINGS)) {
			@Override
			protected void select(boolean value) {
				super.select(value);
				stats.visible = stats.active = selected;
			}
		});
		add(new IconTab(Icons.get(Icons.BACKPACK)) {
			@Override
			protected void select(boolean value) {
				super.select(value);
				inventory.visible = inventory.active = selected;
			}
		});

		layoutTabs();
		select(0);

		RedButton deleteBtn = new RedButton(Messages.get(this, "delete")) {
			@Override
			protected void onClick() {
				confirmDelete();
			}
		};
		deleteBtn.setRect(0, HEIGHT - DELETE_BTN_HEIGHT - 2, WIDTH, DELETE_BTN_HEIGHT);
		add(deleteBtn);
	}

	private void confirmDelete() {
		GameScene.show(new WndOptions(
				Messages.get(this, "delete_title"),
				Messages.get(this, "delete_body", echo.depth),
				Messages.get(this, "delete_yes"),
				Messages.get(this, "delete_no")) {
			@Override
			protected void onSelect(int index) {
				if (index == 0) {
					new EchoStorage().deleteEntry(entry);
					hide();
					WndEchoes.show();
				}
			}
		});
	}

	@Override
	public void hide() {
		if (previousHero != null) {
			Dungeon.hero = previousHero;
		}
		super.hide();
	}

	private class StatsTab extends Group {

		private static final int GAP = 6;
		private float pos;

		StatsTab(EchoStorage.EchoEntry entry) {
			Hero hero = viewHero != null ? viewHero : EchoHeroLoader.load(echo);

			IconTitle title = new IconTitle();
			title.icon(HeroSprite.avatar(hero));
			title.label(Messages.get(WndEchoDetail.class, "title",
					hero.lvl, hero.className()).toUpperCase(Locale.ENGLISH));
			title.color(Window.TITLE_COLOR);
			title.setRect(0, 0, WIDTH, 0);
			add(title);

			pos = title.bottom() + 2 * GAP;

			int strBonus = hero.STR() - hero.STR;
			if (strBonus > 0)
				statSlot(Messages.get(WndEchoDetail.class, "str"), hero.STR + " + " + strBonus);
			else if (strBonus < 0)
				statSlot(Messages.get(WndEchoDetail.class, "str"), hero.STR + " - " + -strBonus);
			else
				statSlot(Messages.get(WndEchoDetail.class, "str"), Integer.toString(hero.STR()));

			if (hero.shielding() > 0) {
				statSlot(Messages.get(WndEchoDetail.class, "health"), hero.HP + "+" + hero.shielding() + "/" + hero.HT);
			} else {
				statSlot(Messages.get(WndEchoDetail.class, "health"), hero.HP + "/" + hero.HT);
			}
			statSlot(Messages.get(WndEchoDetail.class, "exp"), hero.exp + "/" + hero.maxExp());

			pos += GAP;

			if (echo.depth > 0) {
				statSlot(Messages.get(WndEchoDetail.class, "depth"), echo.depth);
			}
			if (echo.gameSeed != 0) {
				statSlot(Messages.get(WndEchoDetail.class, "dungeon_seed"), DungeonSeed.convertToCode(echo.gameSeed));
			}
			// Version gating disabled for now.
			// statSlot(Messages.get(WndEchoDetail.class, "compatible"),
			// Messages.get(WndEchoes.class,
			// echo.isCompatibleWith(Game.version) ? "yes" : "no"));

			long when = echo.timestamp > 0 ? echo.timestamp : entry.sortTime();
			if (when > 0) {
				DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT);
				statSlot(Messages.get(WndEchoDetail.class, "saved"), format.format(new Date(when)));
			}

			// if (!echo.isCompatibleWith(Game.version)) {
			// pos += GAP;
			// RenderedTextBlock warn = PixelScene.renderTextBlock(
			// Messages.get(WndEchoDetail.class, "incompatible_warn"), 6);
			// warn.maxWidth(WIDTH);
			// warn.hardlight(0xFF6666);
			// warn.setPos(0, pos);
			// add(warn);
			// }
		}

		private void statSlot(String label, String value) {
			RenderedTextBlock txt = PixelScene.renderTextBlock(label, 8);
			txt.setPos(0, pos);
			add(txt);

			txt = PixelScene.renderTextBlock(value, 8);
			txt.setPos(WIDTH * 0.55f, pos);
			PixelScene.align(txt);
			add(txt);

			pos += GAP + txt.height();
		}

		private void statSlot(String label, int value) {
			statSlot(label, Integer.toString(value));
		}
	}

	private class InventoryTab extends Group {

		private int count;
		private int col;
		private int row;
		private int slotSize;

		InventoryTab() {
			if (viewHero == null) {
				RenderedTextBlock empty = PixelScene.renderTextBlock(
						Messages.get(WndEchoDetail.class, "no_inventory"), 6);
				empty.maxWidth(WIDTH);
				empty.setPos(0, 20);
				add(empty);
				return;
			}

			Belongings stuff = viewHero.belongings;
			int equipped = 5;
			if (stuff.secondWep != null) {
				equipped++;
			}
			slotSize = fittingInventorySlotSize(equipped + stuff.backpack.capacity());

			placeItem(stuff.weapon != null ? stuff.weapon : placeholder(ItemSpriteSheet.WEAPON_HOLDER));
			placeItem(stuff.armor != null ? stuff.armor : placeholder(ItemSpriteSheet.ARMOR_HOLDER));
			placeItem(stuff.artifact != null ? stuff.artifact : placeholder(ItemSpriteSheet.ARTIFACT_HOLDER));
			placeItem(stuff.misc != null ? stuff.misc : placeholder(ItemSpriteSheet.SOMETHING));
			placeItem(stuff.ring != null ? stuff.ring : placeholder(ItemSpriteSheet.RING_HOLDER));

			if (stuff.secondWep != null) {
				placeItem(stuff.secondWep);
			}

			Bag backpack = stuff.backpack;
			for (Item item : backpack.items.toArray(new Item[0])) {
				if (!(item instanceof Bag)) {
					placeItem(item);
				}
			}

			while ((count - equipped) < backpack.capacity()) {
				placeItem(null);
			}
		}

		private Item placeholder(int image) {
			return new WndBag.Placeholder(image);
		}

		private void placeItem(final Item item) {
			count++;

			int x = col * (slotSize + SLOT_MARGIN);
			int y = TITLE_HEIGHT + row * (slotSize + SLOT_MARGIN);

			InventorySlot slot = new InventorySlot(item) {
				@Override
				protected void onClick() {
					if (item != null && !(item instanceof WndBag.Placeholder)) {
						GameScene.show(new WndInfoItem(item));
					}
				}

				@Override
				protected boolean onLongClick() {
					onClick();
					return true;
				}
			};
			if (item == null || item instanceof WndBag.Placeholder) {
				slot.enable(false);
			}
			slot.setRect(x, y, slotSize, slotSize);
			add(slot);

			if (++col >= COLS) {
				col = 0;
				row++;
			}
		}
	}

}
