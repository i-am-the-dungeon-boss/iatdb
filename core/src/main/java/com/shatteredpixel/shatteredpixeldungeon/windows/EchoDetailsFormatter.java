package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoStorage;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.DungeonSeed;
import com.watabou.utils.Bundle;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Builds human-readable echo list labels and detail text for {@link WndEchoes}.
 */
public final class EchoDetailsFormatter {

	private EchoDetailsFormatter() {
	}

	public static String listLabel(Echo echo) {
		return Messages.get(WndEchoes.class, "list_item", echo.depth, echo.heroClass, echo.lvl);
	}

	public static String formatDetails(EchoStorage.EchoEntry entry, String currentGameVersion) {
		Echo echo = entry.echo;
		StringBuilder sb = new StringBuilder();

		appendLine(sb, "id", echo.echoId != null ? echo.echoId : Messages.get(WndEchoes.class, "none"));
		appendLine(sb, "file", entry.filename());
		if (echo.depth > 0) {
			appendLine(sb, "depth", echo.depth);
		}
		appendLine(sb, "hero_class", echo.heroClass);
		appendLine(sb, "level", echo.lvl);
		appendLine(sb, "hp", echo.hp, echo.ht);
		appendLine(sb, "game_version", echo.gameVersion);
		// Version gating disabled for now.
		// appendLine(sb, "compatible",
		// Messages.get(WndEchoes.class, echo.isCompatibleWith(currentGameVersion) ?
		// "yes" : "no"));

		if (echo.gameSeed != 0) {
			appendLine(sb, "seed", DungeonSeed.convertToCode(echo.gameSeed));
		}

		long when = echo.timestamp > 0 ? echo.timestamp : entry.sortTime();
		if (when > 0) {
			DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
			appendLine(sb, "saved", format.format(new Date(when)));
		}

		appendLine(sb, "echo_data",
				Messages.get(WndEchoes.class, echo.echoData != null ? "present" : "missing"));

		if (echo.echoData != null) {
			try {
				Hero hero = new Hero();
				hero.restoreFromBundle(echo.echoData);
				appendHeroDetails(sb, hero);
			} catch (Throwable ignored) {
				appendLine(sb, "hero_load_failed", Messages.get(WndEchoes.class, "yes"));
			}
		}

		return sb.toString().trim();
	}

	private static void appendHeroDetails(StringBuilder sb, Hero hero) {
		if (hero.subClass != HeroSubClass.NONE) {
			appendLine(sb, "subclass", hero.subClass.title());
		}
		appendLine(sb, "str", hero.STR());
		appendLine(sb, "attack", hero.attackSkill(null));
		appendLine(sb, "defense", hero.defenseSkill(null));
		appendLine(sb, "exp", hero.exp);

		Item weapon = hero.belongings.weapon();
		appendLine(sb, "weapon", weapon != null ? weapon.title() : Messages.get(WndEchoes.class, "none"));

		Item armor = hero.belongings.armor();
		appendLine(sb, "armor", armor != null ? armor.title() : Messages.get(WndEchoes.class, "none"));

		EchoBackpackFormatter.appendTo(sb, hero.belongings.backpack);
	}

	private static void appendLine(StringBuilder sb, String key, Object... args) {
		if (sb.length() > 0)
			sb.append('\n');
		sb.append(Messages.get(WndEchoes.class, key, args));
	}
}
