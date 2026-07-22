package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoFightRecorder;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoHeroSnapshot;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoLeaderboardStorage;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicyChoice;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicyMatcher;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicyStatus;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicyStatusBuilder;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoRoleExecutor;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoBossRegionalDeath;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.EchoBossSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.DeviceCompat;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

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
        properties.add(Property.INORGANIC);
    }

    private Echo echo;
    private Hero echoHero;
    private EchoFightRecorder fightRecorder;
    private EchoPolicy echoPolicy;
    /** Recipe id → current step index (advanced when a recipe step executes). */
    private final Map<String, Integer> recipeSteps = new HashMap<>();

    public Echo getEcho() {
        return echo;
    }

    public EchoPolicy getEchoPolicy() {
        return echoPolicy;
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

    /**
     * Echo hero is never placed on the level; sync {@link Hero#pos} for combat
     * queries only.
     */
    private int withEchoHeroPosInt(IntSupplier action) {
        int savedPos = echoHero.pos;
        echoHero.pos = pos;
        try {
            return action.getAsInt();
        } finally {
            echoHero.pos = savedPos;
        }
    }

    private <T> T withEchoHeroPos(Supplier<T> action) {
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
        if (dmg > 0 && src == Dungeon.hero) {
            fightRecorder.trackDamageTaken(dmg);
        }
        super.damage(dmg, src);
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

    @Override
    protected boolean act() {
        // Pick up kit buffs attached after onAdd (e.g. MeleeWeapon.Charger).
        scheduleEchoKitBuffs();

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
                + " self=[" + String.join(",", status.selfStatuses) + "]"
                + " enemy=[" + String.join(",", status.enemyStatuses) + "]"
                + " ready=" + status.rolesReady
                + " recipes=" + recipeSteps);

        EchoPolicyChoice choice = EchoPolicyMatcher.choose(echoPolicy, status, recipeSteps);
        if (choice == null) {
            debugAct("match → no choice");
            return false;
        }
        debugAct("match → layer=" + choice.layer
                + " role=" + choice.useRole
                + (choice.recipeId != null ? " recipe=" + choice.recipeId : ""));

        int posBefore = pos;
        boolean spent = EchoRoleExecutor.execute(this, echoPolicy, status, choice);
        if (!spent) {
            // Melee / staff fallthrough — let mob AI attack this turn.
            debugAct("execute → not spent (fallthrough), role=" + choice.useRole);
            return false;
        }
        if ("recipes".equals(choice.layer) && choice.recipeId != null) {
            recipeSteps.merge(choice.recipeId, 1, Integer::sum);
            debugAct("recipe step advanced id=" + choice.recipeId
                    + " nextStep=" + recipeSteps.get(choice.recipeId));
        }
        debugAct("execute → spent turn, role=" + choice.useRole);
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

    @Override
    public void die(Object cause) {
        fightRecorder.recordBossDefeat(
                echo,
                Dungeon.depth,
                Dungeon.hero != null ? Dungeon.hero.heroClass : null,
                Game.version);
        super.die(cause);
        EchoBossRegionalDeath.apply(this, cause);
    }

    @Override
    public String description() {
        return Messages.get(this, "desc", echoHero.heroClass.title());
    }
}
