package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoStorage;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollingListPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Image;

import java.util.List;

public class WndEchoes extends Window {

	private static final int WIDTH = 120;
	private static final int HEIGHT = 120;

	public static void show() {
		List<EchoStorage.EchoEntry> entries = new EchoStorage().loadAll();
		if (entries.isEmpty()) {
			GameScene.show(new WndMessage(Messages.get(WndEchoes.class, "empty")));
		} else {
			GameScene.show(new WndEchoes(entries));
		}
	}

	private WndEchoes(List<EchoStorage.EchoEntry> entries) {
		super();

		ScrollingListPane list = new ScrollingListPane();
		add(list);

		list.addTitle(Messages.get(this, "title", entries.size()));

		for (EchoStorage.EchoEntry entry : entries) {
			Echo echo = entry.echo;
			String label = EchoDetailsFormatter.listLabel(echo);
			Image avatar = HeroSprite.avatar(
					EchoHeroLoader.heroClass(echo),
					EchoHeroLoader.armorTier(null, echo));

			ScrollingListPane.ListItem item = new ScrollingListPane.ListItem(avatar, null, label) {
				@Override
				public boolean onClick(float x, float y) {
					if (inside(x, y)) {
						GameScene.show(new WndEchoDetail(entry));
						return true;
					}
					return false;
				}
			};
			// Version gating disabled for now.
			// if (!echo.isCompatibleWith(Game.version)) {
			// item.hardlight(0xFF6666);
			// }
			list.addItem(item);
		}

		resize(WIDTH, HEIGHT);
		list.setRect(0, 0, width, height);
	}

}
