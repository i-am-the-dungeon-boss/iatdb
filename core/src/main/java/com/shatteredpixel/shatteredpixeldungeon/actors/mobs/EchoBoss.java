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
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoFightRecorder;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoHeroSnapshot;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoLeaderboardStorage;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoAoeDots;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicyChoice;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicyMatcher;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicyStatus;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicyStatusBuilder;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoRoleExecutor;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoBossRegionalDeath;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
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
import java.util.Map;

public class EchoBoss extends Mob {

    public static final float BOSS_HP_MULTIPLIER = 1.3f;

    private static final String ECHO = "echo";
    private static final String ECHO_POLICY = "echo_policy";

    {
        spriteClass = EchoBossSprite.class;

        HP = HT = 200;
        defenseSkill = 20;

        EXP = 20;
        maxLvl = 30;

        properties.add(Property.BOSS);
    }

    private static final int DOOR_STALL_BREAK_THRESHOLD = 2;

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
    /**
     * Master-style throw/zap gate: set by {@link #busy()}, cleared when
     * {@link #spendAndNext(float)} runs from the VFX callback. While busy,
     * {@link #act()} returns false so Actor processing waits (like Hero.ready).
     */
    private boolean busy;
    /** When true, policy already deferred turn spend to the VFX callback. */
    private boolean vfxOwnsTurn;

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

    public Hero getEchoHero() {
        return echoHero;
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
     * Used for blind-defense aim and door-stall focus — not for CLOSE_IN /
     * KEEP_DISTANCE movement.
     */
    public int lastSeenEnemyPos() {
        return target;
    }

    /** Records where the enemy was last seen. */
    public void noteEnemySeenAt(int cell) {
        target = cell;
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
     * hunting AI.
     */
    public boolean policyStepFurther(int cell) {
        int oldPos = pos;
        if (!getFurther(cell)) {
            return false;
        }
        moveSprite(oldPos, pos);
        return true;
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
            if (passable[i] && i != pos && EchoAoeDots.isAoeDotAt(this, i)) {
                passable[i] = false;
            }
        }
        return passable;
    }

    /** Adjacent steps also refuse to enter AoE DoT. */
    @Override
    protected boolean cellIsPathable(int cell) {
        return super.cellIsPathable(cell) && !EchoAoeDots.isAoeDotAt(this, cell);
    }

    /** Exposes {@link Mob#cellIsPathable} for leave-AoE neighbour checks. */
    public boolean policyCellPathable(int cell) {
        return cellIsPathable(cell);
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
    public int drRoll() {
        return withEchoHeroPosInt(echoHero::drRoll);
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
        return withEchoHeroPosInt(() -> echoHero.defenseProc(enemy, damage));
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
     * Echo hero is never placed on the level; sync {@link Hero#pos} for combat
     * queries only.
     */
    private int withEchoHeroPosInt(IntAction action) {
        int savedPos = echoHero.pos;
        echoHero.pos = pos;
        try {
            return action.getAsInt();
        } finally {
            echoHero.pos = savedPos;
        }
    }

    private <T> T withEchoHeroPos(ValueAction<T> action) {
        int savedPos = echoHero.pos;
        echoHero.pos = pos;
        try {
            return action.get();
        } finally {
            echoHero.pos = savedPos;
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

    /** Marks this boss waiting on throw/zap VFX (UseContext.TurnOwner). */
    public void busy() {
        busy = true;
        vfxOwnsTurn = true;
    }

    public boolean isBusy() {
        return busy;
    }

    /** Clears busy and advances actor time after a deferred throw/zap. */
    public void spendAndNext(float time) {
        busy = false;
        spend(time);
        next();
    }

    @Override
    protected boolean act() {
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
        // Record last-seen for blind-defense aim / door-stall; movement still
        // uses the live hero cell.
        Hero hero = Dungeon.hero;
        boolean heroVisible = hero != null
                && hero.isAlive()
                && hero.invisible <= 0
                && hero.pos >= 0
                && hero.pos < fieldOfView.length
                && fieldOfView[hero.pos];
        if (heroVisible) {
            noteEnemySeenAt(hero.pos);
        }
        noteDoorStallVisibility(heroVisible);

        fightRecorder.trackTurn();

        if (tryPolicyAct()) {
            return true;
        }
        // Melee / unresolved roles fall through to standard mob hunting AI.
        debugAct("policy did not spend turn → fall through to mob hunting AI");
        return super.act();
    }

    /**
     * Sense → match → resolve → execute (canvas §9).
     * 
     * @return true if the turn was fully spent by policy
     */
    private boolean tryPolicyAct() {
        EchoPolicyStatus status = EchoPolicyStatusBuilder.build(this, echoPolicy);
        debugAct("sense hpSelf=" + fmt(status.selfHpRatio)
                + " hpEnemy=" + fmt(status.enemyHpRatio)
                + " dist=" + status.distance
                + " los=" + status.enemyInLos
                + " on=" + status.onTerrain
                + " self=[" + Strings.join(",", status.selfStatuses) + "]"
                + " enemy=[" + Strings.join(",", status.enemyStatuses) + "]"
                + " ready=" + status.rolesReady
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
