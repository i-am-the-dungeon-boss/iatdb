package com.shatteredpixel.shatteredpixeldungeon.village;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoPlayMode;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger;
import com.shatteredpixel.shatteredpixeldungeon.levels.HouseLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.VillageLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.watabou.utils.Bundle;

import java.io.IOException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GdxTestExtension.class)
class VillageSessionTest {

    @AfterEach
    void clearQuestState() {
        Ghost.Quest.reset();
        GamesInProgress.selectedClass = null;
        InterlevelScene.mode = InterlevelScene.Mode.NONE;
    }

    @Test
    @DisplayName("Being in the village is decided by the avatar, not the depth")
    void inVillageTruthTable() {
        Dungeon.hero = null;
        Assertions.assertThat(VillageSession.inVillage()).isFalse();

        Dungeon.hero = new Hero();
        Assertions.assertThat(VillageSession.inVillage()).isFalse();

        Dungeon.hero = new VillageHero(HeroClass.WARRIOR);
        Assertions.assertThat(VillageSession.inVillage()).isTrue();
    }

    @Test
    @DisplayName("Village setup leaves dungeon quest state alone")
    void prepareGlobalsDoesNotResetQuests() {
        Ghost.Quest.weapon = new Dagger();

        VillageSession.prepareGlobals();

        Assertions.assertThat(Ghost.Quest.weapon).isNotNull();
    }

    @Test
    @DisplayName("Starting a run does reset dungeon quest state — the behaviour town must not share")
    void dungeonInitDoesResetQuests() {
        GamesInProgress.selectedClass = HeroClass.WARRIOR;
        Ghost.Quest.weapon = new Dagger();

        Dungeon.init();

        Assertions.assertThat(Ghost.Quest.weapon).isNull();
    }

    @Test
    @DisplayName("Starting a run grants starting gear — the behaviour town must not share")
    void dungeonInitGrantsStartingGear() {
        GamesInProgress.selectedClass = HeroClass.WARRIOR;

        Dungeon.init();

        Assertions.assertThat(Dungeon.hero.belongings.armor()).isInstanceOf(ClothArmor.class);
    }

    @Test
    @DisplayName("Village setup produces an empty-handed town avatar on depth 0")
    void prepareGlobalsBuildsTheTownAvatar() {
        GamesInProgress.selectedClass = HeroClass.HUNTRESS;

        VillageSession.prepareGlobals();

        Assertions.assertThat(Dungeon.hero).isInstanceOf(VillageHero.class);
        Assertions.assertThat(Dungeon.hero.belongings.armor()).isNull();
        Assertions.assertThat(Dungeon.hero.belongings.backpack.items).isEmpty();
        Assertions.assertThat(Dungeon.depth).isEqualTo(VillageLevel.VILLAGE_DEPTH);
        Assertions.assertThat(Dungeon.branch).isZero();
        Assertions.assertThat(Dungeon.gold).isZero();
        Assertions.assertThat(Dungeon.energy).isZero();
        Assertions.assertThat(Dungeon.challenges).isZero();
        Assertions.assertThat(Dungeon.easyMode).isFalse();
        Assertions.assertThat(Dungeon.daily).isFalse();
        Assertions.assertThat(Actor.all()).isEmpty();
    }

    @Test
    @DisplayName("Village setup still initialises the item appearance tables")
    void prepareGlobalsKeepsAppearanceTables() {
        // a fresh install that boots straight into town has nothing else to
        // set these up, and every one of them NPEs when unset
        Scroll.clearLabels();
        Potion.clearColors();
        Ring.clearGems();

        VillageSession.prepareGlobals();

        Assertions.assertThatCode(() -> {
            Scroll.save(new Bundle());
            Potion.save(new Bundle());
            Ring.save(new Bundle());
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Idling in town never inflates the run clock")
    void runDurationExcludesTheVillage() {
        Assertions.assertThat(Actor.countsTowardRunDuration(null, null)).isFalse();
        Assertions.assertThat(Actor.countsTowardRunDuration(new VaultLevel(), new Hero())).isFalse();
        Assertions.assertThat(
                Actor.countsTowardRunDuration(new VillageLevel(), new VillageHero(HeroClass.WARRIOR)))
                .isFalse();
        Assertions.assertThat(Actor.countsTowardRunDuration(new VillageLevel(), new Hero())).isTrue();
    }

    @Test
    @DisplayName("Village setup keeps run statistics clean")
    void prepareGlobalsResetsStatistics() {
        Statistics.duration = 500f;

        VillageSession.prepareGlobals();

        Assertions.assertThat(Statistics.duration).isZero();
    }

    @Test
    @DisplayName("Entering the village routes through its own loading mode, in the solo slot")
    void prepareVillageEntrySetsUpTheRoute() {
        VillageGateway.prepareVillageEntry();

        Assertions.assertThat(InterlevelScene.mode).isEqualTo(InterlevelScene.Mode.VILLAGE);
        Assertions.assertThat(GamesInProgress.curSlot).isEqualTo(VillageGateway.VILLAGE_SLOT);
        Assertions.assertThat(GamesInProgress.selectedEchoPlayMode)
                .isEqualTo(VillageGateway.villageStorageMode());
        Assertions.assertThat(GamesInProgress.selectedClass).isNotNull();
        Assertions.assertThat(Dungeon.hero).isNull();
        Assertions.assertThat(Dungeon.level).isNull();
    }

    @Test
    @DisplayName("Entering the village lands a town avatar on the ground level")
    void enterBuildsTheGroundLevel() throws IOException {
        VillageGateway.prepareVillageEntry();

        VillageSession.enter();

        Assertions.assertThat(Dungeon.level).isInstanceOf(VillageLevel.class);
        Assertions.assertThat(VillageSession.inVillage()).isTrue();
        Assertions.assertThat(Dungeon.hero.pos).isBetween(0, Dungeon.level.length() - 1);
        Assertions.assertThat(Actor.all()).contains(Dungeon.hero);
        Assertions.assertThat(Statistics.duration).isZero();

        VillageGateway.discardStoredVillage();
    }

    @Test
    @DisplayName("Committing to a run leaves the village behind entirely")
    void beginRunDropsTheTown() {
        VillageSession.prepareGlobals();
        GamesInProgress.selectedClass = HeroClass.WARRIOR;

        try {
            VillageGateway.beginRun(EchoPlayMode.SOLO);
        } catch (NullPointerException noRunningGame) {
            // the trailing scene switch needs a live Game; every state change
            // under test has already happened by then
        }

        // hero == null is what makes InterlevelScene.descend() run Dungeon.init()
        Assertions.assertThat(Dungeon.hero).isNull();
        Assertions.assertThat(Dungeon.level).isNull();
        Assertions.assertThat(GamesInProgress.selectedClass).isNull();
        Assertions.assertThat(GamesInProgress.curSlot).isNotEqualTo(VillageGateway.VILLAGE_SLOT);
        Assertions.assertThat(VillageGateway.villageExists()).isFalse();
    }

    @Test
    @DisplayName("Leaving town drops the avatar, its level, its actors and its quickslots")
    void leaveClearsEverythingTheVillageOwned() throws IOException {
        VillageGateway.prepareVillageEntry();
        VillageSession.enter();
        Dungeon.quickslot.setSlot(0, new Dagger());

        VillageSession.leave();

        Assertions.assertThat(Dungeon.hero).isNull();
        Assertions.assertThat(Dungeon.level).isNull();
        Assertions.assertThat(Actor.all()).isEmpty();
        Assertions.assertThat(Dungeon.quickslot.getItem(0)).isNull();
        Assertions.assertThat(VillageGateway.villageExists()).isFalse();
        Assertions.assertThat(VillageSession.inVillage()).isFalse();
    }

    @Test
    @DisplayName("Leaving is a no-op outside the village, so it cannot wipe a live run")
    void leaveDoesNothingToADungeonRun() {
        GamesInProgress.selectedClass = HeroClass.WARRIOR;
        Dungeon.init();
        Hero runHero = Dungeon.hero;

        VillageSession.leave();

        Assertions.assertThat(Dungeon.hero).isSameAs(runHero);
    }

    @Test
    @DisplayName("The dungeon hero shares nothing with the town avatar")
    void descendingBuildsAWhollySeparateHero() {
        VillageSession.prepareGlobals();
        Hero townAvatar = Dungeon.hero;
        Dagger townItem = new Dagger();
        townAvatar.belongings.backpack.items.add(townItem);
        Dungeon.quickslot.setSlot(0, townItem);

        try {
            VillageGateway.beginRun(EchoPlayMode.SOLO);
        } catch (NullPointerException noRunningGame) {
            // only the trailing scene switch needs a live Game
        }
        // what InterlevelScene.descend() does once hero == null
        GamesInProgress.selectedClass = HeroClass.WARRIOR;
        Dungeon.init();

        Assertions.assertThat(Dungeon.hero).isNotSameAs(townAvatar);
        Assertions.assertThat(Dungeon.hero).isNotInstanceOf(VillageHero.class);
        Assertions.assertThat(Dungeon.hero.belongings).isNotSameAs(townAvatar.belongings);
        Assertions.assertThat(Dungeon.hero.belongings.backpack.items).doesNotContain(townItem);
        Assertions.assertThat(Dungeon.hero.belongings.armor()).isInstanceOf(ClothArmor.class);
        Assertions.assertThat(Dungeon.quickslot.getItem(0)).isNotSameAs(townItem);
        Assertions.assertThat(Actor.all()).doesNotContain(townAvatar);
    }

    @Test
    @DisplayName("A pending item-display refresh survives the hero being cleared")
    void itemDisplaysDoNotRefreshWithoutAHero() {
        // leaving town clears Dungeon.hero while GameScene is still the live
        // scene, so a refresh queued in the village used to land on a null hero
        GameScene.updateItemDisplays = true;
        VillageSession.prepareGlobals();
        VillageSession.leave();

        Assertions.assertThat(Dungeon.hero).isNull();
        Assertions.assertThat(GameScene.refreshItemDisplays()).isFalse();
        Assertions.assertThat(GameScene.updateItemDisplays)
                .as("the request stays pending for the next hero")
                .isTrue();

        GameScene.updateItemDisplays = false;
    }

    @Test
    @DisplayName("You can always walk back out of the house, keyboard included")
    void theHouseCanBeLeftByMovingTowardsTheDoor() {
        // the house is not reachable from the village for now, but it still
        // has to be leavable whenever it is wired back up
        HouseLevel house = new HouseLevel();
        Dungeon.branch = VillageLevel.HOUSE_BRANCH;
        house.create();
        Dungeon.level = house;
        Dungeon.branch = 0;

        LevelTransition out = house.getTransition(LevelTransition.Type.BRANCH_EXIT);
        Dungeon.hero = new VillageHero(HeroClass.WARRIOR);
        Dungeon.hero.pos = out.cell();

        // the hero stands inside the room, not in the doorway: keyboard movement
        // can only ever aim at a neighbouring cell, never at the one underfoot
        Assertions.assertThat(Dungeon.hero.pos).isEqualTo(house.doorstepCell());
        Assertions.assertThat(Dungeon.hero.pos).isNotEqualTo(house.doorCell());
        Assertions.assertThat(house.adjacent(Dungeon.hero.pos, house.doorCell())).isTrue();

        // ...so pressing towards the door leaves, rather than walking into it
        Dungeon.hero.handle(house.doorCell());
        Assertions.assertThat(Dungeon.hero.curAction)
                .isInstanceOf(HeroAction.LvlTransition.class);

        // and the doorstep stays ordinary ground to walk over
        Assertions.assertThat(house.getTransition(house.doorstepCell())).isNull();
    }

    @Test
    @DisplayName("Climbing out of depth 1 saves the run and walks home without the dungeon hero")
    void returningFromDepthOneKeepsTheRunAndDropsTheHero() throws IOException {
        GamesInProgress.selectEchoPlayMode(EchoPlayMode.SOLO);
        GamesInProgress.curSlot = 1;
        GamesInProgress.selectedClass = HeroClass.HUNTRESS;
        Dungeon.init();
        Dungeon.depth = 1;
        Dungeon.switchLevel(Dungeon.newLevel(), -1);
        Hero runHero = Dungeon.hero;

        boolean walkedHome = VillageGateway.prepareReturnToVillage();

        Assertions.assertThat(walkedHome).isTrue();
        // the run is kept, not abandoned: it can be resumed from the mouth later
        Assertions.assertThat(GamesInProgress.gameExists(1)).isTrue();
        // ...but the dungeon hero stays behind
        Assertions.assertThat(Dungeon.hero).isNull();
        Assertions.assertThat(Dungeon.level).isNull();
        Assertions.assertThat(InterlevelScene.mode).isEqualTo(InterlevelScene.Mode.VILLAGE);
        Assertions.assertThat(GamesInProgress.curSlot).isEqualTo(VillageGateway.VILLAGE_SLOT);
        // and the town avatar wears the class that was being played
        Assertions.assertThat(GamesInProgress.selectedClass).isEqualTo(runHero.heroClass);

        Dungeon.deleteGame(1, true);
    }

    @Test
    @DisplayName("The village loading screen has a label of its own")
    void villageLoadingLabelExists() {
        String label = Messages.get(InterlevelScene.Mode.class, InterlevelScene.Mode.VILLAGE.name());

        Assertions.assertThat(label).doesNotContain(Messages.NO_TEXT_FOUND.substring(0, 3));
    }

}
