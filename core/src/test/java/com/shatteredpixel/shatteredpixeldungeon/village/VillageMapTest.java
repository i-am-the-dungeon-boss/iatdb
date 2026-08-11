package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndDungeonMode;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

@ExtendWith(GdxTestExtension.class)
class VillageMapTest {

    private VillageMap village() {
        return VillageMap.create(VillageMap.Area.VILLAGE);
    }

    private VillageMap house() {
        return VillageMap.create(VillageMap.Area.HOUSE);
    }

    @Test
    @DisplayName("Both areas build at their declared size")
    void areasBuildAtDeclaredSize() {
        Assertions.assertThat(village().width).isEqualTo(VillageMap.VILLAGE_SIZE);
        Assertions.assertThat(village().height).isEqualTo(VillageMap.VILLAGE_SIZE);
        Assertions.assertThat(house().width).isEqualTo(VillageMap.HOUSE_WIDTH);
        Assertions.assertThat(house().height).isEqualTo(VillageMap.HOUSE_HEIGHT);
    }

    @Test
    @DisplayName("Both areas are closed: nothing walkable touches the outer ring")
    void areasAreClosed() {
        assertClosed(village());
        assertClosed(house());
    }

    private void assertClosed(VillageMap map) {
        for (int x = 0; x < map.width; x++) {
            assertSolid(map, map.cell(x, 0));
            assertSolid(map, map.cell(x, map.height - 1));
        }
        for (int y = 0; y < map.height; y++) {
            assertSolid(map, map.cell(0, y));
            assertSolid(map, map.cell(map.width - 1, y));
        }
    }

    private void assertSolid(VillageMap map, int cell) {
        Assertions.assertThat(map.passable(cell))
                .as("border cell (%d,%d) of %s must not be walkable",
                        map.x(cell), map.y(cell), map.area)
                .isFalse();
    }

    @Test
    @DisplayName("The village arrival point, front door and dungeon entrance are all walkable")
    void villageLandmarksAreWalkable() {
        VillageMap map = village();
        Assertions.assertThat(map.passable(map.arrival())).isTrue();
        Assertions.assertThat(map.passable(map.door())).isTrue();
        Assertions.assertThat(map.passable(map.dungeonEntrance())).isTrue();
        Assertions.assertThat(map.tiles[map.dungeonEntrance()])
                .isEqualTo(VillageTerrain.DUNGEON_ENTRANCE);
    }

    @Test
    @DisplayName("The player can walk from where they arrive to the dungeon entrance")
    void dungeonEntranceIsReachable() {
        VillageMap map = village();
        List<Integer> route = map.route(map.arrival(), map.dungeonEntrance());
        Assertions.assertThat(route).isNotEmpty();
        Assertions.assertThat(route.get(route.size() - 1)).isEqualTo(map.dungeonEntrance());
    }

    @Test
    @DisplayName("The player can walk from where they arrive to their own front door")
    void frontDoorIsReachable() {
        VillageMap map = village();
        Assertions.assertThat(map.route(map.arrival(), map.door())).isNotEmpty();
    }

    @Test
    @DisplayName("Inside the house, the door back out is reachable from the arrival point")
    void houseDoorIsReachable() {
        VillageMap map = house();
        Assertions.assertThat(map.passable(map.arrival())).isTrue();
        Assertions.assertThat(map.route(map.arrival(), map.door())).isNotEmpty();
        Assertions.assertThat(map.dungeonEntrance()).isEqualTo(-1);
    }

    @Test
    @DisplayName("Routes never step diagonally or wrap around a row edge")
    void routesAreOrthogonalAndDoNotWrap() {
        VillageMap map = village();
        List<Integer> route = map.route(map.arrival(), map.dungeonEntrance());
        int previous = map.arrival();
        for (int cell : route) {
            int dx = Math.abs(map.x(cell) - map.x(previous));
            int dy = Math.abs(map.y(cell) - map.y(previous));
            Assertions.assertThat(dx + dy)
                    .as("step from %d to %d must be one orthogonal move", previous, cell)
                    .isEqualTo(1);
            previous = cell;
        }
    }

    @Test
    @DisplayName("There is no route into solid rock")
    void noRouteIntoWalls() {
        VillageMap map = village();
        Assertions.assertThat(map.route(map.arrival(), map.cell(0, 0))).isEmpty();
    }

    @Test
    @DisplayName("The house holds nobody: villagers only exist outdoors")
    void houseHasNoDungeonEntranceOrVillagers() {
        VillageMap map = house();
        for (int cell = 0; cell < map.length(); cell++) {
            Assertions.assertThat(map.tiles[cell])
                    .isNotEqualTo(VillageTerrain.DUNGEON_ENTRANCE);
        }
    }

    @Test
    @DisplayName("The dungeon prompt maps its options to play modes, and cancel to none")
    void promptOptionsMapToModes() {
        Assertions.assertThat(WndDungeonMode.modeForOption(WndDungeonMode.SOLO))
                .isEqualTo(EchoPlayMode.SOLO);
        Assertions.assertThat(WndDungeonMode.modeForOption(WndDungeonMode.RANKED))
                .isEqualTo(EchoPlayMode.RANKED);
        Assertions.assertThat(WndDungeonMode.modeForOption(WndDungeonMode.CANCEL)).isNull();
    }

    @Test
    @DisplayName("A run always starts in a real run slot")
    void runStartsInARunSlot() {
        Assertions.assertThat(VillageGateway.runSlot())
                .isBetween(1, GamesInProgress.MAX_SLOTS);
    }
}
