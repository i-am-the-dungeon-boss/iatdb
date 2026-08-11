package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.levels.HouseLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.VillageLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.HighGrass;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ShadowCaster;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class VillageLevelBuildTest {

    @BeforeEach
    void onGroundLevel() {
        Dungeon.echoPlayMode = EchoPlayMode.SOLO;
        Dungeon.depth = VillageLevel.VILLAGE_DEPTH;
        Dungeon.branch = 0;
    }

    private VillageLevel village() {
        VillageLevel level = new VillageLevel();
        level.create();
        return level;
    }

    private HouseLevel house() {
        Dungeon.branch = VillageLevel.HOUSE_BRANCH;
        HouseLevel level = new HouseLevel();
        level.create();
        Dungeon.branch = 0;
        return level;
    }

    @Test
    @DisplayName("The village builds at its declared size and starts fully revealed")
    void villageBuildsRevealed() {
        VillageLevel level = village();

        Assertions.assertThat(level.width()).isEqualTo(VillageLevel.SIZE);
        Assertions.assertThat(level.height()).isEqualTo(VillageLevel.SIZE);
        Assertions.assertThat(level.mapped).containsOnly(true);
        Assertions.assertThat(level.visited).containsOnly(true);
    }

    @Test
    @DisplayName("The dungeon mouth is the village's only way out, and it leads to depth 1")
    void villageTransitionsAreWired() {
        VillageLevel level = village();

        LevelTransition toDungeon = level.getTransition(LevelTransition.Type.REGULAR_EXIT);
        Assertions.assertThat(toDungeon).isNotNull();
        Assertions.assertThat(toDungeon.destDepth).isEqualTo(1);
        Assertions.assertThat(toDungeon.destBranch).isZero();
        Assertions.assertThat(level.map[toDungeon.cell()]).isEqualTo(Terrain.EXIT);

        // the house is built and still routes, but nothing here opens onto it
        Assertions.assertThat(level.transitions).hasSize(1);
    }

    @Test
    @DisplayName("The house, though unreachable for now, still leads back to the village")
    void houseReturnsToVillage() {
        HouseLevel level = house();

        LevelTransition back = level.getTransition(LevelTransition.Type.BRANCH_EXIT);
        Assertions.assertThat(back).isNotNull();
        Assertions.assertThat(back.destDepth).isEqualTo(VillageLevel.VILLAGE_DEPTH);
        Assertions.assertThat(back.destBranch).isZero();
        // the doorway triggers it; the hero comes home to the tile inside it
        Assertions.assertThat(level.map[level.doorCell()]).isEqualTo(Terrain.DOOR);
        Assertions.assertThat(back.cell()).isEqualTo(level.doorstepCell());
    }

    @Test
    @DisplayName("The hero can walk from where they arrive to the dungeon mouth")
    void pathFromArrivalToDungeonIsWalkable() {
        VillageLevel level = village();

        Assertions.assertThat(reachable(level, level.arrivalCell(), level.dungeonEntrance()))
                .as("dungeon entrance reachable from the arrival point")
                .isTrue();
    }

    @Test
    @DisplayName("The village has no way up: no entrance stairs and no entrance transition")
    void villageHasNoStairsUp() {
        VillageLevel level = village();

        Assertions.assertThat(level.map).doesNotContain(Terrain.ENTRANCE);
        Assertions.assertThat(level.transitions)
                .extracting(transition -> transition.type)
                .doesNotContain(LevelTransition.Type.REGULAR_ENTRANCE,
                        LevelTransition.Type.SURFACE);
    }

    @Test
    @DisplayName("The hero arrives at the dungeon mouth, a step short of standing on it")
    void arrivalCellIsBesideTheDungeonMouth() {
        VillageLevel level = village();

        int arrival = level.arrivalCell();
        Assertions.assertThat(arrival).isNotEqualTo(level.dungeonEntrance());
        Assertions.assertThat(level.adjacent(arrival, level.dungeonEntrance())).isTrue();
        Assertions.assertThat(level.invalidHeroPos(arrival)).isFalse();
        Assertions.assertThat(level.passable[arrival]).isTrue();
        Assertions.assertThat(level.map[arrival]).isEqualTo(Terrain.EMPTY_SP);
    }

    @Test
    @DisplayName("Village grass is see-through, so nothing in town hides behind a hedge")
    void villageGrassDoesNotBlockSight() {
        VillageLevel level = village();

        int grass = anyGrassCell(level);
        Assertions.assertThat(level.losBlocking[grass]).isFalse();

        // and it stays see-through when the map is edited cell by cell
        Level.set(grass, Terrain.HIGH_GRASS, level);
        Assertions.assertThat(level.losBlocking[grass]).isFalse();
    }

    @Test
    @DisplayName("Walking through village grass leaves it standing and yields nothing")
    void villageGrassIsNeverTrampled() {
        VillageLevel level = village();
        Dungeon.level = level;

        int grass = anyGrassCell(level);
        HighGrass.trample(level, grass);

        Assertions.assertThat(level.map[grass]).isEqualTo(Terrain.HIGH_GRASS);
        Assertions.assertThat(level.heaps.valueList()).isEmpty();
        Assertions.assertThat(level.grassCanBeTrampled()).isFalse();
    }

    @Test
    @DisplayName("Dungeon grass still tramples: the village opt-out is village-only")
    void dungeonGrassStillTramples() {
        Assertions.assertThat(new SewerLevel().grassCanBeTrampled()).isTrue();
    }

    private int anyGrassCell(VillageLevel level) {
        for (int i = 0; i < level.length(); i++) {
            if (level.map[i] == Terrain.HIGH_GRASS) {
                return i;
            }
        }
        throw new AssertionError("the village grew no tall grass");
    }

    @Test
    @DisplayName("Neither ground level spawns monsters or a respawner")
    void groundLevelIsPeaceful() {
        VillageLevel village = village();
        Assertions.assertThat(village.createMob()).isNull();
        Assertions.assertThat(village.addRespawner()).isNull();

        HouseLevel house = house();
        Assertions.assertThat(house.createMob()).isNull();
        Assertions.assertThat(house.addRespawner()).isNull();
        Assertions.assertThat(house.mobs).isEmpty();
    }

    @Test
    @DisplayName("The house is always solo: nobody is ever generated inside it")
    void houseIsAlwaysSolo() {
        Assertions.assertThat(house().mobs).isEmpty();
    }

    @Test
    @DisplayName("Every transition drops the hero somewhere they can legally stand")
    void transitionCellsAreValidHeroPositions() {
        assertTransitionsStandable(village());
        assertTransitionsStandable(house());
    }

    /**
     * Dungeon.switchLevel falls back to getTransition(null).cell() when a saved
     * position is unusable, so a transition on invalid terrain gets the hero
     * placed there anyway. On the outermost ring that also stands them on a row
     * with no wall beyond it, and the shadowcast runs off the end of the map.
     */
    private void assertTransitionsStandable(Level level) {
        for (LevelTransition transition : level.transitions) {
            int cell = transition.cell();
            Assertions.assertThat(level.invalidHeroPos(cell))
                    .as("%s transition at cell %d of %s is not standable",
                            transition.type, cell, level.getClass().getSimpleName())
                    .isFalse();
            assertInterior(level, cell);
        }
        assertInterior(level, level.entrance());
    }

    /** Nothing the hero can occupy may sit on the outermost ring. */
    private void assertInterior(Level level, int cell) {
        int x = cell % level.width();
        int y = cell / level.width();
        Assertions.assertThat(x > 0 && x < level.width() - 1 && y > 0 && y < level.height() - 1)
                .as("cell (%d,%d) of %s must not be on the map border",
                        x, y, level.getClass().getSimpleName())
                .isTrue();
    }

    @Test
    @DisplayName("Field of view survives from every cell the hero can stand on")
    void fieldOfViewNeverEscapesTheMap() {
        assertFieldOfViewWorksEverywhere(village());
        assertFieldOfViewWorksEverywhere(house());
    }

    /**
     * On an out-of-bounds scan castShadow catches, reports and blanks the whole
     * field of view, so a blanked source cell is precisely the symptom of the
     * shadowcast leaving the map.
     */
    private void assertFieldOfViewWorksEverywhere(Level level) {
        boolean[] fov = new boolean[level.length()];
        int distance = level.viewDistance;
        for (int cell = 0; cell < level.length(); cell++) {
            if (level.invalidHeroPos(cell)) {
                continue;
            }
            ShadowCaster.castShadow(cell % level.width(), cell / level.width(),
                    level.width(), fov, level.losBlocking, distance);
            Assertions.assertThat(fov[cell])
                    .as("field of view from cell %d of %s escaped the map",
                            cell, level.getClass().getSimpleName())
                    .isTrue();
        }
    }

    /** Flood fill across passable cells. */
    private boolean reachable(Level level, int from, int to) {
        boolean[] seen = new boolean[level.length()];
        java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
        queue.add(from);
        seen[from] = true;
        int[] steps = { -1, 1, -level.width(), level.width() };
        while (!queue.isEmpty()) {
            int cell = queue.poll();
            if (cell == to) {
                return true;
            }
            for (int step : steps) {
                int next = cell + step;
                if (next < 0 || next >= level.length() || seen[next]) {
                    continue;
                }
                if (level.passable[next] || level.map[next] == Terrain.EXIT
                        || level.map[next] == Terrain.DOOR) {
                    seen[next] = true;
                    queue.add(next);
                }
            }
        }
        return false;
    }
}
