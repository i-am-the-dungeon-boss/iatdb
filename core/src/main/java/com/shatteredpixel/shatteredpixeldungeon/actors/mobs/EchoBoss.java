package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combo;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoFightRecorder;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoHeroCombatDelegate;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoHeroSnapshot;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoLeaderboardStorage;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicy;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicyAction;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicyContext;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.online.EchoPolicyInterpreter;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoBossRegionalDeath;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.EchoBossSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.watabou.noosa.Game;

public class EchoBoss extends Mob {

    public enum IntendedAction {
        ATTACK,
        HEAL,
        MOVE,
        WAIT
    }

    public static final float BOSS_HP_MULTIPLIER = 1.3f;
    public static final int ABILITY_COOLDOWN_TURNS = 50;
    public static final int HEAL_THRESHOLD_PERCENT = 35;
    public static final int MAX_HEALING_POTIONS = 2;

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
    private int abilityCooldown = 0;
    private int healingPotionsUsed = 0;

    public Echo getEcho() {
        return echo;
    }

    public Hero getEchoHero() {
        return echoHero;
    }

    public EchoBoss() {
        super();
        Echo pending = Dungeon.getPendingEcho();
        if (pending != null) {
            initFromEcho(pending, Dungeon.depth);
        }
    }

    public EchoBoss(Echo echo, int depth) {
        super();
        initFromEcho(echo, depth);
    }

    public static int scaledHT(Echo echo, int depth) {
        if (echo == null) return 200;
        float depthBonus = 1f + depth * 0.02f;
        return Math.round(echo.ht * BOSS_HP_MULTIPLIER * depthBonus);
    }

    public void initFromEcho(Echo echo, int depth) {
        if (echo == null || !echo.hasCombatData()) {
            throw new IllegalArgumentException("Echo boss requires echo with hero combat data");
        }
        this.echo = echo;
        echoPolicy = Dungeon.getPendingEchoPolicy();
        fightRecorder = new EchoFightRecorder(new EchoLeaderboardStorage());
        echoHero = EchoHeroSnapshot.restoreHero(echo);
        if (echoHero == null) {
            throw new IllegalArgumentException("Echo boss requires restorable hero combat data");
        }
        HP = HT = scaledHT(echo, depth);
        defenseSkill = echoHero.defenseSkill(null);
        EXP = Math.max(20, echo.lvl * 5);
        maxLvl = Math.max(30, echo.lvl);
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

    public void recordPlayerDefeat() {
        if (fightRecorder != null) {
            fightRecorder.recordBossVictory(
                    echo,
                    Dungeon.depth,
                    Dungeon.hero != null ? Dungeon.hero.heroClass : null,
                    Game.version
            );
        }
    }

    public IntendedAction decideAction(int hpPercent, boolean hasHealingPotion, boolean meleeThreatened, int currentAbilityCooldown) {
        if (echoPolicy != null) {
            EchoPolicyContext context = new EchoPolicyContext()
                    .selfHpRatio(hpPercent / 100f)
                    .heroClass(echo.heroClass)
                    .heroVisible(Dungeon.hero != null)
                    .distance(Dungeon.hero != null && Dungeon.level != null
                            ? Dungeon.level.distance(pos, Dungeon.hero.pos)
                            : 99);
            if (hasHealingPotion) {
                context.hasItem("PotionOfHealing", 1);
            }
            EchoPolicyAction action = EchoPolicyInterpreter.interpret(echoPolicy, context);
            return EchoPolicyInterpreter.toIntendedAction(action);
        }

        if (hpPercent < HEAL_THRESHOLD_PERCENT
                && hasHealingPotion
                && healingPotionsUsed < MAX_HEALING_POTIONS
                && !meleeThreatened) {
            return IntendedAction.HEAL;
        }
        if (meleeThreatened && echoHero.heroClass == HeroClass.MAGE) {
            return IntendedAction.MOVE;
        }
        return IntendedAction.ATTACK;
    }

    public boolean wantsToHeal(int hpPercent, boolean hasHealingPotion, boolean meleeThreatened) {
        return decideAction(hpPercent, hasHealingPotion, meleeThreatened, abilityCooldown) == IntendedAction.HEAL;
    }

    public boolean tryHealFromInventory() {
        if (healingPotionsUsed >= MAX_HEALING_POTIONS) {
            return false;
        }
        PotionOfHealing potion = echoHero.belongings.getItem(PotionOfHealing.class);
        if (potion == null) {
            return false;
        }
        potion.detach(echoHero.belongings.backpack);
        PotionOfHealing.heal(this);
        consumeHealingPotion();
        return true;
    }

    public void consumeHealingPotion() {
        healingPotionsUsed++;
    }

    public int healingPotionsUsed() {
        return healingPotionsUsed;
    }

    public int abilityCooldown() {
        return abilityCooldown;
    }

    private int hpPercent() {
        return HT > 0 ? (HP * 100 / HT) : 100;
    }

    private boolean hasHealingPotion() {
        return echoHero.belongings.getItem(PotionOfHealing.class) != null;
    }

    private boolean isMeleeThreatened() {
        return Dungeon.hero != null
                && Dungeon.level != null
                && Dungeon.level.adjacent(pos, Dungeon.hero.pos);
    }

    @Override
    public int damageRoll() {
        return EchoHeroCombatDelegate.damageRoll(echoHero, pos);
    }

    @Override
    public int attackSkill(Char target) {
        return EchoHeroCombatDelegate.attackSkill(echoHero, pos, target);
    }

    @Override
    public int defenseSkill(Char enemy) {
        return EchoHeroCombatDelegate.defenseSkill(echoHero, pos, enemy);
    }

    @Override
    public int drRoll() {
        return EchoHeroCombatDelegate.drRoll(echoHero, pos);
    }

    @Override
    public float attackDelay() {
        return EchoHeroCombatDelegate.attackDelay(echoHero, pos);
    }

    @Override
    public float speed() {
        return EchoHeroCombatDelegate.speed(echoHero, pos);
    }

    @Override
    public int attackProc(final Char enemy, int damage) {
        return EchoHeroCombatDelegate.attackProc(echoHero, pos, enemy, damage);
    }

    @Override
    public int defenseProc(Char enemy, int damage) {
        return EchoHeroCombatDelegate.defenseProc(echoHero, pos, enemy, damage);
    }

    @Override
    public void damage(int dmg, Object src) {
        if (fightRecorder != null && dmg > 0 && src == Dungeon.hero) {
            fightRecorder.trackDamageTaken(dmg);
        }
        super.damage(dmg, src);
    }

    @Override
    public boolean attack(Char enemy, float dmgMulti, float dmgBonus, float accMulti) {
        if (fightRecorder != null && enemy == Dungeon.hero) {
            int hpBefore = enemy.HP;
            boolean result = super.attack(enemy, dmgMulti, dmgBonus, accMulti);
            fightRecorder.trackDamageDealt(Math.max(0, hpBefore - enemy.HP));
            return result;
        }
        return super.attack(enemy, dmgMulti, dmgBonus, accMulti);
    }

    @Override
    public void notice() {
        super.notice();
        if (!BossHealthBar.isAssigned()) {
            BossHealthBar.assignBoss(this);
            if (Dungeon.level != null) {
                Dungeon.level.seal();
            }
        }
    }

    @Override
    protected boolean act() {
        if (state != HUNTING) {
            return super.act();
        }

        if (fightRecorder != null) {
            fightRecorder.trackTurn();
        }

        if (abilityCooldown > 0) {
            abilityCooldown--;
        }

        if (abilityCooldown <= 0 && Dungeon.hero != null) {
            useArmorAbility();
            abilityCooldown = ABILITY_COOLDOWN_TURNS;
        }

        IntendedAction action = decideAction(
                hpPercent(), hasHealingPotion(), isMeleeThreatened(), abilityCooldown);

        switch (action) {
            case HEAL:
                if (tryHealFromInventory()) {
                    spend(TICK);
                    return true;
                }
                break;
            case MOVE:
                if (Dungeon.hero != null && getFurther(Dungeon.hero.pos)) {
                    spend(TICK);
                    return true;
                }
                break;
            case WAIT:
                spend(TICK);
                return true;
            case ATTACK:
            default:
                break;
        }

        return super.act();
    }

    private void useArmorAbility() {
        if (echoHero.armorAbility != null) {
            if (echoHero.heroClass == HeroClass.WARRIOR) {
                Buff.affect(this, Combo.class).hit(Dungeon.hero);
            }
            if (echoHero.heroClass == HeroClass.ROGUE) {
                Buff.affect(this, Invisibility.class, 3f);
            }
        }
    }

    @Override
    public void die(Object cause) {
        if (fightRecorder != null) {
            fightRecorder.recordBossDefeat(
                    echo,
                    Dungeon.depth,
                    Dungeon.hero != null ? Dungeon.hero.heroClass : null,
                    Game.version
            );
        }
        super.die(cause);
        EchoBossRegionalDeath.apply(this, cause);
    }

    @Override
    public String description() {
        return Messages.get(this, "desc", echoHero.heroClass.title());
    }
}
