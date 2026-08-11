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

package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.ExitButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndDungeonMode;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.PointF;

import java.util.ArrayList;
import java.util.List;

/**
 * The ground level: the village, the player's house, and the way into the
 * dungeon.
 *
 * <p>This scene owns everything it draws. It never touches {@code Dungeon},
 * {@code Hero}, {@code Level} or a run save — the dungeon does not exist yet
 * while the player is here, and is only created when they answer the prompt at
 * the dungeon entrance.
 */
public class VillageScene extends PixelScene {

	private static final float DEFAULT_ZOOM = 2f;

	private VillageMap map;
	private VillageTilemap tiles;
	private VillageAvatar avatar;
	private Group npcGroup;
	private final List<VillageNpc> npcs = new ArrayList<>();

	/** Set when the avatar has been sent somewhere that triggers on arrival. */
	private int pendingTarget = -1;

	@Override
	public void create() {
		super.create();

		map = VillageMap.create(VillageSave.area());
		playAreaMusic();

		Group terrain = new Group();
		add(terrain);

		tiles = new VillageTilemap(Assets.Environment.TILES_CITY, map);
		terrain.add(tiles);

		npcGroup = new Group();
		add(npcGroup);
		if (map.area == VillageMap.Area.VILLAGE) {
			addVillagers();
		}

		int start = startCell();
		avatar = new VillageAvatar(map, VillageSave.look(), start);
		add(avatar);
		VillageSave.setPosition(map.area, start);

		setupCamera();

		add(new PointerArea(0, 0, Camera.main.width, Camera.main.height) {
			@Override
			protected void onClick(PointerEvent event) {
				VillageScene.this.onClick(event);
			}
		});

		ExitButton exit = new ExitButton();
		exit.setPos(Camera.main.width - exit.width(), 0);
		add(exit);

		fadeIn();
	}

	private void playAreaMusic() {
		Music.INSTANCE.play(
				map.area == VillageMap.Area.HOUSE ? Assets.Music.CITY_2 : Assets.Music.CITY_1,
				true);
	}

	/** Where the avatar stands on entry: a remembered cell, or the arrival point. */
	private int startCell() {
		int saved = VillageSave.cell();
		if (VillageSave.area() == map.area && map.passable(saved)) {
			return saved;
		}
		return map.arrival();
	}

	private void addVillagers() {
		addVillager(VillageNpc.Kind.KEEPER, map.cell(24, 9));
		addVillager(VillageNpc.Kind.SMITH, map.cell(11, 19));
		addVillager(VillageNpc.Kind.SAGE, map.cell(17, 15));
		addVillager(VillageNpc.Kind.ELDER, map.cell(10, 25));
	}

	private void addVillager(VillageNpc.Kind kind, int cell) {
		VillageNpc npc = new VillageNpc(map, kind, cell);
		npcs.add(npc);
		npcGroup.add(npc);
	}

	private void setupCamera() {
		Camera.main.zoom(DEFAULT_ZOOM);
		Camera.main.snapTo(
				VillageTilemap.cellX(map, avatar.cell()) + VillageTilemap.SIZE / 2f,
				VillageTilemap.cellY(map, avatar.cell()) + VillageTilemap.SIZE / 2f);
		Camera.main.panFollow(avatar, 5f);
	}

	private void onClick(PointerEvent event) {
		if (Game.scene() != this || windowOpen()) {
			return;
		}
		PointF world = Camera.main.screenToCamera((int) event.current.x, (int) event.current.y);
		int cell = VillageTilemap.cellAt(map, world.x, world.y);
		if (cell < 0) {
			return;
		}

		VillageNpc npc = npcAt(cell);
		if (npc != null) {
			talkTo(npc);
			return;
		}

		pendingTarget = cell;
		avatar.walkTo(cell);
		if (!avatar.walking()) {
			pendingTarget = -1;
		}
	}

	/** True while a dialogue or the mode prompt is up. */
	private boolean windowOpen() {
		for (com.watabou.noosa.Gizmo member : members.toArray(new com.watabou.noosa.Gizmo[0])) {
			if (member instanceof Window) {
				return true;
			}
		}
		return false;
	}

	private VillageNpc npcAt(int cell) {
		for (VillageNpc npc : npcs) {
			if (npc.cell == cell) {
				return npc;
			}
		}
		return null;
	}

	private void talkTo(VillageNpc npc) {
		add(new WndTitledMessage(
				new com.watabou.noosa.Image(npc),
				Messages.titleCase(npc.npcName()),
				npc.chat()));
	}

	@Override
	public void update() {
		super.update();
		if (avatar == null || avatar.walking()) {
			return;
		}

		int cell = avatar.cell();
		VillageSave.setPosition(map.area, cell);

		if (pendingTarget != cell) {
			return;
		}
		pendingTarget = -1;

		if (cell == map.dungeonEntrance()) {
			add(new WndDungeonMode());
		} else if (cell == map.door()) {
			enterOtherArea();
		}
	}

	/** The front door: village to house, or house back to village. */
	private void enterOtherArea() {
		VillageMap.Area next = map.area == VillageMap.Area.VILLAGE
				? VillageMap.Area.HOUSE
				: VillageMap.Area.VILLAGE;
		VillageSave.setPosition(next, -1);
		VillageSave.save();
		Game.switchScene(VillageScene.class);
	}

	@Override
	public void destroy() {
		if (avatar != null) {
			VillageSave.setPosition(map.area, avatar.cell());
			VillageSave.save();
		}
		Camera.main.panFollow(null, 1f);
		super.destroy();
	}

	@Override
	protected void onBackPressed() {
		// leaving the village means leaving the game; let the exit button do it
	}
}
