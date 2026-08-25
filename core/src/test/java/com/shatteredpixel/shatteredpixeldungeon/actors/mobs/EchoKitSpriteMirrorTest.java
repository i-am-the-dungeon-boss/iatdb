package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.EchoTestSupport;
import com.shatteredpixel.shatteredpixeldungeon.heroechoes.GdxTestExtension;
import com.shatteredpixel.shatteredpixeldungeon.sprites.EchoBossSprite;
import com.watabou.noosa.Group;
import java.util.Arrays;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The phantom kit borrowed the body's sprite only for the duration of a call,
 * so any path that forgot to borrow dereferenced null (ANDROID-20 / ANDROID-21).
 * The kit belongs to exactly one boss, so the sprite is mirrored for its whole
 * life instead, and torn down with the body's own.
 */
@ExtendWith(GdxTestExtension.class)
class EchoKitSpriteMirrorTest {

	private static EchoBoss bossOnLevel() {
		Hero player = EchoTestSupport.warriorHero();
		EchoBoss boss = EchoTestSupport.createBossWithPolicy(
				player, EchoTestSupport.healCapabilityPolicy(), 5);
		EchoTestSupport.installEchoBossLevel(player, boss, 2);
		boss.fieldOfView = new boolean[Dungeon.level.length()];
		Arrays.fill(boss.fieldOfView, true);
		return boss;
	}

	/** An on-stage sprite: non-null and parented, so {@code canWorldFx} holds. */
	private static EchoBossSprite stageSprite(EchoBoss boss) {
		EchoBossSprite sprite = new QuietEchoBossSprite();
		sprite.ch = boss;
		boss.sprite = sprite;
		new Group().add(sprite);
		boss.mirrorKitSprite();
		return sprite;
	}

	/** EmoIcons reach {@code GameScene.scene}, which unit tests do not have. */
	private static class QuietEchoBossSprite extends EchoBossSprite {
		@Override
		public void showAlert() {
		}

		@Override
		public void showSleep() {
		}

		@Override
		public void showLost() {
		}
	}

	@Test
	@DisplayName("the kit mirrors the body's sprite once the boss takes a turn")
	void kitMirrorsBodySpriteAfterTurn() {
		EchoBoss boss = bossOnLevel();
		EchoBossSprite sprite = stageSprite(boss);
		Hero kit = boss.getEchoHero();
		kit.sprite = null;

		boss.act();

		Assertions.assertThat(kit.sprite).isSameAs(sprite);
		Assertions.assertThat(Char.canWorldFx(kit)).isTrue();
	}

	@Test
	@DisplayName("destroying the body's sprite clears the kit's mirror too")
	void destroyingBodySpriteClearsKitMirror() {
		EchoBoss boss = bossOnLevel();
		EchoBossSprite sprite = stageSprite(boss);
		Hero kit = boss.getEchoHero();
		Assertions.assertThat(kit.sprite).isSameAs(sprite);

		sprite.destroy();

		Assertions.assertThat(boss.sprite).isNull();
		Assertions.assertThat(kit.sprite)
				.as("a mirrored kit must not outlive the sprite it points at")
				.isNull();
	}

	@Test
	@DisplayName("the mirror survives invisibility — the sprite is hidden, not removed")
	void mirrorSurvivesInvisibility() {
		EchoBoss boss = bossOnLevel();
		EchoBossSprite sprite = stageSprite(boss);
		Hero kit = boss.getEchoHero();

		Buff.affect(boss, Invisibility.class, 20f);
		sprite.update();

		Assertions.assertThat(boss.invisible).isGreaterThan(0);
		Assertions.assertThat(sprite.visible).isFalse();
		Assertions.assertThat(kit.sprite).isSameAs(sprite);
		Assertions.assertThat(Char.canWorldFx(kit)).isTrue();
	}

	@Test
	@DisplayName("a kit buff attaching over the mirrored sprite does not throw")
	void kitBuffOverMirroredSpriteDoesNotThrow() {
		EchoBoss boss = bossOnLevel();
		stageSprite(boss);
		Hero kit = boss.getEchoHero();

		Assertions.assertThatCode(() -> Buff.affect(kit, Invisibility.class, 5f))
				.doesNotThrowAnyException();
	}
}
