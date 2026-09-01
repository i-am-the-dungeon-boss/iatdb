package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoInspectable;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.VillageEchoSprite;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndEchoBossInfo;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * A player's echo, standing in the village because it earned a spot on the
 * homepage's depth or honorable-mention lists.
 *
 * <p>Scenery in the {@link com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Villager}
 * mould — passive, immovable, unhittable — but a real {@code Char} rather than a
 * bare sprite, which is the point: {@code GameScene.getObjectsAtCell} finds
 * things through {@code Actor.findChar}, so being a {@code Char} is what makes a
 * figure inspectable and interactable on walk-up. Remote players, which are
 * sprites with no {@code Char}, are exactly the thing that cannot be inspected.
 *
 * <p>The echoed hero arrives late. Standing here needs only appearance and
 * facts; stats, kit and talents need the echo's data bundle, which is requested
 * for this one figure when somebody looks at it. Until it lands
 * {@link #getEchoHero()} is null and the inspect window falls back to the plain
 * mob view rather than showing empty tabs.
 */
public class VillageEcho extends NPC implements EchoInspectable {

	private static final String FIGURE_POST = "figure_post";
	private static final String FIGURE_DEPTH = "figure_depth";
	private static final String FIGURE_ECHO_ID = "figure_echo_id";
	private static final String FIGURE_USER_NAME = "figure_user_name";
	private static final String FIGURE_HERO_CLASS = "figure_hero_class";
	private static final String FIGURE_ARMOR_TIER = "figure_armor_tier";
	private static final String FIGURE_LVL = "figure_lvl";
	private static final String FIGURE_HP = "figure_hp";
	private static final String FIGURE_HT = "figure_ht";
	private static final String FIGURE_KILLS = "figure_kills";
	private static final String FIGURE_TIMESTAMP = "figure_timestamp";
	private static final String FIGURE_BADGE_KINDS = "figure_badge_kinds";
	private static final String FIGURE_BADGE_COUNTS = "figure_badge_counts";

	private VillageFigure figure = new VillageFigure();
	/** How close the player gets before this body's hero is worth asking for. */
	private static final int PREFETCH_RANGE = 4;

	private HeroClass heroClass = HeroClass.WARRIOR;
	private Hero echoHero;

	{
		state = PASSIVE;
		properties.add(Property.IMMOVABLE);
		spriteClass = VillageEchoSprite.class;
	}

	/** For the bundle loader, which restores the figure straight after. */
	public VillageEcho() {
		super();
	}

	public VillageEcho(VillageFigure figure) {
		super();
		setFigure(figure);
	}

	public VillageFigure figure() {
		return figure;
	}

	public HeroClass heroClass() {
		return heroClass;
	}

	private void setFigure(VillageFigure figure) {
		this.figure = figure == null ? new VillageFigure() : figure;
		this.heroClass = resolveHeroClass(this.figure.heroClass);
		this.HT = Math.max(1, this.figure.ht);
		// Whole, whatever state the run ended in: a figure standing in town is
		// being honoured, not being shown the wound that finished it.
		this.HP = this.HT;
	}

	/**
	 * The class to draw. A figure uploaded by a newer build may name a class this
	 * one has never heard of; it still gets to stand there, as a warrior, rather
	 * than being dropped from the village.
	 */
	private static HeroClass resolveHeroClass(String name) {
		if (name == null) {
			return HeroClass.WARRIOR;
		}
		HeroClass[] classes = HeroClass.values();
		for (int i = 0; i < classes.length; i++) {
			if (classes[i].name().equals(name)) {
				return classes[i];
			}
		}
		return HeroClass.WARRIOR;
	}

	@Override
	public CharSprite sprite() {
		return new VillageEchoSprite(heroClass, figure.armorTier);
	}

	/** A player's name, not a translatable string. */
	@Override
	public String name() {
		if (figure.userName == null || figure.userName.isEmpty()) {
			return Messages.get(this, "name");
		}
		return figure.userName;
	}

	@Override
	public String description() {
		StringBuilder text = new StringBuilder(Messages.get(this, "desc"));
		for (int i = 0; i < figure.badges.size(); i++) {
			String line = badgeLine(figure.badges.get(i));
			if (line != null) {
				text.append("\n\n").append(line);
			}
		}
		return text.toString();
	}

	/**
	 * One badge's sentence, or null when this build does not know the kind — a
	 * newer server may name badges this one has no wording for, and a missing
	 * line reads better than the missing-key marker.
	 */
	private String badgeLine(VillageFigure.Badge badge) {
		String key = "badge_" + badge.kind.replace('-', '_');
		String line = badge.hasCount()
				? Messages.get(this, key, badge.count)
				: Messages.get(this, key);
		if (line == null || line.startsWith("!!!")) {
			return null;
		}
		return line;
	}

	@Override
	public Hero getEchoHero() {
		return echoHero;
	}

	/** Called when this figure's data bundle arrives and has been restored. */
	public void setEchoHero(Hero echoHero) {
		this.echoHero = echoHero;
	}

	/**
	 * Null: the village broadcast carries facts, not echo metadata, and the
	 * bundle that would carry it is only fetched to build the hero above.
	 */
	@Override
	public Echo getEcho() {
		return null;
	}

	@Override
	public int defenseSkill(Char enemy) {
		return INFINITE_EVASION;
	}

	@Override
	protected Char chooseEnemy() {
		return null;
	}

	@Override
	public void damage(int dmg, Object src) {
		// standing figures are scenery; nothing in town can hurt them
	}

	@Override
	public boolean add(Buff buff) {
		return false;
	}

	@Override
	public boolean reset() {
		return true;
	}

	@Override
	protected boolean act() {
		// Fetched on approach rather than on the click: the round trip then
		// finishes while the player is still walking over, so the window opens
		// with the kit and talents already in it instead of filling in late.
		if (echoHero == null && Dungeon.hero != null && Dungeon.level != null
				&& Dungeon.level.distance(pos, Dungeon.hero.pos) <= PREFETCH_RANGE) {
			VillageEchoBundles.request(this);
		}
		// stand still and keep the turn moving
		spend(TICK);
		return true;
	}

	/**
	 * A figure leaves town because the standings changed, never because it was
	 * killed. {@code Mob.destroy()} hands the local player a kill, the
	 * monsters-slain badge, a bestiary entry and experience whenever the body is
	 * {@code ENEMY}; today a town figure never is, but that is an accident of
	 * {@link NPC}'s constructor rather than a promise this class makes. Say it
	 * here, where it matters, so no later change to the figures can turn a
	 * leaderboard push into a badge farm.
	 */
	@Override
	public void destroy() {
		alignment = Alignment.NEUTRAL;
		super.destroy();
	}

	@Override
	public boolean interact(Char c) {
		if (c != Dungeon.hero) {
			return true;
		}
		// Backstop for a body reached without the approach ever firing — a
		// teleport, or a figure that arrived under the player's feet.
		VillageEchoBundles.request(this);
		Game.runOnRenderThread(() -> GameScene.show(WndEchoBossInfo.windowFor(VillageEcho.this)));
		return true;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(FIGURE_POST, figure.post.name());
		bundle.put(FIGURE_DEPTH, figure.depth);
		bundle.put(FIGURE_ECHO_ID, figure.echoId == null ? "" : figure.echoId);
		bundle.put(FIGURE_USER_NAME, figure.userName == null ? "" : figure.userName);
		bundle.put(FIGURE_HERO_CLASS, heroClass.name());
		bundle.put(FIGURE_ARMOR_TIER, figure.armorTier);
		bundle.put(FIGURE_LVL, figure.lvl);
		bundle.put(FIGURE_HP, figure.hp);
		bundle.put(FIGURE_HT, figure.ht);
		bundle.put(FIGURE_KILLS, figure.killCount);
		bundle.put(FIGURE_TIMESTAMP, figure.timestamp);

		String[] kinds = new String[figure.badges.size()];
		int[] counts = new int[figure.badges.size()];
		for (int i = 0; i < figure.badges.size(); i++) {
			kinds[i] = figure.badges.get(i).kind;
			counts[i] = figure.badges.get(i).count;
		}
		bundle.put(FIGURE_BADGE_KINDS, kinds);
		bundle.put(FIGURE_BADGE_COUNTS, counts);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);

		VillageFigure restored = new VillageFigure();
		restored.post = restorePost(bundle.getString(FIGURE_POST));
		restored.depth = bundle.getInt(FIGURE_DEPTH);
		restored.echoId = emptyToNull(bundle.getString(FIGURE_ECHO_ID));
		restored.userName = emptyToNull(bundle.getString(FIGURE_USER_NAME));
		restored.heroClass = bundle.getString(FIGURE_HERO_CLASS);
		restored.armorTier = bundle.getInt(FIGURE_ARMOR_TIER);
		restored.lvl = bundle.getInt(FIGURE_LVL);
		restored.hp = bundle.getInt(FIGURE_HP);
		restored.ht = bundle.getInt(FIGURE_HT);
		restored.killCount = bundle.getInt(FIGURE_KILLS);
		restored.timestamp = bundle.getLong(FIGURE_TIMESTAMP);

		String[] kinds = bundle.getStringArray(FIGURE_BADGE_KINDS);
		int[] counts = bundle.getIntArray(FIGURE_BADGE_COUNTS);
		if (kinds != null) {
			ArrayList<VillageFigure.Badge> badges = restored.badges;
			for (int i = 0; i < kinds.length; i++) {
				int count = counts != null && i < counts.length
						? counts[i]
						: VillageFigure.Badge.NO_COUNT;
				badges.add(new VillageFigure.Badge(kinds[i], count));
			}
		}

		int ht = this.HT;
		setFigure(restored);
		// super restored the mob's own health; the figure's is the same number
		// unless an older bundle predates the fields, in which case keep theirs
		if (restored.ht <= 0) {
			this.HT = ht;
			this.HP = ht;
		}
	}

	private static VillageFigure.Post restorePost(String name) {
		if (VillageFigure.Post.DEPTH.name().equals(name)) {
			return VillageFigure.Post.DEPTH;
		}
		return VillageFigure.Post.MENTION;
	}

	private static String emptyToNull(String value) {
		return value == null || value.isEmpty() ? null : value;
	}
}
