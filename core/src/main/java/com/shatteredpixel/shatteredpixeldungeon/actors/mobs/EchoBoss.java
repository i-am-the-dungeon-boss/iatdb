package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invulnerability;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.action.EchoCombatBuffTransfer;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoHardStun;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss.EchoFightRecorder;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoInspectable;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoHeroSnapshot;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss.EchoLeaderboardStorage;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoAoeDots;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicyChoice;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicyMatcher;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicyStatus;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicyStatusBuilder;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoPolicyHazards;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoRoleExecutor;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.policy.EchoUntouchable;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.boss.EchoBossRegionalDeath;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.EchoBossSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Strings;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EchoBoss extends Mob implements EchoInspectable {

    public static final float BOSS_HP_MULTIPLIER = 1.3f;

    private static final String ECHO = "echo";
    private static final String ECHO_POLICY = "echo_policy";
    private static final String DISENGAGE_TURNS = "disengage_turns";
    private static final String SELF_SHIELD_PEAK = "self_shield_peak";

    {
        spriteClass = EchoBossSprite.class;

        HP = HT = 200;
        defenseSkill = 20;

        EXP = 20;
        maxLvl = 30;

        properties.add(Property.BOSS);
    }

    private static final int DOOR_STALL_BREAK_THRESHOLD = 2;
    /** Retreat-step scoring weight; see {@link #bestRetreatCell}. */
    private static final int PLANT_COVER_WEIGHT = 1000;
    /** Blind last-seen shots allowed after the hero cloaks. */
    private static final int BLIND_DEFENSE_SHOTS = 2;

    private Echo echo;
    private Hero echoHero;
    private EchoFightRecorder fightRecorder;
    private EchoPolicy echoPolicy;
    /** Recipe id → current step index (advanced when a recipe step executes). */
    private final Map<String, Integer> recipeSteps = new HashMap<>();
    /** Door the hero is dancing through; -1 if none. */
    private int doorStallCell = -1;
    private int doorStallCount = 0;
    private boolean doorStallPrevVisible = true;
    private boolean doorStallPrevInitialized = false;
    /** Remaining last-seen aims while the hero is invisible. */
    private int blindDefenseShotsLeft = BLIND_DEFENSE_SHOTS;
    /** Last cell the living hero attacked from (FOV / clear-bush focus). */
    private int lastAttackerPos = -1;
    /** Harmful plant / sheep / grass blocker cells sensed this turn (−1 none). */
    private int plantBlockerCell = -1;
    private int pathBlockerCell = -1;
    private int losBlockerCell = -1;
    /**
     * While true, pathing also refuses cells gas will spread onto next tick.
     * Off for ordinary CLOSE_IN / KEEP_DISTANCE steps: the growth ring around a
     * dense cloud usually spans the whole corridor, and blocking it there only
     * makes the echo shuffle sideways instead of closing. Leave-AoE exits and
     * blink landings opt in, since both pick from a scored candidate list and
     * can fall back when every growth-safe option is gone.
     */
    private boolean avoidPredictedGas = false;
    /**
     * Master-style throw/zap gate: set by {@link #busy()}, cleared when
     * {@link #spendAndNext(float)} runs from the VFX callback. While busy,
     * {@link #act()} returns false so Actor processing waits (like Hero.ready).
     */
    private boolean busy;
    /** When true, policy already deferred turn spend to the VFX callback. */
    private boolean vfxOwnsTurn;
    /**
     * Consecutive turns spent running from an untouchable hero. Bundled so a
     * reload cannot save-scum a fresh
     * {@link EchoUntouchable#MAX_DISENGAGE_TURNS} window.
     */
    private int disengageTurns = 0;
    /**
     * Highest temporary shield this echo has carried since its current shield
     * was last fully spent, for the SHIELD_SELF anti-stacking gate. Bundled
     * because the fight is on a sealed floor and a reload must not hand the echo
     * a free second shield.
     */
    private int selfShieldPeak = 0;
    /**
     * Prep roles already spent in the current untouchable window, so the Java
     * floor does not re-drink Haste every turn. Not bundled — worst case a
     * reload grants one extra prep.
     */
    private final Set<String> preppedThisWindow = new HashSet<>();

    @Override
    public Echo getEcho() {
        return echo;
    }

    public EchoPolicy getEchoPolicy() {
        return echoPolicy;
    }

    /** Debug/sandbox: swap the live policy (e.g. arsenal cycle). */
    public void replacePolicy(EchoPolicy policy) {
        if (policy == null || !policy.isSupported()) {
            throw new IllegalArgumentException("echo boss requires a supported echo_policy");
        }
        echoPolicy = policy;
        recipeSteps.clear();
    }

    /**
     * The single door to the phantom kit — {@code EchoActionContext.of} and
     * every UI reader come through here. Re-asserting the sprite mirror on the
     * way out is what makes it impossible to hand out a kit that has gone stale
     * against a body whose sprite was re-linked.
     */
    @Override
    public Hero getEchoHero() {
        mirrorKitSprite();
        return echoHero;
    }

    /**
     * The phantom kit is owned by exactly one body for its whole life, so it
     * mirrors that body's sprite rather than borrowing it per call — a borrow
     * any caller could forget was the source of ANDROID-20 / ANDROID-21.
     * Cleared together with the body's own slot by
     * {@link com.shatteredpixel.shatteredpixeldungeon.sprites.EchoBossSprite#destroy()}.
     */
    public void mirrorKitSprite() {
        if (echoHero != null) {
            echoHero.sprite = sprite;
        }
    }

    /** Drops the mirror when the sprite it points at is destroyed. */
    public void clearKitSprite() {
        if (echoHero != null) {
            echoHero.sprite = null;
        }
    }

    /**
     * BossHealthBar, WndInfoMob, and examine menus call this without
     * {@link CharSprite#link}; apply echo hero class/tier via linkVisuals.
     */
    @Override
    public CharSprite sprite() {
        CharSprite s = super.sprite();
        s.linkVisuals(this);
        return s;
    }

    /**
     * Bundle / reflection construction; state comes from
     * {@link #restoreFromBundle}.
     */
    public EchoBoss() {
        super();
    }

    public EchoBoss(Echo echo, int depth) {
        this(echo, depth, Dungeon.getPendingEchoPolicy());
    }

    public EchoBoss(Echo echo, int depth, EchoPolicy policy) {
        super();
        initFromEcho(echo, depth, policy, true);
    }

    /** Boss HT = captured HT × {@link #BOSS_HP_MULTIPLIER} × depth bonus. */
    public static int scaledHT(Echo echo, int depth) {
        if (echo == null)
            return 200;
        float depthBonus = 1f + depth * 0.02f;
        return Math.round(echo.ht * BOSS_HP_MULTIPLIER * depthBonus);
    }

    private void initFromEcho(Echo echo, int depth, EchoPolicy policy, boolean scaleHp) {
        if (echo == null || !echo.hasCombatData()) {
            throw new IllegalArgumentException("Echo boss requires echo with hero combat data");
        }
        if (policy == null || !policy.isSupported()) {
            throw new IllegalArgumentException("Echo boss requires a supported echo_policy");
        }
        this.echo = echo;
        echoPolicy = policy;
        fightRecorder = new EchoFightRecorder(new EchoLeaderboardStorage());
        echoHero = EchoHeroSnapshot.restoreHero(echo);
        if (echoHero == null) {
            throw new IllegalArgumentException("Echo boss requires restorable hero combat data");
        }
        EchoHeroSnapshot.refillCharges(echoHero);
        if (scaleHp) {
            HP = HT = scaledHT(echo, depth);
        }
        defenseSkill = echoHero.defenseSkill(null);
        EXP = Math.max(20, echo.lvl * 5);
        maxLvl = Math.max(30, echo.lvl);
    }

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(ECHO, echo.toBundle());
        bundle.put(ECHO_POLICY, echoPolicy.toBundle());
        bundle.put(DISENGAGE_TURNS, disengageTurns);
        bundle.put(SELF_SHIELD_PEAK, selfShieldPeak);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        // Stored echo + policy are authoritative; pending may be cleared or from
        // another fight.
        if (!bundle.contains(ECHO) || !bundle.contains(ECHO_POLICY)) {
            throw new IllegalArgumentException("Echo boss requires echo and echo_policy");
        }
        Echo stored = Echo.fromBundle(bundle.getBundle(ECHO));
        EchoPolicy policy = EchoPolicy.fromBundle(bundle.getBundle(ECHO_POLICY));
        initFromEcho(stored, Dungeon.depth, policy, false);
        // getInt gives 0 on saves written before the untouchable ladder existed.
        disengageTurns = bundle.getInt(DISENGAGE_TURNS);
        selfShieldPeak = bundle.getInt(SELF_SHIELD_PEAK);
        super.restoreFromBundle(bundle);
        if (state != SLEEPING) {
            BossHealthBar.assignBoss(this);
        }
    }

    public static void onHeroDeath() {
        if (!Dungeon.isEchoBossActive() || Dungeon.level == null) {
            return;
        }
        for (Char ch : Actor.chars()) {
            if (ch instanceof EchoBoss && ch.isAlive()) {
                ((EchoBoss) ch).recordPlayerDefeat();
                return;
            }
        }
    }

    private void recordPlayerDefeat() {
        fightRecorder.recordBossVictory(
                echo,
                Dungeon.depth,
                Dungeon.hero != null ? Dungeon.hero.heroClass : null,
                Game.version);
    }

    /**
     * Last cell where the player was seen (Mob hunting {@code target}).
     * Used for door-stall focus and limited blind-defense aim while cloaked —
     * not for CLOSE_IN / KEEP_DISTANCE movement.
     */
    public int lastSeenEnemyPos() {
        return target;
    }

    /** Records where the enemy was last seen. */
    public void noteEnemySeenAt(int cell) {
        target = cell;
    }

    /** Shots left that may aim at {@link #lastSeenEnemyPos()} while cloaked. */
    public int blindDefenseShotsLeft() {
        return blindDefenseShotsLeft;
    }

    /** Re-arms two blind last-seen shots (when the hero is visible again). */
    public void rearmBlindDefense() {
        blindDefenseShotsLeft = BLIND_DEFENSE_SHOTS;
    }

    /** Spends one blind last-seen shot after a successful cloak-time aim. */
    public void consumeBlindDefenseShot() {
        if (blindDefenseShotsLeft > 0) {
            blindDefenseShotsLeft--;
        }
    }

    /** Test seam for remaining blind-defense shots. */
    public void setBlindDefenseShotsLeftForTests(int shots) {
        blindDefenseShotsLeft = shots;
    }

    public int lastAttackerPos() {
        return lastAttackerPos;
    }

    public void noteAttackerAt(int cell) {
        if (cell >= 0) {
            lastAttackerPos = cell;
        }
    }

    /**
     * Focus for CLOSE_IN / CLEAR_LOS when hero is occluded: last attacker, else
     * last seen.
     */
    public int policyFocusCell() {
        if (lastAttackerPos >= 0) {
            return lastAttackerPos;
        }
        return lastSeenEnemyPos();
    }

    /** Consecutive turns already spent running from an untouchable hero. */
    public int disengageTurns() {
        return disengageTurns;
    }

    /**
     * Records this echo's own temporary shield for the SHIELD_SELF anti-stacking
     * gate. Called once per sense with
     * {@code EchoUntouchable.temporaryShielding(this)} so the echo's reading and
     * the hero's can never drift.
     */
    public void noteSelfShield(int current) {
        if (current <= 0) {
            // Fully spent: the next shield is judged on its own peak.
            selfShieldPeak = 0;
        } else if (current > selfShieldPeak) {
            selfShieldPeak = current;
        }
    }

    /**
     * True while the echo's own shield is still above half of its peak — burning
     * a second shielding item now would mostly overwrite the first.
     */
    public boolean hasMostlyIntactShield() {
        return EchoUntouchable.temporaryShielding(this) * 2 > selfShieldPeak;
    }

    public int plantBlockerCell() {
        return plantBlockerCell;
    }

    public int pathBlockerCell() {
        return pathBlockerCell;
    }

    public int losBlockerCell() {
        return losBlockerCell;
    }

    public void setPlantBlockerCell(int cell) {
        plantBlockerCell = cell;
    }

    public void setPathBlockerCell(int cell) {
        pathBlockerCell = cell;
    }

    public void setLosBlockerCell(int cell) {
        losBlockerCell = cell;
    }

    public int doorStallCell() {
        return doorStallCell;
    }

    public int doorStallCount() {
        return doorStallCount;
    }

    public boolean isDoorStalling() {
        return doorStallCount >= DOOR_STALL_BREAK_THRESHOLD
                && doorStallCell >= 0
                && Dungeon.level != null
                && doorStallCell < Dungeon.level.length()
                && isDoorTerrain(Dungeon.level.map[doorStallCell]);
    }

    /**
     * Call each turn with whether the hero is currently visible. Visibility
     * flips near a door accumulate stall pressure for door-break reactions.
     */
    public void noteDoorStallVisibility(boolean heroVisible) {
        if (!doorStallPrevInitialized) {
            doorStallPrevVisible = heroVisible;
            doorStallPrevInitialized = true;
            return;
        }
        if (heroVisible == doorStallPrevVisible) {
            return;
        }
        doorStallPrevVisible = heroVisible;
        int door = findRelevantDoor();
        if (door < 0) {
            return;
        }
        if (door == doorStallCell) {
            doorStallCount++;
        } else {
            doorStallCell = door;
            doorStallCount = 1;
        }
        debugAct("door stall cell=" + doorStallCell + " count=" + doorStallCount);
    }

    public void clearDoorStall() {
        doorStallCell = -1;
        doorStallCount = 0;
    }

    /** Door on/near last-seen (where door-dancing usually happens). */
    public int findRelevantDoor() {
        if (Dungeon.level == null) {
            return -1;
        }
        int focus = lastSeenEnemyPos();
        if (focus < 0 || focus >= Dungeon.level.length()) {
            if (Dungeon.hero != null) {
                focus = Dungeon.hero.pos;
            } else {
                return -1;
            }
        }
        if (isDoorTerrain(Dungeon.level.map[focus])) {
            return focus;
        }
        for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
            int cell = focus + PathFinder.NEIGHBOURS8[i];
            if (Dungeon.level.insideMap(cell) && isDoorTerrain(Dungeon.level.map[cell])) {
                return cell;
            }
        }
        return -1;
    }

    private static boolean isDoorTerrain(int terrain) {
        return terrain == Terrain.DOOR || terrain == Terrain.OPEN_DOOR;
    }

    /**
     * Policy movement: {@link Mob#getCloser} is protected; updates sprite like
     * hunting AI.
     */
    public boolean policyStepCloser(int cell) {
        int oldPos = pos;
        if (!getCloser(cell)) {
            return false;
        }
        moveSprite(oldPos, pos);
        return true;
    }

    /**
     * Policy movement: {@link Mob#getFurther} is protected; updates sprite like
     * hunting AI. While {@code preferPlantCover}
     * a candidate step that puts a harmful plant on the line back to
     * {@code enemyPos} is favored over one that merely maximizes distance.
     * <p>
     * The plant is real cover: {@code Level.pressCell} triggers it for the
     * hero too, so kiting behind one is a genuine deterrent, not just a
     * pathing quirk. Falls back to {@link #getFurther} when no candidate step
     * scores — e.g. every farther cell is a wall — so retreating never fails
     * just because cover happens to be unavailable.
     */
    public boolean policyStepFurther(int enemyPos, boolean preferPlantCover) {
        int retreat = bestRetreatCell(enemyPos, preferPlantCover);
        if (retreat >= 0 && policyStepTo(retreat)) {
            return true;
        }
        int oldPos = pos;
        if (!getFurther(enemyPos)) {
            return false;
        }
        moveSprite(oldPos, pos);
        return true;
    }

    /**
     * Best adjacent retreat cell away from {@code enemyPos}, or {@code -1} if
     * none scores. Lexicographic, widest term first: hazard-free and strictly
     * farther from the hero (a candidate cell must pass both to be considered
     * at all — see the loop below); then, only while kiting, whether a
     * harmful plant sits on the line from the candidate back to the hero;
     * then gas/hazard clearance, mirroring {@link EchoAoeDots#bestExit}'s
     * tiebreak.
     */
    private int bestRetreatCell(int enemyPos, boolean preferPlantCover) {
        if (Dungeon.level == null || enemyPos < 0 || !Dungeon.level.insideMap(enemyPos)) {
            return -1;
        }
        Level level = Dungeon.level;
        int current = level.distance(pos, enemyPos);
        int best = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
            int cell = pos + PathFinder.NEIGHBOURS8[i];
            if (!level.insideMap(cell) || !policyCellPathable(cell)) {
                continue;
            }
            if (level.distance(cell, enemyPos) <= current) {
                continue;
            }
            int score = 0;
            if (preferPlantCover && plantCoversLine(cell, enemyPos)) {
                score += PLANT_COVER_WEIGHT;
            }
            score += EchoAoeDots.gasClearance(this, cell, level);
            if (best < 0 || score > bestScore || (score == bestScore && cell < best)) {
                best = cell;
                bestScore = score;
            }
        }
        return best;
    }

    /** True when a harmful plant occupies any cell on the line from {@code from} to {@code to}. */
    private static boolean plantCoversLine(int from, int to) {
        Ballistica line = new Ballistica(from, to, Ballistica.PROJECTILE);
        for (int cell : line.path) {
            if (EchoAoeDots.isHarmfulPlantAt(cell)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Treat harmful AoE DoT cells as impassable for pathfinding (fire / toxic /
     * corrosive / …), except the cell currently occupied so leave-steps still
     * work.
     */
    @Override
    public boolean[] modifyPassable(boolean[] passable) {
        if (passable == null || Dungeon.level == null) {
            return passable;
        }
        for (int i = 0; i < passable.length; i++) {
            if (passable[i] && i != pos
                    && EchoAoeDots.isAoeHazardForPath(this, i, avoidPredictedGas)) {
                passable[i] = false;
            }
        }
        return passable;
    }

    /** Adjacent steps refuse current/predicted AoE and harmful plants. */
    @Override
    protected boolean cellIsPathable(int cell) {
        return super.cellIsPathable(cell)
                && !EchoAoeDots.isAoeHazardForPath(this, cell, avoidPredictedGas);
    }

    /** Exposes {@link Mob#cellIsPathable} for leave-AoE neighbour checks. */
    public boolean policyCellPathable(int cell) {
        return policyCellPathable(cell, avoidPredictedGas);
    }

    /** As {@link #policyCellPathable(int)} with an explicit growth-ring strictness. */
    public boolean policyCellPathable(int cell, boolean avoidGrowthRing) {
        boolean saved = avoidPredictedGas;
        avoidPredictedGas = avoidGrowthRing;
        try {
            return cellIsPathable(cell);
        } finally {
            avoidPredictedGas = saved;
        }
    }

    /**
     * One-cell step to {@code cell} (already validated). Updates sprite like
     * hunting AI.
     */
    public boolean policyStepTo(int cell) {
        if (!policyCellPathable(cell) || !Dungeon.level.adjacent(pos, cell)) {
            return false;
        }
        int oldPos = pos;
        move(cell);
        moveSprite(oldPos, pos);
        return true;
    }

    /**
     * Leave harmful AoE DoT if a safe neighbour exists. Prefers toward
     * {@code enemyPos} unless {@code kite} (maximize distance).
     */
    public boolean policyStepOutOfAoe(int enemyPos, boolean kite) {
        int step = EchoAoeDots.bestExit(this, enemyPos, kite);
        return step >= 0 && policyStepTo(step);
    }

    @Override
    public int damageRoll() {
        return withEchoHeroPosInt(echoHero::damageRoll);
    }

    @Override
    public int attackSkill(Char target) {
        return withEchoHeroPosInt(() -> echoHero.attackSkill(target));
    }

    @Override
    public int defenseSkill(Char enemy) {
        return withEchoHeroPosInt(() -> echoHero.defenseSkill(enemy));
    }

    @Override
    public boolean canSurpriseAttack() {
        return echoHero.canSurpriseAttack();
    }

    @Override
    public int drRoll() {
        return withEchoHeroPosInt(() -> echoHero.drRoll(this));
    }

    @Override
    public float attackDelay() {
        return withEchoHeroPos(echoHero::attackDelay);
    }

    @Override
    public float speed() {
        // Kit gear/talents; body potion buffs (Haste etc.) via alsoMoveBuffs.
        return withEchoHeroPos(() -> echoHero.combatSpeed(this));
    }

    @Override
    public int attackProc(final Char enemy, int damage) {
        return withEchoHeroPosInt(() -> echoHero.attackProc(enemy, damage));
    }

    @Override
    public int defenseProc(Char enemy, int damage) {
        return withEchoHeroPosInt(() -> {
            int dmg = echoHero.defenseProc(enemy, damage);
            Earthroot.Armor bodyRoot = buff(Earthroot.Armor.class);
            if (bodyRoot != null && echoHero.buff(Earthroot.Armor.class) == null) {
                dmg = bodyRoot.absorb(dmg);
            }
            return dmg;
        });
    }

    /** Local int supplier — RoboVM lacks {@code java.util.function.IntSupplier}. */
    private interface IntAction {
        int getAsInt();
    }

    /** Local value supplier — RoboVM lacks {@code java.util.function.Supplier}. */
    private interface ValueAction<T> {
        T get();
    }

    /**
     * Echo hero is never placed on the level; sync body combat fields onto the
     * kit for combat queries only. Borrows {@link #sprite} so Hero-shaped
     * enchant/glyph VFX do not NPE on a headless kit (Family A / ANDROID-1T).
     * Mirrors kit HP changes back onto the body (Vampiric / Metabolism).
     */
    private int withEchoHeroPosInt(IntAction action) {
        return withEchoHeroCombat(new ValueAction<Integer>() {
            @Override
            public Integer get() {
                return action.getAsInt();
            }
        });
    }

    private <T> T withEchoHeroPos(ValueAction<T> action) {
        return withEchoHeroCombat(action);
    }

    private <T> T withEchoHeroCombat(ValueAction<T> action) {
        int savedPos = echoHero.pos;
        int savedParalysed = echoHero.paralysed;
        Alignment savedAlignment = echoHero.alignment;
        int savedHp = echoHero.HP;
        int savedHt = echoHero.HT;
        boolean[] savedFov = echoHero.fieldOfView;

        echoHero.pos = pos;
        echoHero.paralysed = paralysed;
        // Sprite is mirrored, not lent — re-assert in case combat runs before
        // this body's first turn.
        mirrorKitSprite();
        echoHero.alignment = alignment;
        echoHero.HP = HP;
        echoHero.HT = HT;
        echoHero.fieldOfView = fieldOfView;
        copyGuaranteedHitTrackerToKit();
        HashSet<Buff> buffsBefore = EchoCombatBuffTransfer.snapshot(echoHero);
        try {
            return action.get();
        } finally {
            // Kit heals/damage during procs must land on the on-stage body.
            HP = Math.max(0, Math.min(HT, echoHero.HP));
            EchoCombatBuffTransfer.moveNewCombatBuffs(echoHero, this, buffsBefore);
            moveGuaranteedHitTrackerFromKit();
            echoHero.pos = savedPos;
            echoHero.paralysed = savedParalysed;
            echoHero.alignment = savedAlignment;
            echoHero.HP = savedHp;
            echoHero.HT = savedHt;
            echoHero.fieldOfView = savedFov;
        }
    }

    private void copyGuaranteedHitTrackerToKit() {
        if (buff(EchoHardStun.GuaranteedHitTracker.class) != null
                && echoHero.buff(EchoHardStun.GuaranteedHitTracker.class) == null) {
            Buff.affect(echoHero, EchoHardStun.GuaranteedHitTracker.class);
        }
    }

    private void moveGuaranteedHitTrackerFromKit() {
        EchoHardStun.GuaranteedHitTracker kitTracker =
                echoHero.buff(EchoHardStun.GuaranteedHitTracker.class);
        if (kitTracker == null) {
            return;
        }
        kitTracker.detach();
        if (buff(EchoHardStun.GuaranteedHitTracker.class) == null) {
            Buff.affect(this, EchoHardStun.GuaranteedHitTracker.class);
        }
    }

    @Override
    public void damage(int dmg, Object src) {
        // Hits reveal the Echo (cloak / potion invis). Always dispel on damage —
        // do not gate on invisible>0 in case the counter and buffs ever desync.
        if (dmg > 0) {
            Invisibility.dispel(this);
            revealSpriteAfterInvisibility();
        }
        if (dmg > 0 && src == Dungeon.hero) {
            fightRecorder.trackDamageTaken(dmg);
            if (Dungeon.hero.pos >= 0) {
                noteAttackerAt(Dungeon.hero.pos);
            }
        }
        int preHP = HP;
        super.damage(dmg, src);
        EchoBossRegionalDeath.onDamaged(this, src, dmg, Math.max(0, preHP - HP));
    }

    @Override
    public boolean attack(Char enemy, float dmgMulti, float dmgBonus, float accMulti) {
        if (enemy == Dungeon.hero) {
            int hpBefore = enemy.HP;
            boolean result = super.attack(enemy, dmgMulti, dmgBonus, accMulti);
            fightRecorder.trackDamageDealt(Math.max(0, hpBefore - enemy.HP));
            return result;
        }
        return super.attack(enemy, dmgMulti, dmgBonus, accMulti);
    }

    @Override
    protected boolean doAttack(Char enemy) {
        if (sprite != null && (sprite.visible || enemy.sprite.visible)) {
            sprite.attack(enemy.pos);
            return false;
        } else {
            boolean hit = attack(enemy);
            if (hit) {
                Invisibility.dispel(this);
                revealSpriteAfterInvisibility();
            }
            spend(attackDelay());
            return true;
        }
    }

    @Override
    public void onAttackComplete() {
        boolean hit = attack(enemy);
        // Miss / dodge must not break invisibility. Do not call Mob.onAttackComplete
        // (it always attacks again and always dispels).
        if (hit) {
            Invisibility.dispel(this);
            revealSpriteAfterInvisibility();
        }
        spend(attackDelay());
        next();
    }

    /**
     * EchoBossSprite fully un-renders while stealthed ({@code visible=false},
     * alpha 0). After dispel, force the INVISIBLE state off so the next sprite
     * update restores FOV visibility / alpha even if buff {@code fx(false)} was
     * skipped.
     */
    private void revealSpriteAfterInvisibility() {
        if (invisible > 0 || sprite == null) {
            return;
        }
        sprite.remove(CharSprite.State.INVISIBLE);
    }

    @Override
    protected void onAdd() {
        super.onAdd();
        // Phantom kit is never in Actor.chars(), so its Buffs (Wand.Charger,
        // ClassArmor.Charger, artifact recharge, Regeneration, …) are not
        // auto-scheduled. Register them so natural recharge matches the Hero.
        scheduleEchoKitBuffs();
    }

    @Override
    protected synchronized void onRemove() {
        unscheduleEchoKitBuffs();
        super.onRemove();
    }

    /**
     * Schedules every buff on the phantom echo hero into the global Actor
     * clock. Safe to call repeatedly — {@link Actor#add} no-ops duplicates.
     */
    public void scheduleEchoKitBuffs() {
        if (echoHero == null) {
            return;
        }
        for (Buff buff : echoHero.buffs().toArray(new Buff[0])) {
            Actor.add(buff);
        }
    }

    private void unscheduleEchoKitBuffs() {
        if (echoHero == null) {
            return;
        }
        for (Buff buff : echoHero.buffs().toArray(new Buff[0])) {
            Actor.remove(buff);
        }
    }

    @Override
    public void notice() {
        super.notice();
        if (!BossHealthBar.isAssigned()) {
            BossHealthBar.assignBoss(this);
            // Goo-style: seal on notice when the boss was placed at levelgen.
            // Caves/City/Halls already seal (and spawn) before notice — don't reseal.
            if (Dungeon.level != null && !Dungeon.level.locked) {
                Dungeon.level.seal();
            }
        }
    }

    /** Marks this boss waiting on throw/zap VFX (EchoActionContext busy gate). */
    public void busy() {
        busy = true;
        vfxOwnsTurn = true;
    }

    public boolean isBusy() {
        return busy;
    }

    /**
     * Drops a pending VFX/turn gate without spending time — refused Echo adapter
     * actions.
     */
    public void cancelBusy() {
        busy = false;
        vfxOwnsTurn = false;
    }

    /** Clears busy and advances actor time after a deferred throw/zap. */
    public void spendAndNext(float time) {
        busy = false;
        spend(time);
        next();
    }

    @Override
    protected boolean act() {
        // Re-assert the kit's sprite mirror in case the body's sprite was
        // replaced (level change, re-link) since the last turn.
        mirrorKitSprite();

        // Pick up kit buffs attached after onAdd (e.g. MeleeWeapon.Charger).
        scheduleEchoKitBuffs();

        // Wait for missile/zap callback — same pause pattern as Hero !ready.
        if (busy) {
            return false;
        }

        // Match Mob.act: paralysis / frost / magical sleep skip the whole turn
        // (including policy CLOSE_IN). Roots are handled by getCloser.
        if (paralysed > 0) {
            enemySeen = false;
            spend(TICK);
            debugAct("paralysed → skip turn");
            return true;
        }

        if (state != HUNTING) {
            debugAct("state=" + state + " → default mob act (not HUNTING)");
            return super.act();
        }

        // Char.act FOV update — needed before policy pathfinding when we spend the turn
        // here.
        if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()) {
            fieldOfView = new boolean[Dungeon.level.length()];
        }
        Dungeon.level.updateFieldOfView(this, fieldOfView);
        // Record last-seen for door-stall / blind defense; re-arm cloak shots
        // when visible. Movement still uses the live hero cell.
        Hero hero = Dungeon.hero;
        boolean heroVisible = hero != null
                && hero.isAlive()
                && hero.invisible <= 0
                && hero.pos >= 0
                && hero.pos < fieldOfView.length
                && fieldOfView[hero.pos];
        if (heroVisible) {
            noteEnemySeenAt(hero.pos);
            rearmBlindDefense();
        }
        noteDoorStallVisibility(heroVisible);

        fightRecorder.trackTurn();

        EchoPolicyStatus status = EchoPolicyStatusBuilder.build(this, echoPolicy);
        noteStance(status.untouchableStance);

        if (tryPolicyAct(status)) {
            return true;
        }
        // The hero may be untouchable; the echo still never spends a turn idle.
        if (forcedUntouchableAct(status)) {
            return true;
        }
        // Melee / unresolved roles fall through to standard mob hunting AI. Ranged
        // has already been tried above, so this is the melee half of "ranged for a
        // ranged echo, melee for a melee echo".
        debugAct("policy did not spend turn → fall through to mob hunting AI");
        return super.act();
    }

    /** Per-turn bookkeeping for the untouchable ladder. */
    private void noteStance(EchoUntouchable.Stance stance) {
        if (stance == EchoUntouchable.Stance.RUN) {
            disengageTurns++;
        } else {
            disengageTurns = 0;
        }
        if (stance == EchoUntouchable.Stance.NONE) {
            preppedThisWindow.clear();
        }
    }

    /**
     * The Java floor: with the hero untouchable and the playbook out of answers,
     * shield up, run, prep, or take a last ranged shot — in that order. Returns
     * false so everything else drops to {@code super.act()} and melees.
     * <p>
     * There is deliberately no branch that spends a turn doing nothing.
     * <p>
     * {@code Mob.FLEEING} is not used: {@link #act()} short-circuits the whole
     * policy engine when {@code state != HUNTING}, and {@code Mob.Fleeing.act}
     * uses bare {@code getFurther}, bypassing the hazard- and cover-aware
     * retreat scoring in {@link #bestRetreatCell}. State stays HUNTING and
     * fleeing is a per-turn step. See also {@code Mob.nowhereToRun()}.
     */
    private boolean forcedUntouchableAct(EchoPolicyStatus status) {
        Hero hero = Dungeon.hero;
        switch (status.untouchableStance) {
            case SHIELD_UP:
                if (status.isRoleReady(EchoPolicyHazards.SHIELD_SELF)) {
                    debugAct("forced → SHIELD_SELF");
                    return runChoice(status, new EchoPolicyChoice(
                            EchoPolicyHazards.SHIELD_SELF, "java_untouchable", null));
                }
                break;
            case RUN:
                if (hero != null && policyStepFurther(hero.pos, true)) {
                    debugAct("forced → step away (disengageTurns=" + disengageTurns + ")");
                    spend(1f / speed());
                    return true;
                }
                break;
            case PREP:
                EchoPolicyChoice prep = EchoUntouchable.firstReadyPrep(status, preppedThisWindow);
                if (prep != null) {
                    debugAct("forced → prep " + prep.useRole);
                    preppedThisWindow.add(prep.useRole);
                    return runChoice(status, prep);
                }
                break;
            default:
                break;
        }
        // Policies with no default_roles at all still get a shot off rather than
        // walking into melee range of a ranged kit.
        if (status.isRoleReady(EchoPolicyHazards.RANGED) && status.enemyInLos) {
            debugAct("forced → last-resort RANGED");
            return runChoice(status, new EchoPolicyChoice(
                    EchoPolicyHazards.RANGED, "java_untouchable", null));
        }
        return false;
    }

    /**
     * Sense → match → resolve → execute. Design record:
     * {@code hero-echoes/docs/features/echo-policy.md} § "One turn, end to end".
     *
     * @return true if the turn was fully spent by policy
     */
    private boolean tryPolicyAct(EchoPolicyStatus status) {
        debugAct("sense hpSelf=" + fmt(status.selfHpRatio)
                + " hpEnemy=" + fmt(status.enemyHpRatio)
                + " dist=" + status.distance
                + " los=" + status.enemyInLos
                + " on=" + status.onTerrain
                + " self=[" + Strings.join(",", status.selfStatuses) + "]"
                + " enemy=[" + Strings.join(",", status.enemyStatuses) + "]"
                + " ready=" + status.rolesReady
                + " stance=" + status.untouchableStance
                + " recipes=" + recipeSteps);

        // Door-break / blind-defense are policy reactions (door_break,
        // blind_defense_ranged).
        EchoPolicyChoice choice = EchoPolicyMatcher.choose(echoPolicy, status, recipeSteps);
        if (choice == null) {
            debugAct("match → no choice");
            return false;
        }
        debugAct("match → layer=" + choice.layer
                + " role=" + choice.useRole
                + (choice.recipeId != null ? " recipe=" + choice.recipeId : ""));

        return runChoice(status, choice);
    }

    /**
     * Execute one resolved choice and account for the turn: recipe step advance,
     * the VFX handshake, and movement-vs-tick spend. Shared by the policy path
     * and the Java untouchable floor so a forced prep cannot double-spend the
     * turn a potion already paid for.
     *
     * @return true if the turn was fully spent
     */
    private boolean runChoice(EchoPolicyStatus status, EchoPolicyChoice choice) {
        int posBefore = pos;
        vfxOwnsTurn = false;
        boolean spent = EchoRoleExecutor.execute(this, echoPolicy, status, choice);
        if (!spent) {
            // Melee / staff fallthrough — let mob AI attack this turn.
            debugAct("execute → not spent (fallthrough), role=" + choice.useRole);
            return false;
        }
        if ("recipes".equals(choice.layer) && choice.recipeId != null) {
            Integer prev = recipeSteps.get(choice.recipeId);
            recipeSteps.put(choice.recipeId, (prev != null ? prev : 0) + 1);
            debugAct("recipe step advanced id=" + choice.recipeId
                    + " nextStep=" + recipeSteps.get(choice.recipeId));
        }
        debugAct("execute → spent turn, role=" + choice.useRole);
        // Throw/zap VFX owns spend via spendAndNext (may already have run sync).
        if (vfxOwnsTurn) {
            return true;
        }
        // Match hunting AI: movement costs 1/speed; other roles cost one tick.
        if (pos != posBefore) {
            spend(1f / speed());
        } else {
            spend(TICK);
        }
        return true;
    }

    private static String fmt(float ratio) {
        return String.format(java.util.Locale.ROOT, "%.2f", ratio);
    }

    private static void debugAct(String message) {
        if (DeviceCompat.isDebug()) {
            DeviceCompat.log("EchoBoss", message);
        }
    }

    /**
     * Debug/sandbox: leave combat AI. {@link Mob#aggro} ignores PASSIVE, so hits
     * will not restart hunting.
     */
    public void stopHunting() {
        enemy = null;
        enemySeen = false;
        state = PASSIVE;
    }

    /** Pacifies every living {@link EchoBoss} on the current level. */
    public static int stopAllHunting() {
        if (Dungeon.level == null) {
            return 0;
        }
        int stopped = 0;
        for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
            if (mob instanceof EchoBoss && mob.isAlive()) {
                ((EchoBoss) mob).stopHunting();
                stopped++;
            }
        }
        return stopped;
    }

    @Override
    public void die(Object cause) {
        if (tryReviveWithAnkh()) {
            return;
        }
        fightRecorder.recordBossDefeat(
                echo,
                Dungeon.depth,
                Dungeon.hero != null ? Dungeon.hero.heroClass : null,
                Game.version);
        super.die(cause);
        EchoBossRegionalDeath.apply(this, cause);
    }

    /**
     * Kit ankhs revive the boss in place. Blessed matches hero (quarter HP, cure,
     * invulnerability); unblessed is quarter HP only — no {@code WndResurrect}.
     */
    private boolean tryReviveWithAnkh() {
        if (echoHero == null || echoHero.belongings == null) {
            return false;
        }
        Ankh ankh = null;
        for (Ankh i : echoHero.belongings.getAllItems(Ankh.class)) {
            if (ankh == null || i.isBlessed()) {
                ankh = i;
            }
        }
        if (ankh == null) {
            return false;
        }

        HP = HT / 4;
        if (ankh.isBlessed()) {
            PotionOfHealing.cure(this);
            Buff.prolong(this, Invulnerability.class, Invulnerability.DURATION);
        }
        showAnkhReviveFx();
        Statistics.ankhsUsed++;
        Catalog.countUse(Ankh.class);
        ankh.detach(echoHero.belongings.backpack);
        return true;
    }

    private void showAnkhReviveFx() {
        if (sprite == null || sprite.parent == null) {
            return;
        }
        SpellSprite.show(this, SpellSprite.ANKH);
        GameScene.flash(0x80FFFF40);
        Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
        GLog.w(Messages.get(Hero.class, "revive"));
    }

    @Override
    public String name() {
        if (echo == null) {
            return Messages.get(this, "name");
        }
        return Echo.resolveUserName(echo.userName, echo.heroClass);
    }

    @Override
    public String description() {
        return Messages.get(this, "desc", echoHero.heroClass.title());
    }
}
