package com.shatteredpixel.shatteredpixeldungeon.levels.rooms.sewerboss;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.watabou.utils.Point;

public class EchoBossRoom extends GooBossRoom {
	
	@Override
	public void paint(Level level) {
		Painter.fill( level, this, Terrain.WALL );
		
		Painter.fillDiamond( level, this, 1, Terrain.EMPTY);
		
		for (Door door : connected.values()) {
			door.set( Door.Type.REGULAR );
			Point dir;
			if (door.x == left){
				dir = new Point(1, 0);
			} else if (door.y == top){
				dir = new Point(0, 1);
			} else if (door.x == right){
				dir = new Point(-1, 0);
			} else {
				dir = new Point(0, -1);
			}
			
			Point curr = new Point(door);
			do {
				Painter.set(level, curr, Terrain.EMPTY_SP);
				curr.x += dir.x;
				curr.y += dir.y;
			} while (level.map[level.pointToCell(curr)] == Terrain.WALL);
		}
		
		Painter.fill( level, left + width()/2 - 1, top + height()/2 - 2, 2 + width()%2, 4 + height()%2, Terrain.EMPTY_SP);
		Painter.fill( level, left + width()/2 - 2, top + height()/2 - 1, 4 + width()%2, 2 + height()%2, Terrain.EMPTY_SP);
		
		Echo echo = Dungeon.getPendingEcho();
		if (echo != null && echo.hasCombatData()) {
			EchoBoss boss = new EchoBoss(echo, Dungeon.depth);
			boss.pos = level.pointToCell(center());
			level.mobs.add(boss);
		} else {
			Goo goo = new Goo();
			goo.pos = level.pointToCell(center());
			level.mobs.add(goo);
		}
	}
	
	@Override
	public boolean canPlaceWater(Point p) {
		return false;
	}
}
