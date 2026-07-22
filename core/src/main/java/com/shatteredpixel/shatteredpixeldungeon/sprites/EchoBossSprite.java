package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.Echo;
import com.shatteredpixel.shatteredpixeldungeon.windows.EchoHeroLoader;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;

public class EchoBossSprite extends MobSprite {

    private static final int FRAME_WIDTH = 12;
    private static final int FRAME_HEIGHT = 15;
    private static final int RUN_FRAMERATE = 20;

    private Animation fly;
    private Animation read;

    public EchoBossSprite() {
        super();

        texture(HeroClass.WARRIOR.spritesheet());
        updateArmor(1);
        idle();
    }

    /**
     * Matches {@link HeroSprite#updateArmor()} tier selection for the echo hero.
     */
    public static int armorTierFor(Hero echoHero, Echo echo) {
        if (echoHero != null) {
            int tier = echoHero.tier();
            if (tier > 0) {
                return tier;
            }
        }
        return EchoHeroLoader.armorTier(echoHero, echo);
    }

    public void setup(HeroClass cls, int tier) {
        texture(cls.spritesheet());
        updateArmor(tier);
    }

    public void updateArmor(int tier) {
        TextureFilm film = new TextureFilm(HeroSprite.tiers(), tier, FRAME_WIDTH, FRAME_HEIGHT);

        idle = new Animation(1, true);
        idle.frames(film, 0, 0, 0, 1, 0, 0, 1, 1);

        run = new Animation(RUN_FRAMERATE, true);
        run.frames(film, 2, 3, 4, 5, 6, 7);

        die = new Animation(20, false);
        die.frames(film, 8, 9, 10, 11, 12, 11);

        attack = new Animation(15, false);
        attack.frames(film, 13, 14, 15, 0);

        // Match HeroSprite: ranged / wand shots use zap (clone of attack).
        zap = attack.clone();

        operate = new Animation(8, false);
        operate.frames(film, 16, 17, 16, 17);

        // Match HeroSprite: lunge / leap pose (Rapier, HeroicLeap, Feint, …).
        fly = new Animation(1, true);
        fly.frames(film, 18);

        // Match HeroSprite: scroll read pose.
        read = new Animation(20, false);
        read.frames(film, 19, 20, 20, 20, 20, 20, 20, 20, 20, 19);

        if (ch != null && ch.isAlive()) {
            idle();
        } else if (ch != null) {
            die();
        } else {
            play(idle);
        }
    }

    @Override
    public void link(Char ch) {
        super.link(ch);
        if (!(ch instanceof EchoBoss)) {
            return;
        }

        EchoBoss boss = (EchoBoss) ch;
        Hero echoHero = boss.getEchoHero();
        Echo echo = boss.getEcho();

        HeroClass cls = HeroClass.WARRIOR;
        if (echoHero != null) {
            cls = echoHero.heroClass;
        } else if (echo != null && echo.heroClass != null) {
            try {
                cls = HeroClass.valueOf(echo.heroClass);
            } catch (IllegalArgumentException ignored) {
            }
        }

        setup(cls, armorTierFor(echoHero, echo));
    }

    /**
     * Same scroll-read pose as {@link HeroSprite#read} — without Hero turn
     * ownership (Echo is AI-driven).
     */
    public synchronized void read() {
        animCallback = new Callback() {
            @Override
            public void call() {
                idle();
                if (ch != null) {
                    ch.onOperateComplete();
                }
            }
        };
        play(read);
    }

    /**
     * Same lunge pose as {@link HeroSprite#jump} — without camera follow (Echo is
     * not
     * the player). Missing this made height-0 Rapier jumps look like teleports.
     */
    @Override
    public void jump(int from, int to, float height, float duration, Callback callback) {
        super.jump(from, to, height, duration, callback);
        play(fly);
    }

    @Override
    public void idle() {
        super.idle();
        if (ch != null && ch.flying) {
            play(fly);
        }
    }

    @Override
    public void move(int from, int to) {
        super.move(from, to);
        if (ch != null && ch.flying) {
            play(fly);
        }
    }

    @Override
    public void bloodBurstA(PointF from, int damage) {
        // Match HeroSprite: no blood burst on human-like echoes.
    }
}
